package com.itheima.service;

import com.itheima.pojo.Dept;

import java.util.List;

//接口定义：
public interface DeptService {
    //查询所有的部门数据
    List<Dept> findAll();

    //接口定义：
    void delete(Integer id);

    //接口
    void add(Dept dept);

    //接口定义：
    void update(Dept dept);

    //接口定义：
    Dept search(Integer id);
}

//实现类：

