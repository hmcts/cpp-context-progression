package uk.gov.moj.cpp.progression.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import uk.gov.justice.core.courts.CaseSubjects;
import uk.gov.justice.core.courts.EnforcementAcknowledgmentError;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowDocumentRequestToBeAcknowledged;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.NowsDocumentFailed;
import uk.gov.justice.core.courts.NowsDocumentGenerated;
import uk.gov.justice.core.courts.NowsDocumentSent;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.NowsRequestWithAccountNumberIgnored;
import uk.gov.justice.core.courts.NowsRequestWithAccountNumberUpdated;
import uk.gov.justice.core.courts.RecordNowsDocumentFailed;
import uk.gov.justice.core.courts.RecordNowsDocumentSent;
import uk.gov.justice.core.courts.nowdocument.FinancialOrderDetails;
import uk.gov.justice.core.courts.nowdocument.NowDistribution;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.OrderAddressee;
import uk.gov.justice.core.courts.nowdocument.OrderCourt;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.justice.progression.courts.RecordNowsDocumentGenerated;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.NotificationRequestAccepted;
import uk.gov.moj.cpp.progression.domain.NotificationRequestFailed;
import uk.gov.moj.cpp.progression.domain.NotificationRequestSucceeded;
import uk.gov.moj.cpp.progression.domain.event.MaterialStatusUpdateIgnored;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequestNotSent;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.event.print.PrintRequested;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MaterialAggregateTest {
    @InjectMocks
    private MaterialAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new MaterialAggregate();
    }

    @Test
    public void shouldSaveAccountNumber() {
        final UUID materialId = randomUUID();
        final UUID requestId = randomUUID();
        final UUID userId = randomUUID();
        final String bilingualTemplateName = "BilingualTemplateName";

        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                .withMaterialId(materialId)
                .withRequestId(requestId.toString())
                .withNowContent(NowDocumentContent.nowDocumentContent()
                        .withCases(Collections.singletonList(ProsecutionCase.prosecutionCase()
                                        .withIsCps(false)
                                .build()))
                        .withOrderingCourt(OrderCourt.orderCourt()
                                .withWelshCourtCentre(true)
                                .build())
                        .withFinancialOrderDetails(FinancialOrderDetails.financialOrderDetails()
                                .build())
                        .build())
                .withBilingualTemplateName(bilingualTemplateName)
                .build();

        aggregate.apply(NowDocumentRequestToBeAcknowledged.nowDocumentRequestToBeAcknowledged().withNowDocumentRequest(nowDocumentRequest).build());
        final List<Object> eventStream = aggregate.saveAccountNumber(materialId, requestId, "ACC1234", userId).collect(toList());
        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NowsRequestWithAccountNumberUpdated.class)));
        assertThat(eventStream.get(1).getClass(), is(CoreMatchers.equalTo(NowDocumentRequested.class)));
        final NowDocumentRequested nowDocumentRequested = (NowDocumentRequested)eventStream.get(1);
        assertThat(nowDocumentRequested.getCpsProsecutionCase(), is(false));
        assertThat(nowDocumentRequested.getTemplateName(), is(bilingualTemplateName));
        assertThat(nowDocumentRequested.getUserId(), is(userId));
    }

    @Test
    public void shouldNotSaveAccountNumberWhenSavedBefore() {
        final UUID materialId = randomUUID();
        final UUID requestId = randomUUID();
        final UUID userId = randomUUID();
        final String accountNumber = "ACC1234";

        aggregate.apply(NowsRequestWithAccountNumberUpdated.nowsRequestWithAccountNumberUpdated()
                .withAccountNumber(accountNumber)
                .withRequestId(requestId)
                .build());
        final List<Object> eventStream = aggregate.saveAccountNumber(materialId, requestId, accountNumber, userId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NowsRequestWithAccountNumberIgnored.class)));
    }

    @Test
    public void shouldRecordEnforcementError() {
        final UUID requestId = randomUUID();
        final List<Object> eventStream = aggregate.recordEnforcementError(requestId, "error1", "bad data").collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(EnforcementAcknowledgmentError.class)));
    }

    @Test
    public void shouldRecordNotificationRequestAccepted() {
        final List<Object> eventStream = aggregate.recordNotificationRequestAccepted(randomUUID(), randomUUID(), ZonedDateTime.now()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NotificationRequestAccepted.class)));
    }

    @Test
    public void shouldRecordNotificationRequestFailure() {
        final List<Object> eventStream = aggregate.recordNotificationRequestFailure(randomUUID(), randomUUID(), ZonedDateTime.now(), "bad data", Optional.of(1234)).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NotificationRequestFailed.class)));
    }

    @Test
    public void shouldRecordPrintRequest() {
        final List<Object> eventStream = aggregate.recordPrintRequest(randomUUID(), randomUUID(), false).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(PrintRequested.class)));
    }

    @Test
    public void shouldRecordNotificationRequestSuccess() {
        final List<Object> eventStream = aggregate.recordNotificationRequestSuccess(randomUUID(), randomUUID(), ZonedDateTime.now(), ZonedDateTime.now()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NotificationRequestSucceeded.class)));
    }

    @Test
    public void shouldReturnNowDocumentRequested() {
        final UUID materialId = randomUUID();
        final String templateName = "TemplateName";

        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                .withMaterialId(materialId)
                .withNowContent(NowDocumentContent.nowDocumentContent()
                        .withCases(Collections.singletonList(ProsecutionCase.prosecutionCase()
                                .withIsCps(true)
                                .build()))
                        .withOrderingCourt(OrderCourt.orderCourt()
                                .withWelshCourtCentre(false)
                                .build())
                        .build())
                .withTemplateName(templateName)
                .build();

        final List<Object> eventStream = aggregate.createNowDocumentRequest(materialId, nowDocumentRequest, randomUUID()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NowDocumentRequested.class)));
        final NowDocumentRequested nowDocumentRequested = (NowDocumentRequested)object;
        assertThat(nowDocumentRequested.getCpsProsecutionCase(), is(true));
        assertThat(nowDocumentRequested.getTemplateName(), is(templateName));
    }

    @Test
    public void shouldReturnNowDocumentRequestToBeAcknowledged() {
        final UUID materialId = randomUUID();
        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                .withMaterialId(materialId)
                .withRequestId(randomUUID().toString())
                .withNowContent(NowDocumentContent.nowDocumentContent()
                        .withFinancialOrderDetails(FinancialOrderDetails.financialOrderDetails()
                                .build())
                        .build())
                .build();

        final List<Object> eventStream = aggregate.createNowDocumentRequest(materialId, nowDocumentRequest, randomUUID()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NowDocumentRequestToBeAcknowledged.class)));
    }

    @Test
    public void shouldRecordEmailRequests() {
        final UUID materialId = randomUUID();
        final UUID templateId = randomUUID();
        final UUID notificationId1 = randomUUID();
        final Notification notification1 = createNotification(notificationId1, templateId, "sendTo@hmcts.net", materialId);
        final UUID notificationId2 = randomUUID();
        final Notification notification2 = createNotification(notificationId2, templateId, null, materialId);

        final List<Object> events = aggregate.recordEmailRequest(materialId, Arrays.asList(notification1, notification2)).collect(toList());

        assertThat(events.size(), is(2));
        assertThat(events.get(0).getClass(), is(CoreMatchers.equalTo(EmailRequested.class)));
        assertThat(events.get(1).getClass(), is(CoreMatchers.equalTo(EmailRequestNotSent.class)));
    }

    @Test
    public void shouldRecordNowsMaterialRequest(){
        final Stream<Object> eventStream = aggregate.create(MaterialDetails.materialDetails().build());

        final List events = eventStream.collect(toList());
        assertThat(events.get(0), instanceOf(NowsMaterialRequestRecorded.class));
    }

    @Test
    public void shouldReturnNowsMaterialStatusIgnored() {
        final List<CaseSubjects> caseSubjects = new ArrayList<>();
        caseSubjects.add(CaseSubjects.caseSubjects()
                .withProsecutingAuthorityOUCode("ouCode")
                .build());
        final List<String> cpsDefendantIds = new ArrayList<>();
        cpsDefendantIds.add("defId1");

        final List<Object> eventStream = aggregate.nowsMaterialStatusUpdated(randomUUID(), "status", caseSubjects, cpsDefendantIds).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(MaterialStatusUpdateIgnored.class)));
    }

    @Test
    public void shouldRecordNowsDocumentSent() {
        final UUID materialId = randomUUID();

        final UUID userId = randomUUID();

        final RecordNowsDocumentSent recordNowsDocumentSent = RecordNowsDocumentSent.recordNowsDocumentSent()
                .withMaterialId(materialId)
                .withHearingId(randomUUID())
                .withPayloadFileId(randomUUID())
                .withCpsProsecutionCase(false)
                .withFileName(randomUUID().toString())
                .withNowDistribution(NowDistribution.nowDistribution().build())
                .withOrderAddressee(OrderAddressee.orderAddressee().build())
                .build();

        final List<Object> events = aggregate.recordNowsDocumentSent(materialId, userId, recordNowsDocumentSent).collect(toList());

        assertThat(events.size(), is(1));
        final NowsDocumentSent nowsDocumentSent = (NowsDocumentSent) events.get(0);
        assertThat(nowsDocumentSent.getMaterialId(), is(materialId));
        assertThat(nowsDocumentSent.getPayloadFileId(), is(recordNowsDocumentSent.getPayloadFileId()));
        assertThat(nowsDocumentSent.getCpsProsecutionCase(), is(recordNowsDocumentSent.getCpsProsecutionCase()));
    }

    @Test
    public void shouldRecordNowsDocumentFailed() {
        final UUID materialId = randomUUID();

        final RecordNowsDocumentFailed recordNowsDocumentFailed = RecordNowsDocumentFailed.recordNowsDocumentFailed()
                .withMaterialId(materialId)
                .withPayloadFileId(randomUUID())
                .withReason("Test Reason")
                .withConversionFormat("pdf")
                .withOriginatingSource("NOWs")
                .withRequestedTime(ZonedDateTime.now())
                .withTemplateIdentifier("Test Template")
                .build();

        final List<Object> events = aggregate.recordNowsDocumentFailed(materialId, recordNowsDocumentFailed).collect(toList());

        assertThat(events.size(), is(1));
        final NowsDocumentFailed nowsDocumentFailed = (NowsDocumentFailed) events.get(0);
        assertThat(nowsDocumentFailed.getMaterialId(), is(materialId));
        assertThat(nowsDocumentFailed.getPayloadFileId(), is(recordNowsDocumentFailed.getPayloadFileId()));
        assertThat(nowsDocumentFailed.getReason(), is(recordNowsDocumentFailed.getReason()));

    }

    @Test
    public void shouldRecordNowsDocumentGenerated() {
        final UUID materialId = randomUUID();

        final UUID userId = randomUUID();

        final UUID payloadFileId = randomUUID();

        final UUID systemDocGeneratorId =  randomUUID();

        final RecordNowsDocumentSent recordNowsDocumentSent = RecordNowsDocumentSent.recordNowsDocumentSent()
                .withMaterialId(materialId)
                .withHearingId(randomUUID())
                .withPayloadFileId(randomUUID())
                .withCpsProsecutionCase(false)
                .withFileName(randomUUID().toString())
                .withNowDistribution(NowDistribution.nowDistribution().build())
                .withOrderAddressee(OrderAddressee.orderAddressee().build())
                .build();

        aggregate.recordNowsDocumentSent(materialId, userId, recordNowsDocumentSent).collect(toList());

        final RecordNowsDocumentGenerated recordNowsDocumentGenerated = RecordNowsDocumentGenerated.recordNowsDocumentGenerated()
                .withMaterialId(materialId)
                .withPayloadFileId(payloadFileId)
                .withSystemDocGeneratorId(systemDocGeneratorId)
                .build();

        final List<Object> events = aggregate.recordNowsDocumentGenerated(materialId, recordNowsDocumentGenerated).collect(toList());

        assertThat(events.size(), is(1));
        final NowsDocumentGenerated nowsDocumentGenerated = (NowsDocumentGenerated) events.get(0);
        assertThat(nowsDocumentGenerated.getMaterialId(), is(materialId));
        assertThat(nowsDocumentGenerated.getSystemDocGeneratorId(), is(systemDocGeneratorId));
        assertThat(nowsDocumentGenerated.getUserId(), is(userId));
    }

    private Notification createNotification(final UUID notificationId, final UUID templateId, final String sendToAddress, final UUID materialId) {
        return new Notification(notificationId, templateId, sendToAddress, "replyTo@hmcts.net", Collections.emptyMap(), materialId.toString());
    }
}
