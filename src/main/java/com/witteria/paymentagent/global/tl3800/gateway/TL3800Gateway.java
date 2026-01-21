/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.gateway;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.witteria.paymentagent.domain.payment.dto.request.ApproveRequest;
import com.witteria.paymentagent.domain.payment.dto.request.CancelRequest;
import com.witteria.paymentagent.global.tl3800.client.TL3800Client;
import com.witteria.paymentagent.global.tl3800.proto.TLPacket;
import com.witteria.paymentagent.global.tl3800.request.TL3800RequestBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class TL3800Gateway {

  private final TL3800Client client;
  private final TL3800RequestBuilder requestBuilder;
  private final ReentrantLock lock = new ReentrantLock(true);

  private TLPacket call(Supplier<TLPacket> supplier) {

    lock.lock();
    try {
      client.open();
      return client.requestResponse(supplier.get());
    } catch (Exception e) {
      throw new RuntimeException("TL3800 통신 실패", e);
    } finally {
      try {
        client.close();
      } catch (Exception e) {
        log.warn("TL3800 client close failed", e);
      }
      lock.unlock();
    }
  }

  /** 장치체크 요청: A, 장치체크 응답: a */
  public TLPacket checkDevice() {

    return call(requestBuilder::checkDevice);
  }

  /** 단말기 재시작 요청: R */
  public TLPacket rebootDevice() {

    return call(requestBuilder::rebootDevice);
  }

  /** 거래승인 요청: B, 거래승인 응답: b */
  public TLPacket approve(ApproveRequest request) {

    return call(() -> requestBuilder.approve(request));
  }

  /** 거래취소 요청: C, 거래취소 응답: c */
  public TLPacket cancel(CancelRequest request) {

    return call(() -> requestBuilder.cancel(request));
  }
}
