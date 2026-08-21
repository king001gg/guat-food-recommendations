package com.guatfood.controller;

import com.guatfood.common.Result;
import com.guatfood.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class FileController {

    @Autowired
    private FileService fileService;

    /** 上传图片 */
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadImage(file);
        return Result.success("上传成功", url);
    }
}
