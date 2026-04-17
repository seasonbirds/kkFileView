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

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpServletRequest request) {
        if (!UserConfigConstants.isUserAuthEnabled()) {
            return "redirect:/index";
        }

        String token = getTokenFromCookie(request);
        if (StringUtils.isNotBlank(token) && userService.validateToken(token)) {
            return "redirect:/index";
        }

        String baseUrl = BaseUrlFilter.getBaseUrl();
        if (StringUtils.isNotBlank(baseUrl)) {
            model.addAttribute("baseUrl", baseUrl);
        } else {
            model.addAttribute("baseUrl", request.getContextPath() + "/");
        }

        return "/main/login";
    }

    @PostMapping("/doLogin")
    @ResponseBody
    public ReturnResponse<Object> doLogin(String phone, String password, 
                                           HttpServletRequest request, 
                                           HttpServletResponse response) {
        if (!UserConfigConstants.isUserAuthEnabled()) {
            return ReturnResponse.success("用户认证未启用");
        }

        if (StringUtils.isBlank(phone) || StringUtils.isBlank(password)) {
            return ReturnResponse.failure("手机号或密码不能为空");
        }

        String token = userService.login(phone, password);
        if (StringUtils.isBlank(token)) {
            logger.warn("用户登录失败: phone={}", phone);
            return ReturnResponse.failure("手机号或密码错误");
        }

        Cookie cookie = new Cookie(LoginFilter.LOGIN_COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setMaxAge(UserConfigConstants.getTokenExpireMinutes() * 60);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        logger.info("用户登录成功: phone={}", phone);
        return ReturnResponse.success(token);
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        String token = getTokenFromCookie(request);
        if (StringUtils.isNotBlank(token)) {
            userService.logout(token);
        }

        Cookie cookie = new Cookie(LoginFilter.LOGIN_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        logger.info("用户登出成功");
        return "redirect:/login";
    }

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
