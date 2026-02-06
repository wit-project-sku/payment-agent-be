/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.transport;

import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.fazecast.jSerialComm.SerialPort;
import com.witteria.paymentagent.global.config.TL3800Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TL-3800 시리얼 포트 통신 전송 계층
 *
 * <p>Thread-safe한 읽기/쓰기 연산 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class SerialPortTransport implements TLTransport {

  private final TL3800Config config;
  private SerialPort port;

  // 동시성 제어를 위한 락
  private final Object readLock = new Object();
  private final Object writeLock = new Object();

  @Override
  public void open() {

    if (port != null && port.isOpen()) {
      log.warn("[Serial] Port already open: {}", config.getPort());
      return;
    }

    port = SerialPort.getCommPort(config.getPort());

    // 포트 파라미터 설정
    port.setComPortParameters(
        config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());

    // 흐름 제어 비활성화
    port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

    // 포트 오픈
    if (!port.openPort()) {
      throw new IllegalStateException(
          "Cannot open serial port: " + config.getPort() + " (already in use or no permission)");
    }

    // DTR/RTS 설정 (일부 TL-3800 단말기 요구사항)
    try {
      port.setDTR();
      port.setRTS();
      log.debug("[Serial] DTR/RTS enabled");
    } catch (Throwable e) {
      log.debug("[Serial] DTR/RTS not supported: {}", e.getMessage());
    }

    // 기본 블로킹 타임아웃 설정
    setPortTimeouts(config.getRespWaitMs());

    // 입력 버퍼 비우기
    drainInputBuffer(250);

    log.info(
        "[Serial] OPEN {} - {}bps {}{}{} timeout={}ms",
        config.getPort(),
        config.getBaudRate(),
        config.getDataBits(),
        getParityChar(config.getParity()),
        config.getStopBits(),
        config.getRespWaitMs());
  }

  @Override
  public void close() {
    if (port == null) {
      return;
    }

    synchronized (writeLock) {
      synchronized (readLock) {
        try {
          if (port.isOpen()) {
            drainInputBuffer(50);
            port.closePort();
            log.info("[Serial] CLOSE {}", config.getPort());
          }
        } catch (Exception e) {
          log.warn("[Serial] Error closing port: {}", e.getMessage());
        } finally {
          port = null;
        }
      }
    }
  }

  @Override
  public void write(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return;
    }

    synchronized (writeLock) {
      ensureOpen();

      int offset = 0;
      int remaining = bytes.length;

      while (remaining > 0) {
        int written = port.writeBytes(bytes, remaining, offset);

        if (written <= 0) {
          throw new IllegalStateException("시리얼 포트 쓰기 실패 (written=" + written + ")");
        }

        offset += written;
        remaining -= written;
      }
    }
  }

  @Override
  public int readFully(byte[] buf, int len, int timeoutMs) throws TimeoutException {

    if (buf == null || len <= 0 || len > buf.length) {
      throw new IllegalArgumentException("유효하지 않은 버퍼이거나 길이입니다.");
    }

    synchronized (readLock) {
      ensureOpen();

      // 타임아웃 설정
      if (timeoutMs > 0) {
        setPortTimeouts(timeoutMs);
      }

      long deadline = System.currentTimeMillis() + timeoutMs;
      int offset = 0;

      try {
        while (offset < len) {
          long remaining = deadline - System.currentTimeMillis();
          if (remaining <= 0) {
            throw new TimeoutException(String.format("Read timeout: got %d/%d bytes", offset, len));
          }

          // 남은 시간으로 타임아웃 재설정
          if (timeoutMs > 0 && remaining < timeoutMs) {
            setPortTimeouts((int) remaining);
          }

          // 올바른 오프셋으로 읽기
          int toRead = len - offset;
          int read = port.readBytes(buf, toRead, offset);

          if (read < 0) {
            throw new IllegalStateException(
                String.format("Serial read error at offset %d/%d", offset, len));
          }

          if (read == 0) {
            // 타임아웃 발생 (블로킹 모드에서 0 반환)
            throw new TimeoutException(String.format("Read timeout: got %d/%d bytes", offset, len));
          }

          offset += read;

          if (log.isTraceEnabled()) {
            log.trace("[Serial] << {} bytes (total {}/{})", read, offset, len);
          }
        }

        return offset;

      } finally {
        // 기본 타임아웃 복원
        if (timeoutMs > 0) {
          setPortTimeouts(config.getRespWaitMs());
        }
      }
    }
  }

  @Override
  public int readByte(int timeoutMs) throws TimeoutException {
    synchronized (readLock) {
      ensureOpen();

      // 타임아웃 설정
      if (timeoutMs > 0) {
        setPortTimeouts(timeoutMs);
      }

      byte[] buf = new byte[1];

      try {
        int read = port.readBytes(buf, 1);

        if (read < 0) {
          throw new IllegalStateException("Serial read error");
        }

        if (read == 0) {
          // 타임아웃 (블로킹 모드에서 0 반환)
          return -1;
        }

        return buf[0] & 0xFF;

      } finally {
        // 기본 타임아웃 복원
        if (timeoutMs > 0) {
          setPortTimeouts(config.getRespWaitMs());
        }
      }
    }
  }

  /** 포트 타임아웃 설정 */
  private void setPortTimeouts(int timeoutMs) {
    if (port == null) {
      return;
    }

    port.setComPortTimeouts(
        SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, timeoutMs, timeoutMs);
  }

  /**
   * 입력 버퍼 비우기
   *
   * @param windowMs 최대 대기 시간 (밀리초)
   */
  public void drainInputBuffer(int windowMs) {

    if (port == null || !port.isOpen()) {
      return;
    }

    long deadline = System.currentTimeMillis() + windowMs;
    byte[] buf = new byte[256];
    int totalDrained = 0;

    // Non-blocking 모드로 전환
    port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

    try {
      while (System.currentTimeMillis() < deadline) {
        int available;

        try {
          available = port.bytesAvailable();
        } catch (Throwable e) {
          // bytesAvailable() 미지원 시 read로 시도
          int read = port.readBytes(buf, buf.length);
          if (read > 0) {
            totalDrained += read;
          }
          if (read <= 0) {
            break;
          }
          continue;
        }

        if (available <= 0) {
          break;
        }

        int toRead = Math.min(available, buf.length);
        int read = port.readBytes(buf, toRead);

        if (read > 0) {
          totalDrained += read;
        }

        if (read <= 0) {
          break;
        }

        // CPU 사용량 감소
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      if (totalDrained > 0) {
        log.debug("[Serial] Drained {} bytes from input buffer", totalDrained);
      }

    } finally {
      // 블로킹 모드 복원
      setPortTimeouts(config.getRespWaitMs());
    }
  }

  /** 포트 오픈 상태 확인 */
  public void ensureOpen() {

    if (port == null || !port.isOpen()) {
      throw new IllegalStateException("Serial port is not open");
    }
  }

  /** Parity 값을 문자로 변환 (로깅용) */
  private char getParityChar(int parity) {

    return switch (parity) {
      case SerialPort.NO_PARITY -> 'N';
      case SerialPort.ODD_PARITY -> 'O';
      case SerialPort.EVEN_PARITY -> 'E';
      case SerialPort.MARK_PARITY -> 'M';
      case SerialPort.SPACE_PARITY -> 'S';
      default -> '?';
    };
  }
}
