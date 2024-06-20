package com.bigo.project.bigo.userinfo.service.impl;

import java.util.Date;
import java.util.List;
import com.bigo.common.utils.DateUtils;
import com.bigo.framework.security.LoginUser;
import com.bigo.framework.security.service.TokenService;
import com.bigo.project.bigo.userinfo.domain.BigoUser;
import com.bigo.project.bigo.userinfo.service.IBigoUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bigo.project.bigo.userinfo.mapper.UserGoogleSecretMapper;
import com.bigo.project.bigo.userinfo.domain.UserGoogleSecret;
import com.bigo.project.bigo.userinfo.service.IUserGoogleSecretService;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户谷歌秘钥Service业务层处理
 * 
 * @author bigo
 * @date 2024-03-06
 */
@Service
@Slf4j
public class UserGoogleSecretServiceImpl implements IUserGoogleSecretService 
{
    @Autowired
    private UserGoogleSecretMapper userGoogleSecretMapper;

    @Autowired
    private IBigoUserService bigoUserService;

    @Autowired
    private TokenService tokenService;

    /**
     * 查询用户谷歌秘钥
     * 
     * @param id 用户谷歌秘钥ID
     * @return 用户谷歌秘钥
     */
    @Override
    public UserGoogleSecret selectUserGoogleSecretById(Long id)
    {
        return userGoogleSecretMapper.selectUserGoogleSecretById(id);
    }

    @Override
    public UserGoogleSecret selectUserGoogleSecretByUid(Long Uid)
    {
        return userGoogleSecretMapper.selectUserGoogleSecretByUid(Uid);
    }

    @Override
    @Transactional
    public void createUserSecret(HttpServletRequest request,BigoUser user, String googleSecretKey) {
        UserGoogleSecret insertSecret = new UserGoogleSecret();
        insertSecret.setUid(user.getUid());
        insertSecret.setGoogleSecretKey(googleSecretKey);
        insertSecret.setCreateTime(new Date());
        this.insertUserGoogleSecret(insertSecret);
        //修改用户状态
        BigoUser updateUser = new BigoUser();
        updateUser.setUid(user.getUid());
        updateUser.setGoogleSecretStatus(1);
        bigoUserService.updateUser(updateUser);
        refreshUserInfo(request,user.getUid());
    }


    private void refreshUserInfo(HttpServletRequest request, Long uid){
        //刷新缓存
        BigoUser newUser = bigoUserService.getUserByUid(uid);
        LoginUser loginUser = tokenService.getLoginUser(request);
        String token = loginUser.getToken();
        log.info("refreshUserInfo uid={},token={}",uid,token);
        loginUser.setBigoUser(newUser);
        tokenService.refreshToken(loginUser);
    }
    /**
     * 查询用户谷歌秘钥列表
     * 
     * @param userGoogleSecret 用户谷歌秘钥
     * @return 用户谷歌秘钥
     */
    @Override
    public List<UserGoogleSecret> selectUserGoogleSecretList(UserGoogleSecret userGoogleSecret)
    {
        return userGoogleSecretMapper.selectUserGoogleSecretList(userGoogleSecret);
    }

    /**
     * 新增用户谷歌秘钥
     * 
     * @param userGoogleSecret 用户谷歌秘钥
     * @return 结果
     */
    @Override
    public int insertUserGoogleSecret(UserGoogleSecret userGoogleSecret)
    {
        userGoogleSecret.setCreateTime(DateUtils.getNowDate());
        return userGoogleSecretMapper.insertUserGoogleSecret(userGoogleSecret);
    }

    /**
     * 修改用户谷歌秘钥
     * 
     * @param userGoogleSecret 用户谷歌秘钥
     * @return 结果
     */
    @Override
    public int updateUserGoogleSecret(UserGoogleSecret userGoogleSecret)
    {
        return userGoogleSecretMapper.updateUserGoogleSecret(userGoogleSecret);
    }

    /**
     * 批量删除用户谷歌秘钥
     * 
     * @param ids 需要删除的用户谷歌秘钥ID
     * @return 结果
     */
    @Override
    public int deleteUserGoogleSecretByIds(Long[] ids)
    {
        return userGoogleSecretMapper.deleteUserGoogleSecretByIds(ids);
    }

    /**
     * 删除用户谷歌秘钥信息
     * 
     * @param id 用户谷歌秘钥ID
     * @return 结果
     */
    @Override
    public int deleteUserGoogleSecretById(Long id)
    {
        return userGoogleSecretMapper.deleteUserGoogleSecretById(id);
    }
}
