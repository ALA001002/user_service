package com.bigo.project.bigo.pay.mapper;

import com.bigo.project.bigo.pay.domain.PayOrder;

import java.util.List;

/**
 * 三方支付订单表Mapper接口
 * 
 * @author bigo
 * @date 2021-05-20
 */
public interface PayOrderMapper 
{
    /**
     * 查询三方支付订单表
     * 
     * @param id 三方支付订单表ID
     * @return 三方支付订单表
     */
    public PayOrder selectPayOrderById(Long id);

    /**
     * 查询三方支付订单表列表
     * 
     * @param payOrder 三方支付订单表
     * @return 三方支付订单表集合
     */
    public List<PayOrder> selectPayOrderList(PayOrder payOrder);

    /**
     * 新增三方支付订单表
     * 
     * @param payOrder 三方支付订单表
     * @return 结果
     */
    public int insertPayOrder(PayOrder payOrder);

    /**
     * 修改三方支付订单表
     * 
     * @param payOrder 三方支付订单表
     * @return 结果
     */
    public int updatePayOrder(PayOrder payOrder);

    public int updateByPayOrderId(PayOrder payOrder);

    /**
     * 删除三方支付订单表
     * 
     * @param id 三方支付订单表ID
     * @return 结果
     */
    public int deletePayOrderById(Long id);

    /**
     * 批量删除三方支付订单表
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePayOrderByIds(Long[] ids);

    PayOrder selectOrderId(String payOrderId);

    int updateSuccess(PayOrder payOrder);

    int updateStatusIng(PayOrder payOrder);
}
