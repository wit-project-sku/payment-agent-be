/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.witteria.paymentagent.domain.payment.dto.response.PaymentResponse;
import com.witteria.paymentagent.global.response.TL3800Response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CentralPaymentClient {

  private final RestClient centralApiClient;

  /** 결제 승인 결과 중앙 서버 통보 - 결제는 이미 완료된 상태이므로 - 중앙 서버 통신 실패 시 예외를 던지지 않는다 */
  public void notifyApproveResult(PaymentResponse response) {

    try {
      centralApiClient
          .post()
          .uri("/api/payments/approve")
          .body(response)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("[중앙서버 통보 실패 - 결제승인] 거래고유번호={}", response.getTransactionId(), e);
    }
  }

  /** 결제 취소 결과 중앙 서버 통보 - 취소 완료 이후 중앙 통보 실패는 별도 보상 처리 대상 */
  public void notifyCancelResult(PaymentResponse response) {

    try {
      centralApiClient
          .post()
          .uri("/api/payments/cancel")
          .body(response)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("[중앙서버 통보 실패 - 결제취소] 거래고유번호={}", response.getTransactionId(), e);
    }
  }

  public void notifyMessage(String message) {

    try {
      centralApiClient
          .post()
          .uri("/api/devices")
          .body(TL3800Response.builder().message(message).build())
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("[중앙서버 통보 실패 - 오류] 오류메세지={}", message, e);
    }
  }
}
