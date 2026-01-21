/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.witteria.paymentagent.domain.device.dto.response.PacketResponse;
import com.witteria.paymentagent.global.tl3800.proto.TLPacket;

@Component
public class PacketMapper {

  public PacketResponse toPacketResponse(TLPacket packet) {

    return PacketResponse.builder()
        .terminalId(packet.catOrMid())
        .success(!packet.isFail())
        .jobCode(packet.jobCode().name())
        .responseAt(LocalDateTime.now())
        .build();
  }
}
