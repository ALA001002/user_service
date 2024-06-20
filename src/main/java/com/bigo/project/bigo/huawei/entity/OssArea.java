package com.bigo.project.bigo.huawei.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name="oss_area",uniqueConstraints = {@UniqueConstraint(columnNames = {"area_name"})})
public class OssArea {
    @Id
    @Column(name="id")
    Long id;

    @Column(name="area_name")
    String areaName;

    @Column(name="bucket_name")
    String bucketName;

    @Column(name="del_flag")
    Boolean delFlag;
}
