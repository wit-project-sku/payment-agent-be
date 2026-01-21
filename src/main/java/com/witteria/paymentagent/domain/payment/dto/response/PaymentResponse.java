/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.dto.response;

import java.util.List;

import com.witteria.paymentagent.domain.payment.dto.request.ProductRequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(title = "ApproveResponse DTO", description = "결제 승인 응답")
public class PaymentResponse {

  @Schema(description = "단말기 식별자", example = "7804097001")
  private String terminalId;

  @Schema(description = "거래 식별자", example = "260121000015")
  private String transactionId;

  @Schema(description = "승인 날짜", example = "20260101")
  private String approvedDate;

  @Schema(description = "승인 시간", example = "123456")
  private String approvedTime;

  @Schema(description = "승인 금액", example = "10000")
  private String totalAmount;

  @Schema(description = "카드사 승인번호", example = "25155007")
  private String approvalNumber;

  @Schema(description = "마스킹 카드번호", example = "000053651063********")
  private String cardNumber;

  @Schema(description = "결제자 전화번호", example = "01012345678")
  private String phoneNumber;

  @Schema(description = "커스텀 이미지 URL")
  private String customImageUrl;

  @Schema(description = "배송 여부 (true = 배송, false = 현장수령)", example = "true")
  private Boolean delivery;

  @Schema(description = "결제할 상품 목록")
  private List<ProductRequest> items;
}
