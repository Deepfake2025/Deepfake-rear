package com.basis.strategy.validateStrategy.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: IT 派同学
 * @Date: 2024/12/20
 * @Description: 文件验证策略工厂类
 */
@Component
public class FileValidationStrategyFactory {

    @Autowired
    private DefaultFileValidationStrategy defaultStrategy;

    /**
     * 获取文件验证策略（目前只有默认策略）
     *
     * @return 文件验证策略
     */
    public FileValidationStrategy getStrategy() {
        return defaultStrategy;
    }
}