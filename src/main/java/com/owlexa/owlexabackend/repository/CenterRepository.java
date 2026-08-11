package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.Center;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CenterRepository extends JpaRepository<Center, Long> {
    @EntityGraph(attributePaths = "owner")
    @Query("""
            select center from Center center
            where (:search = ''
                   or lower(center.name) like lower(concat('%', :search, '%'))
                   or lower(center.subdomain) like lower(concat('%', :search, '%')))
            """)
    Page<Center> searchForAdmin(@Param("search") String search, Pageable pageable);
}
