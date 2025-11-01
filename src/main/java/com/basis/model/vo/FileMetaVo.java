package com.basis.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 文件元数据信息
 */
@Data
@ApiModel(value = "FileMeta对象", description = "文件元数据信息")
public class FileMetaVo {

    @ApiModelProperty(value = "文件MIME类型", required = true)
    @Pattern(regexp = "^(image|application|text|video|audio)\\/.+", message = "不支持的文件类型")
    private String mimeType;

    @ApiModelProperty(value = "文件大小（字节）", required = true)
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @ApiModelProperty(value = "文件MD5值（可选）")
    private String md5;

    @ApiModelProperty(value = "文件名（可选）")
    private String fileName;

    @ApiModelProperty(value = "文件用途/类型（可选）", example = "avatar,document,video")
    private String fileType;
}