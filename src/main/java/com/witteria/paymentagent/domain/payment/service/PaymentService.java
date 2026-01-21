/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.payment.service;

import com.witteria.paymentagent.domain.payment.dto.request.ApproveRequest;
import com.witteria.paymentagent.domain.payment.dto.request.CancelRequest;
import com.witteria.paymentagent.domain.payment.dto.response.PaymentResponse;

public interface PaymentService {

  PaymentResponse approve(ApproveRequest request);

  PaymentResponse cancel(CancelRequest request);
}
