/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "TL3800Response DTO", description = "단말기 로그 응답 반환")
public class TL3800Response {

  @Schema(description = "단말기 메세지", example = "[TL3800] 단말기 상태 체크")
  private String message;
}
