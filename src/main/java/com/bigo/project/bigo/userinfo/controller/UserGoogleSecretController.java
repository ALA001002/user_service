package com.bigo.project.bigo.userinfo.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bigo.framework.aspectj.lang.annotation.Log;
import com.bigo.framework.aspectj.lang.enums.BusinessType;
import com.bigo.project.bigo.userinfo.domain.UserGoogleSecret;
import com.bigo.project.bigo.userinfo.service.IUserGoogleSecretService;
import com.bigo.framework.web.controller.BaseController;
import com.bigo.framework.web.domain.AjaxResult;
import com.bigo.common.utils.poi.ExcelUtil;
import com.bigo.framework.web.page.TableDataInfo;

/**
 * 用户谷歌秘钥Controller
 * 
 * @author bigo
 * @date 2024-03-06
 */
@RestController
@RequestMapping("/userinfo/secret")
public class UserGoogleSecretController extends BaseController
{
    @Autowired
    private IUserGoogleSecretService userGoogleSecretService;

    /**
     * 查询用户谷歌秘钥列表
     */
    @PreAuthorize("@ss.hasPermi('userinfo:secret:list')")
    @GetMapping("/list")
    public TableDataInfo list(UserGoogleSecret userGoogleSecret)
    {
        startPage();
        List<UserGoogleSecret> list = userGoogleSecretService.selectUserGoogleSecretList(userGoogleSecret);
        return getDataTable(list);
    }

    /**
     * 导出用户谷歌秘钥列表
     */
    @PreAuthorize("@ss.hasPermi('userinfo:secret:export')")
    @Log(title = "用户谷歌秘钥", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(UserGoogleSecret userGoogleSecret)
    {
        List<UserGoogleSecret> list = userGoogleSecretService.selectUserGoogleSecretList(userGoogleSecret);
        ExcelUtil<UserGoogleSecret> util = new ExcelUtil<UserGoogleSecret>(UserGoogleSecret.class);
        return util.exportExcel(list, "secret");
    }

    /**
     * 获取用户谷歌秘钥详细信息
     */
    @PreAuthorize("@ss.hasPermi('userinfo:secret:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(userGoogleSecretService.selectUserGoogleSecretById(id));
    }

    /**
     * 新增用户谷歌秘钥
     */
    @PreAuthorize("@ss.hasPermi('userinfo:secret:add')")
    @Log(title = "用户谷歌秘钥", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserGoogleSecret userGoogleSecret)
    {
        return toAjax(userGoogleSecretService.insertUserGoogleSecret(userGoogleSecret));
    }

    /**
     * 修改用户谷歌秘钥
     */
    @PreAuthorize("@ss.hasPermi('userinfo:secret:edit')")
    @Log(title = "用户谷歌秘钥", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserGoogleSecret userGoogleSecret)
    {
        return toAjax(userGoogleSecretService.updateUserGoogleSecret(userGoogleSecret));
    }

    /**
     * 删除用户谷歌秘钥
     */
    @PreAuthorize("@ss.hasPermi('userinfo:secret:remove')")
    @Log(title = "用户谷歌秘钥", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(userGoogleSecretService.deleteUserGoogleSecretByIds(ids));
    }
}
