package com.orderly.api.complaint.domain.port;

import com.orderly.api.complaint.domain.model.Complaint;

import java.util.List;
import java.util.UUID;

public interface ComplaintRepository {
    Complaint save(Complaint complaint);

    List<Complaint> findAllByBusinessId(UUID businessId);
}
