package uk.gov.moj.cpp.progression.aggregate;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.CoreTemplateArguments.toMap;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;

import uk.gov.justice.core.courts.AddBreachApplication;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BreachApplicationCreationRequested;
import uk.gov.justice.core.courts.BreachApplicationsToBeAddedToHearing;
import uk.gov.justice.core.courts.BreachedApplications;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationHearingDeleted;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantRequestFromCurrentHearingToExtendHearingCreated;
import uk.gov.justice.core.courts.DefendantRequestToExtendHearingCreated;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.DeleteApplicationForCaseRequested;
import uk.gov.justice.core.courts.DeleteCourtApplicationHearingIgnored;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationRequestCreated;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingDefenceCounselAdded;
import uk.gov.justice.core.courts.HearingDefenceCounselRemoved;
import uk.gov.justice.core.courts.HearingDefenceCounselUpdated;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.HearingDefendantUpdated;
import uk.gov.justice.core.courts.HearingExtendedProcessed;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingOffencesUpdated;
import uk.gov.justice.core.courts.HearingOffencesUpdatedV2;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdatedForAllocationFields;
import uk.gov.justice.core.courts.HearingUpdatedProcessed;
import uk.gov.justice.core.courts.HearingUpdatedWithCourtApplication;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.InitiateApplicationForCaseRequested;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ListingNumberUpdated;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.NextHearingsRequested;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.OnlinePleasAllocation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.PleaModel;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2;
import uk.gov.justice.core.courts.ProsecutionCasesResultedV2;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.core.courts.SharedResultLine;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.UnscheduledHearingListingRequested;
import uk.gov.justice.core.courts.UnscheduledHearingRecorded;
import uk.gov.justice.core.courts.UpdateHearingForAllocationFields;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.core.progression.courts.HearingForApplicationCreatedV2;
import uk.gov.justice.progression.courts.ApplicationsResulted;
import uk.gov.justice.progression.courts.DeleteNextHearingsRequested;
import uk.gov.justice.progression.courts.DeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.ExtendCustodyTimeLimitResulted;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.progression.courts.RelatedHearingRequested;
import uk.gov.justice.progression.courts.RelatedHearingUpdated;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.progression.courts.VejDeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.event.ApplicationHearingDefendantUpdated;
import uk.gov.justice.progression.event.OpaPressListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaPressListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPressListNoticeSent;
import uk.gov.justice.progression.event.OpaPublicListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaPublicListNoticeGenerated;
import uk.gov.justice.progression.event.OpaPublicListNoticeSent;
import uk.gov.justice.progression.event.OpaResultListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaResultListNoticeGenerated;
import uk.gov.justice.progression.event.OpaResultListNoticeSent;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.justice.staginghmi.courts.UpdateHearingFromHmi;
import uk.gov.moj.cpp.progression.test.CoreTestTemplates;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingAggregateTest {

    private static final String GUILTY = "GUILTY";
    private final static String COMMITTING_COURT_CODE = "CCCODE";
    private final static String COMMITTING_COURT_NAME = "Committing Court";
    @InjectMocks
    private HearingAggregate hearingAggregate;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();


    @Test
    public void shouldDoCorrectiononHearingDaysWithoutCourtCentre() throws IOException {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Object response = hearingAggregate.apply(createHearingDaysWithoutCourtCentreCorrected());

        assertThat(response.getClass(), is(CoreMatchers.equalTo(HearingDaysWithoutCourtCentreCorrected.class)));
    }

    @Test
    public void shouldNotGenerateNextHearingWhenCaseWithAutoApplication() throws IOException{
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.prosecution-cases-resulted-v2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(prosecutionCasesResultedV2.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(4));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(events.get(3).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
    }

    @Test
    public void shouldNotGenerateNextHearingWhenCaseWithAutoApplicationAndHasMoreProsecutionCases() throws IOException{
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.two-prosecution-cases-and-more-defendants-resulted-v2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(prosecutionCasesResultedV2.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(7));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(events.get(3).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
        assertThat(events.get(4).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
        assertThat(events.get(5).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
        assertThat(events.get(6).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
    }

    @Test
    public void shouldNotGenerateProsecutionCasesResultedV2EventWhenOnlyApplicationIsResultAndHearingHasApplicationAndCaseBoth() throws IOException{

        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.application-prosecution-case-only-application-resulted-v2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(prosecutionCasesResultedV2.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertTrue(events.stream().noneMatch(event -> event.getClass().equals(ProsecutionCasesResultedV2.class)));
        assertThat(events.size(), is(3));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ApplicationsResulted.class)));

    }

    @Test
    public void shouldNotGenerateNextHearingWhenCaseInactiveWithAutoApplication() throws IOException{
        final UUID hearingId = randomUUID();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted.json", HearingResulted.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(hearingResulted.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(4));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(events.get(3).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
    }

    @Test
    public void shouldGenerateNextHearingWhenCaseActiveWithoutAutoApplication() throws IOException{
        final UUID hearingId = randomUUID();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-without-application.json", HearingResulted.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(hearingResulted.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(4));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(events.get(3).getClass(), is(CoreMatchers.equalTo(NextHearingsRequested.class)));
    }

    @Test
    public void shouldNotGenerateNextHearingForCaseWhenNoNextHearing() throws IOException{
        final UUID hearingId = randomUUID();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-without-next-hearing.json", HearingResulted.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(hearingResulted.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(3));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
    }

    @Test
    public void shouldNotGenerateNextHearingForCaseWhenNextHearingAndNoNewAmendment() throws IOException{
        final UUID hearingId = randomUUID();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-no-new-amendment.json", HearingResulted.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(hearingResulted.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(3));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
    }

    @Test
    public void shouldNotGenerateNextHearingForCaseWhenNextHearingAndAmendmentAsFalse() throws IOException{
        final UUID hearingId = randomUUID();

        final HearingResulted hearingResulted = HearingResulted.hearingResulted()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-no-new-amendment.json", HearingResulted.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(hearingResulted.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(3));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
    }

    @Test
    public void shouldProsecutionCaseDefendantListingStatusChangedV2EventEmittedWithHearingListingStatusWhenHearingResultedForCaseWithApplication() throws IOException{
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.prosecution-cases-resulted-v2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final List<Object> events = hearingAggregate.processHearingResults(prosecutionCasesResultedV2.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(4));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(events.get(3).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
    }



    @Test
    public void shouldApplyBreachApplicationCreationRequestedAndBreachApplicationsToBeAddedEvents() {
        final AddBreachApplication addBreachApplication = AddBreachApplication
                .addBreachApplication()
                .withBreachedApplications(asList(BreachedApplications.breachedApplications()
                                .withApplicationType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .build())
                                .withCourtOrder(CourtOrder.courtOrder()
                                        .withId(randomUUID())
                                        .build())
                                .build(),
                        BreachedApplications.breachedApplications()
                                .withApplicationType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .build())
                                .withCourtOrder(CourtOrder.courtOrder()
                                        .withId(randomUUID())
                                        .build())
                                .build()
                ))
                .withMasterDefendantId(randomUUID())
                .withHearingId(randomUUID())
                .build();

        final List<Object> response = hearingAggregate.addBreachApplication(addBreachApplication).collect(Collectors.toList());
        assertThat(response.size(), is(3));
        assertThat(response.get(0).getClass(), is(CoreMatchers.equalTo(BreachApplicationCreationRequested.class)));
        assertThat(response.get(2).getClass(), is(CoreMatchers.equalTo(BreachApplicationsToBeAddedToHearing.class)));
    }

    @Test
    public void shouldApplyExtendCustodyTimeLimitResultedEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(caseId, toMap(defendantId, singletonList(offenceId))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));
        final UUID hearingId = hearing.getId();

        final ExtendCustodyTimeLimitResulted extendCustodyTimeLimitResulted = new ExtendCustodyTimeLimitResulted.Builder()
                .withHearingId(hearingId)
                .withCaseId(caseId)
                .withOffenceId(offenceId)
                .withExtendedTimeLimit(LocalDate.now())
                .build();

        hearingAggregate.apply(extendCustodyTimeLimitResulted);

        assertThat(hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0)
                .getCustodyTimeLimit().getIsCtlExtended(), is(true));
    }

    @Test
    public void shouldNotRaiseHearingPopulateEventWhenAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(true).build()).build())
                        .build())))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        assertThat(events.findFirst().isPresent(), is(false));
        assertThat(hearing.getProsecutionCases().size(), is(1));
        assertThat(hearing.getCourtApplications().size(), is(1));
    }

    @Test
    public void shouldRaiseHearingPopulateEventWhenProsecutionCaseAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(false).build()).build())
                        .build())))
                .build();

        hearingAggregate.enrichInitiateHearing(hearing);

        final List<HearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToProbationCaseWorker()
                .map(HearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(nullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(notNullValue()));
    }

    @Test
    public void shouldRaiseHearingPopulateVejEventWhenProsecutionCaseAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(null)
                .withCourtApplications(null)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<VejHearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToVEP()
                .map(VejHearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(notNullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(nullValue()));
    }

    @Test
    public void shouldRaiseHearingResultedEventWhenHearingResultsAreSaved() {

        final UUID offenceId = randomUUID();
        final Hearing hearing = getHearing(offenceId);
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        // hearing resulted without listing number
        final Hearing hearingResult = Hearing.hearing().withValuesFrom(hearing).withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                        .withId(randomUUID())
                        .withIsYouth(true)
                        .withOffences(of(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()
                )))
                .build()
        ))).build();

        final Stream<Object> eventStream = hearingAggregate.saveHearingResult(hearingResult, ZonedDateTime.now(), of(randomUUID()));

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(HearingResulted.class));
        int listingNumber = ((HearingResulted) eventList.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getListingNumber();
        assertThat(listingNumber, is(1));
    }

    @Test
    public void shouldRaiseHearingPopulateEventWhenCourtApplicationAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(true).build()).build())
                        .build())))
                .build();

        hearingAggregate.enrichInitiateHearing(hearing);

        final List<HearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToProbationCaseWorker()
                .map(HearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(notNullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(nullValue()));
    }

    @Test
    public void shouldRaiseHearingPopulateVejEventWhenCourtApplicationAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(null)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<VejHearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToVEP()
                .map(VejHearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(notNullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(nullValue()));
    }


    @Test
    public void shouldNotRaiseHearingPopulateEventWhenBoxWork() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(true)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        assertThat(events.findFirst().isPresent(), is(false));
    }

    @Test
    public void shouldNotRaiseHearingPopulateVejEventWhenBoxWork() {
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(true)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        assertThat(events.findFirst().isPresent(), is(false));
    }

    @Test
    public void shouldNotRaiseHearingPopulateEventWhenResulted() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.apply(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .build());

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        assertThat(events.findFirst().isPresent(), is(false));
    }

    @Test
    public void shouldNotPopulateProbationCaseWorkerIfHearingIsNull() {
        setField(hearingAggregate, "hearingListingStatus",  HearingListingStatus.HEARING_INITIALISED);
        setField(hearingAggregate, "hearing", null);
        final List<Object> eventStream = hearingAggregate.populateHearingToProbationCaseWorker().toList();
        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldRaiseHearingTrialVacated() {

        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<Object> eventStream = hearingAggregate.hearingTrialVacated(hearingId, randomUUID()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(HearingTrialVacated.class)));
        final HearingTrialVacated hearingTrialVacated = (HearingTrialVacated) eventStream.get(0);

        assertThat(hearingTrialVacated.getHearingId(), is(hearingId));

    }

    @Test
    public void shouldNotRaiseHearingTrialVacatedWhenHearingIsNotCreated() {

        final UUID hearingId = randomUUID();

        final List<Object> eventStream = hearingAggregate.hearingTrialVacated(hearingId, randomUUID()).collect(toList());
        assertThat(eventStream.size(), is(0));

    }

    @Test
    public void shouldRaiseHearingPopulateEventForOnlyAdults() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()

                        )))
                        .build(), ProsecutionCase.prosecutionCase()
                        .withId(case2Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build())))
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.enrichInitiateHearing(hearing);

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        final HearingPopulatedToProbationCaseworker event = (HearingPopulatedToProbationCaseworker) events.findFirst().get();
        assertThat(event.getHearing().getProsecutionCases().size(), is(1));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case2Id));
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }

    @Test
    public void shouldRaiseHearingPopulateVejEventWithYouth() {
        final UUID case1Id = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build(), ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build())))
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        final VejHearingPopulatedToProbationCaseworker event = (VejHearingPopulatedToProbationCaseworker) events.findFirst().get();
        assertThat(event.getHearing().getProsecutionCases().size(), is(2));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case1Id));
        assertThat(prosecutionCase.getDefendants().size(), is(2));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }

    @Test
    public void shouldRaiseDeletedProbationEventWhenHearingDeleted() {

        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withIsBoxHearing(false)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.enrichInitiateHearing(hearing);

        hearingAggregate.apply(createHearingDeleted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        final DeletedHearingPopulatedToProbationCaseworker event = (DeletedHearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getId(), is(hearingId));
        assertThat(event.getHearing().getProsecutionCases().size(), is(1));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case1Id));
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }

    @Test
    public void shouldRaiseDeletedProbationVejEventWhenHearingDeleted() {

        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.apply(createHearingDeleted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        final VejDeletedHearingPopulatedToProbationCaseworker event = (VejDeletedHearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getId(), is(hearingId));
        assertThat(event.getHearing().getProsecutionCases().size(), is(1));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case1Id));
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }

    @Test
    public void shouldUpdateHearingWithCaseMarkers() {
        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();
        final Marker marker = Marker.marker().withId(randomUUID()).build();

        hearingAggregate.enrichInitiateHearing(hearing);
        hearingAggregate.updateCaseMarkers(singletonList(marker), case1Id, hearingId);

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        final HearingPopulatedToProbationCaseworker event = (HearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getProsecutionCases().get(0).getCaseMarkers().get(0).getId(), is(marker.getId()));
    }

    @Test
    public void shouldUpdateVejHearingWithCaseMarkers() {
        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();
        final Marker marker = Marker.marker().withId(randomUUID()).build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.updateCaseMarkers(singletonList(marker), case1Id, hearingId);

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        final VejHearingPopulatedToProbationCaseworker event = (VejHearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getProsecutionCases().get(0).getCaseMarkers().get(0).getId(), is(marker.getId()));
    }

    @Test
    public void shouldUpdateApplicationInHearingAndRaiseProbationEvent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()))
                        .build())
                .build());

        final List<Object> events = hearingAggregate.updateApplication(CourtApplication.courtApplication()
                .withId(applicationId)
                .withApplicationReference("B")
                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                .build()).collect(toList());

        assertThat(((HearingUpdatedWithCourtApplication) events.get(0)).getCourtApplication().getApplicationReference(), is("B"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtApplications().get(0).getApplicationReference(), is("B"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getId(), is(hearingId));

    }

    @Test
    public void shouldUpdateHearingForAllocationFieldsAndRaiseProbationEvent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withCourtApplications(asList(CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()))
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .build()
        ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields) events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingDays().get(0).getSittingDay(), is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
    }

    @Test
    public void shouldAddNewApplicationToHearingAndRaiseProbationEvent() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("A")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()
        ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields) events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingDays().get(0).getSittingDay(), is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtApplications().get(0).getId(), is(applicationId));
    }

    @Test
    public void shouldAddNewApplicationToHearingHasApplicationsAndRaiseProbationEvent() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withCourtApplications(Stream.of(CourtApplication.courtApplication()
                                .withId(randomUUID())
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()).collect(toList()))
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("A")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()
        ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields) events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingDays().get(0).getSittingDay(), is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtApplications().get(1).getId(), is(applicationId));
    }

    @Test
    public void shouldUpdateApplicationToHearingHasApplicationsAndRaiseProbationEvent() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withCourtApplications(Stream.of(CourtApplication.courtApplication()
                                .withId(randomUUID())
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build(), CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicationReference("C")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()).collect(toList()))
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("B")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()
        ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields) events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingDays().get(0).getSittingDay(), is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtApplications().get(1).getId(), is(applicationId));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtApplications().get(1).getApplicationReference(), is("B"));
    }


    @Test
    public void shouldNotUpdateApplicationToHearingWhenHearingIsNotYetCreated() {
        final UUID applicationId = randomUUID();

        final ZonedDateTime sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("B")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()
        ).collect(toList());

        assertThat(events.size(), is(0));


    }

    @Test
    public void shouldUpdateHearingWithNewProsecutionCaseWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId1)
                                                .build(),
                                        Defendant.defendant()
                                                .withId(defendantId2)
                                                .build()))).build()))
                        .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId2).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().size(), is(2));
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(2));
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(1).getId(), is(caseId2));
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(1).getDefendants().size(), is(1));
    }

    @Test
    public void shouldUpdateVejHearingWithNewProsecutionCaseWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId1)
                                                .build(),
                                        Defendant.defendant()
                                                .withId(defendantId2)
                                                .build()))).build()))
                        .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId2).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToVEP().collect(toList());
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().size(), is(2));
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(2));
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(1).getId(), is(caseId2));
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(1).getDefendants().size(), is(1));
    }

    @Test
    public void shouldUpdateHearingWithNewDefendantWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId1)
                                                .build(),
                                        Defendant.defendant()
                                                .withId(defendantId2)
                                                .build()))).build()))
                        .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().size(), is(1));
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((HearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(3));
    }

    @Test
    public void shouldUpdateVejHearingWithNewDefendantWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId1)
                                                .build(),
                                        Defendant.defendant()
                                                .withId(defendantId2)
                                                .build()))).build()))
                        .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToVEP().collect(toList());
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().size(), is(1));
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((VejHearingPopulatedToProbationCaseworker) objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(3));
    }

    @Test
    public void shouldUpdateListingNumbersOfOffencesOfProsecutionCase() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();
        final UUID caseId1 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId1)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(offenceId1)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(offenceId2)
                                                                .withListingNumber(11)
                                                                .build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(offenceId3)
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(offenceId4)
                                                                .withListingNumber(13)
                                                                .build())))
                                                .build()
                                )))
                                .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(randomUUID())
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(randomUUID())
                                                                .withListingNumber(11)
                                                                .build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(randomUUID())
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(randomUUID())
                                                                .withListingNumber(13)
                                                                .build())))
                                                .build()
                                )))
                                .build()
                )))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateOffencesWithListingNumber(asList(OffenceListingNumbers.offenceListingNumbers()
                        .withOffenceId(offenceId1).withListingNumber(10).build(),
                OffenceListingNumbers.offenceListingNumbers()
                        .withOffenceId(offenceId3).withListingNumber(12).build()
        )).collect(toList());

        ListingNumberUpdated listingNumberUpdated = (ListingNumberUpdated) events.get(0);

        assertThat(listingNumberUpdated.getHearingId(), is(hearingId));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().size(), is(2));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(0).getOffenceId(), is(offenceId1));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(0).getListingNumber(), is(10));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(1).getOffenceId(), is(offenceId3));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(1).getListingNumber(), is(12));
        assertThat(listingNumberUpdated.getProsecutionCaseIds().size(), is(1));
        assertThat(listingNumberUpdated.getProsecutionCaseIds().get(0), is(caseId1));
        assertThat(listingNumberUpdated.getCourtApplicationIds(), is(nullValue()));

    }

    @Test
    public void shouldRaiseUnAllocatedEventWhenStartDateRemoved() {
        final UUID hearingId = randomUUID();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Object> events = hearingAggregate.updateHearing(UpdateHearingFromHmi.updateHearingFromHmi().build()).collect(toList());
        final HearingMovedToUnallocated hearingMovedToUnallocated = (HearingMovedToUnallocated) events.get(0);

        assertThat(hearingMovedToUnallocated.getHearing().getHearingDays(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getJudiciary(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getListingNumber(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getHearingLanguage(), is(HearingLanguage.ENGLISH));
    }

    @Test
    public void shouldOnlyUpdateWhenHearingIsUnResulted() {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String offenceCode = new StringGenerator().next();
        final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHasSharedResults(false)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId)
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(offenceId).withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withMasterDefendantId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withId(randomUUID()).withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Offence> offences = Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build(),
                Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withOffenceTitle("SexualOffence1").build()
        ).collect(Collectors.toList());

        final List<Offence> newOffences = Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build()
        ).collect(Collectors.toList());

        List<Object> events = hearingAggregate.updateOffence(defendantId, offences, newOffences).collect(toList());
        final HearingOffencesUpdatedV2 hearingOffencesUpdated = (HearingOffencesUpdatedV2) events.get(0);
        assertThat(hearingOffencesUpdated.getHearingId(), is(hearingId));
        List<Offence> offencesInHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences();
        assertThat(offencesInHearing.size(), is(2));

    }

    @Test
    public void shouldOnlyUpdateWhenHearingHasNoSetWithHasSharedResult() {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String offenceCode = new StringGenerator().next();
        final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId)
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(offenceId).withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withMasterDefendantId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withId(randomUUID()).withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Offence> offences = Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build(),
                Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withOffenceTitle("SexualOffence1").build()
        ).collect(Collectors.toList());

        final List<Offence> newOffences  =  Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build()
        ).collect(Collectors.toList());

        List<Object> events = hearingAggregate.updateOffence(defendantId, offences, newOffences).collect(toList());
        final HearingOffencesUpdatedV2 hearingOffencesUpdated = (HearingOffencesUpdatedV2)events.get(0);
        assertThat(hearingOffencesUpdated.getHearingId(), is(hearingId));
        List<Offence> offencesInHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences();
        assertThat(offencesInHearing.size(), is(2));

    }

    @Test
    public void ShouldUpdateOffencesOfDefendantInAggregateStateForTheSameDefendant(){
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String offenceCode = new StringGenerator().next();
        final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId)
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(offenceId).withListingNumber(1).withOrderIndex(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(randomUUID()).withListingNumber(1).withOrderIndex(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Offence> offences  =  Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOrderIndex(2).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build(),
                Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withOrderIndex(1).withOffenceTitle("SexualOffence1").build()
        ).collect(Collectors.toList());

        final List<Offence> newOffences  =  Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOrderIndex(2).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build()
        ).collect(Collectors.toList());


        List<Object> events = hearingAggregate.updateOffence(defendantId, offences, newOffences).collect(toList());
        final HearingOffencesUpdatedV2 hearingOffencesUpdated = (HearingOffencesUpdatedV2)events.get(0);
        assertThat(hearingOffencesUpdated.getHearingId(), is(hearingId));
        List<Offence> offencesOfDef1InHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences();
        assertThat(offencesOfDef1InHearing.size(), is(2));
        assertThat(offencesOfDef1InHearing.get(0).getId(), is(offenceId));
        assertThat(offencesOfDef1InHearing.get(0).getOffenceTitle(), is("SexualOffence1"));
        assertThat(offencesOfDef1InHearing.get(1).getId(), not(is(offenceId)));
        assertThat(offencesOfDef1InHearing.get(1).getOffenceTitle(), is("RegularOffence1"));


        List<Offence> offencesOfDef2InHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences();
        assertThat(offencesOfDef2InHearing.size(), is(1));
        assertThat(offencesOfDef2InHearing.get(0).getId(), not(is(offenceId)));
        assertThat(offencesOfDef2InHearing.get(0).getOffenceTitle(), is(nullValue()));
    }

    @Test
    public void ShouldUpdateOffencesOfDefendantInAggregateStateForTheSameDefendantForOnlyUpdatedOffences(){
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String offenceCode = new StringGenerator().next();
        final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId)
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(offenceId).withListingNumber(1).withOrderIndex(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(randomUUID()).withListingNumber(1).withOrderIndex(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Offence> offences  =  Stream.of(
                Offence.offence().withId(offenceId).withOffenceCode(offenceCode).withOrderIndex(1).withOffenceTitle("SexualOffence1").build()
        ).collect(Collectors.toList());


        List<Object> events = hearingAggregate.updateOffence(defendantId, offences, null).collect(toList());
        final HearingOffencesUpdatedV2 hearingOffencesUpdated = (HearingOffencesUpdatedV2)events.get(0);
        assertThat(hearingOffencesUpdated.getHearingId(), is(hearingId));
        List<Offence> offencesOfDef1InHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences();
        assertThat(offencesOfDef1InHearing.size(), is(1));
        assertThat(offencesOfDef1InHearing.get(0).getId(), is(offenceId));
        assertThat(offencesOfDef1InHearing.get(0).getOffenceTitle(), is("SexualOffence1"));

        List<Offence> offencesOfDef2InHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences();
        assertThat(offencesOfDef2InHearing.size(), is(1));
        assertThat(offencesOfDef2InHearing.get(0).getId(), not(is(offenceId)));
        assertThat(offencesOfDef2InHearing.get(0).getOffenceTitle(), is(nullValue()));
    }

    @Test
    public void ShouldAddOffencesOfDefendantInAggregateStateForTheSameDefendantForOnlyNewOffences(){
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String offenceCode = new StringGenerator().next();
        final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId)
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(offenceId).withListingNumber(1).withOrderIndex(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(randomUUID()).withListingNumber(1).withOrderIndex(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Offence> newOffences  =  Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOrderIndex(2).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build()
        ).collect(Collectors.toList());


        List<Object> events = hearingAggregate.updateOffence(defendantId, null, newOffences).collect(toList());
        final HearingOffencesUpdatedV2 hearingOffencesUpdated = (HearingOffencesUpdatedV2)events.get(0);
        assertThat(hearingOffencesUpdated.getHearingId(), is(hearingId));
        List<Offence> offencesOfDef1InHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences();
        assertThat(offencesOfDef1InHearing.size(), is(2));
        assertThat(offencesOfDef1InHearing.get(0).getId(), is(offenceId));
        assertThat(offencesOfDef1InHearing.get(1).getId(), not(is(offenceId)));
        assertThat(offencesOfDef1InHearing.get(1).getOffenceTitle(), is("RegularOffence1"));


        List<Offence> offencesOfDef2InHearing = hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences();
        assertThat(offencesOfDef2InHearing.size(), is(1));
        assertThat(offencesOfDef2InHearing.get(0).getId(), not(is(offenceId)));
        assertThat(offencesOfDef2InHearing.get(0).getOffenceTitle(), is(nullValue()));
    }

    @Test
    public void shouldNotUpdateWhenHearingIfOffenceIsNotInHearing() {
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final String offenceCode = new StringGenerator().next();
        final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHasSharedResults(false)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId)
                                                .withMasterDefendantId(defendantId)
                                                .withOffences(singletonList(Offence.offence().withId(offenceId).withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withMasterDefendantId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withId(randomUUID()).withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Offence> offences = Stream.of(
                Offence.offence().withId(randomUUID()).withOffenceCode(randomUUID().toString()).withOffenceTitle("RegularOffence1").withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction().withId(randomUUID()).withLabel(YOUTH_RESTRICTION).withOrderedDate(LocalDate.now()).build())).build()
        ).collect(Collectors.toList());

        List<Object> events = hearingAggregate.updateOffence(defendantId, offences, null).collect(toList());
        assertThat(events.isEmpty(), is(true));

    }

    @Test
    public void shouldRaiseUnAllocatedEventWhenCourtRoomRemoved() {
        final UUID hearingId = randomUUID();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Object> events = hearingAggregate.updateHearing(UpdateHearingFromHmi.updateHearingFromHmi()
                .withStartDate(LocalDate.now().toString())
                .build()).collect(toList());
        final HearingMovedToUnallocated hearingMovedToUnallocated = (HearingMovedToUnallocated) events.get(0);

        assertThat(hearingMovedToUnallocated.getHearing().getHearingDays().size(), is(1));
        assertThat(hearingMovedToUnallocated.getHearing().getHearingDays().get(0).getCourtRoomId(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getJudiciary(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getListingNumber(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getHearingLanguage(), is(HearingLanguage.ENGLISH));
    }

    @Test
    public void ShouldUpdateOffencesOfDefendantInAggregateStateForTheSameDefendant1(){
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicant(CourtApplicationParty.courtApplicationParty()
                                .withId(defendantId)
                                .withMasterDefendant(MasterDefendant.masterDefendant().withMasterDefendantId(defendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("address1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("address1")
                                                .build())
                                        .build())
                                .build())
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withId(defendantId)
                                .withMasterDefendant(MasterDefendant.masterDefendant().withMasterDefendantId(defendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("address1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("address1")
                                                .build())
                                        .build())
                                .build())
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withPlea(Plea.plea().withOffenceId(offenceId1).build())
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(11)
                                                .build())))
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(defendantId)
                .withProsecutionCaseId(randomUUID())
                .withMasterDefendantId(defendantId)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person().withAddress(Address.address().withAddress1("addressNew").build()).build())
                        .build())
                .build();

        List<Object> events = hearingAggregate.updateApplicationHearing(defendantUpdate).collect(toList());
        final ApplicationHearingDefendantUpdated applicationHearingDefendantUpdated = (ApplicationHearingDefendantUpdated)events.get(0);
        final CourtApplication courtApplication = applicationHearingDefendantUpdated.getHearing().getCourtApplications().get(0);
        assertThat(applicationHearingDefendantUpdated.getHearing().getId(), is(hearingId));
        assertThat(courtApplication.getApplicant().getMasterDefendant().getPersonDefendant().getPersonDetails().getAddress().getAddress1(), is("addressNew"));
        assertThat(courtApplication.getApplicant().getUpdatedOn(), is(notNullValue()));

    }

    @Test
    public void shouldNotRaiseUnAllocatedEventWhenHMINotChanged() {
        final UUID hearingId = randomUUID();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Object> events = hearingAggregate.updateHearing(UpdateHearingFromHmi.updateHearingFromHmi()
                .withStartDate(LocalDate.now().toString())
                .withCourtRoomId(randomUUID())
                .build()).collect(toList());

        assertThat(events.size(), is(0));
    }

    @Test
    public void shouldUpdateVerdictOfOffencesOfProsecutionCase() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();
        final UUID caseId1 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(caseId1)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId1)
                                                        .withVerdict(Verdict.verdict().withOffenceId(offenceId1).build())
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())))
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId3)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId4)
                                                        .withListingNumber(13)
                                                        .build())))
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithVerdict(Verdict.verdict()
                .withOffenceId(offenceId3)
                .withVerdictType(VerdictType.verdictType().withCategoryType("categoryType").build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getVerdict().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1).getId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1).getVerdict(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0).getId(), is(offenceId3));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0).getVerdict().getOffenceId(), is(offenceId3));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0).getVerdict().getVerdictType().getCategoryType(), is("categoryType"));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(1).getId(), is(offenceId4));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(1).getVerdict(), is(nullValue()));
    }


    @Test
    public void shouldProsecutionCaseDefendantListingStatusChangedV2EventEmittedWithHearingListingStatusWhenHearingResultedForApplication() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withVerdict(Verdict.verdict().withOffenceId(offenceId1).build())
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(11)
                                                .build())))
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingForApplicationCreatedV2.hearingForApplicationCreatedV2().withHearing(hearing).withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING).build());

        final List<Object> events = hearingAggregate.processHearingResults(hearing, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) events.stream().filter(e -> e instanceof ProsecutionCaseDefendantListingStatusChangedV2).findFirst().get();

        assertThat(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getId(), is(hearingId));

        assertThat(prosecutionCaseDefendantListingStatusChangedV2.getHearingListingStatus(), is(HearingListingStatus.HEARING_RESULTED));
    }


    @Test
    public void shouldUpdateVerdictOfOffencesOfCourtApplicationCase() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withVerdict(Verdict.verdict().withOffenceId(offenceId1).build())
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(11)
                                                .build())))
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithVerdict(Verdict.verdict()
                .withOffenceId(offenceId2)
                .withVerdictType(VerdictType.verdictType().withCategoryType("categoryType").build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getVerdict().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getVerdict().getVerdictType(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(1).getId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(1).getVerdict().getOffenceId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(1).getVerdict().getVerdictType().getCategoryType(), is("categoryType"));
    }


    @Test
    public void shouldTestHearingListingStatusAfterHearingInitiate() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID defenceCounselId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withVerdict(Verdict.verdict().withOffenceId(offenceId1).build())
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(11)
                                                .build())))
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        final List<Object> events = hearingAggregate.addDefenceCounselToHearing(DefenceCounsel.defenceCounsel()
                .withId(defenceCounselId)
                .withFirstName("FirstName")
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getDefenceCounsels().get(0).getId(), is(defenceCounselId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getDefenceCounsels().get(0).getFirstName(), is("FirstName"));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus(), is(HearingListingStatus.HEARING_INITIALISED));

    }

    @Test
    public void shouldNotUpdateVerdictOfCourtApplicationCaseWhenOffenceIsNotLinked() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithVerdict(Verdict.verdict()
                .withOffenceId(offenceId2)
                .withVerdictType(VerdictType.verdictType().withCategoryType("categoryType").build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder(), is(nullValue()));

    }

    @Test
    public void shouldUpdateVerdictOfOffencesOfCourtOrderOfCourtApplication() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withCourtOrderOffences(asList(CourtOrderOffence.courtOrderOffence()
                                                .withOffence(Offence.offence()
                                                        .withId(offenceId1)
                                                        .withVerdict(Verdict.verdict().withOffenceId(offenceId1).build())
                                                        .build())
                                                .build(),
                                        CourtOrderOffence.courtOrderOffence()
                                                .withOffence(Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())
                                                .build()))
                                .build())
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithVerdict(Verdict.verdict()
                .withOffenceId(offenceId2)
                .withVerdictType(VerdictType.verdictType().withCategoryType("categoryType").build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getVerdict().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getVerdict().getVerdictType(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(1).getOffence().getId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(1).getOffence().getVerdict().getOffenceId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(1).getOffence().getVerdict().getVerdictType().getCategoryType(), is("categoryType"));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases(), is(nullValue()));

    }

    @Test
    public void shouldNotUpdateVerdictOfCourtOrderOfCourtApplicationWhenCourtOrderIsNotLinkedWithOffences() {
        final UUID hearingId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withId(randomUUID())
                                .build())
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithVerdict(Verdict.verdict()
                .withOffenceId(offenceId2)
                .withVerdictType(VerdictType.verdictType().withCategoryType("categoryType").build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases(), is(nullValue()));

    }

    @Test
    public void shouldUpdateVerdictOfCourtApplication() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();


        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithVerdict(Verdict.verdict()
                .withApplicationId(applicationId)
                .withVerdictType(VerdictType.verdictType().withCategoryType("categoryType").build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getVerdict().getVerdictType().getCategoryType(), is("categoryType"));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases(), is(nullValue()));


    }

    @Test
    public void shouldUpdatePleaOfCourtApplication() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();


        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithPlea(PleaModel.pleaModel()
                .withApplicationId(applicationId)
                .withPlea(Plea.plea().withApplicationId(applicationId)
                        .withPleaValue(GUILTY)
                        .build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getPlea().getPleaValue(), is(GUILTY));

    }

    @Test
    public void shouldUpdatePleaOfOffencesOfProsecutionCase() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();
        final UUID caseId1 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(caseId1)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId1)
                                                        .withPlea(Plea.plea().withOffenceId(offenceId1).build())
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())))
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId3)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId4)
                                                        .withListingNumber(13)
                                                        .build())))
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithPlea(PleaModel.pleaModel()
                .withOffenceId(offenceId3)
                .withPlea(Plea.plea().withPleaValue(GUILTY).withOffenceId(offenceId3).build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getPlea().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1).getId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1).getPlea(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0).getId(), is(offenceId3));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0).getPlea().getOffenceId(), is(offenceId3));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(0).getPlea().getPleaValue(), is(GUILTY));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(1).getId(), is(offenceId4));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(1).getOffences().get(1).getPlea(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications(), is(nullValue()));

    }

    @Test
    public void shouldUpdatePleaOfOffencesOfCourtApplicationCase() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withPlea(Plea.plea().withOffenceId(offenceId1).build())
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(11)
                                                .build())))
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithPlea(PleaModel.pleaModel()
                .withOffenceId(offenceId2)
                .withPlea(Plea.plea().withOffenceId(offenceId2).withPleaValue(GUILTY).build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getPlea().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(0).getPlea().getPleaValue(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(1).getId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(1).getPlea().getOffenceId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences().get(1).getPlea().getPleaValue(), is(GUILTY));
    }

    @Test
    public void shouldUpdatePleaOfOffencesWhenCourtApplicationCaseDoesNotHAveOffences() {

        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID caseId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(caseId)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withPlea(Plea.plea().withOffenceId(offenceId1).build())
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(11)
                                                .build())))
                                .build()
                        )))
                        .build()
                )))
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseId(caseId)
                                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                        .withCaseURN("ASBC09834")
                                        .build())
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithPlea(PleaModel.pleaModel()
                .withOffenceId(offenceId2)
                .withPlea(Plea.plea().withOffenceId(offenceId2).withPleaValue(GUILTY).build())
                .build()).collect(toList());
        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getPlea().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(1).getPlea().getPleaValue(), is(GUILTY));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getId(), is(applicationId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtApplicationCases().get(0).getOffences(), is(nullValue()));


    }


    @Test
    public void shouldUpdatePleaWhenCourtApplicationIsLinkedWithHearing() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID caseId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId1)
                                                        .withPlea(Plea.plea().withOffenceId(offenceId1).build())
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())))
                                        .build()
                                )))
                                .build()
                        )))
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("A")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()

        ).collect(toList());

        final List<Object> events = hearingAggregate.updateHearingWithPlea(PleaModel.pleaModel()
                .withOffenceId(offenceId2)
                .withApplicationId(applicationId)
                .withPlea(Plea.plea().withOffenceId(offenceId2).withApplicationId(applicationId).withPleaValue(GUILTY).build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getPlea().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getId(), is(applicationId));


    }


    @Test
    public void shouldNotUpdatePleaWhenCourtApplicationIsNotLinkedWithHearing() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID caseId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                                .withId(caseId)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId1)
                                                        .withPlea(Plea.plea().withOffenceId(offenceId1).build())
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())))
                                        .build()
                                )))
                                .build()
                        )))
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .build())
                .build());

        final List<Object> events = hearingAggregate.updateHearingWithPlea(PleaModel.pleaModel()
                .withOffenceId(offenceId2)
                .withApplicationId(applicationId)
                .withPlea(Plea.plea().withOffenceId(offenceId2).withApplicationId(applicationId).withPleaValue(GUILTY).build())
                .build()).collect(toList());

        assertThat(events.size(), is(0));

    }

    @Test
    public void shouldUpdatePleaOfOffencesOfCourtOrderOfCourtApplication() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID applicationId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withCourtOrderOffences(asList(CourtOrderOffence.courtOrderOffence()
                                                .withOffence(Offence.offence()
                                                        .withId(offenceId1)
                                                        .withPlea(Plea.plea().withOffenceId(offenceId1).build())
                                                        .build())
                                                .build(),
                                        CourtOrderOffence.courtOrderOffence()
                                                .withOffence(Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())
                                                .build()))
                                .build())
                        .build()))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateHearingWithPlea(PleaModel.pleaModel()
                .withOffenceId(offenceId2)
                .withPlea(Plea.plea().withOffenceId(offenceId2).withPleaValue(GUILTY).build())
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getPlea().getOffenceId(), is(offenceId1));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(0).getOffence().getPlea().getPleaValue(), is(nullValue()));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(1).getOffence().getId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(1).getOffence().getPlea().getOffenceId(), is(offenceId2));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications().get(0).getCourtOrder().getCourtOrderOffences().get(1).getOffence().getPlea().getPleaValue(), is(GUILTY));
    }

    @Test
    public void shouldAddNewDefenceCounsel() {
        final UUID defenceCounselId = randomUUID();
        final UUID hearingId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        final List<Object> events = hearingAggregate.addDefenceCounselToHearing(DefenceCounsel.defenceCounsel()
                .withId(defenceCounselId)
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);
        HearingDefenceCounselAdded hearingDefenceCounselAdded = (HearingDefenceCounselAdded) events.get(0);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getDefenceCounsels().get(0).getId(), is(defenceCounselId));

        assertThat(hearingDefenceCounselAdded.getHearingId(), is(hearingId));
        assertThat(hearingDefenceCounselAdded.getDefenceCounsel().getId(), is(defenceCounselId));

    }

    @Test
    public void shouldUpdateNewDefenceCounsel() {
        final UUID defenceCounselId = randomUUID();
        final UUID hearingId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withDefenceCounsels(singletonList(DefenceCounsel.defenceCounsel()
                        .withId(defenceCounselId)
                        .build()))
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        final List<Object> events = hearingAggregate.updateHearingWithDefenceCounsel(DefenceCounsel.defenceCounsel()
                .withId(defenceCounselId)
                .withFirstName("FirstName")
                .build()).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getDefenceCounsels().get(0).getId(), is(defenceCounselId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getDefenceCounsels().get(0).getFirstName(), is("FirstName"));

        HearingDefenceCounselUpdated hearingDefenceCounselUpdated = (HearingDefenceCounselUpdated) events.get(0);
        assertThat(hearingDefenceCounselUpdated.getHearingId(), is(hearingId));
        assertThat(hearingDefenceCounselUpdated.getDefenceCounsel().getId(), is(defenceCounselId));
        assertThat(hearingDefenceCounselUpdated.getDefenceCounsel().getFirstName(), is("FirstName"));
    }

    @Test
    public void shouldRemoveNewDefenceCounsel() {
        final UUID defenceCounselId = randomUUID();
        final UUID hearingId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withDefenceCounsels(singletonList(DefenceCounsel.defenceCounsel()
                        .withId(defenceCounselId)
                        .build()))
                .build();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        final List<Object> events = hearingAggregate.removeDefenceCounselFromHearing(defenceCounselId).collect(toList());

        ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChanged) events.get(1);

        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getId(), is(hearingId));
        assertThat(prosecutionCaseDefendantListingStatusChanged.getHearing().getDefenceCounsels(), is(nullValue()));

        HearingDefenceCounselRemoved hearingDefenceCounselRemoved = (HearingDefenceCounselRemoved) events.get(0);

        assertThat(hearingDefenceCounselRemoved.getHearingId(), is(hearingId));
        assertThat(hearingDefenceCounselRemoved.getId(), is(defenceCounselId));
    }

    @Test
    public void shouldGetHearingType() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final ZonedDateTime inputDate = ZonedDateTime.now();

        final Hearing hearing = getHearing(caseId, defendantId, inputDate, false);
        createHearingInitiated(hearing);

        final HearingType hearingType = hearingAggregate.getHearingType();
        assertThat(hearingType.getDescription(), is("First hearing"));
    }

    @Test
    public void shouldGetHearingDate() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final ZonedDateTime inputDate = ZonedDateTime.now();

        final Hearing hearing = getHearing(caseId, defendantId, inputDate, false);
        createHearingInitiated(hearing);

        final ZonedDateTime hearingDate = hearingAggregate.getHearingDate();
        assertThat(hearingDate, is(inputDate));
    }

    @Test
    public void shouldGenerateOpaPublicListNotice() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false, false);
        createHearingInitiated(hearing);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);

        final Stream<Object> eventStream = hearingAggregate.generateOpaPublicListNotice(prosecutionCase, defendantId, LocalDate.now());
        final Optional<OpaPublicListNoticeGenerated> event = eventStream.map(OpaPublicListNoticeGenerated.class::cast).findFirst();

        assertThat(event.isPresent(), is(true));
        assertThat(event.get().getOpaNotice(), is(notNullValue()));
    }

    @Test
    public void shouldGenerateOpaPressListNotice() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false, false);
        createHearingInitiated(hearing);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final OnlinePleasAllocation pleasAllocation = OnlinePleasAllocation.onlinePleasAllocation().build();

        final Stream<Object> eventStream = hearingAggregate.generateOpaPressListNotice(prosecutionCase, defendantId, pleasAllocation, LocalDate.now());
        final Optional<OpaPressListNoticeGenerated> event = eventStream.map(OpaPressListNoticeGenerated.class::cast).findFirst();

        assertThat(event.isPresent(), is(true));
        assertThat(event.get().getOpaNotice(), is(notNullValue()));
    }

    @Test
    public void shouldGenerateOpaResultListNotice() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false, false);
        createHearingInitiated(hearing);
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);

        final Stream<Object> eventStream = hearingAggregate.generateOpaResultListNotice(prosecutionCase, defendantId, LocalDate.now());
        final Optional<OpaResultListNoticeGenerated> event = eventStream.map(OpaResultListNoticeGenerated.class::cast).findFirst();

        assertThat(event.isPresent(), is(true));
        assertThat(event.get().getOpaNotice(), is(notNullValue()));
    }

    @Test
    public void shouldCheckIsPublicListNoticeAlreadySent() {
        final UUID defendantId = randomUUID();

        final Map<UUID, Set<LocalDate>> noticeSentMap = new HashMap<>();
        noticeSentMap.put(defendantId, emptySet());

        setField(hearingAggregate, "opaPublicListNoticesSent", noticeSentMap);

        final boolean noticeAlreadySent = hearingAggregate.isPublicListNoticeAlreadySent(defendantId, LocalDate.now());

        assertThat(noticeAlreadySent, is(false));
    }

    @Test
    public void shouldCheckIsPublicListNoticeAlreadySentWhenNoticeAlreadySent() {
        final UUID defendantId = randomUUID();
        final Map<UUID, Set<LocalDate>> noticeSentMap = getNoticeSentMap(defendantId);

        setField(hearingAggregate, "opaPublicListNoticesSent", noticeSentMap);

        final boolean noticeAlreadySent = hearingAggregate.isPublicListNoticeAlreadySent(defendantId, LocalDate.now());

        assertThat(noticeAlreadySent, is(true));
    }

    @Test
    public void shouldCheckIsPressListNoticeAlreadySent() {
        final UUID defendantId = randomUUID();

        final Map<UUID, Set<LocalDate>> noticeSentMap = new HashMap<>();
        noticeSentMap.put(defendantId, emptySet());

        setField(hearingAggregate, "opaPressListNoticesSent", noticeSentMap);

        final boolean noticeAlreadySent = hearingAggregate.isPressListNoticeAlreadySent(defendantId, LocalDate.now());

        assertThat(noticeAlreadySent, is(false));
    }

    @Test
    public void shouldCheckIsPressListNoticeAlreadySentWhenNoticeAlreadySent() {
        final UUID defendantId = randomUUID();
        final Map<UUID, Set<LocalDate>> noticeSentMap = getNoticeSentMap(defendantId);

        setField(hearingAggregate, "opaPressListNoticesSent", noticeSentMap);

        final boolean noticeAlreadySent = hearingAggregate.isPressListNoticeAlreadySent(defendantId, LocalDate.now());

        assertThat(noticeAlreadySent, is(true));
    }

    @Test
    public void shouldCheckIsResultListNoticeAlreadySent() {
        final UUID defendantId = randomUUID();

        final Map<UUID, Set<LocalDate>> noticeSentMap = new HashMap<>();
        noticeSentMap.put(defendantId, emptySet());

        setField(hearingAggregate, "opaResultListNoticesSent", noticeSentMap);

        final boolean resultListNoticeAlreadySent = hearingAggregate.isResultListNoticeAlreadySent(defendantId, LocalDate.now());

        assertThat(resultListNoticeAlreadySent, is(false));
    }

    @Test
    public void shouldCheckIsResultListNoticeAlreadySentWhenNoticeAlreadySent() {
        final UUID defendantId = randomUUID();
        final Map<UUID, Set<LocalDate>> noticeSentMap = getNoticeSentMap(defendantId);

        setField(hearingAggregate, "opaResultListNoticesSent", noticeSentMap);

        final boolean resultListNoticeAlreadySent = hearingAggregate.isResultListNoticeAlreadySent(defendantId, LocalDate.now());

        assertThat(resultListNoticeAlreadySent, is(true));
    }

    @Test
    public void shouldDeactivatePublicListNotice() {
        final UUID defendantId = randomUUID();
        final OpaPublicListNoticeDeactivated event = OpaPublicListNoticeDeactivated.opaPublicListNoticeDeactivated().withDefendantId(defendantId).build();

        final Map<UUID, Set<Object>> noticeSentMap = new HashMap<>();
        noticeSentMap.put(defendantId, emptySet());

        setField(hearingAggregate, "opaPublicListNoticesSent", noticeSentMap);

        hearingAggregate.apply(event);

        assertThat(noticeSentMap.containsKey(defendantId), is(false));
    }

    @Test
    public void shouldDeactivatePressListNotice() {
        final UUID defendantId = randomUUID();
        final OpaPressListNoticeDeactivated event = OpaPressListNoticeDeactivated.opaPressListNoticeDeactivated().withDefendantId(defendantId).build();

        final Map<UUID, Set<Object>> noticeSentMap = new HashMap<>();
        noticeSentMap.put(defendantId, emptySet());

        setField(hearingAggregate, "opaPressListNoticesSent", noticeSentMap);

        hearingAggregate.apply(event);

        assertThat(noticeSentMap.containsKey(defendantId), is(false));
    }

    @Test
    public void shouldDeactivateResultListNotice() {
        final UUID defendantId = randomUUID();
        final OpaResultListNoticeDeactivated event = OpaResultListNoticeDeactivated.opaResultListNoticeDeactivated().withDefendantId(defendantId).build();

        final Map<UUID, Set<Object>> noticeSentMap = new HashMap<>();
        noticeSentMap.put(defendantId, emptySet());

        setField(hearingAggregate, "opaResultListNoticesSent", noticeSentMap);

        hearingAggregate.apply(event);

        assertThat(noticeSentMap.containsKey(defendantId), is(false));
    }

    @Test
    public void shouldCheckOpaPublicListCriteriaSuccess() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false, false);
        createHearingInitiated(hearing);

        final boolean result = hearingAggregate.checkOpaPublicListCriteria(defendantId, requestDate);

        assertThat(result, is(true));
    }

    @Test
    public void shouldCheckOpaPublicListCriteriaWithYouthDefendant() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), true, false);
        createHearingInitiated(hearing);

        final boolean result = hearingAggregate.checkOpaPublicListCriteria(defendantId, requestDate);

        assertThat(result, is(false));
    }

    @Test
    public void shouldCheckOpaPublicListCriteriaWithResultShared() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false, true);
        createHearingInitiated(hearing);

        final boolean result = hearingAggregate.checkOpaPublicListCriteria(defendantId, requestDate);

        assertThat(result, is(false));
    }

    @Test
    public void shouldCheckOpaPublicListCriteriaWithSameHearingDateAndRequestDate() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now(), false, false);
        createHearingInitiated(hearing);

        final boolean result = hearingAggregate.checkOpaPublicListCriteria(defendantId, requestDate);

        assertThat(result, is(false));
    }

    @Test
    public void shouldCheckOpaPressListCriteriaSuccess() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false, false);
        createHearingInitiated(hearing);

        final boolean result = hearingAggregate.checkOpaPressListCriteria(requestDate);

        assertThat(result, is(true));
    }

    @Test
    public void shouldCheckOpaPressListCriteriaWithRequestDateSameAsHearingDate() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now(), false, false);
        createHearingInitiated(hearing);

        final boolean result = hearingAggregate.checkOpaPressListCriteria(requestDate);

        assertThat(result, is(false));
    }
    @Test
    public void shouldCheckOpaPressListCriteriaWithResultShared() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false);
        createHearingResulted(ZonedDateTime.now(), hearing);

        final boolean result = hearingAggregate.checkOpaPressListCriteria(requestDate);

        assertThat(result, is(false));
    }

    @Test
    public void shouldCheckOpaResultListCriteriaSuccess() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false);
        createHearingResulted(ZonedDateTime.now(), hearing);


        final boolean result = hearingAggregate.checkOpaResultListCriteria(defendantId, requestDate);

        assertThat(result, is(true));
    }
    @Test
    public void shouldCheckOpaResultListCriteriaWithoutResultNotShared() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false);
        createHearingResulted(null, hearing);

        final boolean result = hearingAggregate.checkOpaResultListCriteria(defendantId, requestDate);

        assertThat(result, is(false));
    }


    @Test
    public void shouldCheckOpaResultListCriteriaWithYouth() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), true);
        createHearingResulted(ZonedDateTime.now(), hearing);

        final boolean result = hearingAggregate.checkOpaResultListCriteria(defendantId, requestDate);

        assertThat(result, is(false));

    }

    @Test
    public void shouldCheckOpaResultListCriteriaSuccessWith4daysAfterResulted() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false);
        createHearingResulted(ZonedDateTime.now().minusDays(4), hearing);

        final boolean result = hearingAggregate.checkOpaResultListCriteria(defendantId, requestDate);

        assertThat(result, is(true));
    }

    @Test
    public void shouldCheckOpaResultListCriteriaWith5daysAfterResulted() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();

        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now().plusDays(1), false);
        createHearingResulted(ZonedDateTime.now().minusDays(5), hearing);

        final boolean result = hearingAggregate.checkOpaResultListCriteria(defendantId, requestDate);

        assertThat(result, is(false));
    }

    @Test
    public void shouldCheckOpaResultListCriteriaWithResultedOnHearingDate() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final LocalDate requestDate = LocalDate.now();
        final Hearing hearing = getHearing(caseId, defendantId, ZonedDateTime.now(), false);
        createHearingResulted(ZonedDateTime.now(), hearing);

        final boolean result = hearingAggregate.checkOpaResultListCriteria(defendantId, requestDate);

        assertThat(result, is(false));
    }

    @Test
    public void shouldBuildDeactivateOpaPublicListNoticeEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();

        final Stream<Object> eventStream = hearingAggregate.generateDeactivateOpaPublicListNotice(caseId, defendantId, hearingId);
        final Optional<OpaPublicListNoticeDeactivated> optionalEvent = eventStream.map(OpaPublicListNoticeDeactivated.class::cast).findFirst();

        assertThat(optionalEvent.isPresent(), is(true));
        final OpaPublicListNoticeDeactivated event = optionalEvent.get();
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldBuildDeactivateOpaPressListNoticeEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();

        final Stream<Object> eventStream = hearingAggregate.generateDeactivateOpaPressListNotice(caseId, defendantId, hearingId);
        final Optional<OpaPressListNoticeDeactivated> optionalEvent = eventStream.map(OpaPressListNoticeDeactivated.class::cast).findFirst();

        assertThat(optionalEvent.isPresent(), is(true));
        final OpaPressListNoticeDeactivated event = optionalEvent.get();
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldBuildDeactivateOpaResultListNoticeEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();

        final Stream<Object> eventStream = hearingAggregate.generateDeactivateOpaResultListNotice(caseId, defendantId, hearingId);
        final Optional<OpaResultListNoticeDeactivated> optionalEvent = eventStream.map(OpaResultListNoticeDeactivated.class::cast).findFirst();

        assertThat(optionalEvent.isPresent(), is(true));
        final OpaResultListNoticeDeactivated event = optionalEvent.get();
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldAddOpaPublicListNoticeEvent() {
        final UUID notificationId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final LocalDate triggerDate = LocalDate.now();

        final Map<UUID, Set<Object>> noticeSentMap = new HashMap<>();

        setField(hearingAggregate, "opaPublicListNoticesSent", noticeSentMap);

        final Stream<Object> eventStream = hearingAggregate.opaPublicListNoticeSent(notificationId, hearingId, defendantId, triggerDate);
        final Optional<OpaPublicListNoticeSent> optionalEvent = eventStream.map(OpaPublicListNoticeSent.class::cast).findFirst();

        assertThat(optionalEvent.isPresent(), is(true));
        final OpaPublicListNoticeSent event = optionalEvent.get();
        assertThat(event.getNotificationId(), is(notificationId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getHearingId(), is(hearingId));
        assertThat(event.getTriggerDate(), is(triggerDate));

        hearingAggregate.opaPublicListNoticeSent(notificationId, hearingId, defendantId, LocalDate.now().minusDays(1));

        final Set<Object> objects = noticeSentMap.get(defendantId);
        assertThat(objects.size(), is(2));
    }

    @Test
    public void shouldAddOpaPressListNoticeEvent() {
        final UUID notificationId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final LocalDate triggerDate = LocalDate.now();

        final Map<UUID, Set<Object>> noticeSentMap = new HashMap<>();

        setField(hearingAggregate, "opaPressListNoticesSent", noticeSentMap);

        final Stream<Object> eventStream = hearingAggregate.opaPressListNoticeSent(notificationId, hearingId, defendantId, triggerDate);
        final Optional<OpaPressListNoticeSent> optionalEvent = eventStream.map(OpaPressListNoticeSent.class::cast).findFirst();

        assertThat(optionalEvent.isPresent(), is(true));
        final OpaPressListNoticeSent event = optionalEvent.get();
        assertThat(event.getNotificationId(), is(notificationId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getHearingId(), is(hearingId));
        assertThat(event.getTriggerDate(), is(triggerDate));

        hearingAggregate.opaPressListNoticeSent(notificationId, hearingId, defendantId, LocalDate.now().minusDays(1));

        final Set<Object> objects = noticeSentMap.get(defendantId);
        assertThat(objects.size(), is(2));
    }

    @Test
    public void shouldAddOpaResultListNoticeEvent() {
        final UUID notificationId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final LocalDate triggerDate = LocalDate.now();

        final Map<UUID, Set<Object>> noticeSentMap = new HashMap<>();

        setField(hearingAggregate, "opaResultListNoticesSent", noticeSentMap);

        final Stream<Object> eventStream = hearingAggregate.opaResultListNoticeSent(notificationId, hearingId, defendantId, triggerDate);
        final Optional<OpaResultListNoticeSent> optionalEvent = eventStream.map(OpaResultListNoticeSent.class::cast).findFirst();

        assertThat(optionalEvent.isPresent(), is(true));
        final OpaResultListNoticeSent event = optionalEvent.get();
        assertThat(event.getNotificationId(), is(notificationId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getHearingId(), is(hearingId));
        assertThat(event.getTriggerDate(), is(triggerDate));

        hearingAggregate.opaResultListNoticeSent(notificationId, hearingId, defendantId, LocalDate.now().minusDays(1));

        final Set<Object> objects = noticeSentMap.get(defendantId);
        assertThat(objects.size(), is(2));
    }

    @Test
    public void shouldRaiseRelatedHearingAndStatusChangeEventWithMultipleCaseAndOneDefendantAndOffences() {
        final Optional<JsonObject> relatedHearingRequestedJsonObject =
                Optional.of(getJsonObjectResponseFromJsonResource("json/progression.event.related-hearing-requested.json"));

        final RelatedHearingRequested extendHearingRequested = jsonObjectToObjectConverter.convert(relatedHearingRequestedJsonObject.get(), RelatedHearingRequested.class);
        final UUID seedingHearingId = extendHearingRequested.getSeedingHearing().getSeedingHearingId();
        final UpdateRelatedHearingCommand updateRelatedHearingCommand = UpdateRelatedHearingCommand.updateRelatedHearingCommand()
                .withHearingRequest(extendHearingRequested.getHearingRequest())
                .withIsAdjourned(extendHearingRequested.getIsAdjourned())
                .withSeedingHearing(extendHearingRequested.getSeedingHearing())
                .withShadowListedOffences(extendHearingRequested.getShadowListedOffences())
                .build();
        final HearingListingNeeds hearingListingNeeds = updateRelatedHearingCommand.getHearingRequest();

        final Optional<JsonObject> hearingJsonObject =
                Optional.of(getJsonObjectResponseFromJsonResource("json/progression-hearing.json"));
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJsonObject.get(), Hearing.class);

        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;

        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);

        final List<Object> eventStream = hearingAggregate.updateRelatedHearing(hearingListingNeeds,
                updateRelatedHearingCommand.getIsAdjourned(),
                updateRelatedHearingCommand.getExtendedHearingFrom(),
                updateRelatedHearingCommand.getIsPartiallyAllocated(),
                updateRelatedHearingCommand.getSeedingHearing(),
                updateRelatedHearingCommand.getShadowListedOffences()).collect(toList());

        assertThat(eventStream.size(), is(2));
        ProsecutionCaseDefendantListingStatusChangedV2 caseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream.get(0);
        assertThat(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().size(), is(2));
        assertThat(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(1).getDefendants().size(), is(1));
        assertThat(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId()
                .equals(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(1).getDefendants().get(0).getId()), is(false));

        RelatedHearingUpdated relatedHearingUpdated = (RelatedHearingUpdated) eventStream.get(1);
        assertThat(relatedHearingUpdated.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().size(), is(2));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(1).getDefendants().size(), is(1));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().get(0).getId()
                .equals(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(1).getDefendants().get(0).getId()), is(false));
    }

    @Test
    public void shouldRaiseRelatedHearingAndStatusChangeEventWithMultipleCaseAndMultipleDefendantAndOffences() {
        final Optional<JsonObject> relatedHearingRequestedJsonObject =
                Optional.of(getJsonObjectResponseFromJsonResource("json/progression.event.related-hearing-requested-2.json"));

        final RelatedHearingRequested extendHearingRequested = jsonObjectToObjectConverter.convert(relatedHearingRequestedJsonObject.get(), RelatedHearingRequested.class);
        final UUID seedingHearingId = extendHearingRequested.getSeedingHearing().getSeedingHearingId();
        final UpdateRelatedHearingCommand updateRelatedHearingCommand = UpdateRelatedHearingCommand.updateRelatedHearingCommand()
                .withHearingRequest(extendHearingRequested.getHearingRequest())
                .withIsAdjourned(extendHearingRequested.getIsAdjourned())
                .withSeedingHearing(extendHearingRequested.getSeedingHearing())
                .withShadowListedOffences(extendHearingRequested.getShadowListedOffences())
                .build();
        final HearingListingNeeds hearingListingNeeds = updateRelatedHearingCommand.getHearingRequest();

        final Optional<JsonObject> hearingJsonObject =
                Optional.of(getJsonObjectResponseFromJsonResource("json/progression-hearing.json"));
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJsonObject.get(), Hearing.class);

        final HearingListingStatus hearingListingStatus = HearingListingStatus.HEARING_INITIALISED;

        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", hearingListingStatus);

        final List<Object> eventStream = hearingAggregate.updateRelatedHearing(hearingListingNeeds,
                updateRelatedHearingCommand.getIsAdjourned(),
                updateRelatedHearingCommand.getExtendedHearingFrom(),
                updateRelatedHearingCommand.getIsPartiallyAllocated(),
                updateRelatedHearingCommand.getSeedingHearing(),
                updateRelatedHearingCommand.getShadowListedOffences()).collect(toList());

        assertThat(eventStream.size(), is(2));
        ProsecutionCaseDefendantListingStatusChangedV2 caseDefendantListingStatusChanged = (ProsecutionCaseDefendantListingStatusChangedV2) eventStream.get(0);
        assertThat(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().size(), is(2));
        assertThat(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getId()
                .equals(caseDefendantListingStatusChanged.getHearing().getProsecutionCases().get(1).getDefendants().get(0).getId()), is(false));

        RelatedHearingUpdated relatedHearingUpdated = (RelatedHearingUpdated) eventStream.get(1);
        assertThat(relatedHearingUpdated.getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().size(), is(2));
        assertThat(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(0).getDefendants().get(0).getId()
                .equals(relatedHearingUpdated.getHearingRequest().getProsecutionCases().get(1).getDefendants().get(0).getId()), is(false));
    }

    @Test
    public void testCommandUpdateHearingForAllocationFieldWithoutCourtApplicationInPayload() {
        final ZonedDateTime sittingDay = ZonedDateTime.now();

        final Optional<JsonObject> hearingJsonObject =
                Optional.of(getJsonObjectResponseFromJsonResource("json/hearing-without-courtApplication.json"));
        final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJsonObject.get(), Hearing.class);
        setField(hearingAggregate, "hearing", hearing);
        setField(hearingAggregate, "hearingListingStatus", HearingListingStatus.HEARING_INITIALISED);


        final List<Object> events =  hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .build()

        ).collect(toList());

        assertThat(events.size(), is(3));
        assertThat(((HearingUpdatedForAllocationFields) events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingDays().get(0).getSittingDay(), is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker) events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));

        assertThat(((VejHearingPopulatedToProbationCaseworker) events.get(2)).getHearing().getHearingDays().get(0).getSittingDay(), is(sittingDay));
        assertThat(((VejHearingPopulatedToProbationCaseworker) events.get(2)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((VejHearingPopulatedToProbationCaseworker) events.get(2)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((VejHearingPopulatedToProbationCaseworker) events.get(2)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));

    }

    /**
     * Returns the JsonObject for the given json resource @param resourceName
     * @param resourceName The json file resource name
     * @return The Json Object
     */
    private JsonObject getJsonObjectResponseFromJsonResource(String resourceName) {
        String response = null;
        try {
            response = Resources.toString(getResource(resourceName), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }

    @Test
    public void should_boxWorkComplete_whenHearingResulted(){
        final Stream<Object> objectStream = hearingAggregate.boxworkComplete();
        assertThat(objectStream.collect(toList()).size(),is(0));
    }

    @Test
    public void shouldGetSavedListingStatusChanged(){
        final ProsecutionCaseDefendantListingStatusChangedV2 savedListingStatusChanged = hearingAggregate.getSavedListingStatusChanged();
        assertThat(savedListingStatusChanged.getHearingListingStatus(),is(HearingListingStatus.HEARING_RESULTED));
    }

    @Test
    public void shouldReturnEmptyStreamWhenInputIsNull_updateListDefendantRequest(){
        final Stream<Object> objectStream = hearingAggregate.updateListDefendantRequest(null, null);
        assertThat(objectStream.collect(toList()).size(), is(0));
    }

    @Test
    public void shouldReturnEmptyStreamWhenInputIsNull_createListDefendantRequest(){
        final Stream<Object> objectStream = hearingAggregate.createListDefendantRequest(null);
        assertThat(objectStream.collect(toList()).size(), is(0));
    }

    @Test
    public void shouldReturnEmptyStreamWhenUnscheduledHearingListedFromThisHearingIsTrue_listUnscheduledHearing(){
        ReflectionUtil.setField(hearingAggregate, "unscheduledHearingListedFromThisHearing",true);
        final UUID hearingId = randomUUID();
        final Stream<Object> objectStream = hearingAggregate.listUnscheduledHearing(Hearing.hearing().withId(hearingId).build());
        assertThat(objectStream.collect(toList()).size(), is(0));
    }


    @Test
    public void shouldRecordUnscheduledHearing(){
        final UUID hearingId = randomUUID();
        final List<UUID> uuidList = Stream.of(randomUUID(),randomUUID(),randomUUID()).collect(toList());
        final Stream<Object> objectStream = hearingAggregate.recordUnscheduledHearing(hearingId, uuidList);
        assertThat(((UnscheduledHearingRecorded)objectStream.collect(toList()).get(0)).getHearingId(), is(hearingId));
    }

    private HearingDeleted createHearingDeleted(final Hearing hearing) {
        return HearingDeleted.hearingDeleted().withHearingId(hearing.getId()).build();
    }

    private void createHearingInitiated(final Hearing hearing) {
        final HearingInitiateEnriched event = HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build();

        hearingAggregate.apply(event);
    }

    private void createHearingResulted(final ZonedDateTime resultedDate,  final Hearing hearing) {
        final HearingResulted event = HearingResulted.hearingResulted().withHearing(hearing).withSharedTime(resultedDate).build();

        hearingAggregate.apply(event);
    }

    private HearingResulted createHearingResulted(final Hearing hearing) {
        return HearingResulted.hearingResulted().withHearing(hearing).build();
    }

    private Hearing getHearing(final UUID caseId, final UUID defendantId, final ZonedDateTime hearingDate, final boolean youth) {
        return getHearing(caseId, defendantId, hearingDate, youth, true);
    }

    private Hearing getHearing(final UUID caseId, final UUID defendantId, final ZonedDateTime hearingDate, final boolean youth, final boolean resultShared) {
        return Hearing.hearing()
                .withId(randomUUID())
                .withHasSharedResults(resultShared)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withEarliestNextHearingDate(hearingDate)
                .withHearingDays(asList(HearingDay.hearingDay().withSittingDay(hearingDate).build()))
                .withType(HearingType.hearingType().withDescription("First hearing").build())
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(caseId)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withIsYouth(youth)
                                .withOffences(of(Offence.offence()
                                        .withId(randomUUID())
                                        .withListingNumber(1)
                                        .build()))
                                .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(false).build()).build())
                        .build())))
                .build();
    }

    private Map<UUID, Set<LocalDate>> getNoticeSentMap(final UUID defendantId) {
        final Set<LocalDate> dateSet = new HashSet<>();
        dateSet.add(LocalDate.now());
        final Map<UUID, Set<LocalDate>> noticeSentMap = new HashMap<>();
        noticeSentMap.put(defendantId, dateSet);

        return noticeSentMap;
    }

    private Hearing getHearing(final UUID offenceId) {
        return Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withIsYouth(true)
                                .withOffences(of(Offence.offence()
                                        .withId(offenceId)
                                        .withListingNumber(1)
                                        .build()))
                                .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(false).build()).build())
                        .build())))
                .build();
    }

    private HearingDaysWithoutCourtCentreCorrected createHearingDaysWithoutCourtCentreCorrected() {
        return HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(asList(HearingDay.hearingDay()
                        .withCourtCentreId(UUID.randomUUID())
                        .withCourtRoomId(UUID.randomUUID())
                        .withListedDurationMinutes(30)
                        .withListingSequence(1)
                        .withSittingDay(ZonedDateTime.now())
                        .build()))
                .withId(UUID.randomUUID())
                .build();
    }

    @Test
    public void shouldCreateHearingDefendantRequest() {
        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build());

        final List<Object> eventStream = hearingAggregate.createHearingDefendantRequest(listDefendantRequests).collect(toList());

        assertThat(eventStream.size(), is(1));
        final HearingDefendantRequestCreated hearingDefendantRequestCreated = (HearingDefendantRequestCreated) eventStream.get(0);
        assertThat(hearingDefendantRequestCreated.getDefendantRequests().size(), is(1));
    }

    @Test
    public void shouldUpdateDefendantHearingResult() {
        final UUID hearingId  = randomUUID();
        final List<SharedResultLine> sharedResultLines = new ArrayList<>();
        sharedResultLines.add(SharedResultLine.sharedResultLine()
                .build());

        final List<Object> eventStream = hearingAggregate.updateDefendantHearingResult(hearingId, sharedResultLines).collect(toList());

        assertThat(eventStream.size(), is(1));
        final ProsecutionCaseDefendantHearingResultUpdated prosecutionCaseDefendantHearingResultUpdated = (ProsecutionCaseDefendantHearingResultUpdated) eventStream.get(0);
        assertThat(prosecutionCaseDefendantHearingResultUpdated.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldCreateHearingApplicationRequest() {
        final List<SharedResultLine> sharedResultLines = new ArrayList<>();
        sharedResultLines.add(SharedResultLine.sharedResultLine()
                .build());

        final List<CourtApplicationPartyListingNeeds> list = new ArrayList<>();
        list.add(CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                .withCourtApplicationId(randomUUID())
                .build());

        final List<Object> eventStream = hearingAggregate.createHearingApplicationRequest(list).collect(toList());

        assertThat(eventStream.size(), is(1));
        final HearingApplicationRequestCreated hearingApplicationRequestCreated = (HearingApplicationRequestCreated) eventStream.get(0);
        assertThat(hearingApplicationRequestCreated.getApplicationRequests().size(), is(1));
    }

    @Test
    public void shouldCreateSummonsData(){
        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build());

        hearingAggregate.createHearingDefendantRequest(listDefendantRequests).collect(toList());

        final List<SharedResultLine> sharedResultLines = new ArrayList<>();
        sharedResultLines.add(SharedResultLine.sharedResultLine()
                .build());

        final List<CourtApplicationPartyListingNeeds> list = new ArrayList<>();
        list.add(CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                .withCourtApplicationId(randomUUID())
                .build());

        hearingAggregate.createHearingApplicationRequest(list).collect(toList());

        final List<ConfirmedProsecutionCaseId> confirmedProsecutionCaseIds = new ArrayList<>();
        final List<UUID> confirmedApplicationIds = new ArrayList<>();

        final Stream<Object> eventStream = hearingAggregate.createSummonsData(CourtCentre.courtCentre().build(),
                ZonedDateTime.now(),confirmedProsecutionCaseIds, confirmedApplicationIds);

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), Matchers.instanceOf(SummonsDataPrepared.class));
    }

    @Test
    public void shouldEnrichInitiateHearing(){
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();
        final Stream<Object> eventStream = hearingAggregate.enrichInitiateHearing(hearing);
        final List events = eventStream.collect(toList());
        assertThat(events.get(0), Matchers.instanceOf(HearingInitiateEnriched.class));
    }

    @Test
    public void shouldUpdateListDefendantRequest(){
        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(ListDefendantRequest.listDefendantRequest()
                .withDefendantId(randomUUID())
                .build());
        ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().build();
        final Stream<Object> eventStream = hearingAggregate.updateListDefendantRequest(listDefendantRequests,confirmedHearing);
        final List events = eventStream.collect(toList());
        assertThat(events.get(0), Matchers.instanceOf(ExtendHearingDefendantRequestUpdated.class));
    }

    @Test
    public void shouldCreateHearingForApplication(){
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();
        final List<ListHearingRequest> listHearingRequests = new ArrayList<>();
        listHearingRequests.add(ListHearingRequest.listHearingRequest()
                .withHearingType(HearingType.hearingType()
                        .withId(randomUUID())
                        .build())
                .build());

        final Stream<Object> eventStream = hearingAggregate.createHearingForApplication(hearing,HearingListingStatus.HEARING_INITIALISED, listHearingRequests);
        final List events = eventStream.collect(toList());
        assertThat(events.get(0), Matchers.instanceOf(HearingForApplicationCreatedV2.class));
    }

    @Test
    public void shouldAssignDefendantRequestFromCurrentHearingToExtendHearing() {
        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build());

        hearingAggregate.createHearingDefendantRequest(listDefendantRequests).collect(toList());
        final Stream<Object> eventStream =  hearingAggregate.assignDefendantRequestFromCurrentHearingToExtendHearing(randomUUID(),randomUUID());

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(DefendantRequestFromCurrentHearingToExtendHearingCreated.class));
    }

    @Test
    public void shouldAssignDefendantRequestToExtendHearing() {
        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build());
        final Stream<Object> eventStream =  hearingAggregate.assignDefendantRequestToExtendHearing(randomUUID(),listDefendantRequests);

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(DefendantRequestToExtendHearingCreated.class));
    }

    @Test
    public void shouldNotAssignDefendantRequestFromCurrentHearingToExtendHearing() {
        final Stream<Object> eventStream =  hearingAggregate.assignDefendantRequestFromCurrentHearingToExtendHearing(randomUUID(),randomUUID());

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldListUnscheduledHearing() {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();
        final Stream<Object> eventStream =  hearingAggregate.listUnscheduledHearing(hearing);

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(UnscheduledHearingListingRequested.class));
    }


    @Test
    public void shouldProcessHearingUpdated() {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing().build();
        final Stream<Object> eventStream =  hearingAggregate.processHearingUpdated(confirmedHearing, hearing);

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(HearingUpdatedProcessed.class));
    }

    @Test
    public void shouldProcessHearingExtended() {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final HearingListingNeeds hearingRequest = HearingListingNeeds.hearingListingNeeds().build();
        final List<UUID> shadowListedOffences = new ArrayList<>();
        shadowListedOffences.add(randomUUID());
        final Stream<Object> eventStream =  hearingAggregate.processHearingExtended(hearingRequest, shadowListedOffences);

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(HearingExtendedProcessed.class));
    }

    @Test
    public void shouldCreateListDefendantRequest() {
        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build());

        hearingAggregate.createHearingDefendantRequest(listDefendantRequests).collect(toList());
        final Stream<Object> eventStream =  hearingAggregate.createListDefendantRequest(ConfirmedHearing.confirmedHearing().build());

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(ExtendHearingDefendantRequestCreated.class));
    }

    @Test
    public void shouldNotRaiseRecordUpdateMatchedDefendantDetailRequest() {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .build();
        final Stream<Object> eventStream =  hearingAggregate.recordUpdateMatchedDefendantDetailRequest(defendantUpdate);

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldRaiseRecordUpdateMatchedDefendantDetailRequestForApplication() {
        final Hearing hearing = CoreTestTemplates.hearingForApplication(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .build();
        final Stream<Object> eventStream = hearingAggregate.recordUpdateMatchedDefendantDetailRequest(defendantUpdate);

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.class));

    }

    @Test
    public void shouldRecordUpdateMatchedDefendantDetailRequest() {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate()
                .withId(hearing.getProsecutionCases().get(0).getDefendants().get(0).getId())
                .withProsecutionCaseId(randomUUID())
                .build();
        final Stream<Object> eventStream =  hearingAggregate.recordUpdateMatchedDefendantDetailRequest(defendantUpdate);

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(ProsecutionCaseUpdateDefendantsWithMatchedRequestedV2.class));
    }

    @Test
    public void shouldEnrichInitiateHearingWhenHearingDefendantRequestIsPresent() {
        final List<ListDefendantRequest> listDefendantRequests = new ArrayList<>();
        listDefendantRequests.add(ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build());

        hearingAggregate.createHearingDefendantRequest(listDefendantRequests).collect(toList());

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();
        final Stream<Object> eventStream = hearingAggregate.enrichInitiateHearing(hearing);
        final List events = eventStream.collect(toList());
        assertThat(events.get(0), Matchers.instanceOf(HearingInitiateEnriched.class));
    }

    @Test
    public void shouldUpdateDefendant(){
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();
        hearingAggregate.enrichInitiateHearing(hearing);

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().build();
        final Stream<Object> eventStream = hearingAggregate.updateDefendant(randomUUID(),defendantUpdate);
        final List events = eventStream.collect(toList());
        assertThat(events.get(0), Matchers.instanceOf(HearingDefendantUpdated.class));
    }

    @Test
    public void shouldNotUpdateDefendantWhenHearingIsResulted(){
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();
        hearingAggregate.enrichInitiateHearing(hearing);

        hearingAggregate.apply(createHearingResulted(hearing));

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().build();
        final List events = hearingAggregate.updateDefendant(randomUUID(),defendantUpdate).collect(toList());
        assertThat(events.isEmpty(), is(true));

    }

    @Test
    public void shouldRaiseHearingDefendantUpdatedWhenProsecutionCasesIsNullInTheHearing(){
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearingId).build();
        hearingAggregate.enrichInitiateHearing(hearing);

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().build();
        final Stream<Object> eventStream = hearingAggregate.updateDefendant(hearingId,defendantUpdate);
        final List events = eventStream.collect(toList());
        assertThat(events.get(0), Matchers.instanceOf(HearingDefendantUpdated.class));
    }

    @Test
    public void shouldProsecutionCaseDefendantListingStatusChangedV2EventEmittedWithHearingListingStatusWhenHearingResultedForApplication_NoDuplicateJudicialResults() {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withCourtOrderOffences(Arrays.asList(CourtOrderOffence.courtOrderOffence()
                                                .withOffence(Offence.offence()
                                                        .withId(randomUUID())
                                                        .withJudicialResults(new ArrayList(Arrays.asList(buildRelatedNextHearingJudicialResultWithAmendmentAs(buildNextHearing(hearingId), false))))
                                                        .withVerdict(Verdict.verdict().withOffenceId(randomUUID()).build())
                                                        .build())
                                                .build(),
                                        CourtOrderOffence.courtOrderOffence()
                                                .withOffence(Offence.offence()
                                                        .withId(randomUUID())
                                                        .withJudicialResults(new ArrayList(Arrays.asList(buildRelatedNextHearingJudicialResultWithAmendmentAs(buildNextHearing(hearingId), false))))
                                                        .withListingNumber(11)
                                                        .build())
                                                .build()))
                                .build())
                        .withJudicialResults(new ArrayList(asList(buildRelatedNextHearingJudicialResultWithAmendmentAs(buildNextHearing(hearingId),false))))
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId1)
                                                .withVerdict(Verdict.verdict().withOffenceId(offenceId1).build())
                                                .build(),
                                        Offence.offence()
                                                .withId(offenceId2)
                                                .withListingNumber(11)
                                                .build())))
                                .build()))
                        .build()))
                .build();

        hearingAggregate.apply(HearingForApplicationCreatedV2.hearingForApplicationCreatedV2().withHearing(hearing).withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING).build());

        final List<Object> events = hearingAggregate.processHearingResults(hearing, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChangedV2 = (ProsecutionCaseDefendantListingStatusChangedV2) events.stream().filter(e -> e instanceof ProsecutionCaseDefendantListingStatusChangedV2).findFirst().get();
        assertThat(prosecutionCaseDefendantListingStatusChangedV2.getHearing().getCourtApplications().get(0).getJudicialResults().size(), is(1));
    }

    private JudicialResult buildRelatedNextHearingJudicialResultWithAmendmentAs(final NextHearing nextHearing, final boolean isNewAmendment) {
        return JudicialResult.judicialResult()
                .withJudicialResultId(randomUUID())
                .withIsUnscheduled(true)
                .withNextHearing(nextHearing)
                .withIsNewAmendment(isNewAmendment)
                .withOrderedDate(LocalDate.now())
                .build();
    }

    private static NextHearing buildNextHearing(final UUID existingHearingId) {
        return NextHearing.nextHearing()
                .withExistingHearingId(existingHearingId)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCode(COMMITTING_COURT_CODE)
                        .withName(COMMITTING_COURT_NAME)
                        .build())
                .build();
    }

    @Test
    public void shouldApplicationIssueDateBeFirstCreationDateWhenAmendAndReshared() throws IOException {
        final UUID hearingId = randomUUID();
        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.prosecution-cases-resulted-v2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();
        final Hearing hearing = prosecutionCasesResultedV2.getHearing();
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);

        final UUID resultId = UUID.fromString("d0695ca5-fdac-429a-888d-a909adce4cd9");
        final LocalDate issueDate = LocalDate.now().minusDays(2);
        hearingAggregate.apply(Stream.of(InitiateApplicationForCaseRequested.initiateApplicationForCaseRequested()
                        .withApplicationId(randomUUID())
                        .withDefendant(defendant)
                        .withHearing(hearing)
                        .withIsAmended(false)
                        .withIssueDate(issueDate)
                        .withNextHearing(NextHearing.nextHearing().build())
                        .withProsecutionCase(prosecutionCase)
                        .withResultId(resultId)
                .build()));


         final List<Object> events = hearingAggregate.processHearingResults(hearing, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(5));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(events.get(4).getClass(), is(CoreMatchers.equalTo(DeleteApplicationForCaseRequested.class)));
        InitiateApplicationForCaseRequested initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) events.get(3);

        assertThat(initiateApplicationForCaseRequested.getIssueDate(), is(issueDate));
    }

    @Test
    public void shouldRaiseDeleteApplicationForCaseRequestedEventWhenHearingResultedWithReshared() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.prosecution-cases-resulted-v2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing = prosecutionCasesResultedV2.getHearing();
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final Offence offence = prosecutionCase.getDefendants().get(0).getOffences().get(0);
        final List<Object> events = hearingAggregate.processHearingResults(hearing, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(4));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        InitiateApplicationForCaseRequested initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) events.get(3);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        assertThat(initiateApplicationForCaseRequested.getIssueDate(), is(LocalDate.now()));
        UUID applicationId = initiateApplicationForCaseRequested.getApplicationId();

        hearingAggregate.apply(events);

        final ProsecutionCasesResultedV2 secondResult = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(prosecutionCasesResultedV2)
                .withHearing(Hearing.hearing().withValuesFrom(hearing)
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withValuesFrom(prosecutionCase)
                                .withDefendants(asList(Defendant.defendant()
                                        .withValuesFrom(defendant)
                                        .withOffences(asList(Offence.offence()
                                                .withValuesFrom(offence)
                                                .withJudicialResults(asList(offence.getJudicialResults().get(0),
                                                        JudicialResult.judicialResult()
                                                                .withValuesFrom(offence.getJudicialResults().get(1))
                                                                .withAmendmentReason("test")
                                                                .build()))
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(secondResult.getHearing(), ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(5));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) secondEvents.get(3);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(true));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), is(applicationId));
        assertThat(initiateApplicationForCaseRequested.getIssueDate(), is(LocalDate.now()));
        DeleteApplicationForCaseRequested deleteApplicationForCaseRequested = (DeleteApplicationForCaseRequested) secondEvents.get(4);
        assertThat(deleteApplicationForCaseRequested.getApplicationId(), is(applicationId));
        assertThat(deleteApplicationForCaseRequested.getSeedingHearingId(), is(hearingId));
    }


    @Test
    public void shouldRaiseDeleteApplicationForCaseRequestedEventWhenHearingResultMultipleDefendantAndMultipleOffencesWithReshared() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.prosecution-cases-resulted-2-defendant-2-offences-v2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing = prosecutionCasesResultedV2.getHearing();
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Defendant defendant1 = prosecutionCase.getDefendants().get(0);
        final Defendant defendant2 = prosecutionCase.getDefendants().get(1);
        final List<Object> events = hearingAggregate.processHearingResults(hearing, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(events.size(), is(7));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        InitiateApplicationForCaseRequested initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) events.get(3);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) events.get(4);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) events.get(5);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        UUID application3Id = initiateApplicationForCaseRequested.getApplicationId();
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) events.get(6);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        UUID application4Id = initiateApplicationForCaseRequested.getApplicationId();

        hearingAggregate.apply(events);

        final ProsecutionCasesResultedV2 secondResult = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(prosecutionCasesResultedV2)
                .withHearing(Hearing.hearing().withValuesFrom(hearing)
                        .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                                .withValuesFrom(hearing.getProsecutionCases().get(0))
                                .withDefendants(asList(Defendant.defendant()
                                                .withValuesFrom(defendant1)
                                                .withOffences(asList(Offence.offence()
                                                                .withValuesFrom(defendant1.getOffences().get(0))
                                                                .withJudicialResults(asList(JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant1.getOffences().get(0).getJudicialResults().get(0))
                                                                                .withIsNewAmendment(false)
                                                                                .build(),
                                                                        JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant1.getOffences().get(0).getJudicialResults().get(1))
                                                                                .withIsNewAmendment(false)
                                                                                .build()))
                                                                .build(),
                                                        Offence.offence()
                                                                .withValuesFrom(defendant1.getOffences().get(1))
                                                                .withJudicialResults(asList(JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant1.getOffences().get(1).getJudicialResults().get(0))
                                                                                .withIsNewAmendment(false)
                                                                                .build(),
                                                                        JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant1.getOffences().get(1).getJudicialResults().get(1))
                                                                                .withIsNewAmendment(false)
                                                                                .build()))
                                                                .build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withValuesFrom(defendant1)
                                                .withOffences(asList(Offence.offence()
                                                                .withValuesFrom(defendant2.getOffences().get(0))
                                                                .withJudicialResults(asList(JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant2.getOffences().get(0).getJudicialResults().get(0))
                                                                                .withIsNewAmendment(true)
                                                                                .build(),
                                                                        JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant2.getOffences().get(0).getJudicialResults().get(1))
                                                                                .withIsNewAmendment(true)
                                                                                .build()))
                                                                .build(),
                                                        Offence.offence()
                                                                .withValuesFrom(defendant2.getOffences().get(1))
                                                                .withJudicialResults(asList(JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant2.getOffences().get(1).getJudicialResults().get(0))
                                                                                .withIsNewAmendment(true)
                                                                                .build(),
                                                                        JudicialResult.judicialResult()
                                                                                .withValuesFrom(defendant2.getOffences().get(1).getJudicialResults().get(1))
                                                                                .withIsNewAmendment(true)
                                                                                .build()))
                                                                .build()))
                                                .build()))

                                .build()))
                        .build())
                .build();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(secondResult.getHearing(), ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(7));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) secondEvents.get(3);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(true));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), is(application3Id));
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) secondEvents.get(4);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(true));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), is(application4Id));
        DeleteApplicationForCaseRequested deleteApplicationForCaseRequested = (DeleteApplicationForCaseRequested) secondEvents.get(5);
        assertThat(deleteApplicationForCaseRequested.getApplicationId(), is(application3Id));
        assertThat(deleteApplicationForCaseRequested.getSeedingHearingId(), is(hearingId));
        deleteApplicationForCaseRequested = (DeleteApplicationForCaseRequested) secondEvents.get(6);
        assertThat(deleteApplicationForCaseRequested.getApplicationId(), is(application4Id));
        assertThat(deleteApplicationForCaseRequested.getSeedingHearingId(), is(hearingId));
    }

    @Test
    public void shouldRaiseDeleteApplicationForCaseRequestedEventWhenHearingResultMultiDefAndMultiOffencesWithResharedWithAutoAndNonAutoApplications() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV1 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-multidefs-multioffs-auto-non-auto-apps-1.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing1 = prosecutionCasesResultedV1.getHearing();
        final ProsecutionCase prosecutionCase1 = hearing1.getProsecutionCases().get(0);
        final List<Object> eventsV1 = hearingAggregate.processHearingResults(hearing1, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(eventsV1.size(), is(7));
        assertThat(eventsV1.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(eventsV1.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(eventsV1.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        InitiateApplicationForCaseRequested initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) eventsV1.get(3);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) eventsV1.get(4);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) eventsV1.get(5);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        UUID application3Id = initiateApplicationForCaseRequested.getApplicationId();
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) eventsV1.get(6);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(false));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), nullValue());
        UUID application4Id = initiateApplicationForCaseRequested.getApplicationId();

        hearingAggregate.apply(eventsV1);

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-multidefs-multioffs-auto-non-auto-apps-2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing2 = prosecutionCasesResultedV2.getHearing();
        final ProsecutionCase prosecutionCase2 = hearing2.getProsecutionCases().get(0);

        final List<Object> secondEvents = hearingAggregate.processHearingResults(hearing2, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(6));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        initiateApplicationForCaseRequested = (InitiateApplicationForCaseRequested) secondEvents.get(3);
        assertThat(initiateApplicationForCaseRequested.getIsAmended(), is(true));
        assertThat(initiateApplicationForCaseRequested.getOldApplicationId(), is(application3Id));
        DeleteApplicationForCaseRequested deleteApplicationForCaseRequested = (DeleteApplicationForCaseRequested) secondEvents.get(4);
        assertThat(deleteApplicationForCaseRequested.getApplicationId(), is(application3Id));
        assertThat(deleteApplicationForCaseRequested.getSeedingHearingId(), is(hearingId));
        deleteApplicationForCaseRequested = (DeleteApplicationForCaseRequested) secondEvents.get(5);
        assertThat(deleteApplicationForCaseRequested.getApplicationId(), is(application4Id));
        assertThat(deleteApplicationForCaseRequested.getSeedingHearingId(), is(hearingId));
    }

    @Test
    public void shouldDeleteApplicationWhenCaseIsAmendedWithNonAutoApplicationResult() throws IOException{
        final UUID hearingId = randomUUID();
        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-v1.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();
        final Hearing hearing = prosecutionCasesResultedV2.getHearing();
        final ProsecutionCase prosecutionCase = hearing.getProsecutionCases().get(0);
        final Defendant defendant = prosecutionCase.getDefendants().get(0);
        final UUID resultId = UUID.fromString("d0695ca5-fdac-429a-888d-a909adce4cd9");
        final LocalDate issueDate = LocalDate.now().minusDays(2);
        hearingAggregate.apply(Stream.of(InitiateApplicationForCaseRequested.initiateApplicationForCaseRequested()
                .withApplicationId(randomUUID())
                .withDefendant(defendant)
                .withHearing(hearing)
                .withIsAmended(true)
                .withIssueDate(issueDate)
                .withNextHearing(NextHearing.nextHearing().build())
                .withProsecutionCase(prosecutionCase)
                .withResultId(resultId)
                .withOldApplicationId(randomUUID())
                .build()));
        final List<Object> events = hearingAggregate.processHearingResults(prosecutionCasesResultedV2.getHearing(),ZonedDateTime.now(),null, LocalDate.now()).collect(toList());
        assertThat(events.size(), is(4));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(events.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(events.get(3).getClass(), is(CoreMatchers.equalTo(DeleteApplicationForCaseRequested.class)));
    }

    @Test
    public void shouldNotRaiseDeleteNextHearingEventInReshareWhenAllNextHearingResultsAreNotDeletedFromResults() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV1 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-removed-not-from-all-results_1.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing1 = prosecutionCasesResultedV1.getHearing();
        final List<Object> eventsV1 = hearingAggregate.processHearingResults(hearing1, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(eventsV1.size(), is(4));
        assertThat(eventsV1.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(eventsV1.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(eventsV1.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        final NextHearingsRequested nextHearingsRequested = (NextHearingsRequested) eventsV1.get(3);
        assertThat(nextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));

        hearingAggregate.apply(eventsV1);

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-removed-not-from-all-results_2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing2 = prosecutionCasesResultedV2.getHearing();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(hearing2, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(3));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
    }

    @Test
    public void shouldRaiseDeleteNextHearingEventInReshareWhenAllNextHearingResultsAreDeletedFromResults() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV1 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-removed-not-from-all-results_1.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing1 = prosecutionCasesResultedV1.getHearing();
        final List<Object> eventsV1 = hearingAggregate.processHearingResults(hearing1, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(eventsV1.size(), is(4));
        assertThat(eventsV1.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(eventsV1.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(eventsV1.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        final NextHearingsRequested nextHearingsRequested = (NextHearingsRequested) eventsV1.get(3);
        assertThat(nextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));

        hearingAggregate.apply(eventsV1);

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-removed-not-from-all-results_2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing2 = prosecutionCasesResultedV2.getHearing();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(hearing2, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(3));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
    }


    @Test
    public void shouldNotDeleteNextHearingWhenCaseIsAmendedButNotAnyAmendmentForJudicialResultWhichCreatesNextHearing() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV1 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-new-amendment-for-judicial-result-which-dont-creates-next-hearing-1.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing1 = prosecutionCasesResultedV1.getHearing();
        final List<Object> eventsV1 = hearingAggregate.processHearingResults(hearing1, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(eventsV1.size(), is(4));
        assertThat(eventsV1.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(eventsV1.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(eventsV1.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        final NextHearingsRequested nextHearingsRequested = (NextHearingsRequested) eventsV1.get(3);
        assertThat(nextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));

        hearingAggregate.apply(eventsV1);

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-new-amendment-for-judicial-result-which-dont-creates-next-hearing-2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing2 = prosecutionCasesResultedV2.getHearing();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(hearing2, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(3));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
    }

    @Test
    public void shouldNotDeleteNextHearingWhenCaseIsAmendedButNotAnyAmendmentForJudicialResultWhichCreatesNextHearingForCourtApplication() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV1 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-new-amendment-for-judicial-result-which-dont-creates-next-hearing-1_for_application.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing1 = prosecutionCasesResultedV1.getHearing();
        final List<Object> eventsV1 = hearingAggregate.processHearingResults(hearing1, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(eventsV1.size(), is(4));
        assertThat(eventsV1.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(eventsV1.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        final NextHearingsRequested nextHearingsRequested = (NextHearingsRequested) eventsV1.get(2);
        assertThat(nextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));
        assertThat(eventsV1.get(3).getClass(), is(CoreMatchers.equalTo(ApplicationsResulted.class)));

        hearingAggregate.apply(eventsV1);

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-next-hearing-new-amendment-for-judicial-result-which-dont-creates-next-hearing-2_for_application.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing2 = prosecutionCasesResultedV2.getHearing();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(hearing2, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(3));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ApplicationsResulted.class)));
    }



    @Test
    public void shouldRaiseNextHearingDeletedEventWhenNextHearingCreatedFirstResultAndAmendedWithAutoApplicationInSecondResult() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV1 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-with-next-hearing-after-amend-auto-application-1.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing1 = prosecutionCasesResultedV1.getHearing();
        final List<Object> eventsV1 = hearingAggregate.processHearingResults(hearing1, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(eventsV1.size(), is(4));
        assertThat(eventsV1.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(eventsV1.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(eventsV1.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        final NextHearingsRequested nextHearingsRequested = (NextHearingsRequested) eventsV1.get(3);
        assertThat(nextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));
        final UUID nextHearingId = nextHearingsRequested.getHearing().getId();

        hearingAggregate.apply(eventsV1);

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-with-next-hearing-after-amend-auto-application-2.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing2 = prosecutionCasesResultedV2.getHearing();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(hearing2, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(6));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(secondEvents.get(3).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
        assertThat(secondEvents.get(4).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
        final DeleteNextHearingsRequested deleteNextHearingsRequested = (DeleteNextHearingsRequested) secondEvents.get(5);
        assertThat(deleteNextHearingsRequested.getHearingId(), is(nextHearingId));
        assertThat(deleteNextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));
    }

    @Test
    public void shouldRaiseNextHearingDeletedEventWhenNextHearingCreatedFirstResultAndAmendedWithAutoApplicationInSecondResultAndNotAllOffencesAreAmended() throws IOException {
        final UUID hearingId = randomUUID();

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV1 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-with-next-hearing-after-amend-auto-application-3.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing1 = prosecutionCasesResultedV1.getHearing();
        final List<Object> eventsV1 = hearingAggregate.processHearingResults(hearing1, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());

        assertThat(eventsV1.size(), is(4));
        assertThat(eventsV1.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(eventsV1.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(eventsV1.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        final NextHearingsRequested nextHearingsRequested = (NextHearingsRequested) eventsV1.get(3);
        assertThat(nextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));
        final UUID nextHearingId = nextHearingsRequested.getHearing().getId();

        hearingAggregate.apply(eventsV1);

        final ProsecutionCasesResultedV2 prosecutionCasesResultedV2 = ProsecutionCasesResultedV2.prosecutionCasesResultedV2()
                .withValuesFrom(convertFromFile("json/progression.event.hearing-resulted-with-next-hearing-after-amend-auto-application-4.json", ProsecutionCasesResultedV2.class, hearingId.toString())).build();

        final Hearing hearing2 = prosecutionCasesResultedV2.getHearing();

        final List<Object> secondEvents = hearingAggregate.processHearingResults(hearing2, ZonedDateTime.now(), null, LocalDate.now()).collect(toList());
        assertThat(secondEvents.size(), is(6));

        assertThat(secondEvents.get(0).getClass(), is(CoreMatchers.equalTo(ProsecutionCaseDefendantListingStatusChangedV2.class)));
        assertThat(secondEvents.get(1).getClass(), is(CoreMatchers.equalTo(HearingResulted.class)));
        assertThat(secondEvents.get(2).getClass(), is(CoreMatchers.equalTo(ProsecutionCasesResultedV2.class)));
        assertThat(secondEvents.get(3).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
        assertThat(secondEvents.get(4).getClass(), is(CoreMatchers.equalTo(InitiateApplicationForCaseRequested.class)));
        final DeleteNextHearingsRequested deleteNextHearingsRequested = (DeleteNextHearingsRequested) secondEvents.get(5);
        assertThat(deleteNextHearingsRequested.getHearingId(), is(nextHearingId));
        assertThat(deleteNextHearingsRequested.getSeedingHearing().getSeedingHearingId(), is(hearingId));
    }


    @Test
    public void shouldRaiseCourtApplicationHearingDeletedWhenIsNotResulted(){
        final UUID applicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));
        final List<Object> events = hearingAggregate.deleteCourtApplicationHearing(hearing.getId(), applicationId, seedingHearingId).collect(toList());

        assertThat(events.size(), is(1));
        final CourtApplicationHearingDeleted courtApplicationHearingDeleted = (CourtApplicationHearingDeleted) events.get(0);

        assertThat(courtApplicationHearingDeleted.getHearingId(), is(hearing.getId()));
        assertThat(courtApplicationHearingDeleted.getApplicationId(), is(applicationId));
        assertThat(courtApplicationHearingDeleted.getSeedingHearingId(), is(seedingHearingId));

    }

    @Test
    public void shouldRaiseDeleteCourtApplicationHearingIgnoredWhenIsResulted(){
        final UUID applicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                        .setJurisdictionType(JurisdictionType.CROWN)
                        .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                        .setConvicted(false)
                        .setCourtApplication(asList(CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withJudicialResults(asList(JudicialResult.judicialResult().withJudicialResultId(randomUUID()).build()))
                                .build())))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));
        final List<Object> events = hearingAggregate.deleteCourtApplicationHearing(hearing.getId(), applicationId, seedingHearingId).collect(toList());

        assertThat(events.size(), is(1));
        final DeleteCourtApplicationHearingIgnored courtApplicationHearingIgnored = (DeleteCourtApplicationHearingIgnored) events.get(0);

        assertThat(courtApplicationHearingIgnored.getHearingId(), is(hearing.getId()));
        assertThat(courtApplicationHearingIgnored.getApplicationId(), is(applicationId));
        assertThat(courtApplicationHearingIgnored.getSeedingHearingId(), is(seedingHearingId));

    }

    public <T> T convertFromFile(final String url, final Class<T> clazz, String hearingId) throws IOException {
        final String content = readFileToString(new File(this.getClass().getClassLoader().getResource(url).getFile())).replace("HEARING_ID", hearingId);
        return OBJECT_MAPPER.readValue(content, clazz);
    }

}