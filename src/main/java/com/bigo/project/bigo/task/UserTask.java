package com.bigo.project.bigo.task;

import com.bigo.common.utils.MarketSituationUtils;
import com.bigo.project.bigo.contract.service.IContractService;
import com.bigo.project.bigo.enums.CurrencyEnum;
import com.bigo.project.bigo.enums.SymbolEnum;
import com.bigo.project.bigo.enums.WalletTransactionStatusEnum;
import com.bigo.project.bigo.userinfo.domain.BigoUser;
import com.bigo.project.bigo.userinfo.domain.UserLevel;
import com.bigo.project.bigo.userinfo.service.IBigoUserService;
import com.bigo.project.bigo.userinfo.service.IUserLevelService;
import com.bigo.project.bigo.wallet.domain.WalletTransaction;
import com.bigo.project.bigo.wallet.service.IWalletService;
import com.bigo.project.bigo.wallet.service.IWalletTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * @author wenxm
 * @Description: 用户定时任务
 * @date 2019/7/27 下午9:50
 */
@Component("userTask")
@Slf4j
public class UserTask {

  /*  @Autowired
    private IBigoUserService userService;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private IContractService contractService;

    @Autowired
    private IUserLevelService levelService;



    *//**
     * 处理用户等级操作
     *//*
    public void userLevelTask() {
        List<BigoUser> userList = userService.listNeedLevelUpUser();
        if(CollectionUtils.isEmpty(userList)){
            return;
        }
        UserLevel level2 = levelService.getByLevel(2);
        UserLevel level3 = levelService.getByLevel(3);
        Integer feeLevel = null, userLevel = null;
        for(BigoUser user : userList){
            //判断推荐的用户是否有入金
            Integer ableUser = walletService.countUserHasMoney(user.getChildUids());
            if(ableUser >= level3.getRequireUser() ){
                userLevel = 3;
            }else if(user.getLevel() ==1 && ableUser >= level2.getRequireUser()){
                userLevel = 2;
            }
            if(userLevel == null){
                continue;
            }
            //手续费不够门槛则不升级
            BigDecimal totalFee = getContractFeeByUid(user.getUid());
            if(totalFee.compareTo(level3.getRequireFee()) >= 0){
                feeLevel = 3;
            }else if(totalFee.compareTo(level2.getRequireFee()) >= 0 && user.getLevel() ==1){
                feeLevel = 2;
            }
            if(feeLevel == null){
                continue;
            }
            Integer level = userLevel > feeLevel ? feeLevel : userLevel;
            BigoUser updateUser = new BigoUser();
            updateUser.setUid(user.getUid());
            updateUser.setLevel(level);
            userService.updateUser(updateUser);
        }
    }

    private BigDecimal getContractFeeByUid(Long uid){
        BigDecimal totalFeeUSDT = contractService.getTotalFeeByUid(uid, CurrencyEnum.USDT.getCode());
        BigDecimal totalFeeETH = contractService.getTotalFeeByUid(uid, CurrencyEnum.ETH.getCode());
        if(totalFeeETH.compareTo(BigDecimal.ZERO) > 0){
            BigDecimal curRate = MarketSituationUtils.getCurrentPriceBySymbol(SymbolEnum.ETHUSDT.getCode());
            BigDecimal ethToUSDT = totalFeeETH.multiply(curRate).setScale(8, RoundingMode.HALF_UP);
            totalFeeUSDT = totalFeeUSDT.add(ethToUSDT);
        }
        return totalFeeUSDT;
    }
*/

}
