/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.witteria.paymentagent.domain.payment.dto.request.ApproveRequest;
import com.witteria.paymentagent.domain.payment.dto.request.CancelRequest;
import com.witteria.paymentagent.domain.payment.dto.response.PaymentResponse;
import com.witteria.paymentagent.global.response.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "결제", description = "키오스크 결제 관련 API")
@RequestMapping("/api/payments")
public interface PaymentController {

  @Operation(summary = "결제 승인", description = "단말기의 결제 요청을 수행하고, 결과를 중앙 서버로 전송합니다.")
  @PostMapping
  ResponseEntity<BaseResponse<PaymentResponse>> approve(
      @Parameter(description = "결제 승인 정보") @RequestBody @Valid ApproveRequest request);

  @Operation(summary = "[관리자] 결제 취소", description = "결제 내역의 취소 요청을 수행하고, 결과를 중앙 서버로 전송합니다.")
  @PostMapping("/dev/{payment-id}/cancel")
  ResponseEntity<BaseResponse<PaymentResponse>> cancel(
      @Parameter(description = "결제 취소 정보") @RequestBody @Valid CancelRequest request);
}
