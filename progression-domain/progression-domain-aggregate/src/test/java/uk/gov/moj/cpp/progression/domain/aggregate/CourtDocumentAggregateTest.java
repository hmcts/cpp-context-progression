package uk.gov.moj.cpp.progression.domain.aggregate;

import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import uk.gov.justice.core.courts.*;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;

import javax.json.Json;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentAudit;
import uk.gov.justice.core.courts.CourtDocumentPrintTimeUpdated;
import uk.gov.justice.core.courts.CourtDocumentSendToCps;
import uk.gov.justice.core.courts.CourtDocumentShareFailed;
import uk.gov.justice.core.courts.CourtDocumentShared;
import uk.gov.justice.core.courts.CourtDocumentUpdateFailed;
import uk.gov.justice.core.courts.CourtDocumentUpdated;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentReviewRequired;
import uk.gov.justice.core.courts.DuplicateShareCourtDocumentRequestReceived;
import uk.gov.justice.core.courts.Material;
import uk.gov.moj.cpp.progression.aggregate.CourtDocumentAggregate;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class CourtDocumentAggregateTest {

    private static final CourtDocument courtDocument = CourtDocument.courtDocument()
            .withCourtDocumentId(randomUUID())
            .withDocumentTypeDescription("documentTypeDescription")
            .withDocumentTypeId(randomUUID())
            .withDocumentCategory(DocumentCategory.documentCategory().build())
            .withMimeType("pdf")
            .withName("name")
            .withMaterials(Arrays.asList(Material.material().withReceivedDateTime(ZonedDateTime.now()).build()))
            .withSendToCps(false)
            .build();

    private CourtDocumentAggregate target = new CourtDocumentAggregate();

    @Test
    public void shouldReturnCourtDocumentSharedWhenNotADuplicate() {
        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();
        createCourtDocument(caseId);
        final List<Object> eventStream = target.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

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
        createCourtDocumentWithDefendant(caseId, Arrays.asList(defendantId1, defendantId2));
        final List<Object> eventStream = target.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

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
                .withSendToCps(false)
                .build();
        this.target.createCourtDocument(courtDocument, true);
    }

    private void createCourtDocumentWithDefendant(final UUID caseId, List<UUID> defandants) {
        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(caseId).build())
                        .withDefendantDocument(DefendantDocument.defendantDocument().withDefendants(defandants).withProsecutionCaseId(caseId).build()).build())
                .withSendToCps(false)
                .build();
        this.target.createCourtDocument(courtDocument, true);
    }


    @Test
    public void shouldReturnDuplicateRequestReceivedWhenDuplicate() {

        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID caseId = randomUUID();
        createCourtDocument(caseId);
        target.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null);
        final List<Object> eventStreamForDuplicate = target.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

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
        createCourtDocumentWithDefendant(caseId, Arrays.asList(defendantId1, defendantId2));

        target.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null);
        final List<Object> eventStreamForDuplicate = target.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

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

        final List<Object> eventStream = target.createCourtDocument(courtDocument, true).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtsDocumentCreated.class)));
    }

    @Test
    public void shouldReturnCourtsDocumentAdded() {

        final List<Object> eventStream = target.addCourtDocument(CourtDocument.courtDocument().build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtsDocumentAdded.class)));
    }

    @Test
    public void shouldReturnCourtDocumentAudit() {
        final UUID materialId = randomUUID();
        Material material = Material.material().withId(materialId).withName("Test").build();
        target.apply(target.addCourtDocument(CourtDocument.courtDocument().withMaterials(asList(material)).build()).collect(toList()));

        final List<Object> eventStream = target.auditCourtDocument(randomUUID(), "View", materialId).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentAudit.class)));
    }

    @Test
    public void shouldReturnCourtDocumentShareFailedWhenCourtDocumentIsRemoved() {

        final UUID courtDocumentId = randomUUID();

        target.apply(target.addCourtDocument(CourtDocument.courtDocument().withIsRemoved(FALSE).withCourtDocumentId(courtDocumentId).build()).collect(toList()));
        target.removeCourtDocument(randomUUID(), randomUUID(), true);
        final List<Object> returnedEventStream = target.shareCourtDocument(randomUUID(), randomUUID(), randomUUID(), null).collect(toList());

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

        target.apply(target.addCourtDocument(CourtDocument.courtDocument().withIsRemoved(FALSE).withCourtDocumentId(courtDocumentId).build()).collect(toList()));
        target.removeCourtDocument(randomUUID(), randomUUID(), true);
        final List<Object> returnedEventStream = target.updateCourtDocument(null, null, null,newArrayList(),newArrayList(),newArrayList()).collect(toList());

        assertThat(returnedEventStream.size(), is(1));
        final Object returnedObject = returnedEventStream.get(0);
        assertThat(returnedObject.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentUpdateFailed.class)));

        final CourtDocumentUpdateFailed returnedEvent = (CourtDocumentUpdateFailed) returnedObject;
        assertThat(returnedEvent.getCourtDocumentId(), is(courtDocumentId));
        assertThat(returnedEvent.getFailureReason(), is(format("Document is deleted. Could not update the given court document id: %s", courtDocumentId)));
    }

    @Test
    public void shouldReturnCourtsDocumentCreatedAndCourtDocumentNotified() {
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentTypeDescription("documentTypeDescription")
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withMimeType("pdf")
                .withName("name")
                .withSendToCps(true)
                .build();
        final List<Object> eventStream = target.createCourtDocument(courtDocument, true).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtsDocumentCreated.class)));
        final Object object1 = eventStream.get(1);
        assertThat(object1.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentSendToCps.class)));
    }

    @Test
    public void shouldReturnOnlyCourtsDocumentCreatedEventWhenProsecutorIsNotCPS() {
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentTypeDescription("documentTypeDescription")
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withMimeType("pdf")
                .withName("name")
                .withSendToCps(true)
                .build();
        final List<Object> eventStream = target.createCourtDocument(courtDocument, false).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CourtsDocumentCreated.class)));
    }


    @Test
    public void shouldReturnCourtsDocumentUpdatedAndCourtDocumentNotified() {
        final UUID courtDocumentId = randomUUID();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription("documentTypeDescription")
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withMaterials(emptyList())
                .withMimeType("pdf")
                .withName("name")
                .withSendToCps(false)
                .build();
        setField(target, "courtDocument", courtDocument);

        final CourtDocument updatedCourtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription("documentTypeDescription")
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withMimeType("pdf")
                .withName("name1")
                .withSendToCps(true)
                .build();

        final List<Object> eventStream = target.updateCourtDocument(updatedCourtDocument, now(), null, newArrayList(),newArrayList(),newArrayList()).collect(toList());

        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentUpdated.class)));
        final Object object1 = eventStream.get(1);
        assertThat(object1.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentSendToCps.class)));
    }

    @Test
    public void shouldUpdateCourtDocumentPrintTime() {
        final UUID materialId = randomUUID();
        final UUID courtDocumentId = randomUUID();
        final ZonedDateTime printedAt = now();

        final Stream<Object> objectStream = target.updateCourtDocumentPrintTime(materialId, courtDocumentId, printedAt);
        final Object event = objectStream.findFirst().get();
        assertThat(event.getClass(), is(equalTo(CourtDocumentPrintTimeUpdated.class)));
    }

    @Test
    public void documentReviewRequiredAddedToCourtDocumentIfUserInHMCTSGroup(){
        final boolean  actionRequired = true;
        final UUID materialId = randomUUID();
        final String section = "abc";
        final Boolean isCpsCase = true;
        final Boolean isUnbundledDocument = true;
        final JsonObject userOrganisationDetails = Json.createObjectBuilder()
                .add("organisationId","1fc69990-bf59-4c4a-9489-d766b9abde9a")
                .add("organisationType","LEGAL_ORGANISATION")
                .add("organisationName", "Bodgit and Scarper LLP")
                .add("addressLine1","Legal House")
                .add("addressLine2","15 Sewell Street")
                .add( "addressLine3", "Hammersmith")
                .add("addressLine4", "London")
                .add("addressPostcode","SE14 2AB")
                .add("phoneNumber","080012345678")
                .add("email","joe@example.com")
                .add("laaContractNumbers",Json.createArrayBuilder()
                        .add("LAA3482374WER")
                        .add("LAA3482374WEM")).build();

        Stream<Object> objectStream =  target.addCourtDocument(courtDocument,actionRequired, materialId, section, isCpsCase, isUnbundledDocument, userOrganisationDetails);
        List<Object> list = objectStream.collect(Collectors.toList());

        assertThat(list, hasItem(isA(DocumentReviewRequired.class)));
    }

    @Test
    public void documentReviewRequiredNotAddedToCourtDocumentIfUserNotInHMCTSGroup(){
        final boolean  actionRequired = true;
        final UUID materialId = randomUUID();
        final String section = "abc";
        final Boolean isCpsCase = true;
        final Boolean isUnbundledDocument = true;
        final JsonObject userOrganisationDetails = Json.createObjectBuilder()
                .add("organisationId","1fc69990-bf59-4c4a-9489-d766b9abde9a")
                .add("organisationType","HMCTS")
                .add("organisationName", "Bodgit and Scarper LLP")
                .add("addressLine1","Legal House")
                .add("addressLine2","15 Sewell Street")
                .add( "addressLine3", "Hammersmith")
                .add("addressLine4", "London")
                .add("addressPostcode","SE14 2AB")
                .add("phoneNumber","080012345678")
                .add("email","joe@example.com")
                .add("laaContractNumbers",Json.createArrayBuilder()
                        .add("LAA3482374WER")
                        .add("LAA3482374WEM")).build();

        Stream<Object> objectStream =  target.addCourtDocument(courtDocument,actionRequired, materialId, section, isCpsCase, isUnbundledDocument, userOrganisationDetails);
        List<Object> list = objectStream.collect(Collectors.toList());

        assertThat(list, not(hasItem(DocumentReviewRequired.class)));
    }

    @Test
    public void documentReviewRequiredNotAddedToCourtDocumentIfUserHasEmptyOrganisation(){
        final boolean  actionRequired = true;
        final UUID materialId = randomUUID();
        final String section = "abc";
        final Boolean isCpsCase = true;
        final Boolean isUnbundledDocument = true;
        final JsonObject userOrganisationDetails = Json.createObjectBuilder()
                .add("organisationId","1fc69990-bf59-4c4a-9489-d766b9abde9a")
                .add("organisationType","")
                .add("organisationName", "Bodgit and Scarper LLP")
                .add("addressLine1","Legal House")
                .add("addressLine2","15 Sewell Street")
                .add( "addressLine3", "Hammersmith")
                .add("addressLine4", "London")
                .add("addressPostcode","SE14 2AB")
                .add("phoneNumber","080012345678")
                .add("email","joe@example.com")
                .add("laaContractNumbers",Json.createArrayBuilder()
                        .add("LAA3482374WER")
                        .add("LAA3482374WEM")).build();

        Stream<Object> objectStream =  target.addCourtDocument(courtDocument,actionRequired, materialId, section, isCpsCase, isUnbundledDocument, userOrganisationDetails);
        List<Object> list = objectStream.collect(Collectors.toList());

        assertThat(list, not(hasItem(DocumentReviewRequired.class)));
    }

    @Test
    public void documentReviewRequiredNotAddedToCourtDocumentIfUserHasNullOrganisation(){
        final boolean  actionRequired = true;
        final UUID materialId = randomUUID();
        final String section = "abc";
        final Boolean isCpsCase = true;
        final Boolean isUnbundledDocument = true;
        final JsonObject userOrganisationDetails = Json.createObjectBuilder()
                .add("organisationId","1fc69990-bf59-4c4a-9489-d766b9abde9a")
                .add("organisationType",JsonObject.NULL)
                .add("organisationName", "Bodgit and Scarper LLP")
                .add("addressLine1","Legal House")
                .add("addressLine2","15 Sewell Street")
                .add( "addressLine3", "Hammersmith")
                .add("addressLine4", "London")
                .add("addressPostcode","SE14 2AB")
                .add("phoneNumber","080012345678")
                .add("email","joe@example.com")
                .add("laaContractNumbers",Json.createArrayBuilder()
                        .add("LAA3482374WER")
                        .add("LAA3482374WEM")).build();

        Stream<Object> objectStream =  target.addCourtDocument(courtDocument,actionRequired, materialId, section, isCpsCase, isUnbundledDocument, userOrganisationDetails);
        List<Object> list = objectStream.collect(Collectors.toList());

        assertThat(list, not(hasItem(DocumentReviewRequired.class)));
    }

    @Test
    public void documentReviewRequiredNotAddedToCourtDocumentIfUserDoesNotHaveAnyOrganisation(){
        final boolean  actionRequired = true;
        final UUID materialId = randomUUID();
        final String section = "abc";
        final Boolean isCpsCase = true;
        final Boolean isUnbundledDocument = true;
        Stream<Object> objectStream =  target.addCourtDocument(courtDocument,actionRequired, materialId, section, isCpsCase, isUnbundledDocument, null);
        List<Object> list = objectStream.collect(Collectors.toList());
        assertThat(list, not(hasItem(DocumentReviewRequired.class)));
    }
}
