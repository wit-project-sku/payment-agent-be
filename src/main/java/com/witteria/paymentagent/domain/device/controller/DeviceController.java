/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.witteria.paymentagent.domain.device.dto.response.PacketResponse;
import com.witteria.paymentagent.global.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "단말기", description = "결제 단말기(TL3800) 관련 API")
@RequestMapping("/api/devices")
public interface DeviceController {

  @Operation(summary = "단말기 상태 확인", description = "단말기 연결 여부, 통신 가능 상태, 초기화 필요 여부를 확인합니다.")
  @PostMapping("/status")
  ResponseEntity<BaseResponse<PacketResponse>> checkDevice();

  @Operation(summary = "단말기 재시작", description = "통신 장애, 카드 리더 오류 시 복구를 위해 단말기를 재시작합니다.")
  @PostMapping("/reboot/dev")
  ResponseEntity<BaseResponse<PacketResponse>> rebootDevice();
}
