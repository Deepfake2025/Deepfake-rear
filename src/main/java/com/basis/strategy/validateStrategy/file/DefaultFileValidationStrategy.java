package com.basis.strategy.validateStrategy.file;

import com.basis.exception.BusinessException;
import com.basis.model.enums.MimeTypeEnum;
import com.basis.model.vo.FileMetaVo;
import com.basis.utils.ThrowUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 默认文件验证策略实现（主要支持视频和音频文件）
 */
@Component
public class DefaultFileValidationStrategy implements FileValidationStrategy {

    @Value("${aliyun.oss.file.max-file-size}")
    private Long fileMaxFileSize;

    @Value("${aliyun.oss.file.allowed-file-types}")
    private String[] allowedFileTypes;

    @Override
    public void validateFileMetadata(FileMetaVo vo) {
        // 验证文件大小限制
        validateFileSize(vo.getFileSize());

        // 验证文件类型（主要是视频、音频类型）
        validateFileType(vo.getMimeType());

        // 验证文件名
        // validateFileName(vo.getFileName());

        // 验证MD5（可选）
        // validateMd5(vo.getMd5());
    }

    /**
     * 验证文件大小
     */
    private void validateFileSize(Long fileSize) {
        ThrowUtil.throwIf(fileSize == null || fileSize <= 0,
                new BusinessException("文件大小必须大于0"));
        ThrowUtil.throwIf(fileSize > fileMaxFileSize,
                new BusinessException("文件大小不能超过" + (fileMaxFileSize / 1024 / 1024) + "MB"));
    }

    /**
     * 验证文件类型（视频和音频）
     */
    private void validateFileType(String mimeType) {
        ThrowUtil.throwIf(StrUtil.isEmpty(mimeType),
                new BusinessException("文件类型不能为空"));

        // 检查是否为支持的视频或音频类型
        boolean isVideo = mimeType.startsWith("video/");
        boolean isAudio = mimeType.startsWith("audio/");

        ThrowUtil.throwIf(!isVideo && !isAudio,
                new BusinessException("仅支持视频和音频文件"));

        // 使用MimeTypeEnum获取文件扩展名，当MIME类型不支持时会抛出异常
        String fileExtension;
        try {
            fileExtension = MimeTypeEnum.getExtensionByMimeType(mimeType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的文件类型: " + mimeType);
        }

        // 检查具体的文件格式是否在允许列表中
        boolean isAllowedType = false;
        for (String allowedType : allowedFileTypes) {
            if (allowedType.equalsIgnoreCase(fileExtension)) {
                isAllowedType = true;
                break;
            }
        }
        ThrowUtil.throwIf(!isAllowedType,
                new BusinessException("仅支持" + String.join(", ", allowedFileTypes) + "格式的文件"));
    }

    /**
     * 验证文件名
     */
    // private void validateFileName(String fileName) {
    //     if (StrUtil.isNotEmpty(fileName)) {
    //         // 检查文件名长度
    //         ThrowUtil.throwIf(fileName.length() > 255,
    //                 new BusinessException("文件名长度不能超过255个字符"));

    //         // 检查文件名是否包含非法字符
    //         String[] illegalChars = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};
    //         for (String illegalChar : illegalChars) {
    //             ThrowUtil.throwIf(fileName.contains(illegalChar),
    //                     new BusinessException("文件名不能包含非法字符: " + illegalChar));
    //         }
    //     }
    // }

    /**
     * 验证MD5
     */
    // private void validateMd5(String md5) {
    //     if (StrUtil.isNotEmpty(md5)) {
    //         // 验证MD5格式（32位十六进制字符串）
    //         ThrowUtil.throwIf(!md5.matches("^[a-fA-F0-9]{32}$"),
    //                 new BusinessException("MD5格式不正确"));
    //     }
    // }

    }