package com.guatfood.common;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 将统一响应体中的业务码映射为真实 HTTP 状态码。
 *
 * <p>保留 {code,message,data} 响应结构以兼容旧前端，同时让 Nginx、监控和
 * 客户端能够按 4xx/5xx 正确处理（登录失败 401、无权限 403、重复 409、限流 429）。</p>
 */
@RestControllerAdvice
public class ResponseStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result<?> result) {
            response.setStatusCode(toHttpStatus(result.getCode()));
        }
        return body;
    }

    static HttpStatus toHttpStatus(int code) {
        if (code >= 200 && code < 400) {
            HttpStatus status = HttpStatus.resolve(code);
            return status != null ? status : HttpStatus.OK;
        }
        if (code >= 400 && code < 600) {
            HttpStatus status = HttpStatus.resolve(code);
            if (status != null) {
                return status;
            }
        }
        return code >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
    }
}
