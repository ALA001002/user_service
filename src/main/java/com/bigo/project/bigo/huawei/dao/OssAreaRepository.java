package com.bigo.project.bigo.huawei.dao;

import com.bigo.project.bigo.huawei.entity.OssArea;
import com.bigo.project.bigo.huawei.entity.OssConfig;
import com.bigo.project.bigo.wallet.dao.BaseRepository;

import java.util.List;

public interface OssAreaRepository extends BaseRepository<OssArea> {

    List<OssArea> findAllByDelFlagFalse();

}
