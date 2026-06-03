package com.orderly.api.complaint.infrastructure.persistence;

import com.orderly.api.complaint.domain.model.Complaint;
import com.orderly.api.complaint.domain.model.ComplaintStatus;
import com.orderly.api.complaint.domain.port.ComplaintRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ComplaintJpaAdapter implements ComplaintRepository {

    private final ComplaintJpaRepository jpaRepository;

    public ComplaintJpaAdapter(ComplaintJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Complaint save(Complaint complaint) {
        ComplaintJpaEntity entity = new ComplaintJpaEntity(
                complaint.id(),
                complaint.businessId(),
                complaint.customerPhone(),
                complaint.description(),
                complaint.evidenceUrl(),
                complaint.status().name(),
                complaint.createdAt());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Complaint> findAllByBusinessId(UUID businessId) {
        return jpaRepository.findAllByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Complaint toDomain(ComplaintJpaEntity e) {
        return Complaint.restore(
                e.getId(), e.getBusinessId(), e.getCustomerPhone(),
                e.getDescription(), e.getEvidenceUrl(),
                ComplaintStatus.valueOf(e.getStatus()), e.getCreatedAt());
    }
}
