/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.transport;

import org.springframework.stereotype.Component;

import com.fazecast.jSerialComm.SerialPort;
import com.witteria.paymentagent.global.config.TL3800Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public final class SerialPortTransport implements TLTransport {

  private final TL3800Config tl3800Config;
  private SerialPort port;

  @Override
  public void open() {
    port = SerialPort.getCommPort(tl3800Config.getPort());

    port.setComPortParameters(
        tl3800Config.getBaudRate(),
        tl3800Config.getDataBits(),
        tl3800Config.getStopBits(),
        tl3800Config.getParity());
    port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

    if (!port.openPort()) {
      throw new IllegalStateException("Cannot open serial port: " + tl3800Config.getPort());
    }

    // 일부 단말은 DTR / RTS 필요
    try {
      port.setDTR();
    } catch (Throwable ignore) {
    }
    try {
      port.setRTS();
    } catch (Throwable ignore) {
    }

    // 기본 블로킹 타임아웃 (응답 기준)
    port.setComPortTimeouts(
        SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
        tl3800Config.getRespWaitMs(),
        tl3800Config.getRespWaitMs());

    drainInput(250);

    log.info(
        "[Serial] OPEN {} {}bps {}-{}-{} timeout={}ms",
        tl3800Config.getPort(),
        tl3800Config.getBaudRate(),
        tl3800Config.getDataBits(),
        tl3800Config.getStopBits(),
        tl3800Config.getParity(),
        tl3800Config.getRespWaitMs());
  }

  @Override
  public void close() {
    if (port == null) {
      return;
    }

    try {
      drainInput(50);
    } catch (Exception ignore) {
    }

    try {
      port.closePort();
    } catch (Exception ignore) {
    }

    log.info("[Serial] CLOSE {}", tl3800Config.getPort());
  }

  @Override
  public void write(byte[] bytes) {
    int written = port.writeBytes(bytes, bytes.length);
    if (written != bytes.length) {
      throw new IllegalStateException("Short write: " + written + "/" + bytes.length);
    }
  }

  @Override
  public int readFully(byte[] buf, int len, int timeoutMs) {
    if (timeoutMs > 0) {
      port.setComPortTimeouts(
          SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
          timeoutMs,
          timeoutMs);
    }

    int offset = 0;
    while (offset < len) {
      int r = port.readBytes(buf, len - offset);
      if (r < 0) {
        throw new IllegalStateException("Read error");
      }
      offset += r;
    }
    return offset;
  }

  @Override
  public int readByte(int timeoutMs) {
    if (timeoutMs > 0) {
      port.setComPortTimeouts(
          SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
          timeoutMs,
          timeoutMs);
    }

    byte[] b = new byte[1];
    int r = port.readBytes(b, 1);
    return (r == 1) ? (b[0] & 0xFF) : -1;
  }

  /** 입력 버퍼 드레인 (라이브러리 purge 의존 제거) */
  private void drainInput(int windowMs) {
    long end = System.currentTimeMillis() + windowMs;
    byte[] buf = new byte[256];

    port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

    try {
      while (System.currentTimeMillis() < end) {
        int available;
        try {
          available = port.bytesAvailable();
        } catch (Throwable e) {
          int r = port.readBytes(buf, buf.length);
          if (r <= 0) break;
          continue;
        }

        if (available <= 0) break;

        int toRead = Math.min(available, buf.length);
        int r = port.readBytes(buf, toRead);
        if (r <= 0) break;

        try {
          Thread.sleep(5);
        } catch (InterruptedException ignore) {
        }
      }
    } finally {
      port.setComPortTimeouts(
          SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
          tl3800Config.getRespWaitMs(),
          tl3800Config.getRespWaitMs());
    }
  }
}
