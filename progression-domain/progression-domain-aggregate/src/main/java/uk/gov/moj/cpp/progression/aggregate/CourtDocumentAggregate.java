package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Stream.builder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.core.courts.CourtDocumentPrintTimeUpdated.courtDocumentPrintTimeUpdated;
import static uk.gov.justice.core.courts.CourtDocumentSharedV2.courtDocumentSharedV2;
import static uk.gov.justice.core.courts.CourtDocumentUpdated.courtDocumentUpdated;
import static uk.gov.justice.core.courts.CourtsDocumentCreated.courtsDocumentCreated;
import static uk.gov.justice.core.courts.DocumentReviewRequired.documentReviewRequired;
import static uk.gov.justice.core.courts.DuplicateShareCourtDocumentRequestReceived.duplicateShareCourtDocumentRequestReceived;
import static uk.gov.justice.core.courts.Material.material;
import static uk.gov.justice.core.courts.SharedCourtDocument.sharedCourtDocument;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.AddCourtDocumentV2;
import uk.gov.justice.core.courts.AddMaterialV2;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentAudit;
import uk.gov.justice.core.courts.CourtDocumentSendToCps;
import uk.gov.justice.core.courts.CourtDocumentShared;
import uk.gov.justice.core.courts.CourtDocumentSharedV2;
import uk.gov.justice.core.courts.CourtDocumentUpdateFailed;
import uk.gov.justice.core.courts.CourtDocumentUpdated;
import uk.gov.justice.core.courts.CourtsDocumentAdded;
import uk.gov.justice.core.courts.CourtsDocumentAddedV2;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.CourtsDocumentRemoved;
import uk.gov.justice.core.courts.CourtsDocumentRemovedBdf;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.SharedCourtDocument;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.event.SendToCpsFlagUpdated;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtDocumentAggregate implements Aggregate {

    private static final long serialVersionUID = 8488391302284572449L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtDocumentAggregate.class);

    private final List<SharedCourtDocument> sharedCourtDocumentList = new ArrayList<>();

    private CourtDocument courtDocument;
    private boolean isRemoved = false;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CourtsDocumentCreated.class).apply(e ->
                        this.courtDocument = e.getCourtDocument()
                ),
                when(CourtsDocumentAdded.class).apply(e ->
                        this.courtDocument = e.getCourtDocument()
                ),
                when(CourtDocumentShared.class).apply(e ->
                        sharedCourtDocumentList.add(e.getSharedCourtDocument())
                ),
                when(CourtDocumentSharedV2.class).apply(e ->
                        sharedCourtDocumentList.add(e.getSharedCourtDocument())
                ),
                when(CourtDocumentUpdated.class).apply(e ->
                        this.courtDocument = e.getCourtDocument()
                ),
                when(CourtsDocumentRemoved.class).apply(e ->
                        this.isRemoved = e.getIsRemoved()
                ),
                when(CourtsDocumentRemovedBdf.class).apply(e ->
                        this.isRemoved = e.getIsRemoved()
                ),
                otherwiseDoNothing());
    }

    public Stream<Object> updateCourtDocumentPrintTime(final UUID materialId, final UUID courtDocumentId, final ZonedDateTime printedAt) {
        final Stream.Builder<Object> builder = builder();
        builder.add(courtDocumentPrintTimeUpdated()
                .withCourtDocumentId(courtDocumentId)
                .withMaterialId(materialId)
                .withPrintedAt(printedAt)
                .build());

        return apply(builder.build());
    }

    public Stream<Object> updateSendToCpsFlag(final UUID courtDocumentId, final boolean sendToCpsFlag, final CourtDocument courtDocument) {
        final Stream.Builder<Object> builder = builder();
        builder.add(SendToCpsFlagUpdated.sendToCpsFlagUpdated()
                .withCourtDocumentId(courtDocumentId)
                .withSendToCps(sendToCpsFlag)
                .withCourtDocument(courtDocument)
                .build());

        return apply(builder.build());
    }

    public Stream<Object> updateCourtDocument(final CourtDocument inputCourtDocumentDetails,
                                              final ZonedDateTime receivedDateTime,
                                              final DocumentTypeRBAC documentTypeRBAC,
                                              final List<UUID> petFormFinalisedDocuments,
                                              final List<UUID> bcmFormFinalisedDocuments,
                                              final List<UUID> ptphFormFinalisedDocuments) {

        if (this.isRemoved) {
            return updateCourtDocumentFailed(this.courtDocument.getCourtDocumentId(), format("Document is deleted. Could not update the given court document id: %s", courtDocument.getCourtDocumentId()));
        }

        LOGGER.debug("court document is being updated .");
        final List<Material> storedMaterials = buildMaterials(receivedDateTime, documentTypeRBAC);
        final UUID materialId  = Optional.ofNullable(storedMaterials)
                .map(materials-> materials.stream().filter(Objects::nonNull).map(v->v.getId()).filter(Objects::nonNull).collect(Collectors.toList()).stream().findFirst().orElse(null))
                .orElse(null);
        LOGGER.info("CourtDocument update request for materialId {}, petFormFinalisedDocuments {}, bcmFormFinalisedDocuments {}, ptphFormFinalisedDocuments{}",
                materialId,petFormFinalisedDocuments,bcmFormFinalisedDocuments,ptphFormFinalisedDocuments);
        final CourtDocument updatedCourtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(this.courtDocument.getCourtDocumentId())
                .withName(inputCourtDocumentDetails.getName())
                .withMimeType(this.courtDocument.getMimeType())
                .withDocumentTypeId(inputCourtDocumentDetails.getDocumentTypeId())
                .withDocumentTypeDescription(inputCourtDocumentDetails.getDocumentTypeDescription())
                .withDocumentCategory(Stream.concat(bcmFormFinalisedDocuments.stream(), ptphFormFinalisedDocuments.stream()).collect(Collectors.toList())
                        .contains(materialId) ? courtDocument.getDocumentCategory() :inputCourtDocumentDetails.getDocumentCategory())
                .withContainsFinancialMeans(inputCourtDocumentDetails.getContainsFinancialMeans())
                .withAmendmentDate(this.courtDocument.getAmendmentDate())
                .withDocumentTypeRBAC(documentTypeRBAC)
                .withSeqNum(inputCourtDocumentDetails.getSeqNum())
                .withMaterials(storedMaterials)
                .withSendToCps(inputCourtDocumentDetails.getSendToCps())
                .build();
        final Stream.Builder<Object> builder = builder();

        builder.add(courtDocumentUpdated().withCourtDocument(updatedCourtDocument).build());

        if (inputCourtDocumentDetails.getSendToCps()) {
            String notificationType = "defence-disclosure";
            if(petFormFinalisedDocuments.contains(materialId)){
                notificationType = "pet-form-finalised";
            }else if(bcmFormFinalisedDocuments.contains(materialId)){
                notificationType = "bcm-form-finalised";
            }else if(ptphFormFinalisedDocuments.contains(materialId)){
                notificationType = "ptph-form-finalised";
            }
            builder.add(CourtDocumentSendToCps.courtDocumentSendToCps().withCourtDocument(updatedCourtDocument).withNotificationType(notificationType).build());
        }

        return apply(builder.build());
    }

    public Stream<Object> updateCourtDocumentFailed(final UUID courtDocumentId, final String failureReason) {
        LOGGER.debug("court document update failed ");
        return apply(Stream.of(CourtDocumentUpdateFailed.courtDocumentUpdateFailed().withCourtDocumentId(courtDocumentId).withFailureReason(failureReason).build()));
    }


    public Stream<Object> shareCourtDocument(final UUID courtDocumentId, final UUID hearingId, final UUID userGroupId, final UUID userId) {
        LOGGER.debug("court document is being shared .");
        final Stream.Builder<Object> builder = builder();
        final List<UUID> defendants = this.courtDocument.getDocumentCategory().getDefendantDocument() != null ? this.courtDocument.getDocumentCategory().getDefendantDocument().getDefendants() : emptyList();
        if (!defendants.isEmpty()) {
            defendants.forEach(defendant ->
                    addEvents(builder, createSharedCourtDocument(courtDocumentId, hearingId, userGroupId, userId, Optional.of(defendant)))
            );
        } else {
            addEvents(builder, createSharedCourtDocument(courtDocumentId, hearingId, userGroupId, userId, Optional.empty()));
        }
        return apply(builder.build());
    }

    private SharedCourtDocument createSharedCourtDocument(final UUID courtDocumentId, final UUID hearingId, final UUID userGroupId, final UUID userId, final Optional<UUID> defendant) {
        final SharedCourtDocument.Builder shareCourtDocumentBuilder = sharedCourtDocument()
                .withCourtDocumentId(courtDocumentId)
                .withHearingId(hearingId)
                .withUserGroupId(userGroupId)
                .withUserId(userId)
                .withSeqNum(this.courtDocument.getSeqNum());

        defendant.ifPresent(shareCourtDocumentBuilder::withDefendantId);

        if (isNull(this.courtDocument.getDocumentCategory().getApplicationDocument())) {
            shareCourtDocumentBuilder.withCaseIds(getProsecutionCaseIds(this.courtDocument.getDocumentCategory()));
        } else {
            shareCourtDocumentBuilder.withApplicationId(this.courtDocument.getDocumentCategory().getApplicationDocument().getApplicationId());
        }
        return shareCourtDocumentBuilder.build();
    }

    private List<UUID> getProsecutionCaseIds(final DocumentCategory documentCategory) {

        if (nonNull(documentCategory.getNowDocument())) {
            return documentCategory.getNowDocument().getProsecutionCases();
        } else if (nonNull(documentCategory.getCaseDocument())) {
            return asList(documentCategory.getCaseDocument().getProsecutionCaseId());
        } else if (nonNull(documentCategory.getDefendantDocument())) {
            return asList(documentCategory.getDefendantDocument().getProsecutionCaseId());
        } else {
            return Collections.emptyList();
        }
    }

    private void addEvents(final Stream.Builder<Object> builder, final SharedCourtDocument sharedCourtDocument) {
        if (isDuplicateSharedCourtDocument(sharedCourtDocument)) {
            builder.accept(duplicateShareCourtDocumentRequestReceived()
                    .withShareCourtDocumentDetails(sharedCourtDocument)
                    .build());
        } else {
            builder.accept(courtDocumentSharedV2()
                    .withSharedCourtDocument(sharedCourtDocument)
                    .build());
        }
    }

    public Stream<Object> createCourtDocument(final CourtDocument courtDocument, final Boolean isCpsCase) {
        LOGGER.debug("court document is being created .");
        final Stream.Builder<Object> builder = builder();
        builder.add(courtsDocumentCreated().withCourtDocument(courtDocument).build());
        if (nonNull(courtDocument.getSendToCps()) && courtDocument.getSendToCps() && nonNull(isCpsCase) && isCpsCase) {
            builder.add(CourtDocumentSendToCps.courtDocumentSendToCps().withCourtDocument(courtDocument).build());
        }
        return apply(builder.build());
    }

    public Stream<Object> addCourtDocument(final CourtDocument courtDocument) {
        LOGGER.debug("Court document being added");
        return apply(Stream.of(CourtsDocumentAdded.courtsDocumentAdded().withCourtDocument(courtDocument).build()));
    }

    public Stream<Object> addCourtDocument(final CourtDocument courtDocument,
                                           final boolean actionRequired,
                                           final UUID materialId,
                                           final String section,
                                           final Boolean isCpsCase,
                                           final Boolean isUnbundledDocument,
                                           final JsonObject userOrganisationDetails) {
        LOGGER.debug("Court document being added");

        final Stream.Builder<Object> streamBuilder = prepareEventsForAddCourtDocument(courtDocument, actionRequired, materialId, section, isCpsCase, isUnbundledDocument, userOrganisationDetails);

        return apply(streamBuilder.build());
    }

    public Stream.Builder<Object> prepareEventsForAddCourtDocument(final CourtDocument courtDocument,
                                                                   final boolean actionRequired,
                                                                   final UUID materialId,
                                                                   final String section,
                                                                   final Boolean isCpsCase,
                                                                   final Boolean isUnbundledDocument,
                                                                   final JsonObject userOrganisationDetails) {

        final Stream.Builder<Object> streamBuilder = builder();

        final String organisationType = userOrganisationDetails != null ? userOrganisationDetails.getString("organisationType", null) : null;
        if (actionRequired && isNotEmpty(organisationType) && !"HMCTS".equals(organisationType)) {
            streamBuilder.add(CourtsDocumentAdded.courtsDocumentAdded()
                    .withCourtDocument(courtDocument)
                    .withIsCpsCase(isCpsCase)
                    .withIsUnbundledDocument(isUnbundledDocument)
                    .build())
                    .add(documentReviewRequired()
                            .withDocumentId(courtDocument.getCourtDocumentId())
                            .withDocumentType(section)
                            .withMaterialId(materialId)
                            .withSource("OTHER")
                            .withUrn("")
                            .withCaseId(getProsecutionCaseId(courtDocument))
                            .withProsecutingAuthority("")
                            .withDocumentName(courtDocument.getName())
                            .withReceivedDateTime(courtDocument.getMaterials().stream().findFirst().orElse(material().build()).getReceivedDateTime())
                            .withCode(singletonList("uploaded-review-required")).build());
        } else {
            streamBuilder.add(CourtsDocumentAdded.courtsDocumentAdded()
                    .withCourtDocument(courtDocument)
                    .withIsCpsCase(isCpsCase)
                    .withIsUnbundledDocument(isUnbundledDocument)
                    .build());
        }

        return streamBuilder;
    }

    public Stream<Object> addCourtDocumentV2(final CourtDocument courtDocument,
                                             final boolean actionRequired,
                                             final String section,
                                             final AddCourtDocumentV2 addCourtDocumentV2,
                                             final JsonObject userOrganisationDetails) {
        LOGGER.debug("Court document V2 being added");

        final Stream.Builder<Object> streamBuilder = prepareEventsForAddCourtDocument(courtDocument, actionRequired, addCourtDocumentV2.getMaterialId(), section, addCourtDocumentV2.getMaterialSubmittedV2().getIsCpsCase(), addCourtDocumentV2.getIsUnbundledDocument(), userOrganisationDetails);
        streamBuilder.add(CourtsDocumentAddedV2.courtsDocumentAddedV2()
                .withCourtDocument(courtDocument)
                .withIsUnbundledDocument(addCourtDocumentV2.getIsUnbundledDocument())
                .withMaterialSubmittedV2(AddMaterialV2.addMaterialV2()
                        .withIsCpsCase(addCourtDocumentV2.getMaterialSubmittedV2().getIsCpsCase())
                        .withCaseSubFolderName(addCourtDocumentV2.getMaterialSubmittedV2().getCaseSubFolderName())
                        .withCourtApplicationSubject(addCourtDocumentV2.getMaterialSubmittedV2().getCourtApplicationSubject())
                        .withExhibit(addCourtDocumentV2.getMaterialSubmittedV2().getExhibit())
                        .withFileName(addCourtDocumentV2.getMaterialSubmittedV2().getFileName())
                        .withMaterialContentType(addCourtDocumentV2.getMaterialSubmittedV2().getMaterialContentType())
                        .withMaterialName(addCourtDocumentV2.getMaterialSubmittedV2().getMaterialName())
                        .withProsecutionCaseSubject(addCourtDocumentV2.getMaterialSubmittedV2().getProsecutionCaseSubject())
                        .withMaterialType(addCourtDocumentV2.getMaterialSubmittedV2().getMaterialType())
                        .withSectionOrderSequence(addCourtDocumentV2.getMaterialSubmittedV2().getSectionOrderSequence())
                        .withWitnessStatement(addCourtDocumentV2.getMaterialSubmittedV2().getWitnessStatement())
                        .withTag(addCourtDocumentV2.getMaterialSubmittedV2().getTag())
                        .build())
                .build());

        return apply(streamBuilder.build());
    }

    private UUID getProsecutionCaseId(final CourtDocument courtDocument) {
        if (null != courtDocument.getDocumentCategory()) {
            final DocumentCategory documentCategory = courtDocument.getDocumentCategory();
            if (documentCategory.getApplicationDocument() != null) {
                return documentCategory.getApplicationDocument().getProsecutionCaseId();
            } else if (documentCategory.getCaseDocument() != null) {
                return documentCategory.getCaseDocument().getProsecutionCaseId();

            } else if (documentCategory.getDefendantDocument() != null) {
                return documentCategory.getDefendantDocument().getProsecutionCaseId();
            }
        }
        return null;
    }

    public Stream<Object> removeCourtDocument(final UUID courtDocumentId, final UUID materialId, final boolean isRemoved) {
        LOGGER.debug("Court document being removed");
        return apply(Stream.of(CourtsDocumentRemoved.courtsDocumentRemoved().withCourtDocumentId(courtDocumentId).withMaterialId(materialId).withIsRemoved(isRemoved).build()));
    }

    public Stream<Object> removeCourtDocumentByBdf(final UUID courtDocumentId, final boolean isRemoved) {
        LOGGER.debug("Court document being removed by BDF");
        return apply(Stream.of(CourtsDocumentRemovedBdf.courtsDocumentRemovedBdf().withCourtDocumentId(courtDocumentId).withIsRemoved(isRemoved).build()));
    }

    private boolean isDuplicateSharedCourtDocument(final SharedCourtDocument sharedCourtDocumentToCheck) {
        return sharedCourtDocumentList.stream().anyMatch(sharedCourtDocument -> compareShardCourtDocument(sharedCourtDocument, sharedCourtDocumentToCheck));
    }

    private boolean compareShardCourtDocument(final SharedCourtDocument sharedCourtDocument, final SharedCourtDocument sharedCourtDocumentToCheck) {
        return Comparator.comparing(SharedCourtDocument::getCourtDocumentId, this::compare)
                .thenComparing(SharedCourtDocument::getHearingId, this::compare)
                .thenComparing(SharedCourtDocument::getUserGroupId, this::compare)
                .thenComparing(SharedCourtDocument::getUserId, this::compare)
                .thenComparing(SharedCourtDocument::getCaseIds, this::compareIdList)
                .thenComparing(SharedCourtDocument::getDefendantId, this::compare)
                .compare(sharedCourtDocument, sharedCourtDocumentToCheck) == 0;
    }

    private int compareIdList(List<UUID> compareTo, List<UUID> compareBy) {
        if (compareTo == null && compareBy == null) {
            return 0;
        } else if (compareTo == null) {
            return -1;
        } else if (compareBy == null) {
            return -1;
        } else if (compareTo.size() != compareBy.size()) {
            return -1;
        }

        int sum = 0;
        for (int i = 0; i < compareTo.size(); i++) {
            sum += compare(compareTo.get(i), compareBy.get(i));
        }

        if (sum == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    private int compare(UUID compareTo, UUID compareBy) {
        if (Objects.equals(compareTo, compareBy)) {
            return 0;
        }
        return -1;
    }

    private List<Material> buildMaterials(final ZonedDateTime receivedDateTime, final DocumentTypeRBAC documentTypeRBAC) {
        final Material commandMaterial = this.courtDocument.getMaterials().stream().findFirst().orElse(null);
        final Material material = commandMaterial != null ? (material().withId(commandMaterial
                .getId()).withGenerationStatus(commandMaterial.getGenerationStatus())
                .withName(commandMaterial.getName())
                .withUploadDateTime(commandMaterial.getUploadDateTime())
                .withReceivedDateTime(receivedDateTime)
                .withUserGroups(documentTypeRBAC.getReadUserGroups())
                .build()) : null;

        return singletonList(material);
    }

    public Stream<Object> auditCourtDocument(final UUID userId, final String action, final UUID materialId) {

        final Material material = this.courtDocument.getMaterials().stream()
                .filter(m -> Objects.equals(m.getId(), materialId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No Material Id Found"));


        final CourtDocumentAudit auditData = CourtDocumentAudit.courtDocumentAudit().withAction(action)
                .withCourtDocumentId(this.courtDocument.getCourtDocumentId())
                .withMaterialId(materialId)
                .withMaterialName(material.getName())
                .withDocumentName(this.courtDocument.getName())
                .withUserId(userId)
                .withDocumentTypeId(this.courtDocument.getDocumentTypeId())
                .withActionDateTime(ZonedDateTime.now()).build();
        return apply(Stream.of(auditData));
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(final boolean removed) {
        isRemoved = removed;
    }

}
