package com.jary.kill.config;

import com.jary.kill.util.KillConstant;
import com.jary.kill.util.KillUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements KillConstant {

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**"); // 忽略对resources目录下所有静态资源的访问
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权

        http.authorizeRequests()
                .antMatchers(
                        "/index",
                        "/",
                        "/kill/execute",
                        "/detail/**"
                )
                .hasAnyAuthority( // 只能是下面3类人能访问上面列出来的路径，其他人访问不了。
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN
                )
                .antMatchers(
                        "/manager",
                        "/kill/delete/**",
                        "/kill/modify/**"
                )
                .hasAnyAuthority( // 只能管理员访问上述路径
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll() // 其他请求都允许直接访问
                .and().csrf().disable();
        // springsecurity还能防止csrf攻击（第三方获取浏览器的cookie后，伪造客户端给服务器发送请求）：
        // 服务器给浏览器的表单里面返回一个隐藏的token值，再次post提交时，服务器要检查cookie和token两项，而第三方就没有token。
        // 此处禁掉


        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 如果用户没有登录
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {  // 如果客户端的请求是异步的，则返回json字符串
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(KillUtil.getJSONString(403, "你还没有登录哦!"));
                        } else { // 如果客户端的请求是同步的，则直接重定向
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 如果登录了，但是权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) { // 如果客户端的请求是异步的，则返回json字符串
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(KillUtil.getJSONString(403, "你没有访问此功能的权限!"));
                        } else { // 如果客户端的请求是同步的，则直接重定向
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });

        // Security底层默认会拦截/logout请求,进行退出处理，这里把它的默认改掉（骗它）
        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
        http.logout().logoutUrl("/securitylogout");
    }

}
