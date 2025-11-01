package com.basis.strategy.validateStrategy.avatar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 头像验证策略工厂类
 */
@Component
public class AvatarValidationStrategyFactory {

    @Autowired
    private DefaultAvatarValidationStrategy defaultStrategy;

    /**
     * 获取头像验证策略（目前只有默认策略）
     *
     * @return 头像验证策略
     */
    public AvatarValidationStrategy getStrategy() {
        return defaultStrategy;
    }
}