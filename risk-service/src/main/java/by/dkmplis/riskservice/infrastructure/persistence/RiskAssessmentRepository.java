package by.dkmplis.riskservice.infrastructure.persistence;

import by.dkmplis.riskservice.domain.model.RiskAssessment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepository
        extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findByTransferId(UUID transferId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from RiskAssessment r
            where r.id = :id
            """)
    Optional<RiskAssessment> findByIdForUpdate(
            @Param("id") UUID id
    );
}
