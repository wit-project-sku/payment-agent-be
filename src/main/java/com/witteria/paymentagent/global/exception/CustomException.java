/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.exception;

import com.witteria.paymentagent.global.exception.model.BaseErrorCode;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

  private final BaseErrorCode errorCode;

  public CustomException(BaseErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
