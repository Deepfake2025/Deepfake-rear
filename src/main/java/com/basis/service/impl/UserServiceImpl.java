package com.basis.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basis.common.Result;
import com.basis.exception.BusinessException;
import com.basis.mapper.UserMapper;
import com.basis.model.entity.User;
import com.basis.model.vo.LoginVo;
import com.basis.model.vo.ProfileVo;
import com.basis.model.vo.RegisterVo;
import com.basis.model.vo.SendVo;
import com.basis.service.IUserService;
import com.basis.strategy.login.LoginStrategy;
import com.basis.strategy.login.LoginStrategyFactory;
import com.basis.strategy.sendStrategy.SendCaptchaStrategy;
import com.basis.strategy.sendStrategy.SendCaptchaStrategyFactory;
import com.basis.utils.PasswordUtils;
import com.basis.utils.ThrowUtil;
import com.basis.utils.UsernameUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.basis.common.ResponseCode.NOTHING_TO_UPDATE;
import static com.basis.common.ResponseCode.USERNAME_OR_PASS_EMPTY;
import static com.basis.common.ResponseCode.USER_ALREADY_EXISTED;
import static com.basis.common.ResponseCode.USER_NOT_EXIST;
import static com.basis.model.constant.BasicConstant.DEFAULT_NICK_NAME;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author IT 派同学
 * @since 2024-12-07
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private LoginStrategyFactory strategyFactory;

    @Autowired
    private SendCaptchaStrategyFactory sendCaptchaStrategyFactory;

    @Value("${proxy.schema}")
    private String schema;
    @Value("${proxy.host}")
    private String host;
    @Value("${proxy.port}")
    private String port;




    /**
     * 退出登录
     */
    @Override
    public void logout() {
        // 判断是否已经登录
        if (StpUtil.isLogin()) {
            // 退出登录
            StpUtil.logout();
        }
    }

    /**
     * 登录操作
     * @param vo 请求体
     * @return
     */
    @Override
    public Result<?> login(LoginVo vo) {
        LoginStrategy strategy = strategyFactory.getStrategy(vo.getLoginType());
        return strategy.login(vo);
    }

    /**
     * 用户注册系统
     * @param vo 注册请求体
     * @return 操作结果
     */
    @Override
    public Result<?> register(RegisterVo vo) {
        // 校验参数
        ThrowUtil.throwIf(StrUtil.isEmpty(vo.getEmail()) || StrUtil.isEmpty(vo.getPassword()),
                new BusinessException(USERNAME_OR_PASS_EMPTY));
        // 根据email查询用户是否存在
        User one = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, vo.getEmail()).last("LIMIT 1"));        // 校验是否存在
        ThrowUtil.throwIf(Objects.nonNull(one), new BusinessException(USER_ALREADY_EXISTED));
        String salt = PasswordUtils.getSalt();
        String encode = PasswordUtils.encode(vo.getPassword(), salt);
        User user = new User();
        // 生成username
        String username = UsernameUtil.generateUsernameFromEmail(vo.getEmail());
        user.setPassword(encode);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setSalt(salt);
        user.setEmail(vo.getEmail());
        user.setUserName(username);
        user.setIsDeleted(false);
        user.setNickName(DEFAULT_NICK_NAME);
        save(user);
        return Result.success();
    }

    /**
     * 发送验证码
     *
     * @param vo 发送验证码的请求体
     * @return 操作结果
     */
    @Override
    public Result<?> sendCaptcha(SendVo vo) {
        SendCaptchaStrategy strategy = sendCaptchaStrategyFactory.getStrategy(vo.getSendType());
        return strategy.send(vo);
    }

    @Override
    public Result<?> getProfile() {
        Object username = StpUtil.getSession().get("username");

        // 根据username在user数据表中查询
        User one = getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username).last("LIMIT 1"));
        ThrowUtil.throwIf(Objects.isNull(one), USER_NOT_EXIST);
    
        // 构造Profile对象
        ProfileVo profile = new ProfileVo();
        profile.setEmail(one.getEmail());
        profile.setNickname(one.getNickName());
        profile.setPhone(one.getPhone() != null ? one.getPhone() : "");
        // 构造avatarUrl
        String avatarUrl = String.format("%s://%s:%s",schema, host, port) + one.getAvatar();
        profile.setAvatarUrl(avatarUrl);


        return Result.success(profile);
    }

    /**
     * 更新个人信息
     * 仅支持更新nickname, phone
     *
     * @param vo
     * @return 操作结果
     */
    @Override
    public Result<?> updateProfile(ProfileVo vo) {
        // 获取当前登录用户
        Object username = StpUtil.getSession().get("username");

        // 根据username查询用户
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username).last("LIMIT 1"));
        ThrowUtil.throwIf(Objects.isNull(user), USER_NOT_EXIST);

        // 更新用户信息
        boolean needUpdate = false;

        if (StrUtil.isNotEmpty(vo.getNickname()) && !Objects.equals(vo.getNickname(), user.getNickName())) {
            user.setNickName(vo.getNickname());
            needUpdate = true;
        }

        if (StrUtil.isNotEmpty(vo.getPhone()) && !Objects.equals(vo.getPhone(), user.getPhone())) {
            user.setPhone(vo.getPhone());
            needUpdate = true;
        }

        // 如果有更新，执行保存
        if (needUpdate) {
            user.setUpdateTime(LocalDateTime.now());
            updateById(user);
            return Result.success();
        } else {
            return Result.fail(NOTHING_TO_UPDATE);
        }

        
    }
}
