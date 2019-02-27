package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.collect.Sets;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;
import uk.gov.moj.cpp.progression.event.converter.OffenceForDefendantUpdatedToEntity;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(EVENT_LISTENER)
public class OffencesForDefendantUpdatedListener {


    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private OffenceForDefendantUpdatedToEntity converter;

    Defendant defendant;

    @Transactional
    @Handles("progression.events.offences-for-defendant-updated")
    public void updateOffencesForDefendant(final JsonEnvelope envelope) {
        final OffencesForDefendantUpdated event = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), OffencesForDefendantUpdated.class);
        defendant = defendantRepository.findBy(event.getDefendantId());
        if (defendant != null && !event.getOffences().isEmpty()) {
            final Set<OffenceDetail> persistedOffenceDetailList = defendant.getOffences();
            final Set<OffenceDetail> offenceDetailList = event.getOffences().stream().map(s -> converter.convert(s)).collect(Collectors.toSet());
            //Delete
            final Set<OffenceDetail> offenceDetailListDel = Sets.difference(persistedOffenceDetailList, offenceDetailList).stream().collect(Collectors.toSet());
            defendant.getOffences().removeAll(offenceDetailListDel);
            //Amend
            mergeOffence(persistedOffenceDetailList, offenceDetailList);
            //Add
            defendant.addOffences(Sets.difference(offenceDetailList, persistedOffenceDetailList).stream().collect(Collectors.toSet()));
            defendantRepository.save(defendant);
        }
    }

    private void mergeOffence(final Set<OffenceDetail> persistedOffenceDetailList, final Set<OffenceDetail> offenceDetailList) {
        persistedOffenceDetailList.forEach(offenceDetailPersisted ->
            offenceDetailList.forEach(offenceDetail -> {
                if (offenceDetailPersisted.equals(offenceDetail)) {
                    offenceDetailPersisted.setCode(offenceDetail.getCode());
                    offenceDetailPersisted.setStartDate(offenceDetail.getStartDate());
                    offenceDetailPersisted.setConvictionDate(offenceDetail.getConvictionDate());
                    offenceDetailPersisted.setOrderIndex(offenceDetail.getOrderIndex());
                    offenceDetailPersisted.setWording(offenceDetail.getWording());
                    offenceDetailPersisted.setOffenceIndicatedPlea(offenceDetail.getOffenceIndicatedPlea());
                    offenceDetailPersisted.setOffencePlea(offenceDetail.getOffencePlea());
                    offenceDetailPersisted.setSection(offenceDetail.getSection());
                    offenceDetailPersisted.setEndDate(offenceDetail.getEndDate());
                    offenceDetailPersisted.setCount(offenceDetail.getCount());
                    offenceDetailPersisted.setCategory(offenceDetail.getCategory());
                }
            })
        );


    }

}
