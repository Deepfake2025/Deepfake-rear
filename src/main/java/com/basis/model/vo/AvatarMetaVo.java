package com.basis.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 头像文件元数据信息
 */
@Data
@ApiModel(value = "AvatarMeta对象", description = "头像文件元数据信息")
public class AvatarMetaVo {

    @ApiModelProperty(value = "文件MIME类型", required = true)
    @Pattern(regexp = "^image/(jpeg|jpg|png|gif)$", message = "仅支持jpg、jpeg、png、gif格式的图片")
    private String mimeType;

    @ApiModelProperty(value = "文件大小（字节）", required = true)
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @ApiModelProperty(value = "文件MD5值（可选）")
    private String md5;

    @ApiModelProperty(value = "图片宽度（可选）")
    private Integer width;

    @ApiModelProperty(value = "图片高度（可选）")
    private Integer height;
}