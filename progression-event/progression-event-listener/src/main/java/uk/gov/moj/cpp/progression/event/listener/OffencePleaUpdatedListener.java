package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.PleaUpdated;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.OffenceRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.UUID;

import static java.time.LocalDate.now;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class OffencePleaUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private OffenceRepository offenceRepository;


    @Handles("progression.events.plea-updated")
    @Transactional
    public void updatePlea(final JsonEnvelope envelope) {
        final PleaUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), PleaUpdated.class);

        final OffenceDetail offenceDetail = offenceRepository.findBy(UUID.fromString(event.getOffenceId()));

        offenceDetail.setPlea(event.getPlea());

    }



}
