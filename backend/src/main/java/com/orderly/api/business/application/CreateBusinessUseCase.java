package com.orderly.api.business.application;

import com.orderly.api.business.domain.model.Business;

/**
 * Application boundary for tenant creation.
 */
public interface CreateBusinessUseCase {

    Business create(CreateBusinessCommand command);
}
