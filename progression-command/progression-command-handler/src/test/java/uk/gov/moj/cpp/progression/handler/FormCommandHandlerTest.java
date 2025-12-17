package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CreateForm.createForm;
import static uk.gov.justice.core.courts.FinaliseForm.finaliseForm;
import static uk.gov.justice.core.courts.FormCreated.formCreated;
import static uk.gov.justice.core.courts.FormDefendants.formDefendants;
import static uk.gov.justice.core.courts.FormDefendantsUpdated.formDefendantsUpdated;
import static uk.gov.justice.core.courts.FormFinalised.formFinalised;
import static uk.gov.justice.core.courts.FormType.BCM;
import static uk.gov.justice.core.courts.FormUpdated.formUpdated;
import static uk.gov.justice.core.courts.RequestEditForm.requestEditForm;
import static uk.gov.justice.core.courts.UpdateFormDefendants.updateFormDefendants;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CreateForm;
import uk.gov.justice.core.courts.EditFormRequested;
import uk.gov.justice.core.courts.FinaliseForm;
import uk.gov.justice.core.courts.FormCreated;
import uk.gov.justice.core.courts.FormDefendantsUpdated;
import uk.gov.justice.core.courts.FormFinalised;
import uk.gov.justice.core.courts.FormUpdated;
import uk.gov.justice.core.courts.RequestEditForm;
import uk.gov.justice.core.courts.UpdateForm;
import uk.gov.justice.core.courts.UpdateFormDefendants;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.FormLockStatus;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FormCommandHandlerTest {

    private static final int FORM_LOCK_DURATION_TIME = 1;
    private static final String BCM_FORM_LOCK_DURATION_IN_MINUTES = "1";
    private static final String PTPH_FORM_LOCK_DURATION_IN_MINUTES = "2";
    private static final UUID COURT_FORM_ID = randomUUID();
    private static final UUID FORM_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID USER_ID_FOR_ERICA = randomUUID();
    private static final UUID USER_ID_FOR_BILL = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(FormCreated.class, FormUpdated.class, FormFinalised.class, FormDefendantsUpdated.class, EditFormRequested.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;
    @Mock
    private HearingAggregate hearingAggregate;

    @InjectMocks
    @Spy
    private FormCommandHandler formCommandHandler;

    @BeforeEach
    public void setup() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        setField(this.formCommandHandler, "bcmFormLockDurationInMinutes", BCM_FORM_LOCK_DURATION_IN_MINUTES);
        setField(this.formCommandHandler, "ptphFormLockDurationInMinutes", PTPH_FORM_LOCK_DURATION_IN_MINUTES);
    }

    @Test
    public void shouldHandleCreateForm() throws EventStreamException {

        final CreateForm createForm = createForm()
                .withCourtFormId(randomUUID())
                .withCaseId(randomUUID())
                .withFormId(randomUUID())
                .withFormData("{}")
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("progression.command.create-form")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<CreateForm> envelope = envelopeFrom(metadata, createForm);

        when(caseAggregate.createForm(any(), any(), any(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(Stream.of(formCreated()
                        .withCaseId(createForm.getCaseId())
                        .withFormId(createForm.getFormId())
                        .withCourtFormId(createForm.getCourtFormId())
                        .build()));

        formCommandHandler.handleCreateForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.form-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(createForm.getCaseId().toString())),
                                withJsonPath("$.formId", is(createForm.getFormId().toString())),
                                withJsonPath("$.courtFormId", is(createForm.getCourtFormId().toString())))
                        ))
        ));

    }

    @Test
    public void shouldHandleUpdateFormDefendants() throws EventStreamException {

        final UpdateFormDefendants createForm = updateFormDefendants()
                .withCourtFormId(randomUUID())
                .withCaseId(randomUUID())
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("progression.command.update-form-defendants")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        final Envelope<UpdateFormDefendants> envelope = envelopeFrom(metadata, createForm);

        when(caseAggregate.updateFormDefendants(any(), any(), anyList(), any(), any()))
                .thenReturn(Stream.of(formDefendantsUpdated()
                        .withCaseId(createForm.getCaseId())
                        .withCourtFormId(createForm.getCourtFormId())
                        .build()));

        formCommandHandler.updateFormDefendants(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.form-defendants-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(createForm.getCaseId().toString())),
                                withJsonPath("$.courtFormId", is(createForm.getCourtFormId().toString())))
                        ))
        ));

    }

    @Test
    public void shouldHandleUpdateForm() throws EventStreamException {

        final UpdateForm updateForm = UpdateForm.updateForm()
                .withCourtFormId(randomUUID())
                .withFormData("{}")
                .withCaseId(randomUUID())
                .build();

        final UUID userId = randomUUID();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-form")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        final Envelope<UpdateForm> envelope = envelopeFrom(metadata, updateForm);

        when(caseAggregate.updateForm(any(), anyString(), any(), any()))
                .thenReturn(Stream.of(formUpdated()
                        .withCaseId(updateForm.getCaseId())
                        .withCourtFormId(updateForm.getCourtFormId())
                        .withFormData(updateForm.getFormData())
                        .withUserId(userId)
                        .build()));

        formCommandHandler.handleUpdateForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.form-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(updateForm.getCaseId().toString())),
                                withJsonPath("$.formData", is(updateForm.getFormData())),
                                withJsonPath("$.courtFormId", is(updateForm.getCourtFormId().toString())),
                                withJsonPath("$.userId", is(userId.toString())))
                        ))
        ));
    }


    @Test
    public void shouldHandleFinaliseForm() throws EventStreamException {
        final ZonedDateTime hearingDateTime = ZonedDateTime.parse("2024-05-28T22:23:12.414Z");
        final String latestHearingId = randomUUID().toString();

        final FinaliseForm finaliseForm = finaliseForm()
                .withCourtFormId(randomUUID())
                .withCaseId(randomUUID())
                .withFinalisedFormData(asList("{}", "{}", "{}"))
                .withHearingDateTime(hearingDateTime)
                .build();

        final UUID userId = randomUUID();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.finalise-form")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        final Envelope<FinaliseForm> envelope = envelopeFrom(metadata, finaliseForm);
        when(caseAggregate.getLatestHearingId()).thenReturn(UUID.fromString(latestHearingId));
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        when(caseAggregate.finaliseForm(any(), any(), any(), anyList(), any()))
                .thenReturn(Stream.of(formFinalised()
                        .withCaseId(finaliseForm.getCaseId())
                        .withCourtFormId(finaliseForm.getCourtFormId())
                        .withFinalisedFormData(asList("{}", "{}", "{}"))
                        .withUserId(userId)
                        .withHearingDateTime(hearingDateTime)
                        .build()));

        formCommandHandler.handleFinaliseForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.form-finalised"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(finaliseForm.getCaseId().toString())),
                                withJsonPath("$.courtFormId", is(finaliseForm.getCourtFormId().toString())),
                                withJsonPath("$.finalisedFormData", is(asList("{}", "{}", "{}"))),
                                withJsonPath("$.userId", is(userId.toString())),
                                withJsonPath("$.hearingDateTime", is(hearingDateTime.toString()))
                                )
                        ))
        ));
    }


    @Test
    public void shouldEditForm_EditingFirstTime_NoLockPresent() throws EventStreamException {
        final ZonedDateTime requestEditTime = now();

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.createForm(COURT_FORM_ID, CASE_ID, FORM_ID, ImmutableList.of(DEFENDANT_ID), "formData", USER_ID_FOR_ERICA, BCM, randomUUID(), null);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        final Envelope<RequestEditForm> envelope = createEnvelope(COURT_FORM_ID, CASE_ID, USER_ID_FOR_ERICA);

        formCommandHandler.handleRequestEditForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.edit-form-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.courtFormId", is(COURT_FORM_ID.toString())),
                                withJsonPath("$.lockStatus.isLocked", is(false)),
                                withJsonPath("$.lockStatus.lockRequestedBy", is(USER_ID_FOR_ERICA.toString())),
                                withJsonPath("$.lockStatus.expiryTime", notNullValue())
                        ))
                )));

        final FormLockStatus lockStatus = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus();
        final ZonedDateTime actualExpiryTime = lockStatus.getLockExpiryTime();
        assertThat(actualExpiryTime, greaterThan(requestEditTime));
        assertThat(lockStatus.getLockedBy(), is(USER_ID_FOR_ERICA));
    }

    @Test
    public void shouldEditForm_ForSecondTime_ByAnotherUser_WhenFormIsLocked() throws EventStreamException {
        final ZonedDateTime requestEditTime = new UtcClock().now();

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.createForm(COURT_FORM_ID, CASE_ID, FORM_ID, ImmutableList.of(DEFENDANT_ID), "blah", USER_ID_FOR_BILL, BCM, randomUUID(), null);

        caseAggregate.requestEditForm(CASE_ID, COURT_FORM_ID, USER_ID_FOR_BILL, ImmutableMap.of(BCM, FORM_LOCK_DURATION_TIME), requestEditTime, false, 0);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        final Envelope<RequestEditForm> envelope = createEnvelope(COURT_FORM_ID, CASE_ID, USER_ID_FOR_ERICA);

        formCommandHandler.handleRequestEditForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.edit-form-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.courtFormId", is(COURT_FORM_ID.toString())),
                                withJsonPath("$.lockStatus.isLocked", is(true)),
                                withJsonPath("$.lockStatus.lockedBy", is(USER_ID_FOR_BILL.toString())),
                                withJsonPath("$.lockStatus.expiryTime", is(getZonedDateTimeToString(requestEditTime.plusMinutes(FORM_LOCK_DURATION_TIME))))
                        ))
                )));

        final ZonedDateTime expectedExpiryTime = requestEditTime.plusMinutes(FORM_LOCK_DURATION_TIME);
        final FormLockStatus lockStatus = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus();
        assertLockStatusDetails(lockStatus, expectedExpiryTime, USER_ID_FOR_BILL);
    }


    @Test
    public void shouldEditForm_ForSecondTime_BySameUser_WhenFormIsLocked() throws EventStreamException {
        final ZonedDateTime originalRequestEditTime = now();

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.createForm(COURT_FORM_ID, CASE_ID, FORM_ID, ImmutableList.of(DEFENDANT_ID), "blah", USER_ID_FOR_BILL, BCM, randomUUID(), null);
        caseAggregate.requestEditForm(CASE_ID, COURT_FORM_ID, USER_ID_FOR_BILL, ImmutableMap.of(BCM, FORM_LOCK_DURATION_TIME), originalRequestEditTime, false , 0);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        final Envelope<RequestEditForm> envelope = createEnvelope(COURT_FORM_ID, CASE_ID, USER_ID_FOR_BILL);

        formCommandHandler.handleRequestEditForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.edit-form-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.courtFormId", is(COURT_FORM_ID.toString())),
                                withJsonPath("$.lockStatus.isLocked", is(false)),
                                withJsonPath("$.lockStatus.expiryTime", notNullValue())
                        ))
                )));

        final FormLockStatus lockStatus = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus();
        assertLockStatusDetails(lockStatus, originalRequestEditTime.plusMinutes(FORM_LOCK_DURATION_TIME), USER_ID_FOR_BILL);
    }


    @Test
    public void shouldEditForm_AfterExpiryTime_ByAnyUser() throws EventStreamException {
        final ZonedDateTime originalRequestEditTime = now().minusMinutes(30);

        final CaseAggregate caseAggregate = new CaseAggregate();
    caseAggregate.createForm(COURT_FORM_ID, CASE_ID, FORM_ID, ImmutableList.of(DEFENDANT_ID), "blah", USER_ID_FOR_BILL, BCM, randomUUID(), null);
        caseAggregate.requestEditForm(CASE_ID, COURT_FORM_ID, USER_ID_FOR_BILL, ImmutableMap.of(BCM, FORM_LOCK_DURATION_TIME), originalRequestEditTime, false , 0);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        final Envelope<RequestEditForm> envelope = createEnvelope(COURT_FORM_ID, CASE_ID, USER_ID_FOR_ERICA);
        ZonedDateTime originalExpiryTime = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus().getLockExpiryTime();
        assertThat(originalExpiryTime, is(originalRequestEditTime.plusMinutes(FORM_LOCK_DURATION_TIME)));

        formCommandHandler.handleRequestEditForm(envelope);

        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus().getLockExpiryTime();

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.edit-form-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.courtFormId", is(COURT_FORM_ID.toString())),
                                withJsonPath("$.lockStatus.isLocked", is(false)),
                                withJsonPath("$.lockStatus.lockRequestedBy", is(USER_ID_FOR_ERICA.toString())),
                                withJsonPath("$.lockStatus.expiryTime", is(getZonedDateTimeToString(expiryTime)))
                        ))
                )));

        final FormLockStatus lockStatus = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus();
        final ZonedDateTime actualExpiryTime = lockStatus.getLockExpiryTime();
        assertThat(originalExpiryTime, is(lessThan(actualExpiryTime)));
        assertLockStatusDetails(lockStatus, expiryTime, USER_ID_FOR_ERICA);
    }


    @Test
    public void shouldEditForm_AfterUpdate_ByAnyUser() throws EventStreamException {
        final ZonedDateTime originalRequestEditTime = new UtcClock().now().truncatedTo(MILLIS);

        final CaseAggregate caseAggregate = new CaseAggregate();
        caseAggregate.createForm(COURT_FORM_ID, CASE_ID, FORM_ID, ImmutableList.of(DEFENDANT_ID), "blah", USER_ID_FOR_BILL, BCM, randomUUID(), null);
        caseAggregate.requestEditForm(CASE_ID, COURT_FORM_ID, USER_ID_FOR_BILL, ImmutableMap.of(BCM, FORM_LOCK_DURATION_TIME), originalRequestEditTime, false , 0);
        caseAggregate.updateForm(CASE_ID, "updateFormData", COURT_FORM_ID, USER_ID_FOR_BILL);

        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);

        final Envelope<RequestEditForm> envelope = createEnvelope(COURT_FORM_ID, CASE_ID, USER_ID_FOR_ERICA);

        formCommandHandler.handleRequestEditForm(envelope);

        ZonedDateTime expiryTime = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus().getLockExpiryTime();

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.edit-form-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(CASE_ID.toString())),
                                withJsonPath("$.courtFormId", is(COURT_FORM_ID.toString())),
                                withJsonPath("$.lockStatus.isLocked", is(false)),
                                withJsonPath("$.lockStatus.lockRequestedBy", is(USER_ID_FOR_ERICA.toString())),
                                withJsonPath("$.lockStatus.expiryTime", is(getZonedDateTimeToString(expiryTime)))
                        ))
                )));

        final FormLockStatus lockStatus = caseAggregate.getFormMap().get(COURT_FORM_ID).getFormLockStatus();
        assertLockStatusDetails(lockStatus, expiryTime, USER_ID_FOR_ERICA);
    }

    private void assertLockStatusDetails(final FormLockStatus lockStatus, final ZonedDateTime expectedExpiryTime, final UUID expectedLockedByUser) {
        final ZonedDateTime actualExpiryTime = lockStatus.getLockExpiryTime();
        assertThat(actualExpiryTime, is(expectedExpiryTime));
        assertThat(lockStatus.getLockedBy(), is(expectedLockedByUser));
    }

    private String getZonedDateTimeToString(final ZonedDateTime datetime){
        return datetime.truncatedTo(MILLIS).toLocalDateTime().toString()+"Z";
    }

    private Envelope<RequestEditForm> createEnvelope(final UUID courtFormId, final UUID caseId, final UUID userId) {
        final RequestEditForm requestEditForm = requestEditForm()
                .withCourtFormId(courtFormId)
                .withCaseId(caseId)
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("progression.command.edit-form")
                .withId(randomUUID())
                .withUserId(userId.toString())
                .build();

        return envelopeFrom(metadata, requestEditForm);
    }

    @Test
    public void shouldHandleCpsCreateForm() throws EventStreamException {

        final CreateForm createForm = createForm()
                .withCourtFormId(randomUUID())
                .withCaseId(randomUUID())
                .withFormId(randomUUID())
                .withFormData("{}")
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(randomUUID())
                        .build()))
                .withUserName("CMS User")
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("progression.command.create-form")
                .withId(randomUUID())
                .build();

        final Envelope<CreateForm> envelope = envelopeFrom(metadata, createForm);

        when(caseAggregate.createForm(any(), any(), any(), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(Stream.of(formCreated()
                        .withCaseId(createForm.getCaseId())
                        .withFormId(createForm.getFormId())
                        .withCourtFormId(createForm.getCourtFormId())
                        .build()));

        formCommandHandler.handleCreateForm(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.form-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.caseId", is(createForm.getCaseId().toString())),
                                withJsonPath("$.formId", is(createForm.getFormId().toString())),
                                withJsonPath("$.courtFormId", is(createForm.getCourtFormId().toString())))
                        ))
        ));

    }


}