package uk.gov.justice.api.resource.utils;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Prosecutor;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.exract.Address;
import uk.gov.justice.progression.courts.exract.Dates;
import uk.gov.justice.progression.courts.exract.HearingDays;
import uk.gov.justice.progression.courts.exract.ProsecutingAuthority;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.base.CaseFormat;

@SuppressWarnings({"squid:S3655", "squid:S1067", "squid:CommentedOutCodeLine",})
public class TransformationHelper {

    private static final String CHAIR = "Chair:";
    private static final String WINGER = "Winger";
    @Inject
    ReferenceDataService referenceDataService;
    @Inject
    private RequestedNameMapper requestedNameMapper;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    private static final Comparator<HearingDays> ascendingHearingDays = (h1, h2) -> {
        if (nonNull(h1.getDay()) && nonNull(h2.getDay())) {
            return h1.getDay().compareTo(h2.getDay());
        }
        return 0;
    };

    private static final Comparator<uk.gov.justice.progression.courts.exract.Hearings> extractHearingsComparator = (hearing1, hearing2) -> {

        if (nonNull(hearing1.getHearingDays()) && !hearing1.getHearingDays().isEmpty()
                && (isNull(hearing2.getHearingDays()) || hearing2.getHearingDays().isEmpty())) {
            hearing1.getHearingDays().sort(ascendingHearingDays);
            return 1;
        }

        if (nonNull(hearing2.getHearingDays()) && !hearing2.getHearingDays().isEmpty()
                && (isNull(hearing1.getHearingDays()) || hearing1.getHearingDays().isEmpty())) {
            hearing2.getHearingDays().sort(ascendingHearingDays);
            return -1;
        }

        if (nonNull(hearing1.getHearingDays()) && nonNull(hearing2.getHearingDays())
                && !hearing1.getHearingDays().isEmpty() && !hearing2.getHearingDays().isEmpty()) {
            hearing1.getHearingDays().sort(ascendingHearingDays);
            hearing2.getHearingDays().sort(ascendingHearingDays);

            return hearing1.getHearingDays().get(0).getDay().compareTo(hearing2.getHearingDays().get(0).getDay());
        }
        return 0;
    };


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

    public Address getAddress(final String address1, final String address2, final String address3, final String address4, final String address5, final String postCode) {
        return Address.address()
                .withAddress1(address1)
                .withAddress2(isNotEmpty(address3)
                        && isNotEmpty(address2) ? address2
                        + SPACE + address3 : address2)
                .withAddress3(isNotEmpty(address5)
                        && isNotEmpty(address4) ? address4
                        + SPACE + address5 : address4)
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
        if (sorted) {
            return transformDates(hearingDaysList.stream().sorted(Comparator.comparing(HearingDay::getSittingDay)).collect(toList()));
        }
        return transformDates(hearingDaysList);
    }

