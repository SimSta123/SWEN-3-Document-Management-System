package at.technikum.swen3.paperless.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class DocumentStorage implements S3Storage {

    private final S3Client s3;
    private final String bucket;

    public DocumentStorage(S3Client s3, @Value("${storage.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String upload(MultipartFile file) throws IOException {
        String objectKey = "documents/" + UUID.randomUUID() + ".pdf";
        Path tempFile = Files.createTempFile("document-", ".pdf");

        //contentType damit information das pdf ist gespeichert wird
        try {
            file.transferTo(tempFile);  //schreibt die Datei in eine temporare
            //contentType damit information das pdf ist gespeichert wird, fürn download
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType("application/pdf").build(), tempFile);
            return objectKey;
        }  finally {
        Files.deleteIfExists(tempFile);
        }
    }

    public InputStream download(String objectKey) throws IOException {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    public void delete(String objectKey) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }



}
