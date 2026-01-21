/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.proto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public final class Proto {

  // 제어 코드
  public static final byte STX = 0x02;
  public static final byte ETX = 0x03;

  // 고정 길이
  public static final int HEADER_BYTES = 35; // STX~DataLen
  public static final int CATMID_LEN = 16;
  public static final int DATETIME_LEN = 14; // YYYYMMDDhhmmss

  public static byte[] asciiLeftPadZero(String s, int len) {

    byte[] dst = new byte[len];
    byte[] src = s == null ? new byte[0] : s.getBytes(StandardCharsets.US_ASCII);
    int copy = Math.min(src.length, len);
    int start = len - copy; // 우측 정렬
    for (int i = 0; i < start; i++) {
      dst[i] = '0';
    }
    System.arraycopy(src, 0, dst, start, copy);
    return dst;
  }

  public static String nowYYYYMMDDhhmmss() {

    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
  }

  /** STX ~ ETX(포함)까지 XOR. */
  public static byte bccXor(byte[] frame, int from, int toInclusive) {

    byte x = 0x00;
    for (int i = from; i <= toInclusive; i++) {
      x ^= frame[i];
    }
    return x;
  }

  public static String printableOrHex(byte[] bytes) {

    if (bytes == null) {
      return "";
    }

    boolean printable = true;
    for (byte v : bytes) {
      int u = v & 0xFF;
      // 공백(0x20) 또는 출력 가능 ASCII(0x21~0x7E)만 허용
      if (!(u == 0x20 || (u >= 0x21 && u <= 0x7E))) {
        printable = false;
        break;
      }
    }
    if (printable) {
      String s = new String(bytes, StandardCharsets.US_ASCII);
      // 우측 0x00 패딩 제거 (안전상 한 번 더 제거)
      return s.replaceAll("\u0000+$", "");
    }
    // 비-ASCII가 섞이면 HEX(대문자)로 반환
    return HexFormat.of().withUpperCase().formatHex(bytes);
  }
}
