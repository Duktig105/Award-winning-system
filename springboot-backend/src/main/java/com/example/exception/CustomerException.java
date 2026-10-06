package com.example.exception;

/**
 * 自定义业务异常类
 * 用于处理业务逻辑中的异常情况，提供统一的异常处理机制
 * 继承自RuntimeException，属于运行时异常
 */
public class CustomerException extends RuntimeException {
  private final String code;  // 异常错误码，用于标识异常类型
  private final String msg;   // 异常错误信息，用于描述具体错误原因

  /**
   * 构造函数（仅带错误信息）
   * 使用默认错误码500
   * @param msg 错误信息
   */
  public CustomerException(String msg) {
    this.code = "500";
    this.msg = msg;
  }

  public String getCode() {
    return code;
  }

  public String getMsg() {
    return msg;
  }
}
