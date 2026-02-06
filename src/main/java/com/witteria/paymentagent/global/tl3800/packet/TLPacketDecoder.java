/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.packet;

import static com.witteria.paymentagent.global.tl3800.proto.Proto.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.witteria.paymentagent.global.tl3800.proto.JobCode;
import com.witteria.paymentagent.global.tl3800.proto.Proto;

public class TLPacketDecoder {

  private TLPacketDecoder() {}

  public static TLPacket parseStrict(byte[] responseFrame) {

    if (responseFrame == null || responseFrame.length < HEADER_BYTES + 2) {
      throw new IllegalArgumentException("프레임 길이 부족");
    }
    if ((responseFrame[0] & 0xFF) != STX) {
      throw new IllegalArgumentException("STX 불일치");
    }

    int p = 1;

    byte[] idBytes = Arrays.copyOfRange(responseFrame, p, p + CATMID_LEN);
    p += CATMID_LEN;

    byte[] dtBytes = Arrays.copyOfRange(responseFrame, p, p + DATETIME_LEN);
    p += DATETIME_LEN;

    JobCode job = JobCode.of(responseFrame[p++]);
    byte response = responseFrame[p++];

    int dataLen = (responseFrame[p++] & 0xFF) | ((responseFrame[p++] & 0xFF) << 8);

    int posEtx = HEADER_BYTES + dataLen;
    int posBcc = posEtx + 1;
    int expectedTotal = posBcc + 1;

    if (responseFrame.length < expectedTotal) {
      throw new IllegalArgumentException(
          String.format(
              "프레임이 완전하지 않습니다. 수신길이=%d, 기대길이=%d (dataLen=%d)",
              responseFrame.length, expectedTotal, dataLen));
    }

    if ((responseFrame[posEtx] & 0xFF) != (ETX & 0xFF)) {
      throw new IllegalArgumentException(
          String.format(
              "ETX 불일치: 위치=%d, 수신값=0x%02X (dataLen=%d, headerLen=%d)",
              posEtx, responseFrame[posEtx] & 0xFF, dataLen, HEADER_BYTES));
    }

    // 3) BCC 검증 (STX~ETX XOR)
    byte calcBcc = Proto.bccXor(responseFrame, 0, posEtx);
    int responseBcc = responseFrame[posBcc] & 0xFF;

    if ((calcBcc & 0xFF) != responseBcc) {
      throw new IllegalArgumentException(
          String.format(
              "BCC 불일치: 계산값=0x%02X, 수신값=0x%02X (ETX위치=%d, dataLen=%d, total=%d, bufLen=%d)",
              calcBcc & 0xFF, responseBcc, posEtx, dataLen, expectedTotal, responseFrame.length));
    }

    // 4) 본문 추출
    byte[] data = Arrays.copyOfRange(responseFrame, HEADER_BYTES, HEADER_BYTES + dataLen);

    // 5) 문자열 필드 정리
    String catStr = Proto.printableOrHex(idBytes);
    String dtStr = new String(dtBytes, StandardCharsets.US_ASCII);

    return new TLPacket(catStr, dtStr, job, response, dataLen, data);
  }

  public static TLPacket parseLenient(byte[] frame) {

    int p = 1;

    byte[] idBytes = Arrays.copyOfRange(frame, p, p + CATMID_LEN);
    p += CATMID_LEN;

    byte[] dtBytes = Arrays.copyOfRange(frame, p, p + DATETIME_LEN);
    p += DATETIME_LEN;

    JobCode job = JobCode.of(frame[p++]);
    byte resp = frame[p++];

    int dataLen = (frame[p++] & 0xFF) | ((frame[p++] & 0xFF) << 8);

    int maxData = Math.max(0, frame.length - HEADER_BYTES - 2);
    int actualLen = Math.min(dataLen, maxData);

    byte[] data = Arrays.copyOfRange(frame, HEADER_BYTES, HEADER_BYTES + actualLen);

    return new TLPacket(
        Proto.printableOrHex(idBytes),
        new String(dtBytes, StandardCharsets.US_ASCII),
        job,
        resp,
        dataLen,
        data);
  }
}
