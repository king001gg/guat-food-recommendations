package com.guatfood.common;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Log log = LogFactory.get();

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.error(400, msg);
    }

    /** 请求体 JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error(400, "请求体格式错误");
    }

    /** 路径/查询参数类型转换失败 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.error(400, "参数格式错误");
    }

    /** 上传文件超过大小限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.error(400, "文件大小超出限制");
    }

    /** 全局兜底（不向客户端暴露内部错误详情） */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "服务器内部错误");
    }
}
