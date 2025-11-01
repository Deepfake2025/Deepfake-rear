package com.basis.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.basis.common.ResponseCode;
import com.basis.common.Result;
import com.basis.exception.BusinessException;
import com.basis.mapper.UserMapper;
import com.basis.model.dto.AvatarUploadCache;
import com.basis.model.entity.User;
import com.basis.model.enums.MimeTypeEnum;
import com.basis.model.vo.AvatarMetaVo;
import com.basis.model.vo.AvatarUrlVo;
import com.basis.model.vo.CallbackBodyVo;
import com.basis.model.vo.LoginVo;
import com.basis.model.vo.ProfileVo;
import com.basis.model.vo.RegisterVo;
import com.basis.model.vo.SendVo;
import com.basis.model.vo.StsCredentialsVo;
import com.basis.service.ICloudStorageService;
import com.basis.service.IUserService;
import com.basis.strategy.login.LoginStrategy;
import com.basis.strategy.login.LoginStrategyFactory;
import com.basis.strategy.sendStrategy.SendCaptchaStrategy;
import com.basis.strategy.sendStrategy.SendCaptchaStrategyFactory;
import com.basis.strategy.validateStrategy.avatar.AvatarValidationStrategy;
import com.basis.strategy.validateStrategy.avatar.AvatarValidationStrategyFactory;
import com.basis.utils.PasswordUtils;
import com.basis.utils.RedisUtils;
import com.basis.utils.ThrowUtil;
import com.basis.utils.UsernameUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Resource;

