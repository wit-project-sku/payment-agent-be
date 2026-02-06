/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.client;

import static com.witteria.paymentagent.global.tl3800.proto.Proto.DATETIME_LEN;
import static com.witteria.paymentagent.global.tl3800.proto.Proto.HEADER_BYTES;
import static com.witteria.paymentagent.global.tl3800.proto.Proto.STX;
import static com.witteria.paymentagent.global.tl3800.util.HexUtils.hexToBytes;
import static com.witteria.paymentagent.global.tl3800.util.HexUtils.toHex;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.witteria.paymentagent.global.config.TL3800Config;
import com.witteria.paymentagent.global.tl3800.packet.TLPacket;
import com.witteria.paymentagent.global.tl3800.packet.TLPacketDecoder;
import com.witteria.paymentagent.global.tl3800.packet.TLPacketEncoder;
import com.witteria.paymentagent.global.tl3800.proto.JobCode;
import com.witteria.paymentagent.global.tl3800.transport.TLTransport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TL3800Client implements AutoCloseable {

  private final TL3800Config tl3800Config;
  private final TLTransport transport;

  // 결제 최종 응답까지 여유 있게
  private static final int FOLLOWUP_WINDOW_MS = 180_000;

  // TL 헤더 내 오프셋: STX(1) + ID(16) + DT(14) + JOB(1) + RESP(1) + LEN(2)
  private static final int POS_DT = 1 + 16; // 17
  private static final int POS_JOB = POS_DT + 14; // 31
  private static final int POS_LEN = POS_JOB + 1 + 1; // 33

  @Override
  public void close() {
    try {
      transport.close();
    } catch (Exception ignore) {
    }
  }

  public TLPacket requestResponse(TLPacket requestPacket) throws Exception {

    final byte[] requestFrame = TLPacketEncoder.encode(requestPacket);
    log.info(
        "[TL3800] >> 송신 업무코드={} 길이={} HEX={}",
        requestPacket.getJobCode(),
        requestFrame.length,
        toHex(requestFrame));

    JobCode expectedJob = expectedResponseJob(requestPacket.getJobCode());
    int tries = 0;

    while (true) {
      // 1. 요청 송신
      transport.write(requestFrame);
      sleepQuiet();

      // 2. ACK/NACK 대기 (제어 신호만 처리)
      Integer ack = waitAckNack(tl3800Config.getAckWaitMs());

      if (ack == null) {
        throw new IllegalStateException("ACK/NACK 응답 시간 초과");
      }

      if (ack == 0x15) { // NACK
        if (++tries <= tl3800Config.getMaxAckRetry()) {
          log.warn("[TL3800] << NACK 수신 → 재시도 {}/{}", tries, tl3800Config.getMaxAckRetry());
          continue;
        }
        throw new IllegalStateException("NACK 수신 (재시도 횟수 초과)");
      }

      if (ack != 0x06) {
        throw new IllegalStateException("알 수 없는 제어 응답: 0x" + Integer.toHexString(ack));
      }

      log.info("[TL3800] << ACK 수신");

      // 3. 응답 프레임 수신 (STX 기준으로 한 번만 읽음)
      TLPacket response;
      try {
        response = readOrFollowUp(expectedJob, requestPacket);
      } catch (IllegalArgumentException e) {
        log.warn("[TL3800] 응답 프레임 파싱 실패 → 재전송 대기: {}", e.getMessage());
        continue;
      }

      if (response != null) {
        return response;
      }

      // 4. 응답이 이벤트였거나 유실된 경우 → 재전송 대기
      return waitResendAndReturnExpected(expectedJob);
    }
  }

  /** 헤더 sanity 검사: 날짜 14자리 숫자/잡코드 유효/데이터 길이 상한 */
  private boolean isSaneHeader(byte[] header) {

    if (header == null || header.length < HEADER_BYTES) {
      log.warn("[TL3800][검증] 헤더가 null이거나 길이가 부족함: len={}", header == null ? 0 : header.length);
      return false;
    }

    // 1) 단말기 ID 검증 (POS_ID ~ POS_DT-1)
    for (int i = 1; i < POS_DT; i++) {
      int v = header[i] & 0xFF;
      // ID는 0x00 또는 '0'~'9'
      if (v != 0x00 && (v < '0' || v > '9')) {
        log.warn(
            "[TL3800][검증] 단말기 ID 바이트 오류 index={} 값=0x{} HEX={}",
            i,
            String.format("%02X", v),
            toHex(header));
        return false;
      }
    }

    // 2) 날짜(DateTime) 검증: 14자리 숫자만 허용
    for (int i = POS_DT; i < POS_DT + DATETIME_LEN; i++) {
      int v = header[i] & 0xFF;
      if (v < '0' || v > '9') {
        log.warn(
            "[TL3800][검증] DateTime 바이트 오류 index={} 값=0x{} HEX={}",
            i,
            String.format("%02X", v),
            toHex(header));
        return false;
      }
    }

    // 3) JobCode 검증
    int job = header[POS_JOB] & 0xFF;
    if (!isKnownJob(job)) {
      log.warn(
          "[TL3800][검증] 알 수 없는 업무 코드 값=0x{} HEX={}", String.format("%02X", job), toHex(header));
      return false;
    }

    // 4) 데이터 길이 합리성
    int dataLen = (header[POS_LEN] & 0xFF) | ((header[POS_LEN + 1] & 0xFF) << 8);
    if (dataLen > 4096) {
      log.warn("[TL3800][검증] 데이터 길이 비정상 dataLen={} HEX={}", dataLen, toHex(header));
      return false;
    }
    return true;
  }

  private boolean isKnownJob(int job) {
    try {
      JobCode.of((byte) job);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private JobCode jobFromHeader(byte[] header) {

    JobCode code;
    try {
      code = JobCode.of(header[POS_JOB]);
    } catch (IllegalArgumentException ex) {
      log.warn("[TL3800] 헤더의 업무 코드가 유효하지 않습니다. → 재전송 대기: {}", ex.getMessage());
      return null;
    }

    return code;
  }

  private int dataLenFromHeader(byte[] header) {

    int dataLen = (header[POS_LEN] & 0xFF) | ((header[POS_LEN + 1] & 0xFF) << 8);

    log.info(
        "[TL3800][DATA] dataLen={} (HEX={} {})",
        dataLen,
        String.format("%02X", header[POS_LEN]),
        String.format("%02X", header[POS_LEN + 1]));

    return dataLen;
  }

  /** EVENT 프레임의 tail(데이터+ETX+BCC)을 읽고 버린다. EVENT는 ACK/NACK 미전송. */
  private void consumeEventFrame(byte[] requestFrame) throws Exception {

    // 헤더에서 Data Length 추출 (LE)
    int dataLen = (requestFrame[33] & 0xFF) | ((requestFrame[34] & 0xFF) << 8);

    // 데이터 + ETX + BCC 읽기
    byte[] dataTail = new byte[dataLen + 2]; // DataLength + ETX(1) + BCC(1)
    transport.readFully(dataTail, dataTail.length, tl3800Config.getRespWaitMs());

    // 로그 찍고 버림
    log.info("[TL3800] EVENT 프레임 소비됨, 길이={}, Data&Tail HEX={}", dataLen, toHex(dataTail));
  }

  /**
   * 헤더를 읽은 뒤 프레임 파싱을 시도. 첫 프레임이 EVENT면 내용을 읽고 버린 뒤 후속 프레임을 기다리고, 비-EVENT는 TLPacket으로 파싱. 파싱 실패
   * 시(NACK 전송됨) null을 반환해 상위에서 재전송을 받게 함.
   */
  private TLPacket readOrFollowUp(JobCode expectedJob, TLPacket requestPacket) throws Exception {

    final int MAX_EVENT_SKIP = 5;
    int eventCount = 0, offset = 0, n;
    byte[] responseFrame;

    if (expectedJob.equals(JobCode.b) || expectedJob.equals(JobCode.c)) {
      responseFrame = new byte[232];
    } else if (expectedJob.equals(JobCode.a)) {
      responseFrame = new byte[41];
    } else {
      responseFrame = new byte[37];
    }
    n = transport.readFully(responseFrame, responseFrame.length, tl3800Config.getRespWaitMs());
    log.info("[TL3800] 응답 전문 수신 HEX={}", hexToBytes(toHex(Arrays.copyOf(responseFrame, n))));

    while (offset < n) {
      if (responseFrame[offset] != 0x02) {
        offset++;
        continue; // STX가 아닌 경우 스킵
      }

      if (n - offset < 35) break; // 헤더 미완성
      int dataLen =
          (responseFrame[offset + 33] & 0xFF) | ((responseFrame[offset + 34] & 0xFF) << 8);
      int frameLen = 35 + dataLen + 2; // 헤더 + 데이터 + ETX + BCC

      if (offset + frameLen > n) break; // 프레임 미완성
      byte[] frame = Arrays.copyOfRange(responseFrame, offset, offset + frameLen);
      offset += frameLen;

      for (int i = 1; i <= 16; i++) {
        if (frame[i] == 0x20) {
          frame[i] = 0x00;
        }
      }

      JobCode job = JobCode.of(frame[POS_JOB]);
      if (job == JobCode.EVENT) {
        eventCount++;

        log.info(
            "[TL3800] EVENT 프레임 수신 ({}/{}) → 소비 후 계속 진행 (예상 업무 코드={})",
            eventCount,
            MAX_EVENT_SKIP,
            expectedJob);

        if (eventCount >= MAX_EVENT_SKIP) {
          throw new IllegalStateException("EVENT 프레임이 연속으로 수신되어 응답을 받을 수 없음");
        }
      } else {
        log.info("[TL3800] nonEVENT 프레임 수신 - {}", frame);
        return readTailParseAndAck(frame, requestPacket); // 요청 응답 처리
      }
    }

    return null;
  }

  /** 파싱 실패 후 재전송을 받아 기대 잡코드가 올 때까지 대기 */
  private TLPacket waitResendAndReturnExpected(JobCode expected) throws Exception {

    final int maxRetries = tl3800Config.getMaxAckRetry(); // NACK 재시도 제한
    int tries = 0;
    long deadline = System.currentTimeMillis() + tl3800Config.getRespWaitMs();

    while (System.currentTimeMillis() < deadline) {
      try {
        TLPacket responsePacket = readNextFrameAndAck(50); // 50ms 단위로 프레임 읽기

        if (responsePacket.getJobCode() == JobCode.EVENT) {
          // EVENT 프레임은 무시하고 후속 프레임 대기
          log.info("[TL3800] EVENT 프레임 수신 → 후속 프레임 대기");
          responsePacket = waitFollowUp(expected);
        }

        if (matchesExpected(expected, responsePacket.getJobCode())) {
          return responsePacket; // 예상 응답이면 바로 반환
        }

        log.warn("[TL3800] 예상치 못한 업무코드 수신: {} (예상 {})", responsePacket.getJobCode(), expected);

      } catch (IllegalArgumentException ex) {
        log.warn("[TL3800] 프레임 파싱 실패: {} → 재시도", ex.getMessage());
      } catch (IllegalStateException ex) {
        // ACK/NACK 시간 초과 등
        if (++tries <= maxRetries) {
          log.warn("[TL3800] 통신 실패 → 재시도 {}/{}", tries, maxRetries);
          Thread.sleep(50); // 짧은 대기 후 재시도
          continue;
        } else {
          throw new IllegalStateException("[TL3800] 최대 재시도 초과", ex);
        }
      }

      // CPU 과부하 방지
      Thread.sleep(5);
    }

    throw new IllegalStateException("[TL3800] 응답 타임아웃: 지정된 시간 내에 올바른 패킷 수신 실패");
  }

  /** FOLLOWUP_WINDOW 동안 다음 프레임들을 계속 수신(매번 ACK)하여 expected 잡코드가 오면 반환 */
  private TLPacket waitFollowUp(JobCode expected) throws Exception {

    long deadline = System.currentTimeMillis() + FOLLOWUP_WINDOW_MS;

    while (true) {
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0) {
        break;
      }

      try {
        int perTry = (int) Math.min(tl3800Config.getRespWaitMs(), remaining);
        TLPacket nextFrame = readNextFrameAndAck(perTry);

        log.info(
            "[TL3800] << RECV(seq) job={} dataLen={}",
            nextFrame.getJobCode(),
            nextFrame.getDataLen());

        if (nextFrame.getJobCode() == JobCode.EVENT) {
          // readNextFrameAndAck 안에서 이미 consumeEventFrame 처리
          continue;
        }
        if (matchesExpected(expected, nextFrame.getJobCode())) {
          return nextFrame;
        }

        log.warn(
            "[TL3800] unexpected job={} (expect={}) — keep waiting",
            nextFrame.getJobCode(),
            expected);
      } catch (IllegalArgumentException e) {
        log.warn("[TL3800] follow-up parse failed: {}", e.getMessage());
      } catch (IllegalStateException e) {
        log.debug("[TL3800] follow-up per-try timeout: {}", e.getMessage());
      }
    }

    throw new IllegalStateException(
        "Follow-up window exceeded (" + FOLLOWUP_WINDOW_MS + " ms) without final " + expected);
  }

  /** 다음 STX부터 한 프레임을 읽어 검증 후 ACK 회신. EVENT는 tail만 읽고 버리고 계속 대기, Non-EVENT는 파싱. */
  private TLPacket readNextFrameAndAck(int waitMs) throws Exception {

    long deadline = System.currentTimeMillis() + waitMs;
    int retries = 0;
    final int maxRetries = 5;

    while (System.currentTimeMillis() < deadline) {
      int remaining = (int) (deadline - System.currentTimeMillis());
      if (remaining <= 0) break;

      int b = transport.readByte(Math.min(50, remaining));
      if (b < 0) {
        continue;
      }

      if (b == 0x02) { // STX 수신
        byte[] header = readResponseHeader();

        if (!isSaneHeader(header)) {
          log.warn("[TL3800] follow-up header sanity failed → resync (HEX={})", toHex(header));
          if (++retries > maxRetries) {
            throw new IllegalStateException("[TL3800] 헤더 재시도 횟수 초과");
          }
          continue;
        }

        JobCode job = jobFromHeader(header);
        if (job == JobCode.EVENT) {
          consumeEventFrame(header);
          log.info("[TL3800] << EVENT 프레임 수신 (무시)");
          continue; // EVENT는 무시하고 다음 프레임 대기
        }

        return readTailParseAndAck(header, null); // 정상 프레임 처리
      }

      log.debug("[TL3800] << 무시된 바이트: 0x{}", String.format("%02X", b));
      Thread.sleep(5); // CPU 과부하 방지
    }

    throw new IllegalStateException("[TL3800] follow-up frame timeout");
  }

  /** 데이터 + 꼬리(ETX+BCC) 수신 → 파싱 → (성공시 ACK / 실패시 NACK 회신 후 예외) */
  private TLPacket readTailParseAndAck(byte[] responseFrame, TLPacket requestPacket) {

    log.info("[TL3800] 헤더 수신 HEX={}", toHex(responseFrame));
    int dataLen = dataLenFromHeader(responseFrame);
    int tailLen = dataLen + 2; // ETX + BCC
    log.info("[TL3800] << 수신: 데이터 길이={}, 읽어야 하는 길이={}", dataLen, tailLen);

    if (responseFrame.length - HEADER_BYTES != tailLen) {
      try {
        transport.write(new byte[] {0x15});
      } catch (Exception ignore) {
      }
      log.warn(
          "[TL3800] >> NACK 전송 (본문 길이 부족: 수신={} 필요한={})",
          responseFrame.length - HEADER_BYTES,
          tailLen);
      throw new IllegalArgumentException("본문 길이 부족");
    }
    log.info("[TL3800] << 수신 완료: 전체 길이={} HEX={}", responseFrame.length, toHex(responseFrame));

    try {
      // 1차: 엄격 검증 (STX/ETX/BCC 모두 확인)
      TLPacket responsePacket = TLPacketDecoder.parseStrict(responseFrame);

      if (requestPacket != null
          && !matchesExpected(requestPacket.getJobCode(), responsePacket.getJobCode())) {
        log.warn(
            "[TL3800] 요청 업무코드와 응답 업무코드 불일치: 요청={} 응답={}",
            requestPacket.getJobCode(),
            responsePacket.getJobCode());
      }

      transport.write(new byte[] {0x06});
      log.info("[TL3800] >> ACK 전송");
      return responsePacket;
    } catch (IllegalArgumentException ex) {
      log.warn("[TL3800] 엄격 파싱 실패: {} → lenient 파서로 재시도", ex.getMessage());

      TLPacket pkt = TLPacketDecoder.parseLenient(responseFrame);

      try {
        transport.write(new byte[] {0x06});
        log.info("[TL3800] >> ACK 전송 (lenient 파싱 후)");
      } catch (Exception ignore) {
      }

      return pkt;
    }
  }

  private Integer waitAckNack(int waitMs) {

    long deadline = System.currentTimeMillis() + waitMs;

    while (System.currentTimeMillis() < deadline) {
      try {
        int b = transport.readByte(50);

        if (b < 0) {
          continue;
        }

        if (b == 0x06) {
          log.info("[TL3800] ACK(0x06) 수신");
          return 0x06;
        }

        if (b == 0x15) {
          log.warn("[TL3800] NACK(0x15) 수신");
          return 0x15;
        }

        // ACK/NACK 이외 바이트
        log.debug("[TL3800] ACK 대기 중 기타 바이트 수신: 0x{}", String.format("%02X", b));

      } catch (Exception e) {
        log.warn("[TL3800] ACK/NACK 대기 중 read 오류", e);
      }
    }

    log.warn("[TL3800] ACK/NACK 대기 시간 초과 ({} ms)", waitMs);
    return null;
  }

  private void sleepQuiet() {
    try {
      Thread.sleep((long) 10);
    } catch (InterruptedException ignored) {
    }
  }

  private byte[] readResponseHeader() throws Exception {

    while (true) {
      // 1. STX 탐색
      int b;
      do {
        b = transport.readByte(tl3800Config.getRespWaitMs());
      } while (b != STX);

      // 2. 후보 헤더 읽기
      byte[] header = new byte[HEADER_BYTES];
      header[0] = STX;

      byte[] rest = new byte[HEADER_BYTES - 1];
      int n = transport.readFully(rest, HEADER_BYTES - 1, tl3800Config.getRespWaitMs());
      if (n != HEADER_BYTES - 1) {
        // 헤더 자체가 깨졌으면 버리고 재동기화
        continue;
      }
      System.arraycopy(rest, 0, header, 1, HEADER_BYTES - 1);

      // 3. SPACE → 0x00 (ID 패딩 보정)
      for (int i = 1; i <= 16; i++) {
        if (header[i] == 0x20) {
          header[i] = 0x00;
        }
      }

      // 4. 헤더 검증
      try {
        log.info("[TL3800] 헤더 검증 시작 HEX={}", hexToBytes(toHex(header)));
        validateHeader(header);
        log.info("[TL3800] 헤더 수신 완료 HEX={}", hexToBytes(toHex(header)));
        return header;
      } catch (IllegalArgumentException ex) {
        log.warn("[TL3800] 헤더 검증 실패 → 재동기화: {}", ex.getMessage());
      }
    }
  }

  /** 요청 잡코드에 대응하는 "기대 응답" 잡코드. 기본은 같은 코드, A→a, B→b 식으로 매핑 */
  private JobCode expectedResponseJob(JobCode code) {

    char c = code.getCode();

    if (c >= 'A' && c <= 'Z') {
      char respChar = Character.toLowerCase(c);
      try {
        return JobCode.of((byte) respChar);
      } catch (IllegalArgumentException ignored) {
        // 대응하는 소문자 JobCode 가 없으면 그냥 원래 값 사용
      }
    }

    return code;
  }

  private void validateHeader(byte[] header) {

    // ID
    for (int i = 1; i <= 16; i++) {
      int v = header[i] & 0xFF;
      if (v != 0x00 && (v < '0' || v > '9')) {
        throw new IllegalArgumentException(
            "단말기 ID 바이트 오류 index=" + i + " val=0x" + Integer.toHexString(v));
      }
    }

    // DateTime
    for (int i = 17; i <= 30; i++) {
      int v = header[i] & 0xFF;
      if (v < '0' || v > '9') {
        throw new IllegalArgumentException(
            "DateTime 바이트 오류 index=" + i + " val=0x" + Integer.toHexString(v));
      }
    }

    // ResponseCode
    if (header[32] != 0x00) {
      throw new IllegalArgumentException("ResponseCode 오류: " + (header[32] & 0xFF));
    }
  }

  /** expected/actual 잡코드가 대소문자만 다른 경우까지 허용 */
  private boolean matchesExpected(JobCode expected, JobCode actual) {
    if (expected == actual) {
      return true;
    }
    if (expected == JobCode.EVENT || actual == JobCode.EVENT) {
      return false;
    }
    return Character.toLowerCase(expected.getCode()) == Character.toLowerCase(actual.getCode());
  }
}
