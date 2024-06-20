package com.bigo.project.bigo.v2ico.controller;

import com.bigo.framework.web.domain.AjaxResult;
import com.bigo.project.bigo.v2ico.request.SymbolConfigReq;
import com.bigo.project.bigo.v2ico.service.SymbolConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.xml.transform.Result;

@RestController
@RequestMapping(value="/v2/ico/")
public class IcoV2Controller {

    @Resource
    SymbolConfigService symbolConfigService;

    @RequestMapping(value="/queryConfig")
    public AjaxResult queryConfig(SymbolConfigReq req){
        return symbolConfigService.queryConfig(req);
    }


}
