package net.friendly_bets.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public void uploadFileToS3Bucket(String key, MultipartFile file, String fileName) throws IOException {
        File localFile = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
        file.transferTo(localFile);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, localFile.toPath());

        Files.deleteIfExists(localFile.toPath()); // Удаление временного локального файла после загрузки
    }

    public byte[] downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), key);

        s3Client.getObject(getObjectRequest, tempFile);

        try {
            return Files.readAllBytes(tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    public List<S3Object> listFiles() {
        return s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
                .contents();
    }
}

