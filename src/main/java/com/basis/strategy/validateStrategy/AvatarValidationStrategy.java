package com.basis.strategy.validateStrategy;

import com.basis.model.vo.AvatarMetaVo;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 头像验证策略接口
 */
public interface AvatarValidationStrategy {

    /**
     * 验证头像文件元数据
     *
     * @param vo 头像文件元数据
     */
    void validateAvatarMetadata(AvatarMetaVo vo);
}