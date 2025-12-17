package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.progression.query.Assignee;
import uk.gov.justice.progression.query.CaseDirections;
import uk.gov.justice.progression.query.DefendantParties;
import uk.gov.justice.progression.query.Offences;
import uk.gov.justice.progression.query.ProsecutionCases;
import uk.gov.justice.progression.query.TrialReadinessHearing;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;

import javax.json.JsonObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CotrQueryApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CotrQueryApiService.class.getCanonicalName());
//
    private static final String COMMA = ",";
    private static final String GET_CASE_DIRECTIONS = "directionsmanagement.query.case-directions-list";
    private static final String COTR_DIRECTION_ID = "86a1c852-b581-4c38-b4d0-ad8d8130d970";
    private static final List<String> STAGE2_DIRECTION_ID = Arrays.asList("29be8453-b3c6-4f5c-815f-7f6b41ac1ac1", "0a18eadf-0970-42ff-b980-b7f383391391");
    private static final List<String> SECTION28_DIRECTION_ID = Arrays.asList("44a07637-d501-4604-9716-1bc664ce2e69", "34dd0d71-1c9b-44b2-8961-c3532faa67bc");

    public TrialReadinessHearing getTrialReadinessHearing(final Hearing hearing, final List<uk.gov.justice.progression.query.CaseDirections> caseDirections) {
        return TrialReadinessHearing.trialReadinessHearing()
                .withId(hearing.getId())
                .withJurisdictionType(hearing.getJurisdictionType())
                .withProsecutionCases(getProsecutionCasesFromHearing(hearing))
                .withOffences(getOffencesFromHearing(hearing))
                .withCaseMarkers(getCaseMarkersFromHearing(hearing))
                .withCourtCentre(hearing.getCourtCentre())
                .withHearingType(hearing.getType())
                .withHearingDay(getEarliestHearingDay(hearing.getHearingDays()))
                .withDefendantParties(getDefendantPartiesFromHearing(hearing))
                .withCaseDirections(caseDirections)
                .build();
    }

    public Optional<JsonObject> getCotrDetails(final ProsecutionCaseQuery prosecutionCaseQuery, final String prosecutionCaseId) {
        final JsonObject payload = createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCaseId)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName("progression.query.cotr-details"),
                payload);

        final JsonEnvelope response = prosecutionCaseQuery.getCotrDetails(requestEnvelope);
        return ofNullable(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getCaseDirectionsByHearingIdAndDirectionIds(final Requester requester, final String hearingId) {
        final List<String> dashboardDirectionIds = new ArrayList<>();
        dashboardDirectionIds.addAll(SECTION28_DIRECTION_ID);
        dashboardDirectionIds.addAll(STAGE2_DIRECTION_ID);
        dashboardDirectionIds.add(COTR_DIRECTION_ID);

        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("directionIds", String.join(COMMA, dashboardDirectionIds))
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName(GET_CASE_DIRECTIONS),
                payload);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return ofNullable(response.payload());
    }

    public String getDefendantFirstName(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return defendant.getPersonDefendant().getPersonDetails().getFirstName();
        }
        if (nonNull(defendant.getLegalEntityDefendant()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation())) {
            return defendant.getLegalEntityDefendant().getOrganisation().getName();
        }
        return EMPTY;
    }

    public String getDefendantLastName(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return defendant.getPersonDefendant().getPersonDetails().getLastName();
        }
        return EMPTY;
    }

    public HearingDay getEarliestHearingDay(final List<HearingDay> hearingDays) {
        LOGGER.info("Get earliest hearing date");
        final Optional<HearingDay> hearingDay =  hearingDays.stream()
                .min(comparing(HearingDay::getSittingDay));
        return hearingDay.orElse(null);
    }

    public List<CaseDirections> convertCasesDirections(List<uk.gov.justice.directionmanagement.query.CaseDirections> caseDirections) {
        return caseDirections.stream()
                .map(cd -> CaseDirections.caseDirections()
                        .withAssignee(createAssignee(cd.getAssignee()))
                        .withCaseId(cd.getCaseId())
                        .withDefaultDirection(cd.getDefaultDirection())
                        .withDirectionTypeId(cd.getDirectionTypeId())
                        .withDisplayText(cd.getDisplayText())
                        .withDueDate(getDueDate(cd))
                        .withHearingType(cd.getHearingType())
                        .withId(cd.getId())
                        .withOrderDate(cd.getOrderDate())
                        .withStatus(cd.getStatus())
                        .withType(getType(cd))
                        .build())
                .collect(toList());
    }

    private String getType(final uk.gov.justice.directionmanagement.query.CaseDirections cd) {
        if(cd.getRefData()!=null && cd.getRefData().getDirectionRefDataId()!=null ){
            return  getDirectionType(cd.getRefData().getDirectionRefDataId().toString());
        }
        return null;
    }

    private LocalDate getDueDate(final uk.gov.justice.directionmanagement.query.CaseDirections cd) {
        return nonNull(cd.getDueDate()) ? LocalDate.parse(cd.getDueDate()) : null;
    }

    private Assignee createAssignee(uk.gov.justice.directionmanagement.query.Assignee assignee) {
        return Assignee.assignee()
                .withAssigneePersonId(assignee.getAssigneePersonId())
                .withAssigneeOrganisationText(assignee.getAssigneeOrganisationText())
                .build();
    }

    private List<DefendantParties> getDefendantPartiesFromHearing(final Hearing hearing) {
        final List<DefendantParties> defendantPartiesList = new ArrayList<>();
        hearing.getProsecutionCases().forEach(prosecutionCase ->
                prosecutionCase.getDefendants().forEach(defendant -> {
                    final DefendantParties defendantParties = DefendantParties.defendantParties()
                            .withId(defendant.getId())
                            .withFirstName(getDefendantFirstName(defendant))
                            .withLastName(getDefendantLastName(defendant))
                            .withIsYouth(isDefendantYouth(defendant))
                            .withBailStatus(nonNull(defendant.getPersonDefendant()) ? defendant.getPersonDefendant().getBailStatus() : null)
                            .withCustodyTimeLimit(nonNull(defendant.getPersonDefendant()) ? createCtlForDefendant(defendant) : null)
                            .withProsecution(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityName())
                            .withDefence(nonNull(defendant.getAssociatedDefenceOrganisation())
                                    && nonNull(defendant.getAssociatedDefenceOrganisation().getDefenceOrganisation())
                                    ? defendant.getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName() : null)
                            .build();
                    defendantPartiesList.add(defendantParties);
                })
        );
        return defendantPartiesList;
    }

    public LocalDate createCtlForDefendant(final Defendant defendant) {
        return ofNullable(defendant.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(offence -> nonNull(offence.getCustodyTimeLimit()) && nonNull(offence.getCustodyTimeLimit().getTimeLimit()))
                .map(offence -> offence.getCustodyTimeLimit().getTimeLimit())
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private Boolean isDefendantYouth(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && Objects.nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return nonNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth()) ? LocalDateUtils.isYouth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), LocalDate.now()) : null;
        } else {
            return false;
        }
    }

    private List<String> getCaseMarkersFromHearing(final Hearing hearing) {
        return hearing.getProsecutionCases().stream()
                .filter(prosecutionCase -> isNotEmpty(prosecutionCase.getCaseMarkers()))
                .flatMap(prosecutionCase -> prosecutionCase.getCaseMarkers().stream())
                .map(Marker::getMarkerTypeDescription)
                .distinct()
                .collect(toList());
    }

    private List<Offences> getOffencesFromHearing(final Hearing hearing) {
        return hearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .map(offence -> Offences.offences()
                        .withId(offence.getId())
                        .withOffenceType(offence.getModeOfTrial())
                        .build())
                .collect(toList());
    }

    private List<ProsecutionCases> getProsecutionCasesFromHearing(final Hearing hearing) {
        return hearing.getProsecutionCases().stream()
                .map(prosecutionCase -> ProsecutionCases.prosecutionCases()
                        .withId(prosecutionCase.getId())
                        .withCaseUrn(
                                nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                                        ? prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()
                                        : prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference())
                        .withProsecutorId(prosecutionCase.getProsecutor() != null ? prosecutionCase.getProsecutor().getProsecutorId() : prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())
                        .build())
                .collect(toList());
    }

    private String getDirectionType(final String directionTypeId) {
        if (COTR_DIRECTION_ID.equals(directionTypeId)) {
            return "COTR";
        }
        if (STAGE2_DIRECTION_ID.contains(directionTypeId)) {
            return "STAGE2";
        }
        if (SECTION28_DIRECTION_ID.contains(directionTypeId)) {
            return "SECTION28";
        }
        return "OTHERS";
    }

    public Optional<JsonObject> getCotrDetailsProsecutionCase(final Requester requester, final String prosecutionCaseId) {
        final JsonObject payload = createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCaseId)
                .build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName("progression.query.cotr.details.prosecutioncase"),
                payload);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return ofNullable(response.payload());
    }




}
