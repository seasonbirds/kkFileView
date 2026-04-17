package cn.keking.web.controller;

import cn.keking.config.UserConfigConstants;
import cn.keking.model.ReturnResponse;
import cn.keking.service.UserService;
import cn.keking.web.filter.BaseUrlFilter;
import cn.keking.web.filter.LoginFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 登录控制器
 * 提供用户登录页面、登录接口和登出接口
 */
@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * 用户服务
     */
    private final UserService userService;

    /**
     * 构造函数注入UserService
     * 
     * @param userService 用户服务
     */
    public LoginController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 登录页面
     * 如果用户已登录，则重定向到首页
     * 
     * @param model 模型对象
     * @param request HTTP请求对象
     * @return 登录页面视图或重定向到首页
     */
    @GetMapping("/login")
    public String loginPage(Model model, HttpServletRequest request) {
        // 检查用户是否已登录
        String token = getTokenFromCookie(request);
        if (StringUtils.isNotBlank(token) && userService.validateToken(token)) {
            // 已登录，重定向到首页
            return "redirect:/index";
        }

        // 返回登录页面
        // baseUrl已通过BaseUrlFilter设置到request中，Freemarker配置了expose-request-attributes=true，会自动暴露到模板中
        return "/main/login";
    }

    /**
     * 用户登录接口
     * 验证用户名和密码，成功后生成Token并设置到Cookie中
     * 
     * @param phone 手机号
     * @param password 密码
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @return 登录结果（ReturnResponse对象）
     */
    @PostMapping("/doLogin")
    @ResponseBody
    public ReturnResponse<Object> doLogin(String phone, String password, 
                                           HttpServletRequest request, 
                                           HttpServletResponse response) {
        // 参数校验
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(password)) {
            return ReturnResponse.failure("手机号或密码不能为空");
        }

        // 调用用户服务进行登录验证
        String token = userService.login(phone, password);
        if (StringUtils.isBlank(token)) {
            // 登录失败
            logger.warn("用户登录失败: phone={}", phone);
            return ReturnResponse.failure("手机号或密码错误");
        }

        // 登录成功，将Token设置到Cookie中
        Cookie cookie = new Cookie(LoginFilter.LOGIN_COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setMaxAge(UserConfigConstants.getTokenExpireMinutes() * 60); // Cookie过期时间（秒）
        cookie.setHttpOnly(true); // 防止XSS攻击
        response.addCookie(cookie);

        // 记录登录成功日志
        logger.info("用户登录成功: phone={}", phone);
        
        // 返回成功响应
        return ReturnResponse.success(token);
    }

    /**
     * 用户登出接口
     * 清除Redis中的Token和Cookie
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @return 重定向到登录页面
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // 从Cookie中获取Token
        String token = getTokenFromCookie(request);
        
        // 清除Redis中的Token
        if (StringUtils.isNotBlank(token)) {
            userService.logout(token);
        }

        // 清除Cookie
        Cookie cookie = new Cookie(LoginFilter.LOGIN_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0); // 立即过期
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        // 记录登出日志
        logger.info("用户登出成功");
        
        // 重定向到登录页面
        return "redirect:/login";
    }

    /**
     * 从Cookie中获取Token
     * 
     * @param request HTTP请求对象
     * @return Token字符串，未找到返回null
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (LoginFilter.LOGIN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
