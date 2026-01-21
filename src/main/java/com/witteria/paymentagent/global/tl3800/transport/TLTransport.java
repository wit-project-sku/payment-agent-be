/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.transport;

/**
 * TL3800 단말과의 저수준 전송 계층 인터페이스.
 *
 * <p>이 인터페이스는 "어떻게 보내고 읽는가"만 책임지며, 오류 의미 해석은 상위 계층(TL3800Client)에서 수행한다.
 */
public interface TLTransport extends AutoCloseable {

  /**
   * 물리적 연결 오픈
   *
   * @throws Exception 포트 오픈 실패, 장치 미존재 등
   */
  void open() throws Exception;

  /**
   * 바이트 블록 전송
   *
   * @throws Exception write 실패
   */
  void write(byte[] bytes) throws Exception;

  /**
   * 지정한 길이만큼 수신될 때까지 블로킹 읽기
   *
   * @param buf 수신 버퍼
   * @param len 기대 길이
   * @param timeoutMs 타임아웃(ms)
   * @return 실제 읽은 바이트 수
   * @throws Exception 타임아웃, read 오류
   */
  int readFully(byte[] buf, int len, int timeoutMs) throws Exception;

  /**
   * 1바이트 읽기
   *
   * @param timeoutMs 타임아웃(ms)
   * @return 읽은 바이트 (0~255), 없으면 -1
   * @throws Exception read 오류
   */
  int readByte(int timeoutMs) throws Exception;

  @Override
  void close();
}
