package com.basis.service.impl;

import com.basis.model.vo.AvatarMetaVo;
import com.basis.model.vo.StsCredentialsVo;
import java.time.LocalDateTime;
import java.util.UUID;

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
import com.basis.service.ICloudStorageService;
import com.basis.common.ResponseCode;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class CloudStorageService implements ICloudStorageService {

    @Value("${aliyun.sts-endpoint}")
    private String endpoint;
    @Value("${aliyun.oss.avatar.bucket-name}")
    private String bucketName;
    @Value("${aliyun.access-key-id}")
    private String accessKeyId;
    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;
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
        //String policy = buildUploadPolicy(username, vo);
        String policy = null;

        log.info(accessKeyId);
        log.info(accessKeySecret);
        log.info(endpoint);
        log.info(roleArn);
        

        // 获取STS临时凭证
        try {
            // 发起STS请求所在的地域。建议保留默认值，默认值为空字符串（""）。
            String regionId = "";
            // 临时访问凭证的有效时间，单位为秒。最小值为900，最大值以当前角色设定的最大会话时间为准
            Long durationSeconds = 3600L;

            // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
            DefaultProfile.addEndpoint(endpoint, regionId, "Sts", endpoint);
            // 构造default profile。
            IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            // 构造client。
            DefaultAcsClient client = new DefaultAcsClient(profile);
            final AssumeRoleRequest request = new AssumeRoleRequest();

            // 适用于Java SDK 3.12.0以下版本。
            request.setMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName(username + "-" + System.currentTimeMillis());
            request.setPolicy(policy);
            request.setDurationSeconds(durationSeconds);
            final AssumeRoleResponse response = client.getAcsResponse(request);

            // 构建返回对象
            StsCredentialsVo credentials = new StsCredentialsVo();
            credentials.setAccessKeyId(response.getCredentials().getAccessKeyId());
            credentials.setAccessKeySecret(response.getCredentials().getAccessKeySecret());
            credentials.setSecurityToken(response.getCredentials().getSecurityToken());
            
            // 设置过期时间（需要转换Date到LocalDateTime）
            credentials.setExpiration(LocalDateTime.now().plusSeconds(durationSeconds));

            credentials.setBucketName(bucketName);
            credentials.setRegion("cn-guangzhou");
            credentials.setEndpoint(endpoint);

            // 生成对象路径
            String fileExtension = getFileExtensionFromMimeType(vo.getMimeType());
            String objectPath = String.format("%s%s/avatar-%s.%s",
                    avatarPath, username, UUID.randomUUID().toString().substring(0, 8), fileExtension);
            credentials.setObjectPath(objectPath);

            credentials.setMaxFileSize(maxFileSize);
            credentials.setAllowedFileTypes(allowedFileTypes);

            return Result.success(credentials);

        } catch (ClientException e) {
            log.error(e.getMessage());
            return Result.fail(ResponseCode.OBTAIN_STS_TOKEN_FAILED);
        }
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
            "        \"acs:oss:*:*:" + bucketName + "/" + avatarPath + username + "/*\"\n" +
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
    
}
