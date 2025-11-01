package com.basis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basis.common.Result;
import com.basis.model.vo.FileMetaVo;
import com.basis.model.vo.CallbackBodyVo;
import com.basis.service.IFileService;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private IFileService fileService;

    @ApiOperation(value = "文件上传初始化")
    @PostMapping(value = "/upload/init", name = "文件上传初始化", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<?> uploadInit(@RequestBody(required = true) FileMetaVo vo) {
        return fileService.uploadInit(vo);
    }

    @ApiOperation(value = "文件上传完成回调")
    @PostMapping(value = "/upload/callback")
    public Result<?> uploadCallback(@RequestBody(required = true) CallbackBodyVo vo) {
        return fileService.uploadCallback(vo);
    }
}
