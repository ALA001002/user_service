package com.bigo.project.bigo.pay.entity;

import com.alibaba.fastjson.JSONObject;
import com.bigo.common.exception.CustomException;
import com.bigo.framework.config.RuoYiConfig;
import com.bigo.project.bigo.pay.domain.PayOrder;
import com.bigo.project.bigo.pay.domain.PayPassage;
import com.bigo.project.bigo.pay.domain.TransOrder;
import com.bigo.project.bigo.pay.service.IPayOrderService;
import com.bigo.project.bigo.pay.service.IPayPassageService;
import com.bigo.project.bigo.pay.vo.ParamVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description:
 * @author: wenxm
 * @date: 2021/5/21 17:04
 */
@Component
public abstract class BasePayment implements PaymentInterface {

    @Autowired
    public IPayPassageService payPassageService;

    @Autowired
    public IPayOrderService payOrderService;

    public String getUrl() {
        return RuoYiConfig.getFileUrl();
    }

    public JSONObject pay(PayOrder payOrder) {
        return null;
    }

    public abstract String getChannelName();

    /**
     * 获取三方支付配置信息
     * 如果是平台账户,则使用平台对应的配置,否则使用商户自己配置的渠道
     * @param payOrder
     * @return
     */
    public String getPayParam(PayOrder payOrder) {
        String payParam = "";
        PayPassage payPassage = payPassageService.selectPayPassageById(payOrder.getPayPassageId());
        if(payPassage != null && payPassage.getStatus() == 1) {
            payParam = payPassage.getParam();
        }
        if(StringUtils.isBlank(payParam)) {
            throw new CustomException("order_failed_please_contact_customer_service");
        }
        JSONObject valueJson = new JSONObject();
        List<ParamVo> voList = JSONObject.parseArray(payParam, ParamVo.class);
        for (ParamVo paramVo : voList) {
            valueJson.put(paramVo.getField(), paramVo.getValue());
        }
        return valueJson.toJSONString();
    }
}
