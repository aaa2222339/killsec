package com.jary.kill.controller;

import com.google.code.kaptcha.Producer;
import com.jary.kill.entity.ItemKill;
import com.jary.kill.entity.KillDto;
import com.jary.kill.entity.User;
import com.jary.kill.service.ItemService;
import com.jary.kill.service.KillService;
import com.jary.kill.service.UserService;
import com.jary.kill.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements KillConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private KillService killService;

    @Autowired
    private ItemService itemService;

    private static final String prefix = "kill";


    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) { // 注册成功
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封邮件!");
            model.addAttribute("target", "/list");
            return "/operate-result";
        } else { // 注册失败
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/register";
        }
    }

    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 给验证码起个唯一的名字并把它返回给客户端的cookie
        String kaptchaOwner = KillUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        response.addCookie(cookie);
        // 将验证码的名字和验证码存入Redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // 将突图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    @RequestMapping(path = "/manager",method = RequestMethod.GET)
    public String manager(Model model){
        List<ItemKill> list=itemService.getKillItems();
        model.addAttribute("list",list);
        return "adminList";
    }

    @RequestMapping(path = "/denied",method = RequestMethod.GET)
    public String denied(Model model){
        model.addAttribute("msg","您无权登录此页面！");
        return "operate-result";
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String userName, String password, String code, boolean rememberme,
                        Model model, /*HttpSession session, */HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        // 检查验证码
        // 根据客户端cookie中的验证码名字找到redis里面的值
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        // 如果能找到，则验证码对，否则错
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/login";
        }

        // 检查账号,密码,并生成对应的登录凭证
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(userName, password, expiredSeconds);
        // 有生成凭证，则说明验证正确，将其和生存时间加入到客户端的cookie，否则登录失败
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/login";
        }
    }


    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext(); // 清理当前用户信息
        return "redirect:/login";
    }

    @RequestMapping(value = prefix+"/modify/{id}",method = RequestMethod.GET)
    public String delete(@PathVariable("id") int id,HttpSession session,Model model) throws Exception {
        ItemKill itemKill =  itemService.getKillDetail(id); // 获得信息
        model.addAttribute("item",itemKill);
        return "edit";
    }

    @RequestMapping(value = prefix+"/execute/{id}",method = RequestMethod.GET)
    public String execute(@PathVariable("id") int id,HttpSession session,Model model) throws Exception {
        User user = hostHolder.getUser();
        ItemKill detail=itemService.getKillDetail(id);
        Map map=killService.killItemV3(detail.getId(),user.getId());
        model.addAttribute("msg", map.get("msg"));
        return "operate-result";
    }

    // value={}:只要是网址有这些就匹配
    @RequestMapping(value = {"/","/index"},method = RequestMethod.GET)
    public String list(Model model) {
        //获取待秒杀商品列表
        List<ItemKill> list=itemService.getKillItems();
        model.addAttribute("list",list);
        return "list";
    }




//    @RequestMapping(value = "/detail/{id}",method = RequestMethod.GET)
//    public String detail(@PathVariable("id") int id,Model model){
//        ItemKill detail=itemService.getKillDetail(id);
//        model.addAttribute("detail",detail);
//        return "info";
//    }

}
