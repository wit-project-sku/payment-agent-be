/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.gateway;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.witteria.paymentagent.domain.payment.dto.request.ApproveRequest;
import com.witteria.paymentagent.domain.payment.dto.request.CancelRequest;
import com.witteria.paymentagent.global.client.CentralPaymentClient;
import com.witteria.paymentagent.global.tl3800.builder.TL3800RequestBuilder;
import com.witteria.paymentagent.global.tl3800.client.TL3800Client;
import com.witteria.paymentagent.global.tl3800.packet.TLPacket;
import com.witteria.paymentagent.global.tl3800.transport.TLTransport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TL3800Gateway {

  private final TL3800Client client;
  private final TL3800RequestBuilder requestBuilder;
  private final ReentrantLock lock = new ReentrantLock(true);
  private final TLTransport transport;
  private final CentralPaymentClient centralPaymentClient;

  public TLPacket call(Supplier<TLPacket> supplier) {

    lock.lock();
    try {
      // 1. 포트 열기 (열려 있으면 무시)
      transport.open();

      // 2. 입력 버퍼 드레인 (이전 잔류 데이터 제거)
      transport.drainInputBuffer(250);

      // 3. 요청 패킷 생성
      TLPacket requestPacket = supplier.get();

      // 4. 요청 전송 및 응답 처리
      return client.requestResponse(requestPacket);
    } catch (Exception e) {
      centralPaymentClient.notifyMessage(e.getMessage());
      log.error("[TL3800] 통신 실패", e);
      throw new RuntimeException("TL3800 통신 실패", e);
    } finally {
      try {
        transport.close(); // ★ 반드시 닫기
      } catch (Exception e) {
        centralPaymentClient.notifyMessage(e.getMessage());
        log.warn("[TL3800] 포트 종료 실패", e);
      }
      lock.unlock();
    }
  }

  /** 장치체크 요청: A, 장치체크 응답: a */
  public TLPacket checkDevice() {

    centralPaymentClient.notifyMessage("[TL3800] 장치체크 요청 - A");
    return call(requestBuilder::checkDevice);
  }

  /** 단말기 재시작 요청: R */
  public TLPacket rebootDevice() {

    centralPaymentClient.notifyMessage("[TL3800] 단말기 재시작 요청 - R");
    return call(requestBuilder::rebootDevice);
  }

  /** 거래승인 요청: B, 거래승인 응답: b */
  public TLPacket approve(ApproveRequest request) {

    centralPaymentClient.notifyMessage("[TL3800] 거래승인 요청 - B");
    return call(() -> requestBuilder.approve(request));
  }

  /** 거래취소 요청: C, 거래취소 응답: c */
  public TLPacket cancel(CancelRequest request) {

    centralPaymentClient.notifyMessage("[TL3800] 거래취소 요청 - C");
    return call(() -> requestBuilder.cancel(request));
  }
}
