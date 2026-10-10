package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PhoneTaxLink;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PhoneTaxLinkRepository extends JpaRepository<PhoneTaxLink, Long> {
    List<PhoneTaxLink> findByPhoneOrderByUpdatedAtDescIdDesc(String phone);

    List<PhoneTaxLink> findByPhoneOrderByUpdatedAtAscIdAsc(String phone);

    Optional<PhoneTaxLink> findByPhoneAndTaxCode(String phone, String taxCode);

    long countByPhone(String phone);

    List<PhoneTaxLink> findByFromOrderCodeIsNotNull();

    List<PhoneTaxLink> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    List<PhoneTaxLink> findByPhoneIn(Collection<String> phones);

    @Query(
        """
        select l from PhoneTaxLink l
        where lower(l.companyName) like lower(concat('%', :needle, '%')) escape '\\'
        order by l.updatedAt desc, l.id desc
        """
    )
    List<PhoneTaxLink> searchByCompanyName(@Param("needle") String needle, Pageable pageable);
}
