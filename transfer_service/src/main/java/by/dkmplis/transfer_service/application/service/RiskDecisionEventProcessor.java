package by.dkmplis.transfer_service.application.service;

import by.dkmplis.transfer_service.application.event.EventEnvelope;
import by.dkmplis.transfer_service.domain.enums.TransferState;
import by.dkmplis.transfer_service.infrastructure.inbox.service.InboxEventService;
import by.dkmplis.transfer_service.infrastructure.inbox.service.InboxRegistration;
import by.dkmplis.transfer_service.infrastructure.kafka.dto.RiskDecisionPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RiskDecisionEventProcessor {

    private static final String RISK_APPROVED =
            "risk.approved";

    private static final String RISK_REJECTED =
            "risk.rejected";

    private final InboxEventService inboxEventService;
    private final TransferService transferService;
    private final TransferStateService stateService;
    private final TransferProcessingService processingService;

    public void process(
            EventEnvelope<RiskDecisionPayload> event,
            String rawEventJson
    ) {
        InboxRegistration registration =
                inboxEventService.register(
                        event,
                        rawEventJson
                );

        if (registration.processed()) {
            return;
        }

        switch (event.eventType()) {
            case RISK_APPROVED -> processApproved(
                    event,
                    registration
            );

            case RISK_REJECTED -> processRejected(
                    event,
                    registration
            );

            default -> throw new IllegalArgumentException(
                    "Unsupported risk event type: "
                            + event.eventType()
            );
        }
    }

    private void processApproved(
            EventEnvelope<RiskDecisionPayload> event,
            InboxRegistration registration
    ) {
        TransferState state = transferService
                .get(event.aggregateId())
                .state();

        switch (state) {
            case PENDING -> stateService.markRiskApproved(
                    event.aggregateId()
            );
            case COMPLETED, REJECTED -> {
                if (registration.newlyRegistered()) {
                    throw new IllegalStateException(
                            "Cannot apply new risk.approved event "
                                    + "to terminal transfer"
                    );
                }

                inboxEventService.markProcessed(
                        event.eventId()
                );

                return;
            }
        }

        processingService.process(
                event.aggregateId()
        );

        inboxEventService.markProcessed(
                event.eventId()
        );
    }

    private void processRejected(
            EventEnvelope<RiskDecisionPayload> event,
            InboxRegistration registration
    ) {
        TransferState state = transferService
                .get(event.aggregateId())
                .state();

        switch (state) {
            case PENDING -> stateService.markRejected(
                    event.aggregateId()
            );

            case REJECTED -> {
                if (registration.newlyRegistered()) {
                    throw new IllegalStateException(
                            "Cannot apply new risk.rejected event "
                                    + "to already rejected transfer"
                    );
                }
            }

            case RISK_APPROVED, COMPLETED -> throw new IllegalStateException(
                    "Risk rejection cannot be applied "
                            + "after risk approval"
            );
        }

        inboxEventService.markProcessed(
                event.eventId()
        );
    }
}