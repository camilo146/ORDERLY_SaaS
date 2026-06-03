package com.orderly.api.customer.domain.port;

import com.orderly.api.customer.domain.model.Customer;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findByBusinessIdAndWhatsappNumber(UUID businessId, String whatsappNumber);

    long countByBusinessId(UUID businessId);
}
