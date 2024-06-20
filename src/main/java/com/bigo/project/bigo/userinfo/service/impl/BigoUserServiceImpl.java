package com.bigo.project.bigo.userinfo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bigo.common.utils.*;
import com.bigo.common.utils.ip.IpUtils;
import com.bigo.project.bigo.agent.domain.Agent;
import com.bigo.project.bigo.agent.service.IAgentRelationService;
import com.bigo.project.bigo.api.vo.ChildVO;
import com.bigo.project.bigo.api.vo.MyTeamVO;
import com.bigo.project.bigo.api.vo.MyTotalTeamVO;
import com.bigo.project.bigo.userinfo.domain.BigoUser;
import com.bigo.project.bigo.userinfo.entity.BigoUserEntity;
import com.bigo.project.bigo.userinfo.mapper.BigoUserMapper;
import com.bigo.project.bigo.userinfo.service.IBigoUserService;
import com.bigo.project.bigo.wallet.service.IWalletService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author wenxm
 * @Date 2020/6/18 9:58
 */
@Service
@Slf4j
public class BigoUserServiceImpl implements IBigoUserService {

    @Autowired
    private BigoUserMapper bigoUserMapper;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private IAgentRelationService relationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(BigoUser user) {
        //注册即为一级用户
        user.setLevel(1);
        user.setUid(geneUid());
        String registerIp = IpUtils.getIpAddr(ServletUtils.getRequest());
        user.setRegisterIp(registerIp);
        String registerAddress = IpUtils.getAddress(registerIp);
        user.setRegisterArea(registerAddress);
        Long result = bigoUserMapper.insertUser(user);
        log.info("======处理钱包=========");
        walletService.addWallet(user.getUid());
        log.info("======建立与代理商的关系=========");
        relationService.insert(user);
        return user.getUid();
    }

    @Override
    public int updateUser(BigoUser user) {
        return bigoUserMapper.updateUser(user);
    }

    @Override
    public Boolean isPhoneExist(String phone) {
        return bigoUserMapper.checkPhoneUnique(phone) > 0;
    }

    @Override
    public Boolean isEmailExist(String email) {
        return bigoUserMapper.checkEmailUnique(email) > 0;
    }

    @Override
    public Boolean isInviteCodeExist(Long inviteCode) {
        return bigoUserMapper.checkInviteCodeUnique(inviteCode) > 0;
    }

    @Override
    public BigoUser getUserByUid(Long uid) {
        return bigoUserMapper.getUserByUid(uid);
    }

    @Override
    public BigoUser getUserByPhone(String phone) {
        return bigoUserMapper.getUserByPhone(phone);
    }

    @Override
    public BigoUser getUserByEmail(String email) {
        return bigoUserMapper.getUserByEmail(email);
    }

    @Override
    public List<ChildVO> listMaskChild(List<Long> uidList) {
        List<ChildVO> list = Lists.newArrayList();
        List<BigoUser> childList = bigoUserMapper.listChild(uidList);
        for(BigoUser user : childList){
            ChildVO userVO = new ChildVO();
            if(StringUtils.isNotEmpty(user.getPhone())){
                userVO.setUsername(PrivacyUtils.maskMobile(user.getPhone()));
            }else{
                userVO.setUsername(PrivacyUtils.maskEmail(user.getEmail()));
            }
            userVO.setUid(user.getUid());
            userVO.setInviteTime(user.getRegisterTime());
            list.add(userVO);
        }
        return list;
    }

    @Override
    public List<BigoUserEntity> listByEntity(BigoUserEntity entity) {
        return bigoUserMapper.listByEntity(entity);
    }

    @Override
    public List<BigoUser> listNeedLevelUpUser() {
        return bigoUserMapper.listNeedLevelUpUser();
    }

    @Override
    public List<BigoUserEntity> listDockUserByCustomerServiceId(Long customerServiceId) {
        return bigoUserMapper.listDockUserByCustomerServiceId(customerServiceId);
    }

    /**
     * 获取受邀人数
     * @param params
     * @return
     */
    @Override
    public Long getInvitedNumber(Map params) {
        return bigoUserMapper.getInvitedNumber(params);
    }

    @Override
    public List<BigoUserEntity> getChildrenList(BigoUserEntity entity) {
        return bigoUserMapper.getChildrenList(entity);
    }

    @Override
    public List<String> getRechargeUser() {
        return bigoUserMapper.getRechargeUser();
    }

    @Override
    public List<Long> getLowerUids(Long parentUid) {
        return bigoUserMapper.getLowerUids(parentUid);
    }

    @Override
    public void updateSendEmailStatus(String email) {
        bigoUserMapper.updateSendEmailStatus(email);
    }

    @Override
    public List<BigoUser> listParentUids(Long uid) {
        return  bigoUserMapper.listParentUids(uid);
    }

