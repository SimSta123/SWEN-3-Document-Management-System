package at.technikum.swen3.worker;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/** The database record is authoritative; absence means stale processing must be discarded. */
@Component
public class DocumentLifecycle {
    private final DataSource database;

    public DocumentLifecycle(DataSource database) {
        this.database = database;
    }

    public boolean exists(long documentId) throws IOException {
        try (var connection = database.getConnection();
                var query = connection.prepareStatement("select id from documents where id = ?")) {
            query.setLong(1, documentId);
            query.setQueryTimeout(30);
            try (var rows = query.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException exception) {
            throw new IOException("Could not check document lifecycle", exception);
        }
    }

    @FunctionalInterface
    public interface IndexOperation {
        void run() throws IOException;
    }

    public boolean whilePresent(long documentId, IndexOperation operation) throws IOException {
        try (var connection = database.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                // Held until AFTER the external write. Backend deletion locks this same row
                // before removing the PDF/index entry, so a late result cannot race past it.
                boolean present;
                try (var query = connection.prepareStatement("select id from documents where id = ? for update")) {
                    query.setLong(1, documentId);
                    query.setQueryTimeout(30);
                    try (var rows = query.executeQuery()) {
                        present = rows.next();
                    }
                }
                if (present) operation.run();
                connection.commit();
                return present;
            } catch (IOException | SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IOException("Could not coordinate document indexing", exception);
        }
    }
}
