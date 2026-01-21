/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.global.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI customOpenAPI() {

    return new OpenAPI()
        // API 기본 정보
        .info(
            new Info()
                .title("💳 WIT Global 결제 중개 API 명세서")
                .version("v1.1.0")
                .description(
                    """
                    ## 주의사항
                    - 파일 업로드 크기 제한: 5MB (1개 파일 기준)

                    ## 문의
                    - 기술 문의: unijun0109@gmail.com
                    """)
                .contact(new Contact().name("Witteria").email("unijun0109@gmail.com")));
  }

  @Bean
  public GroupedOpenApi apiGroup() {
    return GroupedOpenApi.builder().group("api").pathsToMatch("/api/**").build();
  }
}
