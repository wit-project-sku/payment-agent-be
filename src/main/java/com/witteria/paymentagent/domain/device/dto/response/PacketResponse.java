/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "PacketResponse DTO", description = "단말기 공통 응답 반환")
public class PacketResponse {

  @Schema(description = "단말기 식별자", example = "TL3800-001")
  private String terminalId;

  @Schema(description = "요청 성공 여부", example = "true")
  private boolean success;

  @Schema(description = "요청 JobCode", example = "B")
  private String jobCode;

  @Schema(description = "응답 수신 시각", example = "2026-01-19T14:30:55")
  private LocalDateTime responseAt;
}
