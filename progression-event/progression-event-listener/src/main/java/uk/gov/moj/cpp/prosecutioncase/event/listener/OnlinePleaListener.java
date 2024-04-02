package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.OnlinePleaAllocationAdded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.progression.events.OnlinePleaPcqVisitedRecorded;
import uk.gov.moj.cpp.progression.events.OnlinePleaRecorded;
import uk.gov.justice.progression.event.OpaPressListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaPublicListNoticeDeactivated;
import uk.gov.justice.progression.event.OpaResultListNoticeDeactivated;
import uk.gov.moj.cpp.progression.plea.json.schemas.LegalEntityDefendant;
import uk.gov.moj.cpp.progression.plea.json.schemas.Offence;
import uk.gov.moj.cpp.progression.plea.json.schemas.PersonalDetails;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.Address;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.OnlinePleaLegalEntityDetails;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.OnlinePleaPersonalDetails;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PressListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PublicListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ResultListOpaNotice;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PressListOpaNoticeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PublicListOpaNoticeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ResultListOpaNoticeRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class OnlinePleaListener {

    private static final String OUTGOING_PROMPT_DATE_FORMAT = "yyyy-MM-dd";
    private static final Logger LOGGER = getLogger(OnlinePleaListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private OnlinePleaRepository pleaDetailsOnlinePleaRepository;

    @Inject
    private PublicListOpaNoticeRepository publicListOpaNoticeRepository;

    @Inject
    private PressListOpaNoticeRepository pressListOpaNoticeRepository;

    @Inject
    private ResultListOpaNoticeRepository resultListOpaNoticeRepository;

    @Inject
    private Clock clock;


    @Handles("progression.event.online-plea-recorded")
    public void onlinePleaRecorded(final Envelope<OnlinePleaRecorded> event) {
        final OnlinePleaRecorded onlinePleaRecorded = event.payload();
        LOGGER.info("progression.event.online-plea-recorded event received");

        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(onlinePleaRecorded.getCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final ZonedDateTime pleaDateTime = event.metadata().createdAt().orElse(now());
        final OnlinePlea onlinePlea = buildOnlinePlea(getDefendant(prosecutionCase.getDefendants(), onlinePleaRecorded.getPleadOnline().getDefendantId()), onlinePleaRecorded.getPleadOnline(), pleaDateTime);
        pleaDetailsOnlinePleaRepository.save(onlinePlea);

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(getUpdatedDefendants(prosecutionCase.getDefendants(), onlinePleaRecorded.getPleadOnline().getDefendantId(), onlinePleaRecorded.getPleadOnline().getOffences()))
                .build();
        prosecutionCaseRepository.save(getProsecutionCaseEntity(updatedProsecutionCase));

    }

    @Handles("progression.event.online-plea-pcq-visited-recorded")
    public void onlinePleaPcqVisitedRecorded(final Envelope<OnlinePleaPcqVisitedRecorded> event) {
        final OnlinePleaPcqVisitedRecorded onlinePleaPcqVisitedRecorded = event.payload();
        LOGGER.info("progression.event.online-plea-pcq-visited-recorded event received");

        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(onlinePleaPcqVisitedRecorded.getCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withDefendants(getUpdatedDefendants(prosecutionCase.getDefendants(),
                        onlinePleaPcqVisitedRecorded.getPleadOnlinePcqVisited().getDefendantId(),
                        nonNull(onlinePleaPcqVisitedRecorded.getPleadOnlinePcqVisited().getPcqId())
                                ? onlinePleaPcqVisitedRecorded.getPleadOnlinePcqVisited().getPcqId().toString() : null
                )).build();

        prosecutionCaseRepository.save(getProsecutionCaseEntity(updatedProsecutionCase));
    }

    @Handles("progression.event.online-plea-allocation-added")
    public void onlinePleaAllocationAdded(final Envelope<OnlinePleaAllocationAdded> event) {
        final OnlinePleaAllocationAdded pleaAllocationAdded = event.payload();
        LOGGER.info("Received progression.event.online-plea-allocation-added event for caseId {}, hearingId {} defendantId {}",
                pleaAllocationAdded.getCaseId(), pleaAllocationAdded.getHearingId(), pleaAllocationAdded.getDefendantId());

        publicListOpaNoticeRepository.save(getPublicListOpaNotice(pleaAllocationAdded));
        pressListOpaNoticeRepository.save(getPressListOpaNotice(pleaAllocationAdded));
        resultListOpaNoticeRepository.save(getResultListOpaNotice(pleaAllocationAdded));
    }

    @Handles("progression.event.opa-public-list-notice-deactivated")
    public void deactivateOpaPublicListNotice(final Envelope<OpaPublicListNoticeDeactivated> event) {
        final OpaPublicListNoticeDeactivated deactivatedAllocation = event.payload();

        LOGGER.info("Received progression.event.opa-public-list-notice-deactivated event for defendantId {}", deactivatedAllocation.getDefendantId());

        publicListOpaNoticeRepository.deleteByDefendantId(deactivatedAllocation.getDefendantId());
    }

    @Handles("progression.event.opa-press-list-notice-deactivated")
    public void deactivateOpaPressListNotice(final Envelope<OpaPressListNoticeDeactivated> event) {
        final OpaPressListNoticeDeactivated deactivatedAllocation = event.payload();

        LOGGER.info("Received progression.event.opa-press-list-notice-deactivated event for defendantId {}", deactivatedAllocation.getDefendantId());

        pressListOpaNoticeRepository.deleteByDefendantId(deactivatedAllocation.getDefendantId());
    }

    @Handles("progression.event.opa-result-list-notice-deactivated")
    public void deactivateOpaResultListNotice(final Envelope<OpaResultListNoticeDeactivated> event) {
        final OpaResultListNoticeDeactivated deactivatedAllocation = event.payload();

        LOGGER.info("Received progression.event.opa-result-list-notice-deactivated event for defendantId {}", deactivatedAllocation.getDefendantId());

        resultListOpaNoticeRepository.deleteByDefendantId(deactivatedAllocation.getDefendantId());
    }

    private PublicListOpaNotice getPublicListOpaNotice(final OnlinePleaAllocationAdded pleaAllocationAdded) {
        return new PublicListOpaNotice(pleaAllocationAdded.getCaseId(),
                pleaAllocationAdded.getDefendantId(),
                pleaAllocationAdded.getHearingId());
    }

    private PressListOpaNotice getPressListOpaNotice(final OnlinePleaAllocationAdded pleaAllocationAdded) {
        return new PressListOpaNotice(pleaAllocationAdded.getCaseId(),
                pleaAllocationAdded.getDefendantId(),
                pleaAllocationAdded.getHearingId());
    }

    private ResultListOpaNotice getResultListOpaNotice(final OnlinePleaAllocationAdded pleaAllocationAdded) {
        return new ResultListOpaNotice(pleaAllocationAdded.getCaseId(),
                pleaAllocationAdded.getDefendantId(),
                pleaAllocationAdded.getHearingId());
    }

    private List<Defendant> getUpdatedDefendants(final List<Defendant> allDefendants, final UUID defendantId, final List<Offence> pleadOffences) {
        return allDefendants.stream()
                .map(defendant -> defendant.getId().equals(defendantId) ? getUpdatedDefendant(defendant, pleadOffences) : defendant)
                .collect(Collectors.toList());
    }

    private List<Defendant> getUpdatedDefendants(final List<Defendant> allDefendants, final UUID defendantId, final String pcqId) {
        return allDefendants.stream()
                .map(defendant -> defendant.getId().equals(defendantId) ? getUpdatedDefendant(defendant, pcqId) : defendant)
                .collect(Collectors.toList());
    }

    private Defendant getUpdatedDefendant(final Defendant defendant, final List<Offence> pleadOffences) {
        return Defendant.defendant()
                .withValuesFrom(defendant)
                .withOffences(getUpdatedOffences(defendant.getOffences(), pleadOffences))
                .build();
    }

    private Defendant getUpdatedDefendant(final Defendant defendant, final String pcqId) {
        return Defendant.defendant()
                .withValuesFrom(defendant)
                .withPcqId(pcqId)
                .build();
    }

    private List<uk.gov.justice.core.courts.Offence> getUpdatedOffences(List<uk.gov.justice.core.courts.Offence> existingOffences, final List<Offence> pleadOffences) {
        return existingOffences.stream()
                .map(existingOffence -> isOffencePlead(existingOffence, pleadOffences) ? getUpdatedOffence(existingOffence) : existingOffence)
                .collect(Collectors.toList());
    }

    private uk.gov.justice.core.courts.Offence getUpdatedOffence(final uk.gov.justice.core.courts.Offence existingOffence) {
        return uk.gov.justice.core.courts.Offence.offence()
                .withValuesFrom(existingOffence)
                .withOnlinePleaReceived(true)
                .build();
    }

    private boolean isOffencePlead(final uk.gov.justice.core.courts.Offence existingOffence, final List<Offence> pleadOffences) {
        return pleadOffences.stream().anyMatch(offence -> offence.getId().equals(existingOffence.getId().toString()));
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private Defendant getDefendant(final List<Defendant> defendants, final UUID defendantId) {
        return defendants.stream()
                .filter(defendant -> defendant.getId().equals(defendantId))
                .findAny().orElseThrow(IllegalStateException::new);
    }

    private OnlinePlea buildOnlinePlea(final Defendant defendantDetail, final PleadOnline newData, final ZonedDateTime pleaDateTime) {
        final OnlinePlea newOnlinePlea = new OnlinePlea(defendantDetail, newData.getDisabilityNeeds(), pleaDateTime, newData.getLegalEntityFinancialMeans());
        final OnlinePleaPersonalDetails personalDetails = newOnlinePlea.getPersonalDetails();
        final OnlinePleaLegalEntityDetails onlinePleaLegalEntityDetails = newOnlinePlea.getLegalEntityDetails();
        final PersonalDetails newPersonalDetails = newData.getPersonalDetails();
        if (nonNull(newPersonalDetails)) {
            ofNullable(newPersonalDetails.getFirstName()).ifPresent(personalDetails::setFirstName);
            ofNullable(newPersonalDetails.getLastName()).ifPresent(personalDetails::setLastName);
            ofNullable(newPersonalDetails.getNationalInsuranceNumber()).ifPresent(personalDetails::setNationalInsuranceNumber);
            ofNullable(newPersonalDetails.getDateOfBirth()).ifPresent(dateOfBirth -> personalDetails.setDateOfBirth(convertToLocalDate(dateOfBirth)));
            ofNullable(newPersonalDetails.getAddress()).map(OnlinePleaListener::convertToEntity).ifPresent(personalDetails::setAddress);
            ofNullable(newPersonalDetails.getContactDetails()).ifPresent(contactDetails -> {
                personalDetails.setHomeTelephone(contactDetails.getHome());
                personalDetails.setMobile(contactDetails.getMobile());
                personalDetails.setEmail(contactDetails.getEmail());
            });
            ofNullable(newPersonalDetails.getDriverNumber()).ifPresent(personalDetails::setDriverNumber);
            ofNullable(newPersonalDetails.getDriverLicenceDetails()).ifPresent(personalDetails::setDriverLicenceDetails);
        }
        final LegalEntityDefendant newLegalEntityDefendant = newData.getLegalEntityDefendant();
        if (nonNull(newLegalEntityDefendant)) {
            ofNullable(newLegalEntityDefendant.getName()).ifPresent(onlinePleaLegalEntityDetails::setName);
            ofNullable(newLegalEntityDefendant.getIncorporationNumber()).ifPresent(onlinePleaLegalEntityDetails::setIncorporationNumber);
            ofNullable(newLegalEntityDefendant.getPosition()).ifPresent(position -> onlinePleaLegalEntityDetails.setPosition(position.toString()));
            ofNullable(newLegalEntityDefendant.getAddress()).map(OnlinePleaListener::convertToEntity).ifPresent(onlinePleaLegalEntityDetails::setAddress);
            ofNullable(newLegalEntityDefendant.getContactDetails()).ifPresent(contactDetails -> {
                onlinePleaLegalEntityDetails.setHomeTelephone(contactDetails.getHome());
                onlinePleaLegalEntityDetails.setWorkTelephone(contactDetails.getBusiness());
                onlinePleaLegalEntityDetails.setMobile(contactDetails.getMobile());
                onlinePleaLegalEntityDetails.setEmail(contactDetails.getEmail());
            });
        }

        return newOnlinePlea;
    }

    private static Address convertToEntity(final uk.gov.justice.core.courts.Address address) {
        return new Address(address.getAddress1(), address.getAddress2(), address.getAddress3(), address.getAddress4(), address.getAddress5(), address.getPostcode());
    }

    @SuppressWarnings("squid:S00112")
    protected LocalDate convertToLocalDate(final String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(OUTGOING_PROMPT_DATE_FORMAT));
        } catch (DateTimeParseException parseException) {
            throw new RuntimeException(String.format("invalid format for incoming date prompt value: %s", value), parseException);
        }
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }
}
