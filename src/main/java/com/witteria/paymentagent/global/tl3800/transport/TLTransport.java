/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.transport;

import java.util.concurrent.TimeoutException;

/**
 * TL3800 단말과의 저수준 전송 계층 인터페이스.
 *
 * <p>이 인터페이스는 "어떻게 보내고 읽는가"만 책임지며, 오류 의미 해석은 상위 계층(TL3800Client)에서 수행한다.
 */
public interface TLTransport {

  void open();

  void close();

  void write(byte[] bytes);

  int readFully(byte[] buf, int len, int timeoutMs) throws TimeoutException;

  int readByte(int timeoutMs) throws TimeoutException;

  void drainInputBuffer(int windowMs);
}
