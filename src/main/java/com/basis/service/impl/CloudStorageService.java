package com.basis.service.impl;

import com.basis.model.vo.StsCredentialsVo;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.auth.sts.AssumeRoleResponse.Credentials;
import com.basis.exception.BusinessException;
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
    private String aliyunAccessKeyId;
    @Value("${aliyun.access-key-secret}")
    private String aliyunAccessKeySecret;
    @Value("${aliyun.role-arn}")
    private String roleArn;

    /**
     * 获取STS临时凭证的通用方法
     */
    private Credentials getStsCredentials(String policy, String roleSessionName, Long durationSeconds) {
        // 发起STS请求所在的地域。建议保留默认值，默认值为空字符串（""）。
        String regionId = "";

        // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
        try {
            DefaultProfile.addEndpoint(endpoint, regionId, "Sts", endpoint);
        } catch (ClientException e) {
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
        request.setRoleSessionName(roleSessionName);
        request.setPolicy(policy);
        request.setDurationSeconds(durationSeconds);

        final AssumeRoleResponse response;
        try {
            response = client.getAcsResponse(request);
        } catch (ClientException e) {
            log.error("Failed to obtain STS token: {}", e.getMessage());
            throw new BusinessException(ResponseCode.OBTAIN_STS_TOKEN_FAILED);
        }

        return response.getCredentials();
    }

    /**
     * 构建基础STS凭证对象
     */
    private StsCredentialsVo buildBaseStsCredentialsVo(Credentials credentials, Long durationSeconds) {
        StsCredentialsVo vo = new StsCredentialsVo();
        vo.setAccessKeyId(credentials.getAccessKeyId());
        vo.setAccessKeySecret(credentials.getAccessKeySecret());
        vo.setSecurityToken(credentials.getSecurityToken());
        vo.setExpiration(LocalDateTime.now().plusSeconds(durationSeconds));
        vo.setBucketName(bucketName);
        vo.setRegion("cn-guangzhou");
        vo.setEndpoint(endpoint);
        return vo;
    }

    @Override
    public StsCredentialsVo getStsCredentialsVo(String policy, String roleSessionName, Long durationSeconds) {
        return buildBaseStsCredentialsVo(getStsCredentials(policy, roleSessionName, durationSeconds), durationSeconds);
    }

}
