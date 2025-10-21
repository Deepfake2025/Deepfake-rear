package com.basis.service;

import com.basis.common.Result;
import com.basis.model.vo.AvatarMetaVo;
import com.basis.model.vo.CallbackBodyVo;

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
     * 获取头像上传STS临时凭证
     *
     * @param username 用户名
     * @param vo 头像文件元数据
     * @return STS临时凭证信息
     */
    Result<?> getAvatarUploadCredentials(String username, AvatarMetaVo vo);

    /**
     * 处理上传完成回调
     *
     * @return 返回资源路径
     */
    Result<?> handleUploadCallback(CallbackBodyVo vo);


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

    /**
     * 验证上传回调
     *
     * @param authorization 回调签名
     * @param body 回调内容
     * @param path 请求路径
     * @return 验证结果
     */
    // boolean validateCallback(String authorization, String body, String path);
}