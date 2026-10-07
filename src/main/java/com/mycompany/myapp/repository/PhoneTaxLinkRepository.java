package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PhoneTaxLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneTaxLinkRepository extends JpaRepository<PhoneTaxLink, Long> {
    List<PhoneTaxLink> findByPhoneOrderByUpdatedAtDescIdDesc(String phone);

    List<PhoneTaxLink> findByPhoneOrderByUpdatedAtAscIdAsc(String phone);

    Optional<PhoneTaxLink> findByPhoneAndTaxCode(String phone, String taxCode);

    long countByPhone(String phone);

    List<PhoneTaxLink> findByFromOrderCodeIsNotNull();

    List<PhoneTaxLink> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
