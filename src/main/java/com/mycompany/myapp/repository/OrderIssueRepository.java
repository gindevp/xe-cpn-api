package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.OrderIssue;
import com.mycompany.myapp.domain.enumeration.IssueStatus;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrderIssue entity.
 */
@Repository
public interface OrderIssueRepository extends JpaRepository<OrderIssue, Long>, JpaSpecificationExecutor<OrderIssue> {
    List<OrderIssue> findByOrder_IdOrderByOpenedAtAscIdAsc(Long orderId);

    boolean existsByOrder_IdAndIssueStatus(Long orderId, IssueStatus issueStatus);

    long countByOrder_Id(Long orderId);
}
