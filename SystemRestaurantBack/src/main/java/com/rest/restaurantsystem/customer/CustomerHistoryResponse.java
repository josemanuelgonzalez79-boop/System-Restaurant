package com.rest.restaurantsystem.customer;

import java.time.Instant;
import java.util.List;

public record CustomerHistoryResponse(List<CustomerVisitResponse> visits,
                                      List<CustomerAuditResponse> changes) {
    public record CustomerAuditResponse(Long id, String action, String actorUsername, Instant occurredAt) {
        static CustomerAuditResponse from(CustomerEvent event) {
            return new CustomerAuditResponse(event.getId(), event.getAction(),
                    event.getActorUsername(), event.getOccurredAt());
        }
    }
}
