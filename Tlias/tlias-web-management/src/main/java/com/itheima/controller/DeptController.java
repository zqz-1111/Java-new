package com.itheima.controller;

import com.itheima.pojo.Dept;
import com.itheima.pojo.Result;
import com.itheima.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@Slf4j//日志-等同于private static final Logger log=LoggerFactory.getLogger(类名)
@RequestMapping("/depts")
@RestController
public class DeptController {
    @Autowired
    private DeptService deptService;

    //作用目的:查询信息
    @GetMapping
    public Result list(){
        log.info("查看全部部门的数据");
        List<Dept> depts = deptService.findAll();
        return Result.success(depts);
    }

    @DeleteMapping
    public Result delete(@RequestParam Integer id){
        log.info("删除部门:{}",id);
        deptService.delete(id);
        return Result.success();
    }

    @PostMapping
    public Result insert(@RequestBody Dept dept){
        log.info("添加部门信息{}",dept);
        deptService.add(dept);
        return Result.success();
    }

    @GetMapping("/{id:\\d+}")
    public Result search(@PathVariable Integer id){
        log.info("查询单个部门信息{}",id);
        Dept dept=deptService.search(id);
        return Result.success(dept);
    }

    @PutMapping
    public Result update(@RequestBody Dept dept){
        log.info("更新部门信息{}",dept);
        deptService.update(dept);
        return Result.success();
    }
}