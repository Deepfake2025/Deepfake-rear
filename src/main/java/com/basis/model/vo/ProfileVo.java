package com.basis.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 用户资料信息
 */
@Data
@ApiModel(value = "Profile对象", description = "用户资料信息")
public class ProfileVo {

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "用户昵称")
    private String nickname;

    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;

    @ApiModelProperty(value = "手机号")
    private String phone;
}