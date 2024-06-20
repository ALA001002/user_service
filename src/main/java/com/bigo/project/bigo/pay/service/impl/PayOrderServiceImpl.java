package com.bigo.project.bigo.pay.service.impl;

import com.bigo.common.exception.BaseException;
import com.bigo.common.utils.DateUtils;
import com.bigo.project.bigo.config.util.ConfigSettingUtil;
import com.bigo.project.bigo.enums.AssetLogSubTypeEnum;
import com.bigo.project.bigo.enums.AssetLogTypeEnum;
import com.bigo.project.bigo.enums.CurrencyEnum;
import com.bigo.project.bigo.enums.WalletTypeEnum;
import com.bigo.project.bigo.pay.domain.PayOrder;
import com.bigo.project.bigo.pay.mapper.PayOrderMapper;
import com.bigo.project.bigo.pay.service.IPayOrderService;
import com.bigo.project.bigo.userinfo.domain.BigoUser;
import com.bigo.project.bigo.userinfo.mapper.BigoUserMapper;
import com.bigo.project.bigo.wallet.domain.AssetChange;
import com.bigo.project.bigo.wallet.domain.Withdraw;
import com.bigo.project.bigo.wallet.entity.WithdrawEntity;
import com.bigo.project.bigo.wallet.service.IWalletService;
import com.bigo.project.bigo.wallet.service.IWithdrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 三方支付订单表Service业务层处理
 * 
 * @author bigo
 * @date 2021-05-20
 */
@Service
public class PayOrderServiceImpl implements IPayOrderService
{
    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private IWithdrawService withdrawService;

    @Autowired
    private BigoUserMapper userMapper;


    /**
     * 查询三方支付订单表
     * 
     * @param id 三方支付订单表ID
     * @return 三方支付订单表
     */
    @Override
    public PayOrder selectPayOrderById(Long id)
    {
        return payOrderMapper.selectPayOrderById(id);
    }

    /**
     * 查询三方支付订单表列表
     * 
     * @param payOrder 三方支付订单表
     * @return 三方支付订单表
     */
    @Override
    public List<PayOrder> selectPayOrderList(PayOrder payOrder)
    {
        return payOrderMapper.selectPayOrderList(payOrder);
    }

    /**
     * 新增三方支付订单表
     * 
     * @param payOrder 三方支付订单表
     * @return 结果
     */
    @Override
    public int insertPayOrder(PayOrder payOrder)
    {
        payOrder.setCreateTime(DateUtils.getNowDate());
        return payOrderMapper.insertPayOrder(payOrder);
    }

    /**
     * 修改三方支付订单表
     * 
     * @param payOrder 三方支付订单表
     * @return 结果
     */
    @Override
    public int updatePayOrder(PayOrder payOrder)
    {
        payOrder.setUpdateTime(DateUtils.getNowDate());
        return payOrderMapper.updatePayOrder(payOrder);
    }

    /**
     * 批量删除三方支付订单表
     * 
     * @param ids 需要删除的三方支付订单表ID
     * @return 结果
     */
    @Override
    public int deletePayOrderByIds(Long[] ids)
    {
        return payOrderMapper.deletePayOrderByIds(ids);
    }

    /**
     * 删除三方支付订单表信息
     * 
     * @param id 三方支付订单表ID
     * @return 结果
     */
    @Override
    public int deletePayOrderById(Long id)
    {
        return payOrderMapper.deletePayOrderById(id);
    }



    @Override
    @Transactional
    public int updateStatusIng(String payOrderId) {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(payOrderId);
        payOrder.setStatus(1);
        return payOrderMapper.updateStatusIng(payOrder);
    }

    @Override
    public PayOrder selectOrderId(String payOrderId) {
        return payOrderMapper.selectOrderId(payOrderId);
    }

    @Override
    @Transactional
    public int updateStatusSuccess(PayOrder payOrder) {
        PayOrder updatePayOrder = new PayOrder();
        updatePayOrder.setPayOrderId(payOrder.getPayOrderId());
        updatePayOrder.setChannelOrderId(payOrder.getChannelOrderId());
        updatePayOrder.setFee(payOrder.getFee());
        updatePayOrder.setSettValue(payOrder.getSettValue());
        updatePayOrder.setTradeValue(payOrder.getTradeValue());
        updatePayOrder.setStatus(2);
        updatePayOrder.setPaySuccTime(new Date());
        return updateSuccessTransactional(updatePayOrder);
    }

    @Override
    public int updateStatusAndAmount(String payOrderId, Long currencyAmount) {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderId(payOrderId);
        payOrder.setStatus(1);
        payOrder.setCurrencyAmount(new BigDecimal(currencyAmount));
        return payOrderMapper.updateStatusIng(payOrder);
    }

    @Override
    public void updateByPayOrderId(PayOrder updateOrder) {
        payOrderMapper.updateByPayOrderId(updateOrder);
    }

    @Transactional
    public int updateSuccessTransactional(PayOrder payOrder) {
        int count = payOrderMapper.updateSuccess(payOrder);
        if(count != 1){
           throw new BaseException("更新支付订单失败,订单ID:{}", payOrder.getPayOrderId());
        }
        payOrder = this.selectOrderId(payOrder.getPayOrderId());
        Long uid = payOrder.getUid();
        String coin = CurrencyEnum.USDT.getCode();
        BigDecimal rechargeAmount = new BigDecimal(payOrder.getAmount());

        //判断首充奖励是否开启
        Integer firstRechargeStatus = ConfigSettingUtil.getFirstRechargeStatus();
        BigDecimal firstRechargeComplyAmount = ConfigSettingUtil.getFirstRechargeComplyAmount();
        if(firstRechargeStatus == 1
                && rechargeAmount.compareTo(firstRechargeComplyAmount) >=0 ) { //开启
            //判断是否首充
            BigDecimal firstRechargeAmount = withdrawService.getWithdraAmount(uid,
                    coin, 4, 1, 1);

            if(firstRechargeAmount.compareTo(BigDecimal.ZERO) == 0) { //首充
                BigoUser user = userMapper.getUserByUid(uid);
                //奖励金额
                BigDecimal firstRechargeRewards =  ConfigSettingUtil.getFirstRechargeRewards();
                AssetChange firstRecharge = AssetChange.builder()
                        .amount(firstRechargeRewards)
                        .uid(uid)
                        .dim(0).type(AssetLogTypeEnum.CASH_IN_DRAW)
                        .subType(AssetLogSubTypeEnum.CASH_IN_OUTSIDE)
                        .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                        .currency(coin)
                        .build();
                walletService.changeAsset(firstRecharge);
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
                            .currency(coin)
                            .build();
                    walletService.changeAsset(assetChange);
                }
            }
        }

        //添加充值记录
        Withdraw withdraw = new Withdraw();
        withdraw.setUid(uid);
        withdraw.setCoin(coin);
        withdraw.setMoney(new BigDecimal(payOrder.getAmount()));
        withdraw.setFee(BigDecimal.ZERO);
        withdraw.setRemark(payOrder.getPayPassageId().toString());
        withdraw.setType(4);
        withdraw.setStatus(1);
        withdraw.setCheckStatus(1);
        withdraw.setCreateTime(new Date());
        withdraw.setVerifyTime(new Date());
        withdrawService.insert(withdraw);

        //更新成功，更新账户余额，增加流水金额
        AssetChange change = AssetChange.builder().uid(uid)
                .currency(coin)
                .dim(0)
                .type(AssetLogTypeEnum.CASH_IN)
                .subType(AssetLogSubTypeEnum.IN)
                .walletType(WalletTypeEnum.CAPITAL_ACCOUNT.getType())
                .amount(rechargeAmount)
                .build();
        walletService.changeAsset(change);
        return count;
    }
}
