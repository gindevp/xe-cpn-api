package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderReturnRequest;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderReturnRequest entity.
 */
@Repository
public interface OrderReturnRequestRepository extends JpaRepository<OrderReturnRequest, Long> {
    List<OrderReturnRequest> findByOrder_IdOrderByRequestedAtAscIdAsc(Long orderId);

    long countByOrder_Id(Long orderId);
}