    @Override
    public int editUser(BigoUser user) {
        if (!StringUtils.isEmpty(user.getPassword())) {
            user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        }
        if (!StringUtils.isEmpty(user.getPayPassword())) {
            user.setPayPassword(SecurityUtils.encryptPassword(user.getPayPassword()));
        }
        return bigoUserMapper.updateUser(user);
    }

    @Override
    @Transactional
    public void updateUserParent(BigoUser updateUser, BigoUser parentUser) {
        BigoUserEntity entity = new BigoUserEntity();
        entity.setAgentIds(updateUser.getUid().toString());
        // 查询出当前所有下级
        List<BigoUserEntity> getChildrenList = bigoUserMapper.getChildrenList(entity);
        //修改当前下级的所有顶级uid
        Long topUid=null;
        if(parentUser.getTopUid() != null) {
            topUid = parentUser.getTopUid();
        }else {
            topUid = parentUser.getUid();
        }
        if(getChildrenList.size() > 0) {
            List<Long> uidList = new ArrayList<>();
            for (BigoUserEntity bigoUserEntity : getChildrenList) {
                uidList.add(bigoUserEntity.getUid());
            }
            Map map = new HashMap();
            map.put("topUid", topUid);
            map.put("uidList", uidList);
            bigoUserMapper.editUserTopUid(map);
        }
        //修改当前用户的上级id
        updateUser.setTopUid(topUid);
        this.updateUser(updateUser);
    }

    @Override
    public List<MyTeamVO> getMyTeam(Long uid) {
        List<MyTeamVO> voList = bigoUserMapper.getMyTeam(uid);
        if(voList != null && voList.size() > 0){
            voList.forEach(item -> {
                Long teamNum = item.getTeamNum() - 1L;
                item.setTeamNum(teamNum);
                item.setUserName(PrivacyUtils.maskMobile(item.getUserName()));
            });
        }
        return voList;
    }

    @Override
    public Map totalTeamInfo(Long uid) {
        Map map = new HashMap<>();
        List<MyTotalTeamVO>  totalNumList = bigoUserMapper.totalTeamInfo(uid);
        if(totalNumList.size() > 0) {
            List<Long> firstUidList = new ArrayList<>();
            firstUidList.add(uid);

            List<Long> leveOneUidList = getLowerInfo("One", firstUidList, totalNumList, map);
            List<Long> leveTwoUidList = getLowerInfo("Two", leveOneUidList, totalNumList, map);
            List<Long> leveThreeUidList = getLowerInfo("Three", leveTwoUidList, totalNumList, map);

            // 团队总人数
            map.put("totalNum", leveOneUidList.size() + leveTwoUidList.size() + leveThreeUidList.size());
            // 计算团队总余额
//             totalAmount = (BigDecimal) map.get("levelOneBalance");
//            map.put("totalAmount", StringUtils.numberRemoveZero(totalAmount.toPlainString()));
        }
        return map;
    }

    @Override
    public Long ipRegisterCount(String queryIp) {
        return bigoUserMapper.ipRegisterCount(queryIp);
    }

    private List<Long> getLowerInfo(String keyName, List<Long> parentUidList, List<MyTotalTeamVO> totalNumList, Map map) {
        List<Long> uidList = new ArrayList<>();
        if(parentUidList == null || parentUidList.size() == 0) {
            map.put("level"+keyName+"Num", 0);
            map.put("level"+keyName+"Balance", 0);
            return new ArrayList<>();
        }
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (Long uid : parentUidList) {
            for (MyTotalTeamVO vo : totalNumList) {
                if(uid.equals(vo.getParentUid())) {
                    uidList.add(vo.getUid());
                    totalBalance = totalBalance.add(vo.getBalance());
                }
            }
        }
        if(uidList.size() == 0) {
            map.put("level"+keyName+"Num", 0);
            map.put("level"+keyName+"Balance", 0);
            return new ArrayList<>();
        }
        map.put("level"+keyName+"Num", uidList.size());
        map.put("level"+keyName+"Balance", StringUtils.numberRemoveZero(totalBalance.toPlainString()));

        String totalValue = (String) ObjectUtils.defaultIfNull(map.get("totalAmount"),"0");
        BigDecimal sumTotalAmount = new BigDecimal(totalValue);
        sumTotalAmount = sumTotalAmount.add(totalBalance);
        map.put("totalAmount", StringUtils.numberRemoveZero(sumTotalAmount.toPlainString()));
        return uidList;
    }

    /**
     * 生成uid
     * @return
     */
    private Long geneUid(){
        Long uid = RandomNumberUtils.geneRandomUid();
        if(bigoUserMapper.checkInviteCodeUnique(uid) > 0){
            return geneUid();
        }
        return uid;
    }



}
