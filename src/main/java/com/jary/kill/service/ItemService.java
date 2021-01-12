package com.jary.kill.service;

import com.jary.kill.dao.ItemKillMapper;
import com.jary.kill.entity.ItemKill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemKillMapper itemKillMapper;

    public List<ItemKill> getKillItems() {
        return itemKillMapper.selectAll();
    }

    public ItemKill getKillDetail(int id) {
        return itemKillMapper.selectById(id);
    }

    public int modify(int id){
        return itemKillMapper.modify(id);
    }


}