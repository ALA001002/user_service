package com.bigo.project.bigo.wallet.service;

import com.bigo.common.utils.StringUtils;
import com.bigo.common.utils.file.FileUploadUtils;
import com.bigo.framework.config.RuoYiConfig;
import com.bigo.framework.security.LoginUser;
import com.bigo.framework.security.service.TokenService;
import com.bigo.project.bigo.config.util.ConfigSettingUtil;
import com.bigo.project.bigo.enums.*;
import com.bigo.project.bigo.userinfo.domain.BigoUser;
import com.bigo.project.bigo.userinfo.domain.LevelConfig;
import com.bigo.project.bigo.userinfo.service.IBigoUserService;
import com.bigo.project.bigo.userinfo.service.ILevelConfigService;
import com.bigo.project.bigo.wallet.domain.AssetChange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class AsyncService {
    @Autowired
    private IBigoUserService bigoUserService;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private TokenService tokenService;

    @Resource
    ILevelConfigService levelConfigService;

    /**
     * 层级分销
     * @param uid
     * @param amount
     */
    @Async
    public void levelRebate(Long uid, BigDecimal amount) {
        log.info("==========异步调用分销处理===================");
//        BigoUser user = bigoUserService.getUserByUid(uid);
        BigoUser user = null;
        //从下往上查出关系链
        // 获取所有的上级
        List<BigoUser> parents = bigoUserService.listParentUids(uid);
        if(parents == null || parents.size() <= 0) return;
        for (BigoUser parent : parents) {
            if(parent.getUid().equals(uid)) {
                user = parent;
                break;
            }
        }
        if(user == null || user.getParentUid() == null) return;
        // 分销最高层次
        Integer mostRebateLevel = ConfigSettingUtil.getMostRebateLevel();
        // 分销开始层次,默认1层开始
        int level = 1;
//        log.info("levelRebate={},mostRebateLevel={},parents={}",level,mostRebateLevel,parents.size());
        rebate(amount, user, parents, mostRebateLevel, level, uid);
    }

    @Transactional
    public void rebate(BigDecimal amount, BigoUser user, List<BigoUser> parents, Integer mostRebateLevel, int level, Long orderUid) {
        if(level > mostRebateLevel) return;
        // 获取当前层级分销比例
        BigDecimal levelRebate = ConfigSettingUtil.getLevelRebate(level).divide(new BigDecimal(100));
        // 上级ID
        BigoUser parentUser = null;
        for (BigoUser parent : parents) {
            if(user.getParentUid().equals(parent.getUid())) {
                parentUser = parent;
                break;
            }
        }
        if(parentUser == null) return;
        // 如果上级用户的parentUid和topUid是空的，代表他是代理商，则不进行下一步
        if(parentUser.getParentUid() == null && parentUser.getTopUid() == null) return;
        // 处理分销返利
        AssetLogSubTypeEnum assetLogSubTypeEnum = null;
        if(1 == level) {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.FIRST_BACK;
        }else  if(2 == level) {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.SECOND_BACK;
        }else  if(3 == level) {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.THIRD_BACK;
        }else {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.EXTRA_BACK;
        }
        // 返利数量
        BigDecimal rebateNum = amount.multiply(levelRebate);
//        AssetChange firstChange = AssetChange.builder().uid(parentUser.getUid())
//                .currency(CurrencyEnum.USDT.getCode())
//                .dim(0)
//                .type(AssetLogTypeEnum.RAKE_BACK)
//                .subType(assetLogSubTypeEnum)
//                .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
//                .amount(rebateNum.setScale(4,BigDecimal.ROUND_HALF_UP))
//                .amountType(AmountTypeEnum.BANLANCE.getType())
//                .build();
//        walletService.changeAsset(firstChange);

        walletService.retryLockChange(rebateNum.setScale(4,BigDecimal.ROUND_HALF_UP), CurrencyEnum.USDT.getCode(),
                parentUser.getUid(), WalletTypeEnum.CAPITAL_ACCOUNT.getType(), 0, AssetLogTypeEnum.RAKE_BACK, assetLogSubTypeEnum);

        log.info("下单Uid:{},返佣层级:{},返佣Uid:{}", orderUid, level, parentUser.getUid());
        level ++;
        rebate(amount, parentUser, parents, mostRebateLevel, level, orderUid);
    }


    /**
     * 层级分销
     * @param uid
     * @param amount
     */
    @Async
    public void rechargeLevelRebate(Long uid, BigDecimal amount) {
        log.info("==========异步调用分销处理===================");
//        BigoUser user = bigoUserService.getUserByUid(uid);
        BigoUser user = null;
        //从下往上查出关系链
        // 获取所有的上级
        List<BigoUser> parents = bigoUserService.listParentUids(uid);
        if(parents == null || parents.size() <= 0) return;
        for (BigoUser parent : parents) {
            if(parent.getUid().equals(uid)) {
                user = parent;
                break;
            }
        }
        if(user == null || user.getParentUid() == null) return;

        List<LevelConfig> levelConfigList = levelConfigService.selectLevelConfigList(new LevelConfig());
        // 分销最高层次
//        Integer mostRebateLevel = ConfigSettingUtil.getMostRebateLevel();
        // 分销开始层次,默认1层开始
        int level = 1;

        rechargeRebate(amount, user, parents, 3, level, uid,levelConfigList);
    }

    @Transactional
    public void rechargeRebate(BigDecimal amount, BigoUser user, List<BigoUser> parents, Integer mostRebateLevel, int level, Long orderUid,List<LevelConfig> levelConfigList) {
        if(level > mostRebateLevel) return;
        // 获取当前层级分销比例
//        BigDecimal levelRebate = ConfigSettingUtil.getLevelRebate(level).divide(new BigDecimal(100));
        // 上级ID
        BigoUser parentUser = null;
        for (BigoUser parent : parents) {
            if(user.getParentUid().equals(parent.getUid())) {
                parentUser = parent;
                break;
            }
        }
        if(parentUser == null) return;
        // 如果上级用户的parentUid和topUid是空的，代表他是代理商，则不进行下一步
        if(parentUser.getParentUid() == null && parentUser.getTopUid() == null) return;
        LevelConfig config = null;
        for (LevelConfig levelConfig : levelConfigList) {
            if(levelConfig.getLevel().equals(parentUser.getLevel())) {
                config = levelConfig;
                break;
            }
        }
        String[] rebateArray = config.getRebate().split(",");
        String levelRebateStr = rebateArray[level-1];
        BigDecimal levelRebate = new BigDecimal(levelRebateStr);


        // 处理分销返利
        AssetLogSubTypeEnum assetLogSubTypeEnum = null;
        if(1 == level) {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.FIRST_BACK;
        }else  if(2 == level) {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.SECOND_BACK;
        }else  if(3 == level) {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.THIRD_BACK;
        }else {
            assetLogSubTypeEnum = AssetLogSubTypeEnum.EXTRA_BACK;
        }
        // 返利数量
        BigDecimal rebateNum = amount.multiply(levelRebate);
        AssetChange firstChange = AssetChange.builder().uid(parentUser.getUid())
                .currency(CurrencyEnum.USDT.getCode())
                .dim(0)
                .type(AssetLogTypeEnum.RAKE_BACK)
                .subType(assetLogSubTypeEnum)
                .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                .amount(rebateNum.setScale(4,BigDecimal.ROUND_HALF_UP))
                .amountType(AmountTypeEnum.BANLANCE.getType())
                .build();
        walletService.changeAsset(firstChange);
        log.info("下单Uid:{},返佣层级:{},返佣Uid:{}", orderUid, level, parentUser.getUid());
        level ++;
        rebate(amount, parentUser, parents, mostRebateLevel, level, orderUid);
    }

    @Async
    @Transactional
    public void uploadAuth(BigoUser user, MultipartFile[] files, String realName, String country, String driverLicense, Integer authType, String idNum, String passport, HttpServletRequest request) {
        log.info("==========异步调用上传认证图片开始,用户id:{}===================",user.getUid());
        List<String> authImgList = new ArrayList<>();
        try {
            for(int i=0; i<files.length; i++){
                MultipartFile file = files[i];
                if(file.getSize() > FileUploadUtils.AUTH_IMG_MAX_SIZE){
                    continue;
                }
                    /*String suffix = FileUploadUtils.getExtension(file);
                    if (!"jpg".equalsIgnoreCase(suffix) && !"png".equalsIgnoreCase(suffix)) {
                        log.info("用户：{} 上传文件失败，文件格式不正确:{}",user.getUid(), suffix);
                        return AjaxResult.error("incorrect_file_format");
                    }

                    InputStream inputStream = file.getInputStream();
                    // 获取到上传文件的文件头信息
                    boolean isExcel = FileUploadUtils.checkIsImage(inputStream);
                    if (!isExcel) {
                        log.info("用户：{} 上传文件失败，文件格式不正确",user.getUid());
                        return AjaxResult.error("incorrect_file_format");
                    }*/

                // 上传文件路径
                String filePath = RuoYiConfig.getUploadPath();
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                String url = RuoYiConfig.getFileUrl() + fileName;
                authImgList.add(url);
            }
            BigoUser updateUser = new BigoUser();
            updateUser.setUid(user.getUid());
            updateUser.setAuthStatus(1);
            updateUser.setAuthPhotos(StringUtils.join(authImgList, ";"));
            updateUser.setRealName(realName);
            updateUser.setIdNum(idNum);
            updateUser.setPassport(passport);
            updateUser.setCountry(country);
            updateUser.setDriverLicense(driverLicense);
            updateUser.setAuthType(authType);
            updateUser.setUpdateTime(new Date());
            bigoUserService.updateUser(updateUser);
            refreshUserInfo(request, user.getUid());
            log.info("==========异步调用上传认证图片成功,用户id:{}===================",user.getUid());
        } catch (Exception e) {
            log.error("用户：{} 上传身份认证图片失败",user.getUid(),e);
        }
    }

    @Async
    @Transactional
    public void uploadAuthV2(BigoUser user, String files, String realName, String country, String driverLicense, Integer authType, String idNum, String passport, HttpServletRequest request) {
        log.info("==========异步调用上传认证图片开始,用户id:{}===================",user.getUid());
        List<String> authImgList = new ArrayList<>();
        try {
            BigoUser updateUser = new BigoUser();
            updateUser.setUid(user.getUid());
            updateUser.setAuthStatus(1);
            updateUser.setAuthPhotos(files);
            updateUser.setRealName(realName);
            updateUser.setIdNum(idNum);
            updateUser.setPassport(passport);
            updateUser.setCountry(country);
            updateUser.setDriverLicense(driverLicense);
            updateUser.setAuthType(authType);
            bigoUserService.updateUser(updateUser);
            refreshUserInfo(request, user.getUid());
            log.info("==========异步调用上传认证图片成功,用户id:{}===================",user.getUid());
        } catch (Exception e) {
            log.error("用户：{} 上传身份认证图片失败",user.getUid(),e);
        }
    }

    private void refreshUserInfo(HttpServletRequest request, Long uid){
        //刷新缓存
        BigoUser newUser = bigoUserService.getUserByUid(uid);
        LoginUser loginUser = tokenService.getLoginUser(request);
        loginUser.setBigoUser(newUser);
        tokenService.refreshToken(loginUser);
    }
}
