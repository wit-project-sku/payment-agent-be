/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.builder;

import static com.witteria.paymentagent.global.tl3800.proto.Proto.HEADER_BYTES;
import static java.nio.charset.StandardCharsets.US_ASCII;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.witteria.paymentagent.global.tl3800.packet.TLPacket;
import com.witteria.paymentagent.global.tl3800.proto.JobCode;

@Component
public class TL3800ResponseBuilder {

  private static final int TAIL_BYTES = 2; // ETX + BCC

  public TLPacket parse(byte[] raw) throws IllegalArgumentException {

    if (raw == null || raw.length < HEADER_BYTES + TAIL_BYTES) {
      throw new IllegalArgumentException("패킷 길이가 너무 짧습니다.");
    }

    byte stx = raw[0];
    if (stx != 0x02) {
      throw new IllegalArgumentException("STX가 잘못되었습니다.");
    }

    byte[] terminalIdBytes = Arrays.copyOfRange(raw, 1, 17);
    String terminalId = new String(terminalIdBytes, US_ASCII).trim();

    byte[] dateTimeBytes = Arrays.copyOfRange(raw, 17, 31);
    String dateTime14 = new String(dateTimeBytes, US_ASCII).trim();

    byte jobByte = raw[31];
    JobCode jobCode = JobCode.of(jobByte);

    byte responseCode = raw[32];

    int dataLen = (raw[33] & 0xFF) | ((raw[34] & 0xFF) << 8);

    if (raw.length < HEADER_BYTES + dataLen + TAIL_BYTES) {
      throw new IllegalArgumentException("DataLength가 패킷 길이보다 큽니다.");
    }
    byte[] data = Arrays.copyOfRange(raw, HEADER_BYTES, HEADER_BYTES + dataLen);

    // 5️⃣ TLPacket 빌드
    return TLPacket.builder()
        .catOrMid(terminalId)
        .dateTime14(dateTime14)
        .jobCode(jobCode)
        .responseCode(responseCode)
        .dataLen(dataLen)
        .data(data)
        .build();
  }

  /** 예: JobCode A(a)에 대한 Data 파싱 카드모듈상태(1B) + RF모듈상태(1B) + VAN서버상태(1B) + 연동서버상태(1B) */
  public DeviceStatus parseDeviceStatus(TLPacket packet) {

    if (packet.getJobCode() != JobCode.A) {
      throw new IllegalArgumentException("JobCode가 A(a)가 아닙니다.");
    }
    byte[] data = packet.getData();
    if (data.length != 4) {
      throw new IllegalArgumentException("단말기 상태 데이터 길이가 4가 아닙니다.");
    }

    return new DeviceStatus((char) data[0], (char) data[1], (char) data[2], (char) data[3]);
  }

  // 단말기 상태 DTO
  public record DeviceStatus(char cardModule, char rfModule, char vanServer, char linkedServer) {}
}
