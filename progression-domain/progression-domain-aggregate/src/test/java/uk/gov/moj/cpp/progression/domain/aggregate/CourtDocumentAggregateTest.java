package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentAudit;
import uk.gov.justice.core.courts.CourtDocumentShareFailed;
import uk.gov.justice.core.courts.CourtDocumentShared;
import uk.gov.justice.core.courts.CourtDocumentUpdateFailed;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DuplicateShareCourtDocumentRequestReceived;
import uk.gov.justice.core.courts.Material;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class CourtDocumentAggregateTest {

    private static final CourtDocument courtDocument = CourtDocument.courtDocument()
            .withCourtDocumentId(randomUUID())
            .withDocumentTypeDescription("documentTypeDescription")
            .withDocumentTypeId(randomUUID())
            .withDocumentCategory(DocumentCategory.documentCategory().build())
            .withMimeType("pdf")
            .withName("name")
            .build();

    private CourtDocumentAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new CourtDocumentAggregate();
    }

    @Test
    public void shouldReturnCourtDocumentSharedWhenNotADuplicate() {
        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();
        createCourtDocument(caseId);
        final List<Object> eventStream = aggregate.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentShared.class)));

        final CourtDocumentShared courtDocumentShared = (CourtDocumentShared) object;
        assertThat(courtDocumentShared.getSharedCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(courtDocumentShared.getSharedCourtDocument().getHearingId(), is(hearingId));
        assertThat(courtDocumentShared.getSharedCourtDocument().getUserGroupId(), is(userGroupId));
        assertNull(courtDocumentShared.getSharedCourtDocument().getUserId());
    }



    @Test
    public void shouldReturnCourtDocumentDefendantLevelSharedWhenNotADuplicate() {
        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        createCourtDocumentWithDefendant(caseId , Arrays.asList(defendantId1, defendantId2));
        final List<Object> eventStream = aggregate.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentShared.class)));

        final CourtDocumentShared courtDocumentShared = (CourtDocumentShared) object;
        assertThat(courtDocumentShared.getSharedCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(courtDocumentShared.getSharedCourtDocument().getHearingId(), is(hearingId));
        assertThat(courtDocumentShared.getSharedCourtDocument().getUserGroupId(), is(userGroupId));
        assertThat(courtDocumentShared.getSharedCourtDocument().getDefendantId(), is(defendantId1));
        assertThat(courtDocumentShared.getSharedCourtDocument().getCaseIds().get(0), is(caseId));
        assertNull(courtDocumentShared.getSharedCourtDocument().getUserId());


        final Object second = eventStream.get(1);
        assertThat(second.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentShared.class)));

        final CourtDocumentShared secondCourtDocumentShared = (CourtDocumentShared) second;
        assertThat(secondCourtDocumentShared.getSharedCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(secondCourtDocumentShared.getSharedCourtDocument().getHearingId(), is(hearingId));
        assertThat(secondCourtDocumentShared.getSharedCourtDocument().getUserGroupId(), is(userGroupId));
        assertThat(secondCourtDocumentShared.getSharedCourtDocument().getDefendantId(), is(defendantId2));
        assertThat(secondCourtDocumentShared.getSharedCourtDocument().getCaseIds().get(0), is(caseId));
        assertNull(secondCourtDocumentShared.getSharedCourtDocument().getUserId());

    }


    private void createCourtDocument(final UUID caseId) {
        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build()).build())
                .build();
        this.aggregate.createCourtDocument(courtDocument);
    }

    private void createCourtDocumentWithDefendant(final UUID caseId, List<UUID> defandants) {
        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build())
                        .withDefendantDocument(DefendantDocument.defendantDocument().withDefendants(defandants).withProsecutionCaseId(caseId).build()).build())
                .build();
        this.aggregate.createCourtDocument(courtDocument);
    }


    @Test
    public void shouldReturnDuplicateRequestReceivedWhenDuplicate() {

        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();
        createCourtDocument(caseId);
        aggregate.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null);
        final List<Object> eventStreamForDuplicate = aggregate.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

        assertThat(eventStreamForDuplicate.size(), is(1));
        final Object object = eventStreamForDuplicate.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DuplicateShareCourtDocumentRequestReceived.class)));

        final DuplicateShareCourtDocumentRequestReceived duplicateShareCourtDocumentRequestReceived = (DuplicateShareCourtDocumentRequestReceived) object;
        assertThat(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getCourtDocumentId(), is(courtDocumentId));
        assertThat(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getHearingId(), is(hearingId));
        assertThat(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getUserGroupId(), is(userGroupId));
        assertNull(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getUserId());
    }

    @Test
    public void shouldReturnForOneDuplicateRequestReceivedWhenDefendantDocumentsAddedAgain() {

        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        createCourtDocumentWithDefendant(caseId , Arrays.asList(defendantId1, defendantId2));

        aggregate.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null);
        final List<Object> eventStreamForDuplicate = aggregate.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

        assertThat(eventStreamForDuplicate.size(), is(2));
        final Object object = eventStreamForDuplicate.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DuplicateShareCourtDocumentRequestReceived.class)));

        final DuplicateShareCourtDocumentRequestReceived duplicateShareCourtDocumentRequestReceived = (DuplicateShareCourtDocumentRequestReceived) object;
        assertThat(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getCourtDocumentId(), is(courtDocumentId));
        assertThat(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getHearingId(), is(hearingId));
        assertThat(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getUserGroupId(), is(userGroupId));
        assertNull(duplicateShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getUserId());

        final Object second = eventStreamForDuplicate.get(0);
        assertThat(second.getClass(), is(CoreMatchers.<Class<?>>equalTo(DuplicateShareCourtDocumentRequestReceived.class)));

        final DuplicateShareCourtDocumentRequestReceived duplicateSecondShareCourtDocumentRequestReceived = (DuplicateShareCourtDocumentRequestReceived) second;
        assertThat(duplicateSecondShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getCourtDocumentId(), is(courtDocumentId));
        assertThat(duplicateSecondShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getHearingId(), is(hearingId));
        assertThat(duplicateSecondShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getUserGroupId(), is(userGroupId));
        assertNull(duplicateSecondShareCourtDocumentRequestReceived.getShareCourtDocumentDetails().getUserId());
    }

    @Test
    public void shouldReturnCourtsDocumentCreated() {

        final List<Object> eventStream = aggregate.createCourtDocument(courtDocument).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtsDocumentCreated.class)));
    }

    @Test
    public void shouldReturnCourtsDocumentAdded() {

        final List<Object> eventStream = aggregate.addCourtDocument(CourtDocument.courtDocument().build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtsDocumentAdded.class)));
    }

    @Test
    public void shouldReturnCourtDocumentAudit() {
        final  UUID materialId = randomUUID();
        Material material = Material.material().withId(materialId).withName("Test").build();
        aggregate.apply(aggregate.addCourtDocument(CourtDocument.courtDocument().withMaterials(asList(material)).build()).collect(toList()));

        final List<Object> eventStream = aggregate.auditCourtDocument(randomUUID(), "View", materialId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentAudit.class)));
    }

    @Test
    public void shouldReturnCourtDocumentShareFailedWhenCourtDocumentIsRemoved() {

        final UUID courtDocumentId = randomUUID();

        aggregate.apply(aggregate.addCourtDocument(CourtDocument.courtDocument().withIsRemoved(FALSE).withCourtDocumentId(courtDocumentId).build()).collect(toList()));
        aggregate.removeCourtDocument(randomUUID(), randomUUID(), true);
        final List<Object> returnedEventStream = aggregate.shareCourtDocument(randomUUID(), randomUUID(), randomUUID(), null).collect(toList());

        assertThat(returnedEventStream.size(), is(1));
        final Object returnedObject = returnedEventStream.get(0);
        assertThat(returnedObject.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentShareFailed.class)));

        final CourtDocumentShareFailed returnedEvent = (CourtDocumentShareFailed) returnedObject;
        assertThat(returnedEvent.getCourtDocumentId(), is(courtDocumentId));
        assertThat(returnedEvent.getFailureReason(), is(format("Document is deleted. Could not share the given court document id: %s", courtDocumentId)));
    }

    @Test
    public void shouldReturnCourtDocumentUpdateFailedWhenCourtDocumentIsRemoved() {

        final UUID courtDocumentId = randomUUID();

        aggregate.apply(aggregate.addCourtDocument(CourtDocument.courtDocument().withIsRemoved(FALSE).withCourtDocumentId(courtDocumentId).build()).collect(toList()));
        aggregate.removeCourtDocument(randomUUID(), randomUUID(), true);
        final List<Object> returnedEventStream = aggregate.updateCourtDocument(null, null, null).collect(toList());

        assertThat(returnedEventStream.size(), is(1));
        final Object returnedObject = returnedEventStream.get(0);
        assertThat(returnedObject.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentUpdateFailed.class)));

        final CourtDocumentUpdateFailed returnedEvent = (CourtDocumentUpdateFailed) returnedObject;
        assertThat(returnedEvent.getCourtDocumentId(), is(courtDocumentId));
        assertThat(returnedEvent.getFailureReason(), is(format("Document is deleted. Could not update the given court document id: %s", courtDocumentId)));
    }

}
