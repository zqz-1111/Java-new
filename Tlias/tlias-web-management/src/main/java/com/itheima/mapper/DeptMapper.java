package com.itheima.mapper;

import com.itheima.pojo.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeptMapper {
    //SQL语句,查询表 内容
    @Select("select id, name, create_time, update_time from dept order by update_time desc ")
    List<Dept> finAll();


    @Delete("delete from dept where id=#{id}")
    void delete(Integer id);

    @Insert("insert into dept (name,create_time,update_time) values (#{name},#{createTime},#{updateTime})")
    void insert(Dept dept);

    @Update("update dept set name=#{name},update_time=#{updateTime} where id =#{id}")
    void update(Dept dept);

    @Select("select id, name, create_time, update_time from dept where id=#{id}")
    Dept search(Integer id);
}