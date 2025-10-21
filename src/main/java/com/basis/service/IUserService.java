package com.basis.service;

import com.basis.common.Result;
import com.basis.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.basis.model.vo.AvatarMetaVo;
import com.basis.model.vo.AvatarUrlVo;
import com.basis.model.vo.CallbackBodyVo;
import com.basis.model.vo.LoginVo;
import com.basis.model.vo.ProfileVo;
import com.basis.model.vo.RegisterVo;
import com.basis.model.vo.SendVo;

import cn.hutool.setting.profile.Profile;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author IT 派同学
 * @since 2024-12-07
 */
public interface IUserService extends IService<User> {

    /**
     * 退出登录
     */
    void logout();

    /**
     * 登录操作
     * @param vo 请求体
     * @return 操作结果
     */
    Result<?> login(LoginVo vo);

    /**
     * 用户注册系统
     * @param vo 注册请求体
     * @return 操作结果
     */
    Result<?> register(RegisterVo vo);

    /**
     * 发送验证码
     * @param vo 发送验证码的请求体
     * @return 操作结果
     */
    Result<?> sendCaptcha(SendVo vo);


    /**
     * 获取用户信息
     * @return 操作结果
     */
    Result<?> getProfile();

    /**
     * 更新用户信息
     * @return 操作结果
     */
    Result<?> updateProfile(ProfileVo vo);


 /**
     * 上传用户头像初始化
     * @param vo 头像图片文件元数据
     * @return sts-token
     */
    Result<?> uploadInit(AvatarMetaVo vo);

    /**
     * 上传用户头像回调
     * @return 头像URL返回信息
     */
    Result<?> uploadCallback(CallbackBodyVo vo);

    /**
     * 返回用户头像url
     * @return 头像URL返回信息
     */
    Result<?> fecthAvatar();
}
