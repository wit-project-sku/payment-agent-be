/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.proto;

import com.witteria.paymentagent.global.exception.CustomException;
import com.witteria.paymentagent.global.tl3800.exception.TL3800ErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "작업 코드 Enum")
public enum JobCode {
  @Schema(description = "장치체크 요청")
  A('A', "장치체크 요청"),
  @Schema(description = "장치체크 응답")
  a('a', "장치체크 응답"),
  @Schema(description = "거래승인 요청")
  B('B', "거래승인 요청"),
  @Schema(description = "거래승인 응답")
  b('b', "거래승인 응답"),
  @Schema(description = "거래취소 요청")
  C('C', "거래취소 요청"),
  @Schema(description = "거래취소 응답")
  c('c', "거래취소 응답"),
  @Schema(description = "단말기 재시작 요청")
  R('R', "단말기 재시작 요청"),
  @Schema(description = "ACK/NACK 미전송 규칙")
  EVENT('@', "ACK/NACK 미전송 규칙");

  private final char code;
  private final String ko;

  public static JobCode of(byte b) {

    char c = (char) b;
    for (JobCode jc : values()) {
      if (jc.code == c) {
        return jc;
      }
    }
    throw new CustomException(TL3800ErrorCode.INVALID_JOBCODE);
  }
}
