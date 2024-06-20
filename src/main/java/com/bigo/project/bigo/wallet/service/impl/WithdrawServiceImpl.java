package com.bigo.project.bigo.wallet.service.impl;

import com.alibaba.fastjson.JSON;
import com.bigo.common.exception.CustomException;
import com.bigo.common.utils.CoinUtils;
import com.bigo.common.utils.OkHttpUtil;
import com.bigo.project.bigo.api.dto.TransDTO;
import com.bigo.project.bigo.config.util.ConfigSettingUtil;
import com.bigo.project.bigo.enums.AssetLogSubTypeEnum;
import com.bigo.project.bigo.enums.AssetLogTypeEnum;
import com.bigo.project.bigo.enums.CurrencyEnum;
import com.bigo.project.bigo.enums.WalletTypeEnum;
import com.bigo.project.bigo.wallet.domain.AssetChange;
import com.bigo.project.bigo.wallet.domain.WalletAddress;
import com.bigo.project.bigo.wallet.domain.Withdraw;
import com.bigo.project.bigo.wallet.entity.WithdrawEntity;
import com.bigo.project.bigo.wallet.mapper.WalletAddressMapper;
import com.bigo.project.bigo.wallet.mapper.WithdrawMapper;
import com.bigo.project.bigo.wallet.service.IWalletService;
import com.bigo.project.bigo.wallet.service.IWithdrawService;
import com.bigo.project.bigo.wallet.view.TransferData;
import com.bigo.project.bigo.wallet.view.TrxResult;
import com.bigo.project.system.domain.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @description: 提现service实现
 * @author: wenxm
 * @date: 2020/6/27 14:37
 */
@Service
@Slf4j
public class WithdrawServiceImpl implements IWithdrawService {


    @Autowired
    private WithdrawMapper withdrawMapper;

    @Autowired
    private WalletAddressMapper walletAddressMapper;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private StringRedisTemplate redisTemplate;

//    @Autowired
//    private IWalletTransactionService transactionService;
//
//    @Autowired
//    private IUserLevelService levelService;
//
//    @Autowired
//    private IBigoUserService userService;
//
//    @Autowired
//    private BgUserDayBalanceService bgUserDayBalanceService;

    @Value("${config.trxService}")
    private String trxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean withdraw(Withdraw withdraw) {
        Long uid = withdraw.getUid();

        //获取今日提现次数
        Integer withdrawCount = withdrawMapper.getWithdrawCount(uid);
        if(withdrawCount >= ConfigSettingUtil.getWithdrawCount()) {
            throw new CustomException("the_number_of_withdrawals_has_been_capped");
        }
        boolean withdrawStatus = redisTemplate.opsForValue().setIfAbsent(uid+"_withdraw_count","1", 10, TimeUnit.SECONDS);
        if(!withdrawStatus){
            throw new CustomException("operation_failed_please_try_again");
        }

        //获取转出地址
        WalletAddress address = new WalletAddress();
        address.setAddress(withdraw.getToAddress());
        address.setCoin(withdraw.getCoin());
        address.setUid(uid);
        String fromAddress = walletAddressMapper.getAddressByCoin(address);
        BigDecimal fee = withdraw.getMoney().multiply(ConfigSettingUtil.getWithdrawFee().divide(new BigDecimal(100))).setScale(8, RoundingMode.HALF_UP); // 固定手续费
        withdraw.setFee(fee);
        //先扣减转出账户余额

        BigDecimal subMoney = withdraw.getMoney().subtract(fee); //

        withdraw.setStatus(3);
        withdraw.setCheckStatus(0);
        withdraw.setMoney(subMoney);
        walletService.lockChange(subMoney, withdraw.getCoin().toUpperCase(), uid,
                WalletTypeEnum.CAPITAL_ACCOUNT.getType(),1,AssetLogTypeEnum.CASH_OUT,AssetLogSubTypeEnum.CASH_OUT_OUTSIDE);
        //扣除手续费
        walletService.lockChange(fee, CurrencyEnum.USDT.getCode(), uid,
                WalletTypeEnum.CAPITAL_ACCOUNT.getType(),1,AssetLogTypeEnum.FEE, AssetLogSubTypeEnum.CASH_OUT_OUTSIDE);

        withdraw.setFrom(fromAddress);
        //插入提币记录
        withdrawMapper.insert(withdraw);
        return Boolean.TRUE;
    }



