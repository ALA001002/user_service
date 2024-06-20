package com.bigo.project.bigo.wallet.service.impl;

import com.bigo.common.utils.DictUtils;
import com.bigo.project.bigo.config.entity.ConfigSetting;
import com.bigo.project.bigo.config.util.ConfigSettingUtil;
import com.bigo.project.bigo.enums.AssetLogSubTypeEnum;
import com.bigo.project.bigo.enums.AssetLogTypeEnum;
import com.bigo.project.bigo.enums.CurrencyEnum;
import com.bigo.project.bigo.enums.WalletTypeEnum;
import com.bigo.project.bigo.userinfo.domain.BigoUser;
import com.bigo.project.bigo.userinfo.mapper.BigoUserMapper;
import com.bigo.project.bigo.userinfo.service.ILevelConfigService;
import com.bigo.project.bigo.wallet.dao.TransactionRepository;
import com.bigo.project.bigo.wallet.domain.AssetChange;
import com.bigo.project.bigo.wallet.domain.PushData;
import com.bigo.project.bigo.wallet.domain.TronTransaction;
import com.bigo.project.bigo.wallet.domain.Withdraw;
import com.bigo.project.bigo.wallet.entity.WithdrawEntity;
import com.bigo.project.bigo.wallet.jpaEntity.Transaction;
import com.bigo.project.bigo.wallet.mapper.TronTransactionMapper;
import com.bigo.project.bigo.wallet.mapper.WithdrawMapper;
import com.bigo.project.bigo.wallet.service.AsyncService;
import com.bigo.project.bigo.wallet.service.INotifyService;
import com.bigo.project.bigo.wallet.service.IWalletService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;


@Service
@Slf4j
public class NotifyServiceImpl implements INotifyService {

    @Resource
    IWalletService iWalletService;

    @Resource
    WithdrawMapper withdrawMapper;

    @Resource
    TransactionRepository transactionRepository;

    @Resource
    TronTransactionMapper tronTransactionMapper;

    @Resource
    BigoUserMapper userMapper;

    @Resource
    AsyncService asyncService;


    @Transactional
    public String notify(PushData pushData) {
        log.info("notify={}",pushData);
        String method = pushData.getMethod();
        String symbol = pushData.getSymbol();
        String txId = pushData.getTxId();
        if(!"changeBalance".equals(method)){
            return "NOT MATCH";
        }
        Transaction firstByTxid = transactionRepository.findFirstByTxid(txId);
        if(firstByTxid==null){
            return "NOT EXIST";
        }
        boolean score = Optional.ofNullable(firstByTxid.getScore()).orElse(false);
        if(score){
            return "has score";
        }
        boolean test = Optional.ofNullable(pushData.getTest()).orElse(false);
        if("CCT".equals(symbol) && test){
            symbol = "USDT";
        }
        Long uid = pushData.getUid();
        String fromAddress = pushData.getFromAddress();
        String address = pushData.getAddress();
        BigDecimal rechargeBalance = pushData.getBalance();

        String rechargeMinStr =  DictUtils.getDictValue("recharge_amount_limit", "min", "0");
        BigDecimal rechargeMin = new BigDecimal(rechargeMinStr);
        if(rechargeBalance.compareTo(rechargeMin) < 0) return "Recharge amount less than"+rechargeMinStr;

        //判断首充奖励是否开启
        Integer firstRechargeStatus = ConfigSettingUtil.getFirstRechargeStatus();
        BigDecimal firstRechargeComplyAmount = ConfigSettingUtil.getFirstRechargeComplyAmount();
        if(symbol.equals(CurrencyEnum.USDT.getCode())
                && firstRechargeStatus == 1
                && pushData.getBalance().compareTo(firstRechargeComplyAmount) >=0 ) { //开启
            //判断是否首充
            WithdrawEntity param = new WithdrawEntity();
            param.setUid(uid);
            param.setType(4);
            param.setCoin(symbol);
            param.setStatus(1);
            param.setCheckStatus(1);
            BigDecimal rechargeAmount = withdrawMapper.getWithdraAmount(param);
            if(rechargeAmount.compareTo(BigDecimal.ZERO) == 0) { //首充
                BigoUser user = userMapper.getUserByUid(uid);
                //奖励金额
                BigDecimal firstRechargeRewards =  ConfigSettingUtil.getFirstRechargeRewards();
                //奖励金额
//                rechargeBalance = rechargeBalance.add(firstRechargeRewards);
                AssetChange firstRecharge = AssetChange.builder()
                        .amount(firstRechargeRewards)
                        .uid(uid)
                        .dim(0).type(AssetLogTypeEnum.CASH_IN_DRAW)
                        .subType(AssetLogSubTypeEnum.CASH_IN_OUTSIDE)
                        .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                        .currency(symbol)
                        .build();
                iWalletService.changeAsset(firstRecharge);
                // 上级奖励金额
                BigDecimal firstRechargeSuperRewards = ConfigSettingUtil.getFirstRechargeSuperRewards();
                if(firstRechargeSuperRewards.compareTo(BigDecimal.ZERO) > 0) {
                    AssetChange assetChange = AssetChange.builder()
                            .amount(firstRechargeSuperRewards)
                            .uid(user.getParentUid())
                            .dim(0)
                            .type(AssetLogTypeEnum.CASH_IN_DRAW)
                            .subType(AssetLogSubTypeEnum.FIRST_BACK)
                            .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                            .currency(symbol)
                            .build();
                    iWalletService.changeAsset(assetChange);
                }
            }
        }

        AssetChange assetChange = AssetChange.builder()
                .amount(rechargeBalance)
                .uid(uid)
                .dim(0).type(AssetLogTypeEnum.CASH_IN)
                .subType(AssetLogSubTypeEnum.CASH_IN_OUTSIDE)
                .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                .currency(symbol)
                .build();
        iWalletService.changeAsset(assetChange);

        Withdraw inLog = new Withdraw();
        inLog.setCoin("USDT");
        inLog.setUid(uid);
        //外冲-入
        inLog.setType(4);
        inLog.setFrom(fromAddress);
        inLog.setToAddress(address);
        inLog.setMoney(pushData.getBalance());
        inLog.setFee(BigDecimal.ZERO);
        inLog.setStatus(1);
        inLog.setCheckStatus(1);
        inLog.setRemark("TRC recharge");
        inLog.setVerifyTime(new Date());
        inLog.setHash(txId);
        withdrawMapper.insert(inLog);
        TronTransaction tronTransaction = new TronTransaction();
        tronTransaction.setTxid(txId);
        tronTransaction.setScore(1);
        tronTransactionMapper.updateScore(tronTransaction);

        return "SUCCESS";
    }

