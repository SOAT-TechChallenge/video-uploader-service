package com.videoUploaderService.service;

import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;

@Service
public class VideoStorageService {

    private final AmazonS3 amazonS3;
    private final String bucketName;

    public VideoStorageService(AmazonS3 amazonS3,
                               @Value("${aws.s3.bucket}") String bucketName) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
    }

    public String uploadVideo(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String key = "videos/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + extension;

        amazonS3.putObject(bucketName, key, file.getInputStream(), null);

        return key;
    }

    public String getVideoUrl(String key) {
        URL url = amazonS3.getUrl(bucketName, key);
        return url.toString();
    }
}


