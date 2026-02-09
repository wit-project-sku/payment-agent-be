/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.config;

import java.io.File;

import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.fazecast.jSerialComm.SerialPort;

import lombok.Getter;

@Getter
@Configuration
public class TL3800Config {

  @Value("${tl3800.terminal-id}")
  private String terminalId;

  @Value("${tl3800.baud-rate}")
  private int baudRate;

  @Value("${tl3800.data-bits}")
  private int dataBits;

  @Value("${tl3800.stop-bits}")
  private int stopBits;

  @Value("${tl3800.parity}")
  private int parity;

  @Value("${tl3800.ack-wait-ms}")
  private int ackWaitMs;

  @Value("${tl3800.resp-wait-ms}")
  private int respWaitMs;

  @Value("${tl3800.max-ack-retry}")
  private int maxAckRetry;

  private String port;

  @PostConstruct
  public void initPort() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      this.port = detectWindowsPort();
    } else {
      this.port = detectMacPort();
    }
  }

  private String detectMacPort() {

    File dev = new File("/dev");
    File[] ports = dev.listFiles((dir, name) -> name.startsWith("cu.PL2303"));

    if (ports == null || ports.length == 0) {
      throw new IllegalStateException("TL3800 USB 시리얼을 찾을 수 없음 (mac/linux)");
    }

    return "/dev/" + ports[0].getName();
  }

  private String detectWindowsPort() {

    SerialPort[] ports = SerialPort.getCommPorts();

    List<String> usbHints = Arrays.asList(
        "usb", "serial", "pl2303", "prolific", "cp210", "silicon labs", "ftdi", "uart"
    );

    // 후보 1차: 설명에 힌트가 있는 포트
    for (SerialPort p : ports) {
      String desc = safeLower(p.getPortDescription());
      String name = safeLower(p.getDescriptivePortName());
      boolean looksUsbSerial = usbHints.stream().anyMatch(h -> desc.contains(h) || name.contains(h));
      if (!looksUsbSerial) continue;

      if (tryOpenAndVerify(p)) {
        return p.getSystemPortName();
      }
    }

    // 후보 2차: 전부 순회 (마지막 수단)
    for (SerialPort p : ports) {
      if (tryOpenAndVerify(p)) {
        return p.getSystemPortName();
      }
    }

    throw new IllegalStateException("TL3800 USB 시리얼을 찾을 수 없음 (windows)");
  }

  private boolean tryOpenAndVerify(SerialPort port) {
    try {
      port.setBaudRate(baudRate);
      port.setNumDataBits(dataBits);
      port.setNumStopBits(stopBits);
      port.setParity(parity);

      // READ 타임아웃은 짧게: 탐지 단계에서 오래 걸리면 안 됨
      port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, ackWaitMs, ackWaitMs);

      if (!port.openPort()) return false;

      // 방법 1) 단말기가 연결 시 자동으로 ACK/STX를 주는 경우: 짧게 읽어서 확인
      byte[] buf = new byte[64];
      int n = port.readBytes(buf, buf.length);

      if (n > 0) {
        for (int i = 0; i < n; i++) {
          int b = buf[i] & 0xFF;
          if (b == 0x06 /* ACK */ || b == 0x02 /* STX */) {
            return true;
          }
        }
      }

      // 방법 2) 일부 단말기는 "아무 것도 먼저 안 줌" → 여기서는 false 처리하고,
      return false;
    } catch (Exception ignored) {
      return false;
    } finally {
      try {
        if (port != null && port.isOpen()) port.closePort();
      } catch (Exception ignored) {}
    }
  }

  private String safeLower(String s) {
    return s == null ? "" : s.toLowerCase();
  }
}