    //   If isBenchChairman= true then display name should be Chair: name, if isDeputy= true then Winger1: name
    //   i.e. "judicialDisplayName": "Chair: Elizabeth Cole, Winger1: Sharon Reed-Jones, Winger2: Greg Walsh"
    public String transformJudicialDisplayName(final List<JudicialRole> judicialRoles) {
        final StringBuilder sb = new StringBuilder();
        int winger = 1;
        for (final JudicialRole judicialRole : judicialRoles) {
            if (nonNull(judicialRole.getIsBenchChairman()) && Boolean.TRUE.equals(judicialRole.getIsBenchChairman())) {
                sb.append(CHAIR + " ");
            } else if (nonNull(judicialRole.getIsDeputy()) && Boolean.TRUE.equals(judicialRole.getIsDeputy())) {
                sb.append(WINGER);
                sb.append(winger++);
                sb.append(": ");
            }
            sb.append(getJudgeName(judicialRole.getJudicialId()));
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public String getJudgeName(final UUID judicialId) {
        final Optional<JsonObject> jsonObject = referenceDataService.getJudiciary(judicialId);
        return jsonObject.map(object -> requestedNameMapper.getRequestedJudgeName(object)).orElse(EMPTY);
    }

    public String getCamelCase(final String value) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, value);
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
        return referenceDataService.getProsecutor(jsonEnvelope, prosecutionCaseIdentifier);
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

    String getCaseReference(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return prosecutionCaseIdentifier.getCaseURN() != null ? prosecutionCaseIdentifier.getCaseURN() : prosecutionCaseIdentifier.getProsecutionAuthorityReference();
    }

    String getPersonName(final Person person) {
        if (nonNull(person)) {
            final StringJoiner joiner = new StringJoiner(" ");
            joiner.add(isNotBlank(person.getFirstName()) ? person.getFirstName() : EMPTY);
            joiner.add(isNotBlank(person.getMiddleName()) ? person.getMiddleName() : EMPTY);
            joiner.add(isNotBlank(person.getLastName()) ? person.getLastName() : EMPTY);
            return joiner.toString().trim();
        }
        return null;
    }

    String getDefendantAge(final uk.gov.justice.core.courts.Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())) {
            final String dateOfBirthText = defendant.getPersonDefendant().getPersonDetails().getDateOfBirth().toString();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            final LocalDate dateOfBirth = LocalDate.parse(dateOfBirthText, formatter);
            final LocalDate now = LocalDate.now();
            return Integer.toString(now.getYear() - dateOfBirth.getYear());
        }
        return EMPTY;
    }

    List<HearingDays> transformHearingDays(final List<HearingDay> hearingDaysList) {
        if (hearingDaysList.size() > 2) {
            return getToAndFromHearingDays(hearingDaysList);
        }

        return hearingDaysList.stream().map(hd ->
                HearingDays.hearingDays()
                        .withDay(hd.getSittingDay().toLocalDate())
                        .withTime(hd.getSittingDay().format(formatter))
                        .build()
        ).collect(Collectors.toList());
    }

    public static List<uk.gov.justice.progression.courts.exract.Hearings> getHearingsSortedByHearingDaysAsc(final List<uk.gov.justice.progression.courts.exract.Hearings> extractHearings) {
        if (extractHearings.isEmpty()) {
            return extractHearings;
        }
        final List<uk.gov.justice.progression.courts.exract.Hearings> newExtractHearings = new ArrayList<>(extractHearings);
        if (newExtractHearings.size() == 1) {
            newExtractHearings.get(0).getHearingDays().sort(ascendingHearingDays);
        } else {
            newExtractHearings.sort(extractHearingsComparator);
        }

        return newExtractHearings;
    }

    private List<HearingDays> getToAndFromHearingDays(final List<HearingDay> hearingDaysList) {
        final List<HearingDays> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDays.hearingDays()
                .withDay(hearingDaysList.get(0).getSittingDay().toLocalDate())
                .build());
        hearingDays.add(HearingDays.hearingDays()
                .withDay(hearingDaysList.get(hearingDaysList.size() - 1).getSittingDay().toLocalDate())
                .build());
        return hearingDays;
    }

    boolean getAppealPendingFlag(final List<CourtApplication> courtApplications) {
        return courtApplications.stream()
                .filter(Objects::nonNull)
                .filter(ca -> ca.getType().getAppealFlag() != null)
                .filter(ca -> ca.getType().getAppealFlag())
                .map(CourtApplication::getApplicationStatus)
                .anyMatch(applicationStatus -> applicationStatus.equals(ApplicationStatus.DRAFT) || applicationStatus.equals(ApplicationStatus.LISTED)
                        || applicationStatus.equals(ApplicationStatus.EJECTED));
    }

    public ProsecutingAuthority transformProsecutor(final Prosecutor prosecutor) {
        return ProsecutingAuthority.prosecutingAuthority()
                .withAddress(prosecutor.getAddress())
                .withName(prosecutor.getProsecutorName())
                .build();
    }

}