    @Override
    public int insert(Withdraw withdraw) {
        return withdrawMapper.insert(withdraw);
    }

    @Override
    public List<Withdraw> listWithdraw(Withdraw withdraw) {
        return withdrawMapper.listWithdraw(withdraw);
    }

    @Override
    public Withdraw getByTransactionId(Long transactionId) {
        Withdraw params = new Withdraw();
        params.setTransactionId(transactionId);
        return withdrawMapper.getByParam(params);
    }

    @Override
    public Withdraw getById(Long id) {
        Withdraw params = new Withdraw();
        params.setId(id);
        return withdrawMapper.getByParam(params);
    }

    @Override
    public int update(Withdraw withdraw) {
        return withdrawMapper.update(withdraw);
    }

    @Override
    public List<WithdrawEntity> listByEntity(WithdrawEntity entity) {
        return withdrawMapper.listByEntity(entity);
    }


    @Override
    public List<WithdrawEntity> withdrawListByEntity(WithdrawEntity entity) {
        return withdrawMapper.withdrawListByEntity(entity);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean checkWithdraw(Withdraw withdraw) {
        Withdraw params = new Withdraw();
        params.setId(withdraw.getId());
        params = withdrawMapper.getByParam(params);
        //审核通过
        if(withdraw.getCheckStatus() == 1){
            if(params.getType() ==1){//内转：入
                //获取转出地址
                WalletAddress address = new WalletAddress();
                address.setAddress(params.getToAddress());
                address.setCoin(params.getCoin());
                WalletAddress toAddress = walletAddressMapper.getByAddressAndCoin(address);

                /*AssetChange toChange = AssetChange.builder().uid(toAddress.getUid())
                        .currency(toAddress.getCoin())
                        .dim(0)
                        .type(AssetLogTypeEnum.CASH_IN)
                        .subType(AssetLogSubTypeEnum.CASH_IN_INSIDE)
                        .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                        .amount(params.getMoney())
                        .build();
                walletService.changeAsset(toChange);*/
                walletService.lockChange(params.getMoney(), toAddress.getCoin().toUpperCase(),toAddress.getUid(),WalletTypeEnum.CAPITAL_ACCOUNT.getType(),
                        0,AssetLogTypeEnum.CASH_IN, AssetLogSubTypeEnum.CASH_IN_INSIDE);
                //插入记录
                Withdraw inLog = new Withdraw();
                inLog.setCoin(withdraw.getCoin());
                inLog.setUid(toAddress.getUid());
                //内转-入
                inLog.setType(3);
                inLog.setFrom(params.getFrom());
                inLog.setToAddress(toAddress.getAddress());
                inLog.setMoney(params.getMoney());
                inLog.setFee(BigDecimal.ZERO);
                inLog.setStatus(1);
                inLog.setCheckStatus(1);
                inLog.setRemark(withdraw.getRemark());
                withdrawMapper.insert(inLog);
            }else {
                withdraw.setStatus(1);
                withdraw.setCheckStatus(1);
                if(params.getType()==2 && "USDT".equals(withdraw.getCoin())){
                    TransferData build = TransferData.builder().
                            withdrawId(withdraw.getId())
                            .price(withdraw.getMoney()).symbol(withdraw.getCoin()).address(params.getToAddress()).build();

                    String result = OkHttpUtil.postJsonParams(trxService+"/api/transfer",JSON.toJSONString(build));
                    log.info("transferToUser={}",result);
                    TrxResult trxResult = JSON.parseObject(result, TrxResult.class);

                    if(trxResult==null || trxResult.getCode()!=0){
                        log.info("trxResult={},msg={},req={}",trxResult,trxResult!=null?trxResult.getMsg():"",build);
                        throw new CustomException("线上提现失败，TRX服务请求错误，请联系技术人员查看！");
                       /* withdraw.setStatus(2);
                        withdraw.setCheckStatus(2);
                        if(trxResult!=null){
                            withdraw.setError(trxResult.getMsg()+";"+(withdraw.getError()==null?"":withdraw.getError()));
                        }
                        AssetChange change = AssetChange.builder().uid(withdraw.getUid())
                                .currency("USDT")
                                .dim(0)
                                .type(AssetLogTypeEnum.CASH_OUT_FAILED)
                                .subType(AssetLogSubTypeEnum.WALLET_FAILED)
                                .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                                .amount(withdraw.getMoney())
                                .build();
                        walletService.changeAsset(change);*/
                    }
                }
            }
        }else if(withdraw.getCheckStatus() == 2){
            //驳回请求，返回已扣除的金额
            withdraw.setStatus(2);
            String coin = withdraw.getCoin();
            if("USDT_TRC20".equals(coin)){
                coin = "USDT";
            }
            walletService.lockChange(params.getMoney(), coin.toUpperCase(), withdraw.getUid(), WalletTypeEnum.CAPITAL_ACCOUNT.getType(),
                    0,AssetLogTypeEnum.CASH_OUT_FAILED, AssetLogSubTypeEnum.REJECT_FAILED);

            walletService.lockChange(params.getFee(), CurrencyEnum.USDT.getCode(), withdraw.getUid(), WalletTypeEnum.CAPITAL_ACCOUNT.getType(),
                    0,AssetLogTypeEnum.FEE, AssetLogSubTypeEnum.REJECT_FAILED);
        }
        withdrawMapper.update(withdraw);
        return Boolean.TRUE;
    }



    private BigDecimal getTodayWithdrawQuantity(Long uid, Integer type){
        SimpleDateFormat sp=new SimpleDateFormat("yyyy-MM-dd");
        String today = sp.format(new Date());
        //定义每天的24h时间范围
        String beginTime = today + " 00:00:00";
        String endTime = today + " 23:59:59";
        Map<String, Object> params = new HashMap<>();
        params.put("uid", uid);
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        params.put("type", type);
        return withdrawMapper.getDayWithdrawQuantity(params);
    }

    private Boolean isExistWithdraw(Long uid){
        SimpleDateFormat sp=new SimpleDateFormat("yyyy-MM-dd");
        String today = sp.format(new Date());
        //定义每天的24h时间范围
        String beginTime = today + " 00:00:00";
        String endTime = today + " 23:59:59";
        Map<String, Object> params = new HashMap<>();
        params.put("uid", uid);
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        return withdrawMapper.isExistWithdraw(params) > 0;
    }

    @Override
    public BigDecimal getWithdraAmount(Long uid, String coin,Integer type,Integer checkStatus, Integer status) {
        WithdrawEntity entity = new WithdrawEntity();
        entity.setType(type);
        entity.setUid(uid);
        entity.setCoin(coin);
        entity.setCheckStatus(checkStatus);
        entity.setStatus(status);
        return withdrawMapper.getWithdraAmount(entity);
    }

    @Override
    public BigDecimal withdrawAuditRecord(WithdrawEntity entity) {
        return withdrawMapper.withdrawAuditRecord(entity);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkRecharge(Withdraw withdraw) {
        //审核通过
        if(withdraw.getCheckStatus() == 1){
            withdraw.setType(5);
            withdraw.setStatus(1);
            AssetChange change = AssetChange.builder().uid(withdraw.getUid())
                    .currency(withdraw.getCoin())
                    .dim(0)
                    .type(AssetLogTypeEnum.CASH_IN)
                    .subType(AssetLogSubTypeEnum.CASH_IN_OUTSIDE)
                    .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                    .amount(withdraw.getMoney())
                    .build();
            walletService.changeAsset(change);
        }else if (withdraw.getCheckStatus() == 2){
            // 驳回
            withdraw.setStatus(2);
        }
        withdrawMapper.update(withdraw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agentPayWithdraw(WithdrawEntity withdraw, SysUser sysUser) {
        Withdraw params = new Withdraw();
        params.setCoin(CurrencyEnum.USDT.getCode());
        params.setMoney(withdraw.getMoney());
        params.setFee(BigDecimal.ZERO);
        params.setToAddress(withdraw.getToAddress());
        params.setStatus(0);
        params.setCheckStatus(0);
        params.setType(1);
        params.setCreateTime(new Date());
        params.setOperatorId(sysUser.getUserId());
        params.setRemark(withdraw.getRemark());
        Integer id = withdrawMapper.insert(params);
       /* TransferData build = TransferData.builder().
                withdrawId(params.getId())
                .price(withdraw.getMoney())
                .symbol(CurrencyEnum.USDT.getCode())
                .address(params.getToAddress())
                .build();

        String result = OkHttpUtil.postJsonParams(trxService+"/api/transfer",JSON.toJSONString(build));
        log.info("transferToUser={}",result);
        TrxResult trxResult = JSON.parseObject(result, TrxResult.class);
        if(trxResult==null || trxResult.getCode()!=0){
            throw new CustomException("人工代付失败");
        }*/

    }

    /**
     * 线下打款
     * @param entity
     * @param sysUser
     */
    @Override
    public void offlinePay(WithdrawEntity entity, SysUser sysUser) {
        Withdraw params = new Withdraw();
        params.setId(entity.getId());
        Withdraw withdraw = withdrawMapper.getByParam(params);
        //审核通过
        withdraw.setStatus(1);
        withdraw.setCheckStatus(1);
        withdraw.setVerifyTime(new Date());
        withdraw.setType(3);
        withdraw.setOperatorId(sysUser.getUserId());
        withdrawMapper.update(withdraw);
    }

    @Override
    @Transactional
    public void manualPayment(Withdraw withdraw) {
        boolean paymentStatus = redisTemplate.opsForValue().setIfAbsent(withdraw.getId()+"_"+withdraw.getToAddress(),withdraw.getToAddress(), 20, TimeUnit.SECONDS);
        if(!paymentStatus) return;
        Withdraw params = new Withdraw();
        params.setId(withdraw.getId());
        params.setStatus(1);
        params.setCheckStatus(1);
        params.setVerifyTime(new Date());
        withdrawMapper.update(params);

        TransferData build = TransferData.builder().
                withdrawId(withdraw.getId())
                .price(withdraw.getMoney())
                .symbol(CurrencyEnum.USDT.getCode())
                .address(withdraw.getToAddress())
                .build();

        String result = OkHttpUtil.postJsonParams(trxService+"/api/transfer",JSON.toJSONString(build));
        log.info("transferToUser={}",result);
        TrxResult trxResult = JSON.parseObject(result, TrxResult.class);
        if(trxResult==null || trxResult.getCode()!=0) {
            throw new CustomException("人工代付失败,提现ID="+withdraw.getId());
        }
    }

    @Override
    public List<Withdraw> getManualPayment(Withdraw params) {
        return withdrawMapper.getManualPayment(params);
    }

    @Override
    public BigDecimal rechargeAmount(List<Long> uidList) {
        return withdrawMapper.rechargeAmount(uidList);
    }

    @Override
    public BigDecimal withdrawAmount(List<Long> uidList) {
        return withdrawMapper.withdrawAmount(uidList);
    }


}
