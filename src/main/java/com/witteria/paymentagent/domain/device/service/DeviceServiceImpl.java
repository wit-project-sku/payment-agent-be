/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.service;

import org.springframework.stereotype.Service;

import com.witteria.paymentagent.domain.device.dto.response.PacketResponse;
import com.witteria.paymentagent.domain.device.mapper.PacketMapper;
import com.witteria.paymentagent.global.exception.CustomException;
import com.witteria.paymentagent.global.tl3800.exception.TL3800ErrorCode;
import com.witteria.paymentagent.global.tl3800.gateway.TL3800Gateway;
import com.witteria.paymentagent.global.tl3800.packet.TLPacket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

  private final TL3800Gateway gateway;
  private final PacketMapper packetMapper;

  @Override
  public PacketResponse checkDevice() {

    try {
      TLPacket packet = gateway.checkDevice();

      if (packet.isFail()) {
        throw new CustomException(TL3800ErrorCode.DEVICE_CHECK_FAILED);
      }

      return packetMapper.toPacketResponse(packet);
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("단말기 상태 확인 중 통신 오류", e);
      throw new CustomException(TL3800ErrorCode.DEVICE_CONNECTION_FAILED);
    }
  }

  @Override
  public PacketResponse rebootDevice() {

    try {
      TLPacket packet = gateway.rebootDevice();

      if (packet.isFail()) {
        throw new CustomException(TL3800ErrorCode.DEVICE_REBOOT_FAILED);
      }

      return packetMapper.toPacketResponse(packet);
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("단말기 재시작 중 통신 오류", e);
      throw new CustomException(TL3800ErrorCode.DEVICE_CONNECTION_FAILED);
    }
  }
}
