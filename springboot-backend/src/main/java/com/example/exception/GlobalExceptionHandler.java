package com.example.exception;

import com.example.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import com.example.team.TeamBusinessException;

/**
 * 全局异常处理器
 * 统一捕获和处理系统中抛出的异常，实现异常处理的集中化管理
 * 避免异常直接抛给前端，提高系统友好性和安全性
 */
// 同时处理普通控制器和组队模块控制器的业务错误。
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TeamBusinessException.class)
    @ResponseBody
    public Result handleTeamBusinessException(TeamBusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

  // 日志记录器，用于记录异常信息
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * 处理所有未被捕获的Exception类型异常（通用异常处理）
   * 作为系统的最后一道异常防线，捕获所有未被专门处理的异常
   * @param e 捕获到的异常对象
   * @return Result 统一响应结果，包含错误信息
   */
  @ExceptionHandler(Exception.class) // 指定处理Exception类型的异常
  @ResponseBody // 将返回结果转换为JSON格式响应给前端
  public Result error(Exception e) {
    // 记录异常详细信息到日志，便于问题排查
    log.error("系统异常: " + e.getClass().getName() + " - " + e.getMessage(), e);
    // 返回统一错误响应，包含异常类型和消息（开发环境可显示详细信息）
    return Result.error("系统异常: " + e.getMessage());
  }

  /**
   * 处理自定义业务异常（CustomerException）
   * 专门处理系统中手动抛出的业务逻辑异常
   * @param e 自定义异常对象
   * @return Result 统一响应结果，包含自定义的错误码和错误信息
   */
  @ExceptionHandler(CustomerException.class) // 指定处理自定义异常
  @ResponseBody // 将返回结果转换为JSON格式响应给前端
  public Result customerError(CustomerException e) {
    // 记录自定义异常信息到日志
    log.error("自定义错误", e);
    // 使用异常中携带的错误码和信息构建响应结果
    return Result.error(e.getCode(), e.getMsg());
  }
}
