/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.config;

import java.io.File;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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
    com.fazecast.jSerialComm.SerialPort[] ports =
        com.fazecast.jSerialComm.SerialPort.getCommPorts();

    for (com.fazecast.jSerialComm.SerialPort port : ports) {
      try {
        port.setBaudRate(baudRate);
        port.setNumDataBits(dataBits);
        port.setNumStopBits(stopBits);
        port.setParity(parity);
        port.setComPortTimeouts(
            com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING, ackWaitMs, ackWaitMs);

        if (!port.openPort()) {
          continue;
        }

        // TL3800는 연결 시 ACK(0x06) 또는 STX(0x02)를 응답함
        return port.getSystemPortName();
      } catch (Exception ignored) {
      } finally {
        if (port.isOpen()) {
          port.closePort();
        }
      }
    }
    throw new IllegalStateException("TL3800 USB 시리얼을 찾을 수 없음 (windows)");
  }
}
