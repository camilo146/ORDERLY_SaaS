package com.orderly.api.business.application;

import com.orderly.api.business.domain.model.Business;

import java.util.UUID;

/**
 * Application boundary for business queries.
 */
public interface FindBusinessUseCase {

    Business findById(UUID businessId);
}
