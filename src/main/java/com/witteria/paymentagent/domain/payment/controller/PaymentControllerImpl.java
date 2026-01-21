/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.witteria.paymentagent.domain.payment.dto.request.ApproveRequest;
import com.witteria.paymentagent.domain.payment.dto.request.CancelRequest;
import com.witteria.paymentagent.domain.payment.dto.response.PaymentResponse;
import com.witteria.paymentagent.domain.payment.service.PaymentService;
import com.witteria.paymentagent.global.response.BaseResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PaymentControllerImpl implements PaymentController {

  private final PaymentService paymentService;

  @Override
  public ResponseEntity<BaseResponse<PaymentResponse>> approve(ApproveRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success(201, "결제가 완료되었습니다.", paymentService.approve(request)));
  }

  @Override
  public ResponseEntity<BaseResponse<PaymentResponse>> cancel(CancelRequest request) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BaseResponse.success(201, "결제 취소가 완료되었습니다.", paymentService.cancel(request)));
  }
}
