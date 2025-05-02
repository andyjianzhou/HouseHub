package com.housing.backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service implements IS3Service {

    @Value("${aws.region}")
    private String region;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    private AmazonS3 s3Client;
    
    @PostConstruct
    public void init() {
        // This is where the AmazonS3 client is created
        // An amazon client is what allows us to interact with the S3 service. It also allows us to interact with other AWS services such as DynamoDB, etc.
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
    }
    
    public String uploadFile(MultipartFile file, String key) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        
        s3Client.putObject(new PutObjectRequest(
            bucketName, 
            key, 
            file.getInputStream(), 
            metadata
        ).withCannedAcl(CannedAccessControlList.PublicRead));
        
        return s3Client.getUrl(bucketName, key).toString();
    }
    
    // Optional: Helper method to generate a unique file key
    public String generateUniqueFileKey(Long userId, String originalFilename) {
        return "user-" + userId + "/" + UUID.randomUUID() + "-" + originalFilename;
    }
}