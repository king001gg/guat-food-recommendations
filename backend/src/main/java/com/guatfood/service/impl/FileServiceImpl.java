package com.guatfood.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.guatfood.common.BusinessException;
import com.guatfood.service.FileService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@Service
public class FileServiceImpl implements FileService {

    private static final Log log = LogFactory.get();

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext + ", 仅支持: " + ALLOWED_EXT);
        }

        String fileName = IdUtil.fastSimpleUUID() + "." + ext;
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(dir, fileName);
        try {
            Thumbnails.of(file.getInputStream())
                    .size(1920, 1920)
                    .keepAspectRatio(true)
                    .outputQuality(0.85)
                    .toFile(destFile);
        } catch (IOException e) {
            log.error("图片上传失败: {}", e.getMessage());
            throw new BusinessException("图片上传失败，请检查文件是否为有效图片");
        }

        return "/uploads/" + fileName;
    }
}