import static com.basis.common.ResponseCode.NOTHING_TO_UPDATE;
import static com.basis.common.ResponseCode.USERNAME_OR_PASS_EMPTY;
import static com.basis.common.ResponseCode.USER_ALREADY_EXISTED;
import static com.basis.common.ResponseCode.USER_NOT_EXIST;
import static com.basis.common.ResponseCode.NOT_FOUND;
import static com.basis.model.constant.BasicConstant.DEFAULT_NICK_NAME;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author IT 派同学
 * @since 2024-12-07
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private LoginStrategyFactory strategyFactory;

    @Autowired
    private SendCaptchaStrategyFactory sendCaptchaStrategyFactory;

    @Autowired
    private AvatarValidationStrategyFactory avatarValidationStrategyFactory;

    @Autowired
    private ICloudStorageService cloudStorageService;

    @Resource
    private RedisUtils redisUtils;

    @Value("${proxy.schema}")
    private String schema;
    @Value("${proxy.host}")
    private String host;
    @Value("${proxy.port}")
    private String port;

    // 阿里云OSS头像上传配置
    @Value("${aliyun.oss.avatar.max-file-size}")
    private Long avatarMaxFileSize;

    @Value("${aliyun.oss.avatar.allowed-file-types}")
    private String[] allowedFileTypes;

    @Value("${aliyun.oss.avatar.avatar-path}")
    private String avatarPath;

    @Value("${aliyun.oss.avatar.bucket-name}")
    private String bucketName;

    @Value("${aliyun.endpoint}")
    private String endpoint;

    @Value("${aliyun.access-key-id}")
    private String aliyunAccessKeyId;

    @Value("${aliyun.access-key-secret}")
    private String aliyunAccessKeySecret;

    @Value("${aliyun.role-arn}")
    private String roleArn;

    @Value("${aliyun.oss.avatar.max-file-size}")
    private Long maxFileSize;

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
     * 
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
     * 
     * @param vo 注册请求体
     * @return 操作结果
     */
    @Override
    public Result<?> register(RegisterVo vo) {
        // 校验参数
        ThrowUtil.throwIf(StrUtil.isEmpty(vo.getEmail()) || StrUtil.isEmpty(vo.getPassword()),
                new BusinessException(USERNAME_OR_PASS_EMPTY));
        // 根据email查询用户是否存在
        User one = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, vo.getEmail()).last("LIMIT 1")); // 校验是否存在
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
        // TODO: 私用redis缓存加快查询速度
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
        String avatarUrl = String.format("%s://%s:%s", schema, host, port) + one.getAvatar();
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

    /**
     * 构建头像上传权限策略
     */
    private String buildAvatarUploadPolicy(String username, AvatarMetaVo vo) {
        // 根据用户和文件元数据构建精细化的权限策略
        return "{\n" +
                "  \"Version\": \"1\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": [\n" +
                "        \"oss:PutObject\"\n" +
                "      ],\n" +
                "      \"Resource\": [\n" +
                "        \"acs:oss:*:*:" + bucketName + "/" + avatarPath + "*\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /**
     * 上传头像初始化
     *
     * @param vo 头像图片文件元数据
     * @return sts-token
     */
    @Override
    public Result<?> uploadInit(AvatarMetaVo vo) {
        // 获取当前登录用户
        String username = StpUtil.getSession().get("username").toString();

        // 检查meta数据是否符合上传标准
        AvatarValidationStrategy validationStrategy = avatarValidationStrategyFactory.getStrategy();
        validationStrategy.validateAvatarMetadata(vo);

        /*
         * 申请STS凭证
         */
        // 构建精细化的权限策略
        String policy = buildAvatarUploadPolicy(username, vo);

        // 获取当前登录用户信息
        String currentUsername = StpUtil.getSession().get("username").toString();
        if (!currentUsername.equals(username)) {
            return Result.fail(ResponseCode.AUTH_FORBID);
        }

        // 临时访问凭证的有效时间，单位为秒。最小值为900，最大值以当前角色设定的最大会话时间为准
        Long durationSeconds = 3600L;
        String roleSessionName = username + "-" + System.currentTimeMillis();

        // 获取STS临时凭证
        StsCredentialsVo result = cloudStorageService.getStsCredentialsVo(policy, roleSessionName, durationSeconds);

        // 生成对象路径
        String fileExtension = getFileExtensionFromMimeType(vo.getMimeType());
        String objectPath = String.format("/%savatar-%s%s.%s",
                avatarPath, username, UUID.randomUUID().toString().substring(0, 8), fileExtension);
        result.setObjectPath(objectPath);

        result.setMaxFileSize(maxFileSize);
        result.setAllowedFileTypes(allowedFileTypes);

        // 创建上传缓存对象
        AvatarUploadCache uploadCache = AvatarUploadCache.builder()
                .username(username)
                .objectPath(objectPath)
                .expectedBucket(bucketName)
                .uploadTime(System.currentTimeMillis())
                .build();

        // 使用STS accessKeyId作为缓存key，有效期2小时（比STS凭证多1小时缓冲）
        redisUtils.setValueTimeout(result.getAccessKeyId(), uploadCache, 2 * durationSeconds);

        log.info("Avatar upload info cached with accessKeyId: {}, username: {}, objectPath: {}",
                result.getAccessKeyId(), username, objectPath);

        return Result.success(result);
    }

    /**
     * 处理头像上传回调
     */
    private AvatarUrlVo handleAvatarUploadCallback(CallbackBodyVo vo, AvatarUploadCache data) {
        // 验证缓存与回调信息一致性
        ThrowUtil.throwIf(!data.getExpectedBucket().equals(vo.getBucketName()),
                         NOT_FOUND, "Bucket name mismatch");

        // 构建完整的OSS URL
        String ossUrl = String.format("%s", data.getObjectPath());
        AvatarUrlVo auv = new AvatarUrlVo(ossUrl, "success", data.getUsername());
        if(Objects.isNull(ossUrl)) auv.setStatus("ossUrl");

        return auv;
    }

    @Override
    public Result<?> uploadCallback(CallbackBodyVo vo) {
        // 验证回调信息与缓存一致性，构造ossUrl
        ThrowUtil.throwIf(Objects.isNull(vo.getAccessKeyId()), NOT_FOUND);
        log.info("Received upload callback for accessKeyId: {}", vo.getAccessKeyId());

        // 从缓存中获取上传信息
        Object cachedData = redisUtils.getValue(vo.getAccessKeyId());
        ThrowUtil.throwIf(Objects.isNull(cachedData), NOT_FOUND, "Upload cache not found");

        // 处理头像上传回调
        AvatarUrlVo auv;
        if (cachedData instanceof AvatarUploadCache) {
            AvatarUploadCache data = (AvatarUploadCache) cachedData;
            auv = handleAvatarUploadCallback(vo, data);
        }
        else {
            ThrowUtil.throwIf(true, NOT_FOUND, "Invalid cache type");
            return Result.fail(NOT_FOUND);
        }

        // 将ossUrl 更新到Mysql
        // String username = StpUtil.getSession().get("username").toString();

        // 根据username查询用户
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, auv.getUsername()).last("LIMIT 1"));
        ThrowUtil.throwIf(Objects.isNull(user), USER_NOT_EXIST);

        // 更新用户头像URL
        user.setAvatar(auv.getAvatarUrl());
        user.setUpdateTime(LocalDateTime.now());

        if (updateById(user)) {
            // 返回完整的头像URL
            auv.setAvatarUrl(String.format("%s://%s:%s%s", schema, host, port, auv.getAvatarUrl()));
            return Result.success(auv);
        } else {
            return Result.fail("Write avatar oss-url into db failed");
        }
    }

    @Override
    public Result<?> fecthAvatar() {
        // TODO: 使用redis缓存数据库信息，来加快查询速度
        String username = StpUtil.getSession().get("username").toString();
        // 直接从数据库拿到User
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, username).last("LIMIT 1"));
        ThrowUtil.throwIf(Objects.isNull(user), USER_NOT_EXIST);

        AvatarUrlVo auv = new AvatarUrlVo(String.format("%s://%s:%s%s", schema, host, port, user.getAvatar()),
                "success", username);
        return Result.success(auv);
    }

    /**
     * 根据MIME类型获取文件扩展名
     *
     * @param mimeType MIME类型
     * @return 文件扩展名
     * @throws BusinessException 当MIME类型不支持时抛出
     */
    private String getFileExtensionFromMimeType(String mimeType) {
        try {
            return MimeTypeEnum.getExtensionByMimeType(mimeType);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported MIME type: {}", mimeType);
            throw new BusinessException(ResponseCode.PARAM_ERROR, "不支持的文件类型: " + mimeType);
        }
    }

}