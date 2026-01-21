/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Value("${cors.allowed-origins}")
  private String[] allowedOrigins;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration config = new CorsConfiguration();

    // 환경 변수에 정의된 출처만 허용
    config.setAllowedOrigins(Arrays.asList(allowedOrigins));
    // 리스트에 작성한 HTTP 메소드 요청만 허용
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    // 리스트에 작성한 헤더들이 포함된 요청만 허용
    config.addAllowedHeader("*");
    // Preflight 요청 결과를 3600초 동안 캐시
    config.setMaxAge(3600L);

    // 모든 경로에 대해 위의 CORS 설정을 적용
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return source;
  }
}
