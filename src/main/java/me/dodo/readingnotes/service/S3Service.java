package me.dodo.readingnotes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class S3Service {
    private final S3Client s3Client;

    @Value("${supabase.storage.url}")
    private String baseUrl;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }


    // 사진 업로드
    public String uploadProfileImage(byte[] file, String fileName, String contentType) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file));

        return getPublicUrl(fileName);
    }
    private String getPublicUrl(String fileName) {
        return baseUrl + "/" + bucket + "/" + fileName;
    }

    // 사진 삭제
    public void deleteFile(String fileName){
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }
}
