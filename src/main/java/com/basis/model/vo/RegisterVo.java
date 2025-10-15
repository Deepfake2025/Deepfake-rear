package com.basis.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @Description: 注册新用户请求体
 */
@Data
public class RegisterVo {

    @ApiModelProperty(value = "密码")
    @NotEmpty(message = "密码不能为空")
    private String password;

    @ApiModelProperty(value = "邮箱")
    private String email;
}
