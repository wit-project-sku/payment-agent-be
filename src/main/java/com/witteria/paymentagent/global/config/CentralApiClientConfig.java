/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CentralApiClientConfig {

  @Value("${central.base-url}")
  private String baseUrl;

  @Bean
  public RestClient centralApiClient(RestClient.Builder builder) {

    return builder.baseUrl(baseUrl).build();
  }
}
