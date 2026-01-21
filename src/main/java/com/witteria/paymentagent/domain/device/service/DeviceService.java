/* 
 * Copyright (c) WIT Global 
 */
package com.witteria.paymentagent.domain.device.service;

import com.witteria.paymentagent.domain.device.dto.response.PacketResponse;

public interface DeviceService {

  PacketResponse checkDevice();

  PacketResponse rebootDevice();
}
