/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.request;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.nio.ByteBuffer;

import org.springframework.stereotype.Component;

import com.witteria.paymentagent.domain.payment.dto.request.ApproveRequest;
import com.witteria.paymentagent.domain.payment.dto.request.CancelRequest;
import com.witteria.paymentagent.global.config.TL3800Config;
import com.witteria.paymentagent.global.tl3800.proto.JobCode;
import com.witteria.paymentagent.global.tl3800.proto.Proto;
import com.witteria.paymentagent.global.tl3800.proto.TLPacket;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TL3800RequestBuilder {

  private final TL3800Config tl3800Config;

  /** A: 단말기 상태체크 */
  public TLPacket checkDevice() {

    return TLPacket.builder()
        .catOrMid(tl3800Config.getTerminalId())
        .jobCode(JobCode.A)
        .data(new byte[0])
        .build();
  }

  /** R: 단말기 리셋 */
  public TLPacket rebootDevice() {

    return TLPacket.builder()
        .catOrMid(tl3800Config.getTerminalId())
        .jobCode(JobCode.R)
        .data(new byte[0])
        .build();
  }

  /** B: 거래 승인 (필수 30B + AuthNo(12, space) + D8(8) + 확장길이(2,"00") = 52B) */
  public TLPacket approve(ApproveRequest request) {

    ByteBuffer bb = ByteBuffer.allocate(30);
    bb.put("1".getBytes(US_ASCII)); // 거래구분 1
    bb.put(Proto.asciiLeftPadZero(request.getTotalAmount(), 10)); // 금액(10)
    bb.put(Proto.asciiLeftPadZero("", 8)); // 부가세(8)
    bb.put(Proto.asciiLeftPadZero("", 8)); // 봉사료(8)
    bb.put(Proto.asciiLeftPadZero("", 2)); // 할부(2)
    bb.put("1".getBytes(US_ASCII)); // 서명여부(1)

    bb.flip();
    byte[] payload = new byte[bb.remaining()];
    bb.get(payload);

    return TLPacket.builder()
        .catOrMid(tl3800Config.getTerminalId())
        .jobCode(JobCode.B)
        .data(payload)
        .build();
  }

  /**
   * C: 거래 취소
   *
   * <p>프로토콜: 취소구분코드(1) + 거래구분코드(1) + 승인금액(10) + 세금(8) + 봉사료(8) + 할부개월(2) + 서명여부(1) + 승인번호(12) +
   * 원거래일자(8) + 원거래시간(6) + 부가정보길이(2) + 부가정보(N) → DataLength = 57 or 57+2+N
   */
  public TLPacket cancel(CancelRequest request) {

    String signFlag = "1";
    String normAmount = lPad(request.getCancelAmount(), 10);
    String normTax = lPad("0", 8);
    String normSvc = lPad("0", 8);
    String normInst = lPad("00", 2);
    String normApprovalNo = rPad(request.getApprovalNumber());
    String extraLenStr = lPad("0", 2);
    String normOriginalDate = request.getOriginalDate().toString().replace("-", ""); // yyyyMMdd
    String normOriginalTime = request.getOriginalTime().toString().replace(":", ""); // HHmmss

    String cancelTxt =
        request.getCancelType() // 1
            + request.getTransactionType() // 1
            + normAmount // 10
            + normTax // 8
            + normSvc // 8
            + normInst // 2
            + signFlag // 1
            + normApprovalNo // 12
            + normOriginalDate // 8
            + normOriginalTime // 6
            + extraLenStr; // 2

    System.out.println(cancelTxt);

    byte[] data = cancelTxt.getBytes(US_ASCII);

    return TLPacket.builder()
        .catOrMid(tl3800Config.getTerminalId())
        .jobCode(JobCode.C)
        .data(data)
        .build();
  }

  private static String lPad(String src, int len) {

    if (src == null) {
      src = "";
    }

    if (src.length() >= len) {
      return src.substring(src.length() - len);
    }

    return String.valueOf('0').repeat(len - src.length()) + src;
  }

  private static String rPad(String src) {

    if (src == null) {
      src = "";
    }

    if (src.length() >= 12) {
      return src.substring(0, 12);
    }

    return src + " ".repeat(12 - src.length());
  }
}
