/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "ProductRequest DTO", description = "상품 정보 데이터 전송")
public class ProductRequest {

  @NotNull(message = "상품 식별자는 필수입니다.") @Schema(description = "상품 식별자", example = "1")
  private Long productId;

  @NotNull(message = "상품 수량은 필수입니다.") @Schema(description = "상품 수량", example = "1", minimum = "1")
  private Integer quantity;
}
