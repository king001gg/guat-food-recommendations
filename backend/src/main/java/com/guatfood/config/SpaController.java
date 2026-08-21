package com.guatfood.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA history 模式回退。
 *
 * <p>前端使用 Vue Router 的 history 模式，刷新 /login、/admin 等深链时，
 * 后端没有对应接口，需要把这些「无后缀」的前端路由统一回退到 index.html，
 * 由前端路由接管。（/api、/uploads 由各自的 Controller / 静态资源映射处理，
 * 不在此列。）</p>
 */
@Controller
public class SpaController {

    @GetMapping(value = {"/{path:[^\\.]*}", "/windows/**", "/dishes/**", "/admin/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
