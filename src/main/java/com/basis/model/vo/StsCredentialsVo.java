package com.basis.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: STS临时凭证信息
 */
@Data
@ApiModel(value = "StsCredentials对象", description = "STS临时凭证信息")
public class StsCredentialsVo {

    @ApiModelProperty(value = "临时访问密钥ID")
    private String accessKeyId;

    @ApiModelProperty(value = "临时访问密钥Secret")
    private String accessKeySecret;

    @ApiModelProperty(value = "安全令牌")
    private String securityToken;

    @ApiModelProperty(value = "凭证过期时间")
    private LocalDateTime expiration;

    @ApiModelProperty(value = "OSS桶名称")
    private String bucketName;

    @ApiModelProperty(value = "OSS区域")
    private String region;

    @ApiModelProperty(value = "OSS端点")
    private String endpoint;

    @ApiModelProperty(value = "文件对象路径")
    private String objectPath;

    @ApiModelProperty(value = "文件大小限制（字节）")
    private Long maxFileSize;

    @ApiModelProperty(value = "允许的文件类型")
    private String[] allowedFileTypes;
}