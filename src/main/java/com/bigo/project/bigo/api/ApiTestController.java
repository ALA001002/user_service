package com.bigo.project.bigo.api;

import com.bigo.framework.web.domain.AjaxResult;
import com.bigo.framework.websocket.server.WebSocketNotifyServer;
import com.bigo.project.bigo.api.domain.PushReq;
import com.bigo.project.bigo.enums.AssetLogSubTypeEnum;
import com.bigo.project.bigo.enums.AssetLogTypeEnum;
import com.bigo.project.bigo.enums.WalletTypeEnum;
import com.bigo.project.bigo.v2ico.request.LockReq;
import com.bigo.project.bigo.wallet.service.IWalletService;
import com.bigo.project.bigo.wallet.service.impl.WalletServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/message/")
public class ApiTestController {

    @Autowired
    private IWalletService walletService;

    @RequestMapping(value="push")
    public String push(PushReq req){
        WebSocketNotifyServer.sendMessage(WebSocketNotifyServer.NotifyType.get(req.getType()));
        return "success";
    }

    @PostMapping(value="lockChange")
    public AjaxResult lockChange(@RequestBody LockReq req){
        walletService.lockChange(req.getAmount(),"USDT",req.getUserId(), WalletTypeEnum.CAPITAL_ACCOUNT.getType(),1, AssetLogTypeEnum.STOCK, AssetLogSubTypeEnum.FROZEN);
        return AjaxResult.success();
    }
    @PostMapping(value="unlockChange")
    public AjaxResult unlock(@RequestBody LockReq req){
        walletService.lockChange(req.getAmount(),"USDT",req.getUserId(), WalletTypeEnum.CAPITAL_ACCOUNT.getType(),0, AssetLogTypeEnum.STOCK, AssetLogSubTypeEnum.RELEASE);
        return AjaxResult.success();
    }
    @PostMapping(value="addBalance")
    public AjaxResult addBalance(@RequestBody LockReq req){
        walletService.lockChange(req.getAmount(),"USDT",req.getUserId(), WalletTypeEnum.CAPITAL_ACCOUNT.getType(),0, AssetLogTypeEnum.STOCK);
        return AjaxResult.success();
    }
}