   /* @Transactional
    public String notify(PushData pushData) {
        log.info("notify={}",pushData);
        String method = pushData.getMethod();
        String symbol = pushData.getSymbol();
        String txId = pushData.getTxId();
        if(!"changeBalance".equals(method)){
            return "NOT MATCH";
        }
        Transaction firstByTxid = transactionRepository.findFirstByTxid(txId);
        if(firstByTxid==null){
            return "NOT EXIST";
        }
        boolean score = Optional.ofNullable(firstByTxid.getScore()).orElse(false);
        if(score){
           return "has score";
        }
        boolean test = Optional.ofNullable(pushData.getTest()).orElse(false);
        if("CCT".equals(symbol) && test){
            symbol = "USDT";
        }
        Long uid = pushData.getUid();
        String fromAddress = pushData.getFromAddress();
        String address = pushData.getAddress();
        BigDecimal rechargeBalance = pushData.getBalance();

        //判断首充奖励是否开启
        Integer firstRechargeStatus = ConfigSettingUtil.getFirstRechargeStatus();
        BigDecimal firstRechargeComplyAmount = ConfigSettingUtil.getFirstRechargeComplyAmount();

            //判断是否首充
            WithdrawEntity param = new WithdrawEntity();
            param.setUid(uid);
            param.setType(4);
            param.setCoin(symbol);
            param.setStatus(1);
            param.setCheckStatus(1);
            BigDecimal rechargeAmount = withdrawMapper.getWithdraAmount(param);
            if(rechargeAmount.compareTo(BigDecimal.ZERO) == 0) { //首充
                BigoUser user = userMapper.getUserByUid(uid);

                //首充返佣

                if(symbol.equals(CurrencyEnum.USDT.getCode())
                        && firstRechargeStatus == 1
                        && pushData.getBalance().compareTo(firstRechargeComplyAmount) >=0 ) { //开启奖励
                //奖励金额
                BigDecimal firstRechargeRewards =  ConfigSettingUtil.getFirstRechargeRewards();
                //奖励金额
//                rechargeBalance = rechargeBalance.add(firstRechargeRewards);
                AssetChange firstRecharge = AssetChange.builder()
                        .amount(firstRechargeRewards)
                        .uid(uid)
                        .dim(0).type(AssetLogTypeEnum.CASH_IN_DRAW)
                        .subType(AssetLogSubTypeEnum.CASH_IN_OUTSIDE)
                        .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                        .currency(symbol)
                        .build();
                iWalletService.changeAsset(firstRecharge);
                // 上级奖励金额
                BigDecimal firstRechargeSuperRewards = ConfigSettingUtil.getFirstRechargeSuperRewards();
                if(firstRechargeSuperRewards.compareTo(BigDecimal.ZERO) > 0) {
                    AssetChange assetChange = AssetChange.builder()
                            .amount(firstRechargeSuperRewards)
                            .uid(user.getParentUid())
                            .dim(0)
                            .type(AssetLogTypeEnum.CASH_IN_DRAW)
                            .subType(AssetLogSubTypeEnum.FIRST_BACK)
                            .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                            .currency(symbol)
                            .build();
                    iWalletService.changeAsset(assetChange);
                }
            }
        }

        AssetChange assetChange = AssetChange.builder()
                .amount(rechargeBalance)
                .uid(uid)
                .dim(0).type(AssetLogTypeEnum.CASH_IN)
                .subType(AssetLogSubTypeEnum.CASH_IN_OUTSIDE)
                .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                .currency(symbol)
                .build();
        iWalletService.changeAsset(assetChange);

        Withdraw inLog = new Withdraw();
        inLog.setCoin("USDT");
        inLog.setUid(uid);
        //外冲-入
        inLog.setType(4);
        inLog.setFrom(fromAddress);
        inLog.setToAddress(address);
        inLog.setMoney(pushData.getBalance());
        inLog.setFee(BigDecimal.ZERO);
        inLog.setStatus(1);
        inLog.setCheckStatus(1);
        inLog.setRemark("TRC recharge");
        inLog.setVerifyTime(new Date());
        inLog.setHash(txId);
        withdrawMapper.insert(inLog);
        TronTransaction tronTransaction = new TronTransaction();
        tronTransaction.setTxid(txId);
        tronTransaction.setScore(1);
        tronTransactionMapper.updateScore(tronTransaction);

        return "SUCCESS";
    }*/
}
