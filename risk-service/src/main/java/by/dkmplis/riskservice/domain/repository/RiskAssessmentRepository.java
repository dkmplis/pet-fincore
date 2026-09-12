package by.dkmplis.riskservice.domain.repository;

import by.dkmplis.riskservice.domain.model.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepository
        extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findByTransferId(UUID transferId);
}
