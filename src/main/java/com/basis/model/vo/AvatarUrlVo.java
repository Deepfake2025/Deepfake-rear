package com.basis.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/23
 * @Description: 头像URL返回对象
 */
@Data
@ApiModel(value = "AvatarUrl对象", description = "头像URL返回信息")
public class AvatarUrlVo {

    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;

    @ApiModelProperty(value = "上传状态")
    private String status;


    public AvatarUrlVo(String avatarUrl, String status) {
        this.avatarUrl = avatarUrl;
        this.status = status;
    }

}