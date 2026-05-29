package com.itheima.config;

import com.itheima.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//import com.itheima.filter.TokenFilter;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.context.annotation.Bean;

/**
 * 配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")           // 拦截所有请求
                .excludePathPatterns("/login");   // 排除登录接口
    }

    // ========== Filter 配置（已注释，改用 Interceptor）==========
//    @Bean
//    public FilterRegistrationBean<TokenFilter> tokenFilter() {
//        FilterRegistrationBean<TokenFilter> registrationBean = new FilterRegistrationBean<>();
//        registrationBean.setFilter(new TokenFilter());
//        registrationBean.addUrlPatterns("/*"); // 拦截所有请求
//        registrationBean.setOrder(1); // 设置过滤器优先级
//        return registrationBean;
//    }
}
