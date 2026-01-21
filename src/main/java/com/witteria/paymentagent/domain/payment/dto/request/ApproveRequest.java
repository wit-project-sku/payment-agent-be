/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "ApproveRequest DTO", description = "결제 승인 요청을 위한 데이터 전송")
public class ApproveRequest {

  @NotEmpty(message = "결제할 상품 목록은 필수입니다.")
  @Valid
  @Schema(description = "결제할 상품 목록")
  private List<ProductRequest> items;

  @NotBlank(message = "결제 금액은 필수입니다.")
  @Schema(description = "결제 금액", example = "10")
  private String totalAmount;

  @NotBlank(message = "결제자 전화번호는 필수입니다.")
  @Pattern(regexp = "\\d{10,11}")
  @Schema(description = "결제자 전화번호", example = "01012345678")
  private String phoneNumber;

  @Schema(description = "커스텀 이미지 URL")
  private String customImageUrl;

  @NotNull(message = "배송 여부는 필수입니다.") @Schema(description = "배송 여부 (true = 배송, false = 현장수령)", example = "true")
  private Boolean delivery;
}
