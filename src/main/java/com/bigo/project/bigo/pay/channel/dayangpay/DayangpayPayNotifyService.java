package com.bigo.project.bigo.pay.channel.dayangpay;

import com.alibaba.fastjson.JSONObject;
import com.bigo.common.constant.PayConstant;
import com.bigo.common.utils.PayDigestUtil;
import com.bigo.project.bigo.pay.domain.PayOrder;
import com.bigo.project.bigo.pay.entity.BasePayNotify;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: wenxm
 * @date: 2021/5/21 19:12
 */
@Slf4j
@Service
public class DayangpayPayNotifyService extends BasePayNotify {

    @Override
    public String getChannelName() {
        return DayangpayConfig.CHANNEL_NAME;
    }

    @Override
    public JSONObject doNotify(Object notifyData) {
        String logPrefix = "【大洋支付回调】";
        log.info("====== 开始处理大洋支付回调通知 ======");
        HttpServletRequest request = (HttpServletRequest) notifyData;
        JSONObject retObj = buildRetObj();
        Map<String, Object> payContext = new HashMap();
        PayOrder payOrder;
        String respString = DayangpayConfig.RETURN_VALUE_FAIL;
        try {
            JSONObject paramsObject = getJson(request);
            log.info("大洋支付回调参数:{}",paramsObject);
            if(!verifyPayParams(payContext, paramsObject)) {
                retObj.put("resResult", DayangpayConfig.RETURN_VALUE_FAIL);
                return retObj;
            }
            payOrder = (PayOrder) payContext.get("payOrder");
            // 处理订单
            Integer payStatus = payOrder.getStatus(); // 0：订单生成，1：支付中，-1：支付失败，2：支付成功，3：业务处理完成
            if (payStatus != 2 && payStatus != 3) {
                int updatePayOrderRows = payOrderService.updateStatusSuccess(payOrder);
                if (updatePayOrderRows != 1) {
                    log.error("{}更新支付状态失败,将payOrderId={},更新payStatus={}失败", logPrefix, payOrder.getPayOrderId(),"FAIL");
                    retObj.put(PayConstant.RESPONSE_RESULT, "处理订单失败");
                    return retObj;
                }
                log.info("{}更新支付状态成功,将payOrderId={},更新payStatus={}成功", logPrefix, payOrder.getPayOrderId(), PayConstant.PAY_STATUS_SUCCESS);
                payOrder.setStatus(PayConstant.PAY_STATUS_SUCCESS);
            }
            log.info("====== 完成处理大洋支付回调 ======");
            respString = DayangpayConfig.RETURN_VALUE_SUCCESS;
        }catch (Exception e) {
            log.error(e.getMessage());
            log.error(logPrefix + "处理异常");
            e.printStackTrace();
        }
        retObj.put("resResult", respString);
        return retObj;
    }

    public boolean verifyPayParams(Map<String, Object> payContext, JSONObject paramsObject) {
        // 校验结果是否成功PAYED
        String outTradeNo = paramsObject.getString("out_trade_no"); //商家订单号
        Integer status = paramsObject.getInteger("status"); // 订单状态
        String signature = paramsObject.getString("signature");
        String amount = paramsObject.getString("amount");
        String tradeNo = paramsObject.getString("trade_no");

        if(status != 1){
            log.error("=========大洋支付回调Status异常, Status={}, 订单号={} ", status, outTradeNo);
            payContext.put("retMsg", "支付回调Status异常");
            return false;
        }

        // 查询payOrder记录
        String payOrderId = outTradeNo;
        PayOrder payOrder = payOrderService.selectOrderId(payOrderId);
        if (payOrder == null) {
            log.error("Can't found payOrder form db. payOrderId={}, ", payOrderId);
            payContext.put("retMsg", "Can't found payOrder");
            return false;
        }
        DayangpayConfig config = new DayangpayConfig(getPayParam(payOrder));

        paramsObject.remove("signature");
        String signValue = PayDigestUtil.HmacSHA256(paramsObject, config.getPrivateKey());
        log.info("Sign Result:" + signValue);
//        paramsStr = paramsStr + "&secret=" + config.getKey();
//        log.info("Sign Before MD5:" + paramsStr);
//        String signValue = PayDigestUtil.md5(paramsStr, "utf-8").toUpperCase();
//        log.info("Sign Result:" + signValue);

        if(!signValue.equals(signature)) {
            log.error("验证签名失败. payOrderId={}, ", payOrderId);
            payContext.put("retMsg", "验证签名失败");
            return false;
        }
        // 核对金额
        BigDecimal payAmount = payOrder.getCurrencyAmount();
        BigDecimal resultAmount = new BigDecimal(amount);
        if (payAmount.compareTo(resultAmount) != 0) {
            log.error("金额不一致. usd_amount={},payOrderId={}", resultAmount, payOrderId);
            payContext.put("retMsg", "金额不一致");
            return false;
        }

/*        // 查询订单，进行二次验证
        JSONObject queryParamMap = new JSONObject();
        queryParamMap.put("service", "App.Order.GetBuyOrderInfo");
        queryParamMap.put("merchant_id", config.getMchId());
        queryParamMap.put("out_order_id", out_order_id);
        queryParamMap.put("nonce", RandomStringUtils.randomNumeric(16));
        String timeStr = String.valueOf(System.currentTimeMillis());
        timeStr = timeStr.substring(0,timeStr.length()-3);
        queryParamMap.put("timestamp", timeStr);

        String querySignValue = PayDigestUtil.createSortParam(queryParamMap, "sign");
        querySignValue = querySignValue + "&secret=" + config.getKey();
        log.info("Sign Before MD5:" + querySignValue);
        querySignValue = PayDigestUtil.md5(querySignValue, "utf-8").toUpperCase();
        log.info("Sign Result:" + querySignValue);
        queryParamMap.put("sign",querySignValue);

        log.info("[{}]查询订单请求数据:{}", getChannelName(), queryParamMap.toJSONString());
        String url = config.getQueryUrl();
        log.info("[{}]查询订单请求URL:{}", getChannelName(), url);
        Map map = queryParamMap;
        String result = OkHttpUtil.post(url, map);
        log.info("[{}]查询订单返回数据:{}", getChannelName(), result);
        if (StringUtils.isBlank(result)) {
            log.error("查询回调订单失败，result={}, payOrderId={} ", result, payOrder.getPayOrderId());
            payContext.put("retMsg", "查询回调订单失败");
            return false;
        }
        JSONObject resObj = JSONObject.parseObject(result);
        String ret = resObj.getString("ret");
        if("200".equals(ret)){
            resObj = resObj.getJSONObject("data");
            int orderStatus = resObj.getInteger("order_status");
            if(orderStatus != 1) {
                log.error("处理回调订单失败，查询订单状态为：orderStatus={}, payOrderId={} ", orderStatus, payOrder.getPayOrderId());
                payContext.put("retMsg", "处理回调订单失败");
                return false;
            }
        } else {
            String msg = resObj.getString("msg");
            log.error("处理回调订单失败，查询订单状态为:resultCode={}, Msg={}, payOrderId={}", ret, msg,  payOrder.getPayOrderId());
            return false;
        }*/
        payOrder.setChannelOrderId(tradeNo);
        payOrder.setFee(BigDecimal.ZERO);
        payOrder.setTradeValue(BigDecimal.ZERO);
        payOrder.setSettValue(BigDecimal.ZERO);
        payContext.put("payOrder", payOrder);
        return true;
    }

    private JSONObject getJson(HttpServletRequest request) throws IOException {
        BufferedReader streamReader = new BufferedReader( new InputStreamReader(request.getInputStream(), "UTF-8"));
        StringBuilder responseStrBuilder = new StringBuilder();
        String inputStr;
        while ((inputStr = streamReader.readLine()) != null){
            responseStrBuilder.append(inputStr);
        }
        if(responseStrBuilder.length() > 0) {
            return JSONObject.parseObject(responseStrBuilder.toString());
        }
        JSONObject requestJson = new JSONObject();
        Enumeration paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String[] pv = request.getParameterValues(paramName);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pv.length; i++) {
                if (pv[i].length() > 0) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(pv[i]);
                }
            }
            requestJson.put(paramName, sb.toString());
        }
        return requestJson;
    }
}
