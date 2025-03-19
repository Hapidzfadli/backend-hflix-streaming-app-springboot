package com.hapidzfadli.hflix.app.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

public interface MinioService {
    void uploadFile(String bucketName, String objectName, File file) throws Exception;
    void uploadFile(String bucketName, String objectName, MultipartFile file) throws Exception;
    InputStream getObject(String bucketName, String objectName) throws Exception;
    InputStream getObjectRange(String bucketName, String objectName, long start, long end) throws Exception;
    void deleteObject(String bucketName, String objectName) throws Exception;
    String getPresignedUrl(String bucketName, String objectName, int expirySeconds) throws Exception;
}
