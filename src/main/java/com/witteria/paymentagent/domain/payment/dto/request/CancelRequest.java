/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "CancelRequest DTO", description = "결제 취소 요청을 위한 데이터 전송")
public class CancelRequest {

  @NotBlank(message = "취소 구분 코드는 필수입니다.")
  @Schema(description = "취소 구분 코드 (요청 전문 취소: 1, 직전 거래 취소: 2", example = "2")
  @Pattern(regexp = "[1-2]")
  private String cancelType;

  @NotBlank(message = "거래 구분 코드는 필수입니다.")
  @Schema(description = "거래 구분 코드 (IC 신용승인: 1)", example = "1")
  @Pattern(regexp = "1")
  private String transactionType;

  @NotBlank(message = "취소 요청 금액은 필수입니다.")
  @Pattern(regexp = "\\d{1,10}")
  @Schema(description = "취소 요청 금액", example = "10")
  private String cancelAmount;

  //  @NotBlank(message = "세금 항목은 필수입니다.")
  //  @Pattern(regexp = "\\d{1,8}")
  //  @Schema(description = "세금", example = "0")
  //  private String tax;
  //
  //  @NotBlank(message = "봉사료 항목은 필수입니다.")
  //  @Pattern(regexp = "\\d{1,8}")
  //  @Schema(description = "봉사료", example = "0")
  //  private String svc;
  //
  //  @NotBlank(message = "할부개월 항목은 필수입니다.")
  //  @Pattern(regexp = "\\d{2}")
  //  @Schema(description = "할부개월(00: 일시불)", example = "00")
  //  private String inst;
  //
  //  @NotNull(message = "할부개월 항목은 필수입니다.")
  //  @Schema(description = "서명 여부 (true: 서명, false: 비서명)", example = "true")
  //  private Boolean noSign;

  @NotBlank(message = "승인번호 항목은 필수입니다.")
  @Pattern(regexp = "\\d{1,12}")
  @Schema(description = "승인번호", example = "03304901")
  private String approvalNumber;

  @NotNull(message = "원거래일자 항목은 필수입니다.") @JsonFormat(pattern = "yyyyMMdd")
  @Schema(description = "원거래일자(YYYYMMDD)", example = "20260101")
  private LocalDate originalDate;

  @NotNull(message = "원거래시간 항목은 필수입니다.") @JsonFormat(pattern = "HHmmss")
  @Schema(description = "원거래시간(hhmmss)", example = "123030")
  private LocalTime originalTime;

  //  @Size(max = 200)
  //  @Schema(description = "부가 정보 (선택)", example = "")
  //  private String extra;
}
