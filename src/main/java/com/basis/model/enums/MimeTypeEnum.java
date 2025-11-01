package com.basis.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: Claude
 * @Date: 2025/10/31
 * @Description: MIME类型枚举
 */
@Getter
@AllArgsConstructor
public enum MimeTypeEnum {

    // 图片类型
    IMAGE_JPEG("image/jpeg", "jpg"),
    IMAGE_JPG("image/jpg", "jpg"),
    IMAGE_PNG("image/png", "png"),
    IMAGE_GIF("image/gif", "gif"),
    IMAGE_WEBP("image/webp", "webp"),

    // 视频类型
    VIDEO_MP4("video/mp4", "mp4"),
    VIDEO_AVI("video/avi", "avi"),
    VIDEO_QUICKTIME("video/quicktime", "mov"),
    VIDEO_WMV("video/x-ms-wmv", "wmv"),
    VIDEO_FLV("video/x-flv", "flv"),
    VIDEO_WEBM("video/webm", "webm"),
    VIDEO_MKV("video/x-matroska", "mkv"),

    // 音频类型
    AUDIO_MPEG("audio/mpeg", "mp3"),
    AUDIO_WAV("audio/wav", "wav"),
    AUDIO_AAC("audio/aac", "aac"),
    AUDIO_OGG("audio/ogg", "ogg"),
    AUDIO_FLAC("audio/flac", "flac");

    /**
     * MIME类型
     */
    private final String mimeType;

    /**
     * 文件扩展名
     */
    private final String extension;

    /**
     * 根据MIME类型获取扩展名
     *
     * @param mimeType MIME类型
     * @return 文件扩展名
     * @throws IllegalArgumentException 当MIME类型不支持时抛出
     */
    public static String getExtensionByMimeType(String mimeType) {
        for (MimeTypeEnum mimeTypeEnum : values()) {
            if (mimeTypeEnum.mimeType.equals(mimeType)) {
                return mimeTypeEnum.extension;
            }
        }

        // 对于枚举中没有定义的类型，尝试从MIME类型中提取扩展名
        if (mimeType != null && mimeType.contains("/")) {
            String[] parts = mimeType.split("/");
            if (parts.length == 2) {
                return parts[1].toLowerCase();
            }
        }

        throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
    }

    /**
     * 检查MIME类型是否被支持
     *
     * @param mimeType MIME类型
     * @return 是否支持
     */
    public static boolean isSupported(String mimeType) {
        for (MimeTypeEnum mimeTypeEnum : values()) {
            if (mimeTypeEnum.mimeType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }
}