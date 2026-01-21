/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.witteria.paymentagent.domain.device.dto.response.PacketResponse;
import com.witteria.paymentagent.domain.device.service.DeviceService;
import com.witteria.paymentagent.global.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DeviceControllerImpl implements DeviceController {

  private final DeviceService deviceService;

  @Override
  public ResponseEntity<BaseResponse<PacketResponse>> checkDevice() {

    return ResponseEntity.ok(BaseResponse.success(deviceService.checkDevice()));
  }

  @Override
  public ResponseEntity<BaseResponse<PacketResponse>> rebootDevice() {

    return ResponseEntity.ok(BaseResponse.success(deviceService.rebootDevice()));
  }
}
