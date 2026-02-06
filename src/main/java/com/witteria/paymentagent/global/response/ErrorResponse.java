/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "ErrorResponse DTO", description = "공통 에러 응답 형식")
public class ErrorResponse {

  @Schema(description = "요청 성공 여부", example = "true")
  private boolean success;

  @Schema(description = "HTTP 상태 코드", example = "200")
  private int code;

  @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
  private String message;
}
