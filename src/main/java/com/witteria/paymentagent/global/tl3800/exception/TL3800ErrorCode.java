/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.exception;

import org.springframework.http.HttpStatus;

import com.witteria.paymentagent.global.exception.model.BaseErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TL3800ErrorCode implements BaseErrorCode {
  DEVICE_CHECK_FAILED("TL4001", "단말기 상태 체크에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
  DEVICE_CONNECTION_FAILED("TL4002", "단말기 연결에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
  DEVICE_REBOOT_FAILED("TL4003", "단말기 재시작에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE),
  DEVICE_RESPONSE_TIMEOUT("TL4004", "단말기 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
  DEVICE_COMMUNICATION_ERROR("TL4005", "단말기 통신 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),

  APPROVAL_DECLINED("TL4006", "결제가 승인되지 않았습니다.", HttpStatus.BAD_REQUEST),
  CANCEL_DECLINED("TL4007", "결제 취소가 승인되지 않았습니다.", HttpStatus.BAD_REQUEST),
  INVALID_APPROVAL_REQUEST("TL4008", "잘못된 결제 승인 요청입니다.", HttpStatus.BAD_REQUEST),
  DUPLICATE_APPROVAL("TL4009", "이미 처리된 결제입니다.", HttpStatus.CONFLICT),

  CANCEL_FAILED("TL4010", "결제 취소에 실패했습니다.", HttpStatus.CONFLICT),
  ORIGINAL_TRANSACTION_NOT_FOUND("TL4011", "원거래를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  INVALID_JOBCODE("TL4012", "잘못된 작업코드입니다.", HttpStatus.CONFLICT),

  DEVICE_INTERNAL_ERROR("TL5001", "단말기 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  UNKNOWN_DEVICE_ERROR("TL5002", "알 수 없는 단말기 오류입니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String code;
  private final String message;
  private final HttpStatus status;
}
