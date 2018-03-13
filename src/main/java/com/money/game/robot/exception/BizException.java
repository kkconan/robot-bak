package com.money.game.robot.exception;


import com.money.game.robot.constant.ErrorEnum;
import com.money.game.core.constant.ResponseData;
import com.money.game.core.exception.BaseException;

public class BizException extends BaseException {

    private static final long serialVersionUID = 5117980659382038192L;
   private String code;

    public BizException() {
        super();
    }

    public BizException(String message) {
        super(message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ErrorEnum errorEnum) {
        super(errorEnum.getValue());
        this.code = errorEnum.getCode();
    }

    public BizException(ResponseData responseData) {
        super(responseData.getErrorCode());
        this.code = responseData.getErrorMessage();
    }

    @Override
    public String getCode() {
        return code;
    }
    @Override
    public void setCode(String code) {
        this.code = code;
    }

}
