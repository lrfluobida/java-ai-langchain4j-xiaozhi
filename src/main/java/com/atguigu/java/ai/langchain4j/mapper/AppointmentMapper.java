package com.atguigu.java.ai.langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.atguigu.java.ai.langchain4j.entity.Appointment;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
}