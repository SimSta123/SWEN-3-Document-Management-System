package at.technikum.swen3.paperless.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3Storage {
    String upload(MultipartFile file) throws IOException;
    void delete(String objectKey);
}
