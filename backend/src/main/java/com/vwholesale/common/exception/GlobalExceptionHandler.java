package com.vwholesale.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.vwholesale.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLogin(NotLoginException ex) {
        return ApiResponse.fail(401, "登录已过期，请重新登录");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(Exception ex) {
        return ApiResponse.fail(400, "参数校验失败");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.fail(500, "服务器内部错误");
    }
}
