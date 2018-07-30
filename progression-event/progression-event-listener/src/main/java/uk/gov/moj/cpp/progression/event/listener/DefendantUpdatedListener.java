package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.*;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantBailDocument;
import uk.gov.moj.cpp.progression.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class DefendantUpdatedListener {

    public static final String UNCONDITIONAL = "unconditional";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseProgressionDetailRepository caseRepository;

    @Handles("progression.events.defendant-updated")
    @Transactional
    public void defendantUpdated(final JsonEnvelope envelope) {
        DefendantUpdated event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), DefendantUpdated.class);

        CaseProgressionDetail caseDetail = caseRepository.findBy(event.getCaseId());
        Defendant defendant = caseDetail.getDefendant(event.getDefendantId());

        if(event.getBailStatus() !=null ) {
            defendant.setBailStatus(event.getBailStatus());
        }
        if(event.getCustodyTimeLimitDate() != null) {
            defendant.setCustodyTimeLimitDate(event.getCustodyTimeLimitDate());
        }
        if(event.getBailDocument() != null) {
            updatedActiveDocument(event.getBailDocument(), defendant);
        }
        if(event.getPerson() != null){
            updatePerson(event.getPerson(), defendant);
        }
        if (event.getInterpreter() != null) {
            defendant.setInterpreter(new InterpreterDetail(event.getInterpreter().getNeeded(),
                    event.getInterpreter().getLanguage()));
        }
        if(event.getDefenceSolicitorFirm() != null){
            defendant.setDefenceSolicitorFirm(event.getDefenceSolicitorFirm());
        }
    }


    private void updatedActiveDocument(BailDocument bailDocument, Defendant defendant) {
        DefendantBailDocument defendantBailDocument = new DefendantBailDocument();
        defendantBailDocument.setDocumentId(bailDocument.getMaterialId());
        defendantBailDocument.setId(bailDocument.getId());
        defendantBailDocument.setActive(Boolean.TRUE);
        defendant.addDefendantBailDocument(defendantBailDocument);
    }

    private void updatePerson(Person person, Defendant defendant){
        if(defendant.getPerson() == null){
            uk.gov.moj.cpp.progression.persistence.entity.Person personEntity = new uk.gov.moj.cpp.progression.persistence.entity.Person();
            personEntity.setPersonId(person.getId());
            defendant.setPerson(personEntity);
        }
        defendant.getPerson().setTitle(person.getTitle());
        defendant.getPerson().setFirstName(person.getFirstName());
        defendant.getPerson().setLastName(person.getLastName());
        defendant.getPerson().setDateOfBirth(person.getDateOfBirth());
        defendant.getPerson().setNationality(person.getNationality());
        defendant.getPerson().setGender(person.getGender());
        defendant.getPerson().setHomeTelephone(person.getHomeTelephone());
        defendant.getPerson().setWorkTelephone(person.getWorkTelephone());
        defendant.getPerson().setMobile(person.getMobile());
        defendant.getPerson().setEmail(person.getEmail());
        defendant.getPerson().setFax(person.getFax());
        if(person.getAddress() != null) {
            updateAddress(defendant.getPerson(), person.getAddress());
        }
    }

    private void updateAddress(final uk.gov.moj.cpp.progression.persistence.entity.Person person, final Address address) {

        if(person.getAddress() == null){
            uk.gov.moj.cpp.progression.persistence.entity.Address addressEntity = new uk.gov.moj.cpp.progression.persistence.entity.Address();
            person.setAddress(addressEntity);
        }
        person.getAddress().setAddress1(address.getAddress1());
        person.getAddress().setAddress2(address.getAddress2());
        person.getAddress().setAddress3(address.getAddress3());
        person.getAddress().setAddress4(address.getAddress4());
        person.getAddress().setPostCode(address.getPostCode());
    }
}
