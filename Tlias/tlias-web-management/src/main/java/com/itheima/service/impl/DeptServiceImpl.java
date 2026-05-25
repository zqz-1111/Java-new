package com.itheima.service.impl;

import com.itheima.mapper.DeptMapper;
import com.itheima.pojo.Dept;
import com.itheima.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

//实现类：

@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    private DeptMapper deptMapper;

    @Override
    public List<Dept> findAll() {
        return deptMapper.finAll();
    }

    //实现类
    @Override
    public void delete(Integer id) {
        deptMapper.delete(id);
    }

    //实现类：
    @Override
    public void add(Dept dept) {
        //在Service中把其他两个值,时间定义
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());

        //把dept对象封装给deptMapper
        deptMapper.insert(dept);
    }

    //实现类：
    @Override
    public void update(Dept dept) {
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.update(dept);
    }

    //实现类：
    @Override
    public Dept search(Integer id) {
        return deptMapper.search(id);
    }
}