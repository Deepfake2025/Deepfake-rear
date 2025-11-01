package com.basis.strategy.validateStrategy.avatar;

import com.basis.exception.BusinessException;
import com.basis.model.enums.MimeTypeEnum;
import com.basis.model.vo.AvatarMetaVo;
import com.basis.utils.ThrowUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 默认头像验证策略实现
 */
@Component
public class DefaultAvatarValidationStrategy implements AvatarValidationStrategy {

    @Value("${aliyun.oss.avatar.max-file-size}")
    private Long avatarMaxFileSize;

    @Value("${aliyun.oss.avatar.allowed-file-types}")
    private String[] allowedFileTypes;

    @Override
    public void validateAvatarMetadata(AvatarMetaVo vo) {
        // 验证文件大小限制（从配置文件读取）
        validateFileSize(vo.getFileSize());

        // 验证文件类型（根据配置文件中的允许类型）
        validateFileType(vo.getMimeType());

        // 验证图片尺寸（可选）
        validateImageDimensions(vo);
    }

    /**
     * 验证文件大小
     */
    private void validateFileSize(Long fileSize) {
        ThrowUtil.throwIf(fileSize == null || fileSize <= 0,
                new BusinessException("文件大小必须大于0"));
        ThrowUtil.throwIf(fileSize > avatarMaxFileSize,
                new BusinessException("文件大小不能超过" + (avatarMaxFileSize / 1024 / 1024) + "MB"));
    }

    /**
     * 验证文件类型
     */
    private void validateFileType(String mimeType) {
        ThrowUtil.throwIf(StrUtil.isEmpty(mimeType),
                new BusinessException("文件类型不能为空"));

        // 检查是否为图片类型
        if (!mimeType.startsWith("image/")) {
            throw new BusinessException("仅支持图片文件");
        }

        // 使用MimeTypeEnum获取文件扩展名，当MIME类型不支持时会抛出异常
        String fileExtension;
        try {
            fileExtension = MimeTypeEnum.getExtensionByMimeType(mimeType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("不支持的图片类型: " + mimeType);
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
                new BusinessException("仅支持" + String.join(", ", allowedFileTypes) + "格式的图片"));
    }

    /**
     * 验证图片尺寸
     */
    private void validateImageDimensions(AvatarMetaVo vo) {
        if (vo.getWidth() != null && vo.getHeight() != null) {
            ThrowUtil.throwIf(vo.getWidth() <= 0 || vo.getHeight() <= 0,
                    new BusinessException("图片尺寸必须大于0"));
            // 验证尺寸限制（可选）
            ThrowUtil.throwIf(vo.getWidth() > 2000 || vo.getHeight() > 2000,
                    new BusinessException("图片尺寸不能超过2000x2000像素"));
        }
    }

    }