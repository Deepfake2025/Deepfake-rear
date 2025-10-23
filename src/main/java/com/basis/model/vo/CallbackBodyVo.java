package com.basis.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Description: 回调响应信息
 */
@Data
@ApiModel(value = "OSS回调对象", description = "OSS上传回调信息")
public class CallbackBodyVo {
    @ApiModelProperty(value = "临时访问密钥ID")
    private String accessKeyId;

    @ApiModelProperty(value = "OSS桶名称")
    private String bucketName;
}

