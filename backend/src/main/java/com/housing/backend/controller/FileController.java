package com.housing.backend.controller;

import com.housing.backend.service.S3Service;
import com.housing.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final S3Service s3Service;
    private final UserService userService;

    public FileController(S3Service s3Service, UserService userService) {
        this.s3Service = s3Service;
        this.userService = userService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        
        try {
            String originalFilename = file.getOriginalFilename();
            String key = s3Service.generateUniqueFileKey(userId, originalFilename);
            
            String fileUrl = s3Service.uploadFile(file, key);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("key", key);
            response.put("fileName", originalFilename);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}