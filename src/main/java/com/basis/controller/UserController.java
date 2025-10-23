package com.basis.controller;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.annotations.ApiOperation;

import com.basis.common.Result;
import com.basis.model.vo.LoginVo;
import com.basis.model.vo.ProfileVo;
import com.basis.model.vo.RegisterVo;
import com.basis.model.vo.AvatarMetaVo;
import com.basis.model.vo.CallbackBodyVo;
import com.basis.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author IT 派同学
 * @since 2024-12-07
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;


    @GetMapping("/check")
    public Result<?> check() {
        log.info("login ---> {}", StpUtil.isLogin());
        log.info("result ----> {}", StpUtil.hasRole("SYS_ADMIN"));
        log.info("permission ----> {}", StpUtil.hasPermission("per:add"));
        log.info("permissions ----> {}", StpUtil.getPermissionList(1));
        return Result.success();
    }

    @ApiOperation(value = "获取用户信息")
    @GetMapping(value = "/get-profile", name = "获取用户信息", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<?> getProfile() {
        return userService.getProfile();
    }

    @ApiOperation(value = "更新用户信息")
    @PostMapping(value = "/update-profile", name = "更新用户信息", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<?> updateProfile(@RequestBody(required = false) ProfileVo vo) {
        return userService.updateProfile(vo);
    }


    @ApiOperation(value = "用户头像上传初始化")
    @PostMapping(value = "/avatar-upload/init")
    public Result<?> uploadInit(@RequestBody(required = true) AvatarMetaVo vo) {
        return userService.uploadInit(vo);
    }

    @ApiOperation(value = "用户头像上传完成回调")
    @PostMapping(value = "/avatar-upload/callback")
    public Result<?> uploadCallback(@RequestBody(required = true) CallbackBodyVo vo) {
        return userService.uploadCallback(vo);
    }

    @ApiOperation(value = "获取用头像url")
    @GetMapping(value = "/fetch-avatar")
    public Result<?> fetchAvatar() {
        return userService.fecthAvatar();
    }

}
