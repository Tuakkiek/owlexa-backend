package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CenterRepository extends JpaRepository<Center, Long> {

    boolean existsBySubdomain(String subdomain);

    Optional<Center> findBySubdomain(String subdomain);

    List<Center> findAllByOwner_Id(Long ownerId);

    @EntityGraph(attributePaths = "owner")
    @Query("""
            select center from Center center
            where (:search = ''
                   or lower(center.name) like lower(concat('%', :search, '%'))
                   or lower(center.subdomain) like lower(concat('%', :search, '%')))
            """)
    Page<Center> searchForAdmin(@Param("search") String search, Pageable pageable);
}
