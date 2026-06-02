package com.sky.utils;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.PutObjectArgs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class MinIOUtil {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("image/jpeg")
                    .build());

            String url = endpoint + "/" + bucketName + "/" + objectName;
            log.info("文件上传到:{}", url);
            return url;
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage());
            throw new RuntimeException("文件上传失败");
        }
    }

    /**
     * 文件删除
     *
     * @param objectName
     */
    public void delete(String objectName) {
        try {
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());

            log.info("文件删除成功：{}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败：{}", e.getMessage());
            throw new RuntimeException("文件删除失败");
        }
    }
}
