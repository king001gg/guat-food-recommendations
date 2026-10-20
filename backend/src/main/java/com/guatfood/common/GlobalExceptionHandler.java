package com.guatfood.common;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.guatfood.security.RateLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器。
 *
 * <p>业务异常仍返回统一 Result，HTTP 状态码由 {@link ResponseStatusAdvice} 映射；
 * 框架级异常在这里补齐 400/404/405/415/409/429，避免全部变成 200。</p>
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

    /** 限流异常：429 + Retry-After，不能使用 401/403，否则前端会误判为登录失效 */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Result<?>> handleRateLimit(RateLimitExceededException e) {
        log.warn("请求被限流: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .header("X-RateLimit-Remaining", "0")
                .body(Result.error(429, e.getMessage()));
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

    /** 表单绑定 / 查询参数校验异常 */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.error(400, msg);
    }

    /** 方法参数约束校验（如 @RequestParam @Min） */
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public Result<?> handleConstraintViolation(Exception e) {
        return Result.error(400, "参数校验失败");
    }

    /** 缺少必填查询参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParameter(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少参数: " + e.getParameterName());
    }

    /** 缺少 multipart 文件字段 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<?> handleMissingPart(MissingServletRequestPartException e) {
        return Result.error(400, "缺少文件字段: " + e.getRequestPartName());
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

    /** 不支持的 HTTP 方法 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "不支持的请求方法");
    }

    /** 不支持的 Content-Type */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<?> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return Result.error(415, "不支持的请求格式");
    }

    /** 上传文件超过大小限制 / 其他 multipart 错误 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.error(400, "文件大小超出限制");
    }

    @ExceptionHandler(MultipartException.class)
    public Result<?> handleMultipart(MultipartException e) {
        return Result.error(400, "文件上传请求格式错误");
    }

    /** 静态资源 / 接口不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResource(NoResourceFoundException e) {
        return Result.error(404, "资源不存在");
    }

    /** 唯一索引等数据冲突（并发注册、重复操作兜底） */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("数据冲突: {}", e.getMostSpecificCause().getMessage());
        return Result.error(409, "数据冲突，请刷新后重试");
    }

    /** 全局兜底（不向客户端暴露内部错误详情） */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "服务器内部错误");
    }
}
