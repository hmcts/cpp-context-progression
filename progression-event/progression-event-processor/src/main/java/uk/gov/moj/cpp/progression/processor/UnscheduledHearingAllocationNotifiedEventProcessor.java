package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static uk.gov.justice.core.courts.notification.EmailChannel.emailChannel;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.service.utils.DefendantDetailsExtractor.getDefendantFullName;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.progression.courts.UnscheduledHearingAllocationNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class UnscheduledHearingAllocationNotifiedEventProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnscheduledHearingAllocationNotifiedEventProcessor.class);

    private static final String EMAIL = "email";
    public static final String DATE_OF_HEARING = "dateOfHearing";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String SITTING_AT = "sittingAt";
    public static final String URN = "urn";
    public static final String CASE_NUMBER = "caseNumber";
    public static final String DEFENDANT_NAME = "defendantName";
    private static final String LJA = "lja";
    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";
    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";
    private static final String NAME = "name";
    private static final String EMPTY = "";
    private static final String CASE_AT_A_GLANCE_LINK = "caseAtAGlanceLink";

    @Inject
    private NotificationService notificationService;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Handles("progression.event.unscheduled-hearing-allocation-notified")
    public void unscheduledHearingAllocationNotified(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received '{}' event with payload {}", "unscheduled-hearing-allocation-notified", event.toObfuscatedDebugString());
        }

        final UnscheduledHearingAllocationNotified notificationEvent = jsonObjectConverter.convert(event.payloadAsJsonObject(), UnscheduledHearingAllocationNotified.class);
        final Hearing hearing = notificationEvent.getHearing();
        final JsonObject enforcementArea = getEnforcementAreaByCourtCentreId(event, hearing.getCourtCentre().getId());
        final String enforcementAreaEmail = enforcementArea.getString(EMAIL);
        final LjaDetails ljaDetails = buildLjaDetails(enforcementArea);

        hearing.getProsecutionCases().forEach(
            prosecutionCase -> {

                final List<EmailChannel> emailChannels = prosecutionCase.getDefendants().stream()
                        .map(defendant -> buildEmailChannel(hearing, prosecutionCase, defendant, ljaDetails, enforcementAreaEmail))
                        .collect(Collectors.toList());

                final UUID notificationId = randomUUID();
                notificationService.sendEmail(event, notificationId, prosecutionCase.getId(), null, null, emailChannels);

            }
        );

    }

    private EmailChannel buildEmailChannel(final Hearing hearing, final ProsecutionCase prosecutionCase, final Defendant defendant, final LjaDetails ljaDetails, final String enforcementAreaEmail) {
        final String defendantFullName = getDefendantFullName(defendant);
        final String caseURN = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
        final ZonedDateTime hearingStartDateTime = getEarliestDate(hearing.getHearingDays());

        final Map<String, Object> map = new HashMap<>();
        map.put(DATE_OF_HEARING, hearingStartDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyy HH:mm a")).toString());
        map.put(COURT_CENTRE, buildCourtCentre(ljaDetails));
        map.put(SITTING_AT, hearing.getCourtCentre().getName());
        map.put(URN, caseURN);
        map.put(CASE_NUMBER, prosecutionCase.getId().toString());
        map.put(DEFENDANT_NAME, defendantFullName);
        map.put(CASE_AT_A_GLANCE_LINK, applicationParameters.getEndClientHost()+applicationParameters.getCaseAtaGlanceURI()+prosecutionCase.getId().toString());

        final Personalisation personalisation = new Personalisation(map);

        return emailChannel()
                .withTemplateId(fromString(applicationParameters.getUnscheduledHearingAllocationEmailTemplateId()))
                .withSendToAddress(enforcementAreaEmail)
                .withPersonalisation(personalisation)
                .build();
    }

    private String buildCourtCentre(final LjaDetails ljaDetails){
        return ljaDetails.getLjaCode() + SPACE + ljaDetails.getLjaName();
    }

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private LjaDetails buildLjaDetails(final JsonObject enforcementArea){
        final JsonObject localJusticeArea = enforcementArea.getJsonObject(LOCAL_JUSTICE_AREA);
        return  LjaDetails.ljaDetails()
                .withLjaCode(ofNullable(localJusticeArea).map(area -> area.getString(NATIONAL_COURT_CODE, EMPTY)).orElse(EMPTY))
                .withLjaName(ofNullable(localJusticeArea).map(area -> area.getString(NAME, EMPTY)).orElse(EMPTY))
                .build();
    }

    private JsonObject getEnforcementAreaByCourtCentreId(final JsonEnvelope event, final UUID courtCentreId){
        final JsonObject courtCentreJson = referenceDataService.getOrganisationUnitById(courtCentreId, event, requester).orElseThrow(IllegalArgumentException::new);
        return referenceDataService.getEnforcementAreaByLjaCode(event, courtCentreJson.getString(LJA), requester);
    }

}
