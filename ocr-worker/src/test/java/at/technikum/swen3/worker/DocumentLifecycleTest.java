package at.technikum.swen3.worker;

import java.io.IOException;
import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.*;

class DocumentLifecycleTest {
    private DriverManagerDataSource database;
    private DocumentLifecycle documents;

    @BeforeEach
    void createRecords() throws Exception {
        // Override only for a disposable PostgreSQL test database; never the application DB.
        database = new DriverManagerDataSource(
                System.getProperty("lifecycle.test.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000"),
                System.getProperty("lifecycle.test.user", "sa"), System.getProperty("lifecycle.test.password", ""));
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.execute("drop table if exists documents");
            statement.execute("create table documents (id bigint primary key)");
            statement.execute("insert into documents values (42)");
        }
        documents = new DocumentLifecycle(database);
    }

    @Test
    void deletedRecordSkipsTheIndexOperation() throws Exception {
        deleteRecord();
        assertThat(documents.exists(42)).isFalse();
        assertThat(documents.whilePresent(42, () -> fail("Must not index a deleted document"))).isFalse();
    }

    @Test
    void indexingAlreadyInProgressFinishesBeforeDeletionRemovesItsEntry() throws Exception {
        var indexed = new AtomicReference<String>();
        var indexingStarted = new CountDownLatch(1);
        var finishIndexing = new CountDownLatch(1);
        var deleteAttempted = new CountDownLatch(1);
        var deletionAcquiredLock = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var index = executor.submit(() -> documents.whilePresent(42, () -> {
                indexingStarted.countDown();
                await(finishIndexing);
                indexed.set("Late OCR text");
            }));
            await(indexingStarted);
            var delete = executor.submit(() -> {
                deleteAttempted.countDown();
                try (var connection = lockedDocument()) {
                    deletionAcquiredLock.countDown();
                    indexed.set(null); // Backend removes the external entry while holding the row lock.
                    connection.createStatement().executeUpdate("delete from documents where id = 42");
                    connection.commit();
                }
                return true;
            });
            try {
                await(deleteAttempted);
                assertThat(deletionAcquiredLock.await(150, TimeUnit.MILLISECONDS)).isFalse();
            } finally {
                finishIndexing.countDown();
            }
            assertThat(index.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(delete.get(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(indexed.get()).isNull();
        assertThat(documents.exists(42)).isFalse();
    }

    @Test
    void deletionInProgressBlocksIndexingAndACommittedDeleteSkipsTheWrite() throws Exception {
        try (var connection = lockedDocument(); var executor = Executors.newSingleThreadExecutor()) {
            var attempted = new CountDownLatch(1);
            var wrote = new CountDownLatch(1);
            var index = executor.submit(() -> {
                attempted.countDown();
                return documents.whilePresent(42, wrote::countDown);
            });
            try {
                await(attempted);
                assertThat(wrote.await(150, TimeUnit.MILLISECONDS)).isFalse();
                connection.createStatement().executeUpdate("delete from documents where id = 42");
                connection.commit();
            } finally {
                connection.rollback();
            }
            assertThat(index.get(5, TimeUnit.SECONDS)).isFalse();
            assertThat(wrote.getCount()).isEqualTo(1);
        }
    }

    @Test
    void rolledBackDeletionLetsWaitingIndexingContinue() throws Exception {
        try (var connection = lockedDocument(); var executor = Executors.newSingleThreadExecutor()) {
            connection.createStatement().executeUpdate("delete from documents where id = 42");
            var attempted = new CountDownLatch(1);
            var wrote = new CountDownLatch(1);
            var index = executor.submit(() -> {
                attempted.countDown();
                return documents.whilePresent(42, wrote::countDown);
            });
            try {
                await(attempted);
                assertThat(wrote.await(150, TimeUnit.MILLISECONDS)).isFalse();
            } finally {
                connection.rollback();
            }
            assertThat(index.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(wrote.getCount()).isZero();
        }
        assertThat(documents.exists(42)).isTrue();
    }

    @Test
    void failedIndexingReleasesItsLockAndLeavesTheRecordAvailable() throws Exception {
        assertThatThrownBy(() -> documents.whilePresent(42, () -> { throw new IOException("ES down"); }))
                .isInstanceOf(IOException.class).hasMessage("ES down");
        assertThat(documents.exists(42)).isTrue();
        deleteRecord();
        assertThat(documents.exists(42)).isFalse();
    }

    private Connection lockedDocument() throws Exception {
        var connection = database.getConnection();
        connection.setAutoCommit(false);
        var statement = connection.prepareStatement("select id from documents where id = 42 for update");
        statement.setQueryTimeout(5);
        try (statement; var rows = statement.executeQuery()) {
            assertThat(rows.next()).isTrue();
        }
        return connection;
    }

    private void deleteRecord() throws Exception {
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("delete from documents where id = 42");
        }
    }

    private static void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IOException("Test operation timed out");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(exception);
        }
    }
}
