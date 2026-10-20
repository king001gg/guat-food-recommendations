package com.guatfood.config;

import com.guatfood.security.RateLimitInterceptor;
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

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

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
        // 内置的种子图片（打包在 jar 内，部署时无需额外复制）
        registry.addResourceHandler("/uploads/seed/**")
                .addResourceLocations("classpath:/seed-images/");

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
                )
                .order(0);

        // 限流放在 JWT 之后：上传/评分可拿到当前用户 ID；登录/注册不经过 JWT，仍会执行
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/upload/image",
                        "/api/ratings"
                )
                .order(1);
    }
}
