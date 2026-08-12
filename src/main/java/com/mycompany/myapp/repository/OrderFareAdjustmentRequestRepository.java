package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderFareAdjustmentRequest;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderFareAdjustmentRequest entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrderFareAdjustmentRequestRepository extends JpaRepository<OrderFareAdjustmentRequest, Long> {}
