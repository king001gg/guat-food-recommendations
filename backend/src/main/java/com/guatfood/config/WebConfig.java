package com.guatfood.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置: CORS / 静态资源 / 拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    /** CORS 跨域 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /** 上传文件静态资源映射 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = uploadPath;
        if (!path.endsWith("/")) path += "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path);
    }

    /** JWT 拦截器: 拦截所有 /api/**，GET 放行，POST/PUT/DELETE 需认证 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register"
                );
    }
}
