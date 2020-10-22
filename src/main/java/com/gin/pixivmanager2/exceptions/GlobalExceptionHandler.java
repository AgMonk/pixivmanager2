package com.gin.pixivmanager2.exceptions;


import com.gin.pixivmanager2.entity.response.ResError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;

/**
 * @author bx002
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 参数校验
     *
     * @param e 异常
     * @return 报错
     */
    @ResponseBody
    @ExceptionHandler({BindException.class})
    public ResError illegalArgument(BindException e) {
        StringBuilder message = new StringBuilder();
        List<ObjectError> allErrors = e.getAllErrors();
        for (ObjectError allError : allErrors) {
            message.append(allError.getDefaultMessage()).append(" ");
        }
        log.warn(message.toString());
        return new ResError(3000, message.toString());
    }

    /**
     * 参数校验
     *
     * @param e 异常
     * @return 报错
     */
    @ResponseBody
    @ExceptionHandler({ConstraintViolationException.class})
    public ResError illegalArgument(ConstraintViolationException e) {
        String message = e.getMessage();
        message = message.substring(message.indexOf(".")+1);
        return new ResError(3000, message);
    }

    @ResponseBody
    @ExceptionHandler({BusinessException.class})
    public ResError businessExceptionHandler(BusinessException e) {
        log.warn(e.getMessage());
        return new ResError(e.getCode(), e.getMessage());
    }


    @ResponseBody
    @ExceptionHandler({Exception.class})
    public ResError handler(Exception e) {
        e.printStackTrace();
        return null;
    }


}
