package com.basis.service.impl;

import com.basis.model.vo.AvatarMetaVo;
import com.basis.model.vo.AvatarUrlVo;
import com.basis.model.vo.CallbackBodyVo;
import com.basis.model.vo.StsCredentialsVo;
import com.basis.model.dto.AvatarUploadCache;

import static com.basis.common.ResponseCode.AUTH_FORBID;
import static com.basis.common.ResponseCode.NOT_FOUND;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.basis.common.Result;
import com.basis.exception.BusinessException;
import com.basis.service.ICloudStorageService;
import com.basis.common.ResponseCode;
import com.basis.utils.RedisUtils;
import com.basis.utils.ThrowUtil;
import com.basis.service.IUserService;
import com.basis.model.entity.User;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;


@Slf4j
@Service
public class CloudStorageService implements ICloudStorageService {

    @Resource
    private RedisUtils redisUtils;

    @Value("${aliyun.sts-endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.avatar.bucket-name}")
    private String bucketName;
    @Value("${aliyun.access-key-id}")
    private String aliyunAccessKeyId;
    @Value("${aliyun.access-key-secret}")
    private String aliyunAccessKeySecret;
    @Value("${aliyun.role-arn}")
    private String roleArn;
    @Value("${aliyun.oss.avatar.avatar-path}")
    private String avatarPath;
    @Value("${aliyun.oss.avatar.max-file-size}")
    private Long maxFileSize;
    @Value("${aliyun.oss.avatar.allowed-file-types}")
    private String[] allowedFileTypes;

    @Override
    public Result<?> getAvatarUploadCredentials(String username, AvatarMetaVo vo) {
        // 构建精细化的权限策略
        String policy = buildUploadPolicy(username, vo);
        // String policy = null;

        log.info(aliyunAccessKeyId);
        log.info(aliyunAccessKeySecret);
        log.info(endpoint);
        log.info(roleArn);

        // 获取当前登录用户信息
        String currentUsername = StpUtil.getSession().get("username").toString();
        if (!currentUsername.equals(username)) {
            return Result.fail(ResponseCode.AUTH_FORBID);
        }

        // 获取STS临时凭证
        
        // 发起STS请求所在的地域。建议保留默认值，默认值为空字符串（""）。
        String regionId = "";
        // 临时访问凭证的有效时间，单位为秒。最小值为900，最大值以当前角色设定的最大会话时间为准
        Long durationSeconds = 3600L;

        // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
        try {
            // DefaultProfile.addEndpoint(endpoint, regionId, "Sts", endpoint);
            DefaultProfile.addEndpoint(endpoint, regionId, "Sts", endpoint);
        }  catch (ClientException e) {
            log.error("Failed to obtain STS token: {}", e.getMessage());
            throw new BusinessException(ResponseCode.OBTAIN_STS_TOKEN_FAILED);
        }
        // 构造default profile。
        IClientProfile profile = DefaultProfile.getProfile(regionId, aliyunAccessKeyId, aliyunAccessKeySecret);
        // 构造client。
        DefaultAcsClient client = new DefaultAcsClient(profile);
        final AssumeRoleRequest request = new AssumeRoleRequest();

        // 适用于Java SDK 3.12.0以下版本。
        request.setMethod(MethodType.POST);
        request.setRoleArn(roleArn);
        request.setRoleSessionName(username + "-" + System.currentTimeMillis());
        request.setPolicy(policy);
        request.setDurationSeconds(durationSeconds);
        // 将当前的 try-catch 替换为：
        final AssumeRoleResponse response;
        try {
            response = client.getAcsResponse(request);
        } catch (ClientException e) {
            log.error("Failed to obtain STS token: {}", e.getMessage());
            throw new BusinessException(ResponseCode.OBTAIN_STS_TOKEN_FAILED);
        }
        
        // 获取STS凭证信息
        String accessKeyId = response.getCredentials().getAccessKeyId();
        String accessKeySecret = response.getCredentials().getAccessKeySecret();
        String securityToken = response.getCredentials().getSecurityToken();

        // 构建返回对象
        StsCredentialsVo credentials = new StsCredentialsVo();
        credentials.setAccessKeyId(accessKeyId);
        credentials.setAccessKeySecret(accessKeySecret);
        credentials.setSecurityToken(securityToken);
        
        // 设置过期时间（需要转换Date到LocalDateTime）
        credentials.setExpiration(LocalDateTime.now().plusSeconds(durationSeconds));

        credentials.setBucketName(bucketName);
        credentials.setRegion("cn-guangzhou");
        credentials.setEndpoint(endpoint);

        // 生成对象路径
        String fileExtension = getFileExtensionFromMimeType(vo.getMimeType());
        String objectPath = String.format("/%savatar-%s%s.%s",
                avatarPath, username, UUID.randomUUID().toString().substring(0, 8), fileExtension);
        credentials.setObjectPath(objectPath);

        credentials.setMaxFileSize(maxFileSize);
        credentials.setAllowedFileTypes(allowedFileTypes);

        // 创建上传缓存对象
        AvatarUploadCache uploadCache = AvatarUploadCache.builder()
                .username(username)
                .objectPath(objectPath)
                .expectedBucket(bucketName)
                .uploadTime(System.currentTimeMillis())
                .build();

        // 使用STS accessKeyId作为缓存key，有效期2小时（比STS凭证多1小时缓冲）
        redisUtils.setValueTimeout(accessKeyId, uploadCache, 2 * durationSeconds);

        log.info("Avatar upload info cached with accessKeyId: {}, username: {}, objectPath: {}",
            accessKeyId, username, objectPath);

        return Result.success(credentials);

       
    }



    /**
     * 构建上传权限策略
     */
    private String buildUploadPolicy(String username, AvatarMetaVo vo) {
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
            "        \"acs:oss:*:*:" + bucketName + "/" + avatarPath + "*\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    /**
     * 从MIME类型获取文件扩展名
     */
    private String getFileExtensionFromMimeType(String mimeType) {
        switch (mimeType) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            default:
                return "jpg";
        }
    }



    @Override
    public Result<?> handleUploadCallback(CallbackBodyVo vo) {
        ThrowUtil.throwIf(Objects.isNull(vo.getAccessKeyId()), NOT_FOUND);
        log.info("Received upload callback for accessKeyId: {}", vo.getAccessKeyId());

        // 从缓存中获取上传信息
        Object cachedData = redisUtils.getValue(vo.getAccessKeyId());
        ThrowUtil.throwIf(Objects.isNull(cachedData), NOT_FOUND, "Upload cache not found");
        ThrowUtil.throwIf(!(cachedData instanceof AvatarUploadCache), NOT_FOUND);
        AvatarUploadCache data = (AvatarUploadCache) cachedData;


        // 验证缓存与回调信息一致性
        ThrowUtil.throwIf(!data.getExpectedBucket().equals(vo.getBucketName()),
                         NOT_FOUND, "Bucket name mismatch");

        // 构建完整的OSS URL
        String ossUrl = String.format("%s", data.getObjectPath());
        AvatarUrlVo auv = new AvatarUrlVo(ossUrl, "success", data.getUsername());
        if(Objects.isNull(ossUrl)) auv.setStatus("ossUrl");


        return Result.success(auv);
    }
    
}
