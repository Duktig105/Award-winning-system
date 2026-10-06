package com.example.common;

/**
 * 统一响应结果封装类
 * 用于后端接口与前端交互时，返回标准化的响应数据格式
 * 包含状态码、响应数据和提示信息三部分
 */
@SuppressWarnings("unused")
public class Result {
  private String code;  // 响应状态码：作为前端判断请求成功还是失败的依据（200表示成功）
  private Object data;  // 响应数据：返回给前端的具体业务数据，成功时可能包含数据，失败时可能为null
  private String msg;   // 响应消息：返回给前端的提示信息，失败时包含错误详情，成功时包含成功提示

  /**
   * 成功响应（无返回数据）
   * @return Result 成功响应对象，状态码为200，消息为"请求成功"
   */
  public static Result success() {
    Result result = new Result();
    result.setCode("200");
    result.setMsg("请求成功");
    return result;
  }

  /**
   * 成功响应（带返回数据）
   * @param data 要返回给前端的业务数据
   * @return Result 成功响应对象，状态码为200，消息为"请求成功"，包含业务数据
   */
  public static Result success(Object data) {
    Result result = new Result();
    result.setCode("200");  // 设置成功状态码
    result.setData(data);   // 设置返回数据
    result.setMsg("请求成功");  // 设置成功提示信息
    return result;
  }

  /**
   * 成功响应（带消息和返回数据）
   * @param msg 成功提示信息
   * @param data 要返回给前端的业务数据
   * @return Result 成功响应对象，状态码为200，包含自定义消息和业务数据
   */
  public static Result success(String msg, Object data) {
    Result result = new Result();
    result.setCode("200");  // 设置成功状态码
    result.setData(data);   // 设置返回数据
    result.setMsg(msg);     // 设置自定义成功提示信息
    return result;
  }

  /**
   * 错误响应（使用默认错误码500）
   * @param msg 错误提示信息，描述具体错误原因
   * @return Result 错误响应对象，状态码为500，包含错误信息
   */
  public static Result error(String msg) {
    Result result = new Result();
    result.setCode("500");  // 设置默认错误状态码
    result.setMsg(msg);     // 设置错误提示信息
    return result;
  }

  /**
   * 错误响应（自定义错误码）
   * @param code 自定义错误状态码，可根据业务需求定义不同错误类型
   * @param msg 错误提示信息，描述具体错误原因
   * @return Result 错误响应对象，包含自定义错误码和错误信息
   */
  public static Result error(String code, String msg) {
    Result result = new Result();
    result.setCode(code);   // 设置自定义错误状态码
    result.setMsg(msg);     // 设置错误提示信息
    return result;
  }

  // getter和setter方法：用于访问和修改私有成员变量
  // 注意：这些getter方法被JSON序列化使用，前端通过res.code、res.data、res.msg访问
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }
}
