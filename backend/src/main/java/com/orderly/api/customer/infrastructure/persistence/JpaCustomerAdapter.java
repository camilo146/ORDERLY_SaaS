package com.orderly.api.customer.infrastructure.persistence;

import com.orderly.api.customer.domain.model.Customer;
import com.orderly.api.customer.domain.port.CustomerRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCustomerAdapter implements CustomerRepository {

    private final CustomerJpaRepository repo;

    public JpaCustomerAdapter(CustomerJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = toEntity(customer);
        CustomerJpaEntity saved = repo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Customer> findByBusinessIdAndWhatsappNumber(UUID businessId, String whatsappNumber) {
        return repo.findByBusinessIdAndWhatsappNumber(businessId, whatsappNumber).map(this::toDomain);
    }

    @Override
    public long countByBusinessId(UUID businessId) {
        return repo.countByBusinessId(businessId);
    }

    private CustomerJpaEntity toEntity(Customer c) {
        CustomerJpaEntity e = new CustomerJpaEntity();
        e.setId(c.id());
        e.setBusinessId(c.businessId());
        e.setWhatsappNumber(c.whatsappNumber());
        e.setFullName(c.fullName());
        e.setLastOrderAt(c.lastOrderAt());
        e.setCreatedAt(c.createdAt());
        return e;
    }

    private Customer toDomain(CustomerJpaEntity e) {
        return new Customer(e.getId(), e.getBusinessId(), e.getWhatsappNumber(),
                e.getFullName(), e.getLastOrderAt(), e.getCreatedAt());
    }
}
