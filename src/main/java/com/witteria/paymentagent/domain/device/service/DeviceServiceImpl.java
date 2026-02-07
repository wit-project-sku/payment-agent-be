/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.service;

import org.springframework.stereotype.Service;

import com.witteria.paymentagent.domain.device.dto.response.PacketResponse;
import com.witteria.paymentagent.domain.device.mapper.PacketMapper;
import com.witteria.paymentagent.global.client.CentralPaymentClient;
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
  private final CentralPaymentClient client;

  @Override
  public PacketResponse checkDevice() {

    try {
      TLPacket packet = gateway.checkDevice();

      if (packet.isFail()) {
        client.notifyMessage("[TL3800] 단말기 상태 체크 실패(패킷)");
        throw new CustomException(TL3800ErrorCode.DEVICE_CHECK_FAILED);
      }

      client.notifyMessage("[TL3800] 단말기 상태 체크 성공");
      return packetMapper.toPacketResponse(packet);
    } catch (CustomException e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      throw e;
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.error("단말기 상태 확인 중 통신 오류", e);
      throw new CustomException(TL3800ErrorCode.DEVICE_CONNECTION_FAILED);
    }
  }

  @Override
  public PacketResponse rebootDevice() {

    try {
      TLPacket packet = gateway.rebootDevice();

      if (packet.isFail()) {
        client.notifyMessage("[TL3800] 단말기 재시작 실패(패킷)");
        throw new CustomException(TL3800ErrorCode.DEVICE_REBOOT_FAILED);
      }

      client.notifyMessage("[TL3800] 단말기 재시작 성공");
      return packetMapper.toPacketResponse(packet);
    } catch (CustomException e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      throw e;
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.error("단말기 재시작 중 통신 오류", e);
      throw new CustomException(TL3800ErrorCode.DEVICE_CONNECTION_FAILED);
    }
  }
}
