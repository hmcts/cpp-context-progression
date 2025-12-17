package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.pojo.PrisonCustodySuite;
import uk.gov.moj.cpp.progression.helper.CustodialEstablishmentUpdateHelper;
import uk.gov.moj.cpp.progression.helper.HearingResultHelper;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UpdateDefendantService;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static java.util.Collections.singletonList;

@ServiceComponent(EVENT_PROCESSOR)
public class HearingResultedCaseUpdatedProcessor {

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private Requester requester;

    @Inject
    private UpdateDefendantService updateDefendantService;

    @Inject
    private CustodialEstablishmentUpdateHelper custodialEstablishmentUpdateHelper;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private HearingResultHelper hearingResultHelper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingResultedCaseUpdatedProcessor.class.getCanonicalName());

    static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";

    @Handles("progression.event.hearing-resulted-case-updated")
    public void process(final JsonEnvelope jsonEnvelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received Hearing Resulted Case Updated with payload : {}", jsonEnvelope.toObfuscatedDebugString());
        }
        final HearingResultedCaseUpdated hearingResultedCaseUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingResultedCaseUpdated.class);

        final boolean isHearingAdjourned = hearingResultHelper.doProsecutionCasesContainNextHearingResults(singletonList(hearingResultedCaseUpdated.getProsecutionCase()));
        if (isHearingAdjourned) {
            LOGGER.info("Update civil fee process has been started.");
            progressionService.updateCivilFees(jsonEnvelope, hearingResultedCaseUpdated.getProsecutionCase());
        }

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED)
                .apply(jsonEnvelope.payload()));

        //update defendant custodial establishment when hearing resulted with Prison / Hospital selected
        if (nonNull(hearingResultedCaseUpdated.getProsecutionCase()) && nonNull(hearingResultedCaseUpdated.getProsecutionCase().getDefendants())) {
            final List<PrisonCustodySuite> prisonCustodySuites = referenceDataService.getPrisonsCustodySuites(requester);

            hearingResultedCaseUpdated.getProsecutionCase().getDefendants()
                    .forEach(defendant -> {
                        final Optional<CustodialEstablishment> defendantsWithCustodialEstablishmentToUpdateOpt = custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant, prisonCustodySuites);
                        LOGGER.info("Found defendant={} to update CustodyEstablishment={}", defendant.getId(), defendantsWithCustodialEstablishmentToUpdateOpt);
                        defendantsWithCustodialEstablishmentToUpdateOpt
                                .ifPresent(custodialEstablishment -> updateDefendantService.updateDefendantCustodialEstablishment(jsonEnvelope.metadata(), defendant, custodialEstablishment));
                    });
        }
    }
}