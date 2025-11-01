package com.basis.service;

import com.basis.model.vo.StsCredentialsVo;

/**
 * <p>
 * 云存储服务接口
 * 提供阿里云OSS相关的STS临时凭证和文件操作功能
 * </p>
 *
 * @author IT 派同学
 * @since 2024-12-07
 */
public interface ICloudStorageService {

    /** 
     * 申请STS临时凭证
     *
     * @return 返回临时凭证
     */
    StsCredentialsVo getStsCredentialsVo(String policy, String roleSessionName, Long durationSeconds);


    /**
     * 处理上传完成回调
     *
     * @param callbackBody OSS回调数据
     * @return 文件上传结果
     */
    // Object handleUploadCallback(String callbackBody);

    /**
     * 删除文件
     *
     * @param objectName OSS对象名称
     */
    // void deleteFile(String objectName);


}