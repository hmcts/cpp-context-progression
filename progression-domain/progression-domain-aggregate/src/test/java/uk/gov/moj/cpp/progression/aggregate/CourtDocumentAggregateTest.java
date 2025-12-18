package uk.gov.moj.cpp.progression.aggregate;

import org.hamcrest.CoreMatchers;

import org.junit.jupiter.api.Test;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentAudit;
import uk.gov.justice.core.courts.CourtDocumentPrintTimeUpdated;
import uk.gov.justice.core.courts.CourtDocumentSendToCps;
import uk.gov.justice.core.courts.CourtDocumentSharedV2;
import uk.gov.justice.core.courts.CourtDocumentUpdateFailed;
import uk.gov.justice.core.courts.CourtDocumentUpdated;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentReviewRequired;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.DuplicateShareCourtDocumentRequestReceived;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.progression.event.SendToCpsFlagUpdated;

public class CourtDocumentAggregateTest {

    private static final String READ_USER = "ReadUser";
    private static final String DOCUMENT_TYPE_DESCRIPTION = "documentTypeDescription";
    private static final String PDF = "pdf";
    private static final String NAME = "name1";

    private static final CourtDocument courtDocument = CourtDocument.courtDocument()
            .withCourtDocumentId(randomUUID())
            .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
            .withDocumentTypeId(randomUUID())
            .withDocumentCategory(DocumentCategory.documentCategory().build())
            .withMimeType(PDF)
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
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentSharedV2.class)));

        final CourtDocumentSharedV2 courtDocumentSharedV2 = (CourtDocumentSharedV2) object;
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getHearingId(), is(hearingId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getUserGroupId(), is(userGroupId));
        assertNull(courtDocumentSharedV2.getSharedCourtDocument().getUserId());
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
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentSharedV2.class)));

        final CourtDocumentSharedV2 courtDocumentSharedV2 = (CourtDocumentSharedV2) object;
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getHearingId(), is(hearingId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getUserGroupId(), is(userGroupId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getDefendantId(), is(defendantId1));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getCaseIds().get(0), is(caseId));
        assertNull(courtDocumentSharedV2.getSharedCourtDocument().getUserId());


        final Object second = eventStream.get(1);
        assertThat(second.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentSharedV2.class)));

        final CourtDocumentSharedV2 secondCourtDocumentSharedV2 = (CourtDocumentSharedV2) second;
        assertThat(secondCourtDocumentSharedV2.getSharedCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(secondCourtDocumentSharedV2.getSharedCourtDocument().getHearingId(), is(hearingId));
        assertThat(secondCourtDocumentSharedV2.getSharedCourtDocument().getUserGroupId(), is(userGroupId));
        assertThat(secondCourtDocumentSharedV2.getSharedCourtDocument().getDefendantId(), is(defendantId2));
        assertThat(secondCourtDocumentSharedV2.getSharedCourtDocument().getCaseIds().get(0), is(caseId));
        assertNull(secondCourtDocumentSharedV2.getSharedCourtDocument().getUserId());

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

    private void createCourtDocumentWithApplication(final UUID applicationId) {
        CourtDocument courtDocument = CourtDocument.courtDocument()
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withApplicationDocument(ApplicationDocument.applicationDocument().withApplicationId(applicationId).build())
                        .build())
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
    public void shouldReturnCourtSharedEventForApplication() {
        final UUID courtDocumentId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID applicationId = randomUUID();
        createCourtDocumentWithApplication(applicationId);

        final List<Object> sharedCourtDocumentEvents = target.shareCourtDocument(courtDocumentId, hearingId, userGroupId, null).collect(toList());

        assertThat(sharedCourtDocumentEvents.size(), is(1));
        final Object object = sharedCourtDocumentEvents.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtDocumentSharedV2.class)));

        final CourtDocumentSharedV2 courtDocumentSharedV2 = (CourtDocumentSharedV2) object;

        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getCourtDocumentId(), is(courtDocumentId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getHearingId(), is(hearingId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getUserGroupId(), is(userGroupId));
        assertThat(courtDocumentSharedV2.getSharedCourtDocument().getApplicationId(), is(applicationId));
        assertNull(courtDocumentSharedV2.getSharedCourtDocument().getUserId());

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
    public void shouldReturnCourtsDocumentUpdatedAndCourtDocumentNotified_pet() {
        final UUID courtDocumentId = randomUUID();
        final UUID materialId = randomUUID();
        createCourtDocumentWithMaterial(courtDocumentId, materialId);

        final CourtDocument updatedCourtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withMimeType(PDF)
                .withName(NAME)
                .withSendToCps(true)
                .build();

        final List<Object> eventStream = target.updateCourtDocument(updatedCourtDocument, now(), DocumentTypeRBAC.documentTypeRBAC()
                .withReadUserGroups(newArrayList(READ_USER))
                .build(), newArrayList(materialId),newArrayList(),newArrayList()).collect(toList());
        assertThat(((CourtDocumentSendToCps) eventStream.get(1)).getNotificationType(), is("pet-form-finalised"));
    }

    @Test
    public void shouldReturnCourtsDocumentUpdatedAndCourtDocumentNotified_bcm() {
        final UUID courtDocumentId = randomUUID();
        final UUID materialId = randomUUID();
        createCourtDocumentWithMaterial(courtDocumentId, materialId);

        final CourtDocument updatedCourtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withMimeType(PDF)
                .withName(NAME)
                .withSendToCps(true)
                .build();

        final List<Object> eventStream = target.updateCourtDocument(updatedCourtDocument, now(), DocumentTypeRBAC.documentTypeRBAC()
                .withReadUserGroups(newArrayList(READ_USER))
                .build(), newArrayList(), newArrayList(materialId),newArrayList()).collect(toList());
        assertThat(((CourtDocumentSendToCps) eventStream.get(1)).getNotificationType(), is("bcm-form-finalised"));
    }

    @Test
    public void shouldReturnCourtsDocumentUpdatedAndCourtDocumentNotified_ptph() {
        final UUID courtDocumentId = randomUUID();
        final UUID materialId = randomUUID();
        createCourtDocumentWithMaterial(courtDocumentId, materialId);

        final CourtDocument updatedCourtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory().build())
                .withMimeType(PDF)
                .withName(NAME)
                .withSendToCps(true)
                .build();

        final List<Object> eventStream = target.updateCourtDocument(updatedCourtDocument, now(), DocumentTypeRBAC.documentTypeRBAC()
                .withReadUserGroups(newArrayList(READ_USER))
                .build(), newArrayList(), newArrayList(), newArrayList(materialId)).collect(toList());
        assertThat(((CourtDocumentSendToCps) eventStream.get(1)).getNotificationType(), is("ptph-form-finalised"));
    }

    private void createCourtDocumentWithMaterial(final UUID courtDocumentId, final UUID materialId){
        final List<Material> materials = new ArrayList<>();
        materials.add(Material.material()
                .withId(materialId)
                .build());

        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withDocumentTypeDescription("documentTypeDescription")
                .withDocumentTypeId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument().withProsecutionCaseId(randomUUID()).build()).build())
                .withMaterials(materials)
                .withSendToCps(false)
                .withMimeType("pdf")
                .withName("name")
                .build();
        this.target.createCourtDocument(courtDocument, true);
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
        final JsonObject userOrganisationDetails = JsonObjects.createObjectBuilder()
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
                .add("laaContractNumbers",JsonObjects.createArrayBuilder()
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
        final JsonObject userOrganisationDetails = JsonObjects.createObjectBuilder()
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
                .add("laaContractNumbers",JsonObjects.createArrayBuilder()
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
        final JsonObject userOrganisationDetails = JsonObjects.createObjectBuilder()
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
                .add("laaContractNumbers",JsonObjects.createArrayBuilder()
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
        final JsonObject userOrganisationDetails = JsonObjects.createObjectBuilder()
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
                .add("laaContractNumbers",JsonObjects.createArrayBuilder()
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

    @Test
    public void shouldUpdateSendToCpsFlag() {
        final UUID courtDocumentId = randomUUID();
        final Stream<Object> objectStream = target.updateSendToCpsFlag(randomUUID(), true, CourtDocument.courtDocument().withCourtDocumentId(courtDocumentId).build());
        final Object event = objectStream.findFirst().get();
        assertThat(event.getClass(), is(equalTo(SendToCpsFlagUpdated.class)));
        assertThat(((SendToCpsFlagUpdated)event).getCourtDocument().getCourtDocumentId(), is(courtDocumentId));
    }
}
