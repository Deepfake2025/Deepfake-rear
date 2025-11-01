package com.basis.service;

import com.basis.common.Result;
import com.basis.model.vo.FileMetaVo;
import com.basis.model.vo.CallbackBodyVo;

/**
 * <p>
 * 文件服务接口
 * 提供文件上传相关的STS临时凭证和回调处理功能
 * </p>
 *
 * @author IT 派同学
 * @since 2024-12-07
 */
public interface IFileService {

    /**
     * 文件上传初始化，获取STS临时凭证
     *
     * @param vo 文件元数据
     * @return STS临时凭证信息
     */
    Result<?> uploadInit(FileMetaVo vo);

    /**
     * 处理文件上传完成回调
     *
     * @param vo OSS回调数据
     * @return 文件上传结果
     */
    Result<?> uploadCallback(CallbackBodyVo vo);
}