package com.basis.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传缓存对象
 * 用于在OSS回调时验证上传数据的合法性
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadCache {

    /**
     * 用户名
     */
    private String username;

    /**
     * OSS对象路径
     */
    private String objectPath;

    /**
     * 预期的bucket名称
     */
    private String expectedBucket;

    /**
     * 上传时间戳
     */
    private Long uploadTime;

    /**
     * 文件类型（视频、音频等）
     */
    private String fileType;
}