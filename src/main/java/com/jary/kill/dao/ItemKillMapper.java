package com.jary.kill.dao;

import com.jary.kill.entity.ItemKill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItemKillMapper {
    List<ItemKill> selectAll();

    ItemKill selectById(@Param("id") int id);

    int updateKillItem(@Param("killId") int killId);

    ItemKill selectByIdV2(@Param("id") int id);

    int updateKillItemV2(@Param("killId") int killId);

    int modify(@Param("id") int id);
}