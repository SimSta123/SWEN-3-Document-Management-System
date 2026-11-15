package at.technikum.swen3.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Component
public class PdfStorage {
    private final S3Client s3;
    private final String bucket;

    public PdfStorage(S3Client s3, @Value("${storage.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public void download(String key, Path target) throws IOException {
        try (var input = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            Files.copy(input, target);
        }
    }
}
