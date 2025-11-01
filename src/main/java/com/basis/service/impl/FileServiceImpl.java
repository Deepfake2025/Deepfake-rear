package com.basis.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;

import com.basis.common.ResponseCode;
import com.basis.common.Result;
import com.basis.exception.BusinessException;
import com.basis.model.vo.CallbackBodyVo;
import com.basis.model.vo.FileMetaVo;
import com.basis.model.vo.StsCredentialsVo;
import com.basis.model.dto.FileUploadCache;
import com.basis.model.enums.MimeTypeEnum;
import com.basis.service.ICloudStorageService;
import com.basis.service.IFileService;
import com.basis.strategy.validateStrategy.file.FileValidationStrategy;
import com.basis.strategy.validateStrategy.file.FileValidationStrategyFactory;
import com.basis.utils.RedisUtils;
import com.basis.utils.ThrowUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Resource;

import static com.basis.common.ResponseCode.NOT_FOUND;

/**
 * <p>
 * 文件服务实现类
 * </p>
 *
 * @author IT 派同学
 * @since 2024-12-07
 */
@Slf4j
@Service
public class FileServiceImpl implements IFileService {

    @Autowired
    private ICloudStorageService cloudStorageService;

    @Autowired
    private FileValidationStrategyFactory fileValidationStrategyFactory;

    @Resource
    private RedisUtils redisUtils;

    @Value("${proxy.schema}")
    private String schema;
    
    @Value("${proxy.host}")
    private String host;
    
    @Value("${proxy.port}")
    private String port;

    @Value("${aliyun.oss.file.file-path}")
    private String filePath;

    @Value("${aliyun.oss.file.bucket-name}")
    private String bucketName;

        // 文件上传相关配置
    @Value("${aliyun.oss.file.max-file-size}")
    private Long fileMaxFileSize;

    @Value("${aliyun.oss.file.allowed-file-types}")
    private String[] fileAllowedFileTypes;


    /**
     * 文件上传初始化
     *
     * @param vo 文件元数据
     * @return STS临时凭证
     */
    @Override
    public Result<?> uploadInit(FileMetaVo vo) {
        // 获取当前登录用户
        String username = StpUtil.getSession().get("username").toString();
        log.info("File upload init requested by user: {}, file type: {}, file size: {}",
                username, vo.getMimeType(), vo.getFileSize());

        // 使用文件验证策略验证文件元数据
        FileValidationStrategy validationStrategy = fileValidationStrategyFactory.getStrategy();
        validationStrategy.validateFileMetadata(vo);

        // 申请STS凭证
        // 构建精细化的权限策略
        String policy = buildFileUploadPolicy(username, vo);

        log.info("File upload request - User: {}, File Type: {}, File Size: {}",
                username, vo.getMimeType(), vo.getFileSize());

        // 临时访问凭证的有效时间，单位为秒。最小值为900，最大值以当前角色设定的最大会话时间为准
        Long durationSeconds = 3600L;
        String roleSessionName = username + "-file-" + System.currentTimeMillis();

        // 获取STS临时凭证
        StsCredentialsVo result = cloudStorageService.getStsCredentialsVo(policy, roleSessionName, durationSeconds);

        // 生成对象路径（针对文件）
        String fileExtension = getFileExtensionFromMimeType(vo.getMimeType());
        String filePrefix = vo.getFileType() != null ? vo.getFileType() : "file";
        String objectPath = String.format("/%s%s-%s%s.%s",
                filePath, filePrefix, username, UUID.randomUUID().toString().substring(0, 8), fileExtension);
        result.setObjectPath(objectPath);

        result.setMaxFileSize(fileMaxFileSize);
        result.setAllowedFileTypes(fileAllowedFileTypes);

        // 创建文件上传缓存对象
        FileUploadCache uploadCache = FileUploadCache.builder()
                .username(username)
                .objectPath(objectPath)
                .expectedBucket(bucketName)
                .uploadTime(System.currentTimeMillis())
                .fileType(vo.getFileType())
                .build();

        // 使用STS accessKeyId作为缓存key，有效期2小时
        redisUtils.setValueTimeout(result.getAccessKeyId(), uploadCache, 2 * durationSeconds);

        log.info("File upload info cached with accessKeyId: {}, username: {}, objectPath: {}",
                result.getAccessKeyId(), username, objectPath);

        return Result.success(result);
    }

    /**
     * 处理文件上传完成回调
     *
     * @param vo 回调信息
     * @return 操作结果
     */
    @Override
    public Result<?> uploadCallback(CallbackBodyVo vo) {
        log.info("File upload callback received: {}", vo);

        // 验证回调信息与缓存一致性
        ThrowUtil.throwIf(Objects.isNull(vo.getAccessKeyId()), NOT_FOUND);
        log.info("Received upload callback for accessKeyId: {}", vo.getAccessKeyId());

        // 从缓存中获取上传信息
        Object cachedData = redisUtils.getValue(vo.getAccessKeyId());
        ThrowUtil.throwIf(Objects.isNull(cachedData), NOT_FOUND, "Upload cache not found");

        // 处理文件上传缓存
        if (cachedData instanceof FileUploadCache) {
            FileUploadCache data = (FileUploadCache) cachedData;
            // 验证缓存与回调信息一致性
            ThrowUtil.throwIf(!data.getExpectedBucket().equals(vo.getBucketName()), NOT_FOUND, "Bucket name mismatch");
        }
        else {
            ThrowUtil.throwIf(true, NOT_FOUND, "Invalid cache type");
            return Result.fail(NOT_FOUND);
        }

        // TODO: 将文件信息注册到mysql的 detection_file中

        // 返回成功状态
        return Result.success();
    }


    /**
     * 构建文件上传权限策略
     */
    private String buildFileUploadPolicy(String username, FileMetaVo vo) {
        // 根据用户和文件元数据构建精细化的权限策略
        return "{\n" +
            "  \"Version\": \"1\",\n" +
            "  \"Statement\": [\n" +
            "    {\n" +
            "      \"Effect\": \"Allow\",\n" +
            "      \"Action\": [\n" +
            "        \"oss:PutObject\"\n" +
            "      ],\n" +
            "      \"Resource\": [\n" +
            "        \"acs:oss:*:*:" + bucketName + "/" + filePath + "*\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    /**
     * 根据MIME类型获取文件扩展名
     *
     * @param mimeType MIME类型
     * @return 文件扩展名
     * @throws BusinessException 当MIME类型不支持时抛出
     */
    private String getFileExtensionFromMimeType(String mimeType) {
        try {
            return MimeTypeEnum.getExtensionByMimeType(mimeType);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported MIME type: {}", mimeType);
            throw new BusinessException(ResponseCode.PARAM_ERROR, "不支持的文件类型: " + mimeType);
        }
    }
}