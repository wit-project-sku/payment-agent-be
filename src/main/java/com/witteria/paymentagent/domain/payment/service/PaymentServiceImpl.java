/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

import com.witteria.paymentagent.domain.payment.dto.request.ApproveRequest;
import com.witteria.paymentagent.domain.payment.dto.request.CancelRequest;
import com.witteria.paymentagent.domain.payment.dto.request.ProductRequest;
import com.witteria.paymentagent.domain.payment.dto.response.PaymentResponse;
import com.witteria.paymentagent.global.client.CentralPaymentClient;
import com.witteria.paymentagent.global.config.TL3800Config;
import com.witteria.paymentagent.global.exception.CustomException;
import com.witteria.paymentagent.global.tl3800.exception.TL3800ErrorCode;
import com.witteria.paymentagent.global.tl3800.gateway.TL3800Gateway;
import com.witteria.paymentagent.global.tl3800.packet.TLPacket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

  private final TL3800Gateway gateway;
  private final TL3800Config tl3800Config;
  private final CentralPaymentClient client;

  @Override
  public PaymentResponse approve(ApproveRequest request) {

    final TLPacket packet;
    try {
      packet = gateway.approve(request);
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.error("[TL3800] 단말기 결제 승인 처리 중 예외 발생", e);
      throw e;
    }

    if (packet == null) {
      client.notifyMessage("[TL3800] gateway.approve()가 null TLPacket을 반환했습니다.");
      throw new IllegalStateException("[TL3800] gateway.approve()가 null TLPacket을 반환했습니다.");
    }

    if (packet.isFail()) {
      client.notifyMessage("[TL3800] 결제가 승인되지 않았습니다.");
      throw new CustomException(TL3800ErrorCode.APPROVAL_DECLINED);
    }

    final PaymentResponse response;
    try {
      response =
          parsePaymentResponse(
              packet,
              request.getPhoneNumber(),
              request.getCustomImageUrl(),
              request.getDelivery(),
              request.getItems());
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.error("[TL3800] 승인 응답 파싱 중 예외 발생", e);
      throw e;
    }

    try {
      client.notifyMessage("[TL3800] 단말기 결제 승인 요청 성공");
      client.notifyApproveResult(response);
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.warn("[TL3800] 승인 결과 알림 전송 중 예외 발생(승인 결과는 정상 반환)", e);
    }

    return response;
  }

  @Override
  public PaymentResponse cancel(CancelRequest request) {

    final TLPacket packet;
    try {
      packet = gateway.cancel(request);
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.error("[TL3800] 단말기 결제 취소 처리 중 예외 발생", e);
      throw e;
    }

    if (packet == null) {
      throw new IllegalStateException("[TL3800] gateway.cancel()가 null TLPacket을 반환했습니다.");
    }

    if (packet.isFail()) {
      throw new CustomException(TL3800ErrorCode.CANCEL_DECLINED);
    }

    final PaymentResponse response;
    try {
      response = parsePaymentResponse(packet, null, null, null, null);
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.error("[TL3800] 취소 응답 파싱 중 예외 발생", e);
      throw e;
    }

    // 알림 전송 실패로 결제 취소 결과 자체가 실패로 처리되지 않도록 분리
    try {
      client.notifyMessage("[TL3800] 단말기 결제 취소 요청 성공");
      client.notifyCancelResult(response);
    } catch (Exception e) {
      client.notifyMessage("[TL3800] " + e.getMessage());
      log.warn("[TL3800] 취소 결과 알림 전송 중 예외 발생(취소 결과는 정상 반환)", e);
    }

    return response;
  }

  private PaymentResponse parsePaymentResponse(
      TLPacket packet,
      String phoneNumber,
      String customImageUrl,
      Boolean delivery,
      List<ProductRequest> items) {

    int pos = 0; // DATA 영역 시작 (TLPacket.data()는 순수 DATA)

    System.out.println(
        new String(packet.getData(), pos, packet.getDataLen(), StandardCharsets.US_ASCII).trim());

    // 거래구분코드 (1)
    pos += 1;

    // 거래매체 (1)
    pos += 1;

    // 카드번호 (20)
    String cardNumber = new String(packet.getData(), pos, 20, StandardCharsets.US_ASCII).trim();
    pos += 20;

    // 승인금액 (10)
    String totalAmountRaw = new String(packet.getData(), pos, 10, StandardCharsets.US_ASCII).trim();
    String totalAmount = totalAmountRaw.replaceFirst("^0+(?!$)", "");
    pos += 10;

    // 세금/잔여횟수 (8)
    pos += 8;

    // 봉사료/사용횟수 (8)
    pos += 8;

    // 할부개월 (2)
    pos += 2;

    // 승인번호 (12)
    String approvalNumber = new String(packet.getData(), pos, 12, StandardCharsets.US_ASCII).trim();
    pos += 12;

    // 매출일자 (8)
    String approvedDate = new String(packet.getData(), pos, 8, StandardCharsets.US_ASCII).trim();
    pos += 8;

    // 매출시간 (6)
    String approvedTime = new String(packet.getData(), pos, 6, StandardCharsets.US_ASCII).trim();
    pos += 6;

    // 거래고유번호 (12)
    String transactionId = new String(packet.getData(), pos, 12, StandardCharsets.US_ASCII).trim();

    return PaymentResponse.builder()
        .terminalId(tl3800Config.getTerminalId())
        .transactionId(transactionId)
        .approvedDate(approvedDate)
        .approvedTime(approvedTime)
        .totalAmount(totalAmount)
        .approvalNumber(approvalNumber)
        .cardNumber(cardNumber)
        .phoneNumber(phoneNumber)
        .customImageUrl(customImageUrl)
        .delivery(delivery)
        .items(items)
        .build();
  }
}
