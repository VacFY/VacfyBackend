package com.dev.vacfy.monitoring.infrastructure.persistence.jpa.repositories;

import com.dev.vacfy.monitoring.domain.model.aggregates.VaccineProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaccineProductRepository extends JpaRepository<VaccineProduct, Long> {
    Optional<VaccineProduct> findByGtin(String gtin);
}
