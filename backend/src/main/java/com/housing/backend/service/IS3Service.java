package com.housing.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface IS3Service {
    String uploadFile(MultipartFile file, String key) throws IOException;
    String generateUniqueFileKey(Long userId, String originalFilename);
}
