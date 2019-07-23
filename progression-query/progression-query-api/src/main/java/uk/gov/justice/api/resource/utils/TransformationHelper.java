package uk.gov.justice.api.resource.utils;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationResponse;
import uk.gov.justice.core.courts.CourtApplicationResponseType;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.exract.Address;
import uk.gov.justice.progression.courts.exract.Dates;
import uk.gov.justice.progression.courts.exract.ProsecutingAuthority;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.base.CaseFormat;

@SuppressWarnings({"squid:S3655", "squid:S1067"})
public class TransformationHelper {
    @Inject
    ReferenceDataService referenceDataService;
    private int winger = 1;

    public String getName(final String firstName, final String middleName, final String lastName) {
        final StringBuilder sb = new StringBuilder();
        if (isNotEmpty(firstName)) {
            sb.append(firstName);
            sb.append(SPACE);
        }
        if (isNotEmpty(middleName)) {
            sb.append(middleName);
            sb.append(SPACE);
        }
        if (isNotEmpty(lastName)) {
            sb.append(lastName);
        }
        return sb.toString().trim();
    }

    public Address getAddress(final String address1, final String address2, final String address3, final String address4, final String address5, final String postCode){
        return Address.address()
                .withAddress1(address1)
                .withAddress2(isNotEmpty(address3)
                        && isNotEmpty(address2)? address2
                        +SPACE+address3 : address2)
                .withAddress3(isNotEmpty(address5)
                        && isNotEmpty(address4)? address4
                        +SPACE+address5 : address4)
                .withPostCode(postCode)
                .build();
    }
    public Hearings getLatestHearings(final List<Hearings> hearingsList) {
        return hearingsList.stream().sorted(comparing(h -> getSittingDay(h.getHearingDays()))).reduce((first, second) -> second).orElse(null);
    }
    public Hearing getLatestHearing(final List<Hearing> hearingsList) {
        return hearingsList.stream().sorted(comparing(h -> getSittingDay(h.getHearingDays()))).reduce((first, second) -> second).orElse(null);
    }
    private ZonedDateTime getSittingDay(final List<HearingDay> hearingDays) {
        return hearingDays.stream().sorted(comparing(HearingDay::getSittingDay).reversed()).findFirst().get().getSittingDay();
    }
    public List<Dates> transformDates(final List<HearingDay> hearingDaysList) {
        if (hearingDaysList.size() > 2) {
            return getToAndFromDays(hearingDaysList);
        }
        return hearingDaysList.stream().map(hday ->
                Dates.dates().withDay(hday.getSittingDay().toLocalDate()).build()
        ).collect(toList());

    }
    public List<Dates> transformDates(final List<HearingDay> hearingDaysList, boolean sorted) {
        if(sorted){
            return transformDates(hearingDaysList.stream().sorted(Comparator.comparing(HearingDay::getSittingDay)).collect(toList()));
        }
        return transformDates(hearingDaysList);
    }
    //   If isBenchChairman= true then display name should be Chair: name, if isDeputy= true then Winger1: name
    //   i.e. "judicialDisplayName": "Chair: Elizabeth Cole, Winger1: Sharon Reed-Jones, Winger2: Greg Walsh"
    public String transformJudicialDisplayName(final List<JudicialRole> judicialRoles) {
        final StringBuilder sb = new StringBuilder();
        winger = 1;
        judicialRoles.forEach(j -> {

            if (nonNull(j.getIsBenchChairman()) && j.getIsBenchChairman()) {
                sb.append("Chair: ");
            } else if (nonNull(j.getIsDeputy()) && j.getIsDeputy()) {
                sb.append("Winger");
                sb.append(getWingerAndIncrement());
                sb.append(": ");
            }
            sb.append(getName(j.getFirstName(), j.getMiddleName(), j.getLastName()));
            sb.append(" ");

        });
        return sb.toString().trim();
    }

    public String getCamelCase(final String value) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value);
    }
    public String transformApplicationResponse(final List<CourtApplicationRespondent> respondents) {
        return respondents.stream()
                .filter(Objects::nonNull)
                .filter(r -> nonNull(r.getApplicationResponse()))
                .map(CourtApplicationRespondent::getApplicationResponse)
                .map(CourtApplicationResponse::getApplicationResponseType)
                .map(CourtApplicationResponseType::getDescription)
                .findAny()
                .orElse(null);

    }

    public boolean isApplicationResponseAvailable(final List<CourtApplicationRespondent> respondents ){
        return nonNull(respondents) && respondents.stream().
                anyMatch(r -> nonNull(r.getApplicationResponse()));
    }
    public LocalDate transformApplicationResponseDate(final List<CourtApplicationRespondent> respondents) {
        return respondents.stream()
                .filter(Objects::nonNull)
                .filter(r -> nonNull(r.getApplicationResponse()))
                .map(CourtApplicationRespondent::getApplicationResponse)
                .map(CourtApplicationResponse::getApplicationResponseDate)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);

    }
    public uk.gov.justice.core.courts.Address getCourtAddress(final UUID userId, final UUID courtCentreId) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("referencedata.query.organisation-unit.v2")
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );

        return referenceDataService.getCourtCentreAddress(jsonEnvelope, courtCentreId);
    }

    ProsecutingAuthority transformProsecutingAuthority(final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final UUID userId) {
        final JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("referencedata.query.get-prosecutor")
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .add("id", prosecutionCaseIdentifier.getProsecutionAuthorityId().toString())
                        .build()
        );
        return referenceDataService.getProsecutor(jsonEnvelope, prosecutionCaseIdentifier.getProsecutionAuthorityId().toString());
    }

    private int getWingerAndIncrement() {
        return winger++;
    }
    private List<Dates> getToAndFromDays(final List<HearingDay> hearingDaysList) {
        final List<Dates> dates = new ArrayList<>();
        dates.add(Dates.dates()
                .withDay(hearingDaysList.get(0).getSittingDay().toLocalDate())
                .build());
        dates.add(Dates.dates()
                .withDay(hearingDaysList.get(hearingDaysList.size() - 1).getSittingDay().toLocalDate())
                .build());
        return dates;
    }

}
