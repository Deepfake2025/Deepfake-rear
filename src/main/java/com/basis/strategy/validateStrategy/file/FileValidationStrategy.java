package com.basis.strategy.validateStrategy.file;

import com.basis.model.vo.FileMetaVo;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 文件验证策略接口
 */
public interface FileValidationStrategy {

    /**
     * 验证文件元数据
     *
     * @param vo 文件元数据
     */
    void validateFileMetadata(FileMetaVo vo);
}