/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.tl3800.util;

import java.util.HexFormat;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class HexUtils {

  public static String toHex(byte[] bytes) {

    if (bytes == null) {
      return "";
    }
    return HexFormat.of().formatHex(bytes);
  }

  public static byte[] hexToBytes(String hex) {
    hex = hex.replaceAll("\\s+", "");
    int len = hex.length();
    if (len % 2 != 0) {
      throw new IllegalArgumentException("HEX 길이가 짝수가 아님");
    }

    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    return out;
  }
}
