/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.mapper;

import org.springframework.stereotype.Component;

import com.witteria.paymentagent.domain.device.dto.response.PacketResponse;
import com.witteria.paymentagent.global.tl3800.packet.TLPacket;

@Component
public class PacketMapper {

  public PacketResponse toPacketResponse(TLPacket packet) {

    return PacketResponse.builder()
        .terminalId(packet.getCatOrMid())
        .jobCode(packet.getJobCode().name())
        .responseAt(packet.getDateTime14())
        .dataLen(packet.getDataLen())
        .build();
  }
}
