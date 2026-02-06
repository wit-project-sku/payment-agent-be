/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.packet;

import static com.witteria.paymentagent.global.tl3800.proto.Proto.*;

import java.nio.charset.StandardCharsets;

import com.witteria.paymentagent.global.tl3800.proto.Proto;

public final class TLPacketEncoder {

  private TLPacketEncoder() {}

  public static byte[] encode(TLPacket packet) {

    if (packet.getDateTime14() == null || packet.getDateTime14().length() != 14) {
      throw new IllegalArgumentException("dateTime14는 14자리(YYYYMMDDhhmmss)여야 합니다.");
    }

    int dataLen = packet.getDataLen();
    byte[] data = packet.getData();

    byte[] out = new byte[HEADER_BYTES + dataLen + 2];
    int i = 0;

    out[i++] = STX;

    // CAT/MID (16B)
    byte[] idBytes = packet.getCatOrMid().getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(idBytes, 0, out, i, Math.min(idBytes.length, CATMID_LEN));
    i += CATMID_LEN;

    // DateTime
    byte[] dt = packet.getDateTime14().getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(dt, 0, out, i, DATETIME_LEN);
    i += DATETIME_LEN;

    out[i++] = (byte) packet.getJobCode().getCode();
    out[i++] = packet.getResponseCode();

    // DataLength (LE)
    out[i++] = (byte) (dataLen & 0xFF);
    out[i++] = (byte) ((dataLen >>> 8) & 0xFF);

    // Data
    System.arraycopy(data, 0, out, i, dataLen);
    i += dataLen;

    // ETX
    out[i++] = ETX;

    // BCC
    out[i++] = Proto.bccXor(out, 0, i - 1);

    return out;
  }
}
