package com.notaskflow.storage;

import com.notaskflow.config.MinioProperties;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.exception.FileStorageException;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 文件存储服务。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private static final Integer DOWNLOAD_EXPIRY_SECONDS = 3600;

    private static final Integer UPLOAD_EXPIRY_SECONDS = 900;

    private final MinioClient minioClient;

    private final MinioProperties properties;

    /**
     * 上传用户头像。
     *
     * @param userId 用户标识
     * @param file 文件对象
     * @return 存储路径
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        return upload("avatars/" + userId, file);
    }

    /**
     * 上传业务附件。
     *
     * @param spaceId 空间标识
     * @param file 文件对象
     * @return 存储路径
     */
    public String uploadAttachment(Long spaceId, MultipartFile file) {
        return upload("spaces/" + spaceId + "/attachments", file);
    }

    /**
     * 生成文件管理直传对象路径。
     *
     * @param spaceId 空间标识
     * @param originalFilename 原始文件名
     * @return 对象路径
     */
    public String buildManagedFileObjectName(Long spaceId, String originalFilename) {
        String fileName = safeFileName(originalFilename);
        return "spaces/" + spaceId + "/managed/" + UUID.randomUUID() + "_" + fileName;
    }

    /**
     * 上传文件管理对象。
     *
     * @param objectName 对象路径
     * @param inputStream 文件输入流
     * @param fileSize 文件大小
     * @param contentType 文件类型
     */
    public void uploadManagedObject(String objectName, InputStream inputStream, Long fileSize, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .stream(inputStream, fileSize, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("上传文件失败", exception);
        }
    }

    /**
     * 校验直传文件元信息。
     *
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param contentType 文件类型
     */
    public void validateUploadMetadata(String fileName, Long fileSize, String contentType) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小必须大于0");
        }
        if (fileSize > properties.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (!isAllowedContentType(contentType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件类型不允许");
        }
    }

    /**
     * 生成预签名上传地址。
     *
     * @param storagePath 存储路径
     * @return 预签名上传地址
     */
    public String presignedUploadUrl(String storagePath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(properties.getBucketName())
                    .object(storagePath)
                    .expiry(UPLOAD_EXPIRY_SECONDS)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("生成上传地址失败", exception);
        }
    }

    /**
     * 生成预签名下载地址。
     *
     * @param storagePath 存储路径
     * @return 预签名地址
     */
    public String presignedDownloadUrl(String storagePath) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.getBucketName())
                    .object(storagePath)
                    .expiry(DOWNLOAD_EXPIRY_SECONDS)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("生成下载地址失败", exception);
        }
    }

    /**
     * 判断对象是否存在。
     *
     * @param storagePath 存储路径
     * @return true 表示对象存在
     */
    public boolean exists(String storagePath) {
        String objectName = normalizeObjectName(storagePath);
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 查询默认存储桶名称。
     *
     * @return 存储桶名称
     */
    public String bucketName() {
        return properties.getBucketName();
    }

    /**
     * 查询最大文件大小。
     *
     * @return 最大文件大小
     */
    public Long maxFileSize() {
        return properties.getMaxFileSize();
    }

    /**
     * 查询允许上传的文件类型。
     *
     * @return 允许上传的文件类型
     */
    public List<String> allowedMimeTypes() {
        return properties.getAllowedMimeTypes();
    }

    /**
     * 读取对象内容。
     *
     * @param storagePath 存储路径
     * @param maxBytes 最大读取字节数
     * @return 对象字节内容
     */
    public byte[] readObject(String storagePath, int maxBytes) {
        String objectName = normalizeObjectName(storagePath);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.getBucketName())
                .object(objectName)
                .build())) {
            return inputStream.readNBytes(maxBytes);
        } catch (Exception exception) {
            throw new FileStorageException("读取文件失败", exception);
        }
    }

    /**
     * 打开对象读取流，调用方负责关闭。
     *
     * @param storagePath 存储路径
     * @return 对象读取流
     */
    public InputStream openObject(String storagePath) {
        String objectName = normalizeObjectName(storagePath);
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("读取文件失败", exception);
        }
    }

    /**
     * 查询对象元数据。
     *
     * @param storagePath 存储路径
     * @return 对象元数据
     */
    public StatObjectResponse statObject(String storagePath) {
        String objectName = normalizeObjectName(storagePath);
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("查询文件元数据失败", exception);
        }
    }

    /**
     * 删除对象存储文件。
     *
     * @param storagePath 存储路径
     */
    public void delete(String storagePath) {
        String objectName = normalizeObjectName(storagePath);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw new FileStorageException("删除文件失败", exception);
        }
    }

    private String normalizeObjectName(String storagePath) {
        if (!StringUtils.hasText(storagePath)
                || (!storagePath.startsWith("http://") && !storagePath.startsWith("https://"))) {
            return storagePath;
        }
        URI uri = URI.create(storagePath);
        String path = URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8);
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String bucketPrefix = properties.getBucketName() + "/";
        if (normalizedPath.startsWith(bucketPrefix)) {
            return normalizedPath.substring(bucketPrefix.length());
        }
        return normalizedPath;
    }

    /**
     * 上传文件到指定目录。
     *
     * @param directory 目录
     * @param file 文件对象
     * @return 存储路径
     */
    private String upload(String directory, MultipartFile file) {
        validateFile(file);
        String originalFilename = safeFileName(file.getOriginalFilename());
        String objectName = directory + "/" + UUID.randomUUID() + "_" + originalFilename;
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucketName())
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectName;
        } catch (Exception exception) {
            throw new FileStorageException("上传文件失败", exception);
        }
    }

    private String safeFileName(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "unnamed";
        }
        return StringUtils.cleanPath(originalFilename);
    }

    /**
     * 校验文件大小和类型。
     *
     * @param file 文件对象
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (!isAllowedContentType(file.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件类型不允许");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        List<String> allowedMimeTypes = properties.getAllowedMimeTypes();
        if (allowedMimeTypes.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String normalizedContentType = contentType.toLowerCase();
        return allowedMimeTypes.stream().anyMatch(allowedMimeType -> {
            String normalizedAllowedMimeType = allowedMimeType.toLowerCase();
            if (normalizedAllowedMimeType.endsWith("/*")) {
                String prefix = normalizedAllowedMimeType.substring(0, normalizedAllowedMimeType.length() - 1);
                return normalizedContentType.startsWith(prefix);
            }
            return normalizedAllowedMimeType.equals(normalizedContentType);
        });
    }
}
