/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.packet;

import com.witteria.paymentagent.global.tl3800.proto.JobCode;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class TLPacket {

  private final String catOrMid;
  private final String dateTime14;
  private final JobCode jobCode;
  private final byte responseCode;
  private final int dataLen;
  private final byte[] data;

  public boolean isFail() {
    return responseCode != 0x00;
  }
}
