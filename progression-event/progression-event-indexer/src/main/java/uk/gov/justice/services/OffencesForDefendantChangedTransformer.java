package uk.gov.justice.services;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.courts.AddedOffences;
import uk.gov.justice.progression.courts.DeletedOffences;
import uk.gov.justice.progression.courts.OffencesForDefendantChanged;
import uk.gov.justice.progression.courts.UpdatedOffences;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.unifiedsearch.client.domain.CaseDetails;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.bazaarvoice.jolt.Transform;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OffencesForDefendantChangedTransformer implements Transform {

    public static final String PROSECUTION = "PROSECUTION";
    List<Defendant> defendantList = new ArrayList<>();

    private DomainToIndexMapper domainToIndexMapper = new DomainToIndexMapper();
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Override
    public Object transform(final Object input) {
        final JsonObject jsonObject = new ObjectToJsonObjectConverter(objectMapper).convert(input);

        final OffencesForDefendantChanged offencesForDefendantChanged =
                new JsonObjectToObjectConverter(objectMapper).convert(jsonObject, OffencesForDefendantChanged.class);
        final List<AddedOffences> addedOffences = offencesForDefendantChanged.getAddedOffences();
        final List<UpdatedOffences> updatedOffences = offencesForDefendantChanged.getUpdatedOffences();
        final List<DeletedOffences> deletedOffences = offencesForDefendantChanged.getDeletedOffences();
        final CaseDetails caseDetails = new CaseDetails();

        defendantList = getDefendantListByOffences(addedOffences, updatedOffences, deletedOffences);
        final List<Party> parties = new ArrayList<>();
        for (final Defendant defendant : defendantList) {
            parties.add(domainToIndexMapper.party(defendant));
        }

        caseDetails.setCaseId(defendantList.get(0).getProsecutionCaseId().toString());
        caseDetails.set_case_type(PROSECUTION);
        caseDetails.setParties(parties);
        return caseDetails;
    }

    private Optional<Defendant> getSameDefendant(final UUID defendantId) {
        return defendantList.stream().filter(defendant -> defendant.getId().equals(defendantId)).findFirst();
    }

    private Defendant addOffenceToDefendant(final Defendant defendant, final List<Offence> offences) {
        final List<Offence> newOffenceList = defendant.getOffences();
        newOffenceList.addAll(offences);
        return Defendant.defendant().withValuesFrom(defendant).withOffences(newOffenceList).build();
    }


    private List<Defendant> getDefendantListByOffences(List<AddedOffences> addedOffences, List<UpdatedOffences> updatedOffences, List<DeletedOffences> deletedOffences) {

        if (updatedOffences != null) {
            updatedOffences.stream().forEach(offence -> addOrCreateDefendant(offence.getDefendantId(), offence.getProsecutionCaseId(), offence.getOffences()));
        }


        if (addedOffences != null) {
            addedOffences.stream().forEach(offence -> addOrCreateDefendant(offence.getDefendantId(), offence.getProsecutionCaseId(), offence.getOffences()));
        }


        if (deletedOffences != null && !deletedOffences.isEmpty()) {
            final List<Offence> offencesToDelete = new ArrayList<>();
            deletedOffences.stream()
                    .flatMap(deletedOffence -> deletedOffence.getOffences().stream())
                    .distinct()
                    .map(id -> Offence.offence().withId(id).build())
                    .forEach(offencesToDelete::add);

            addOrCreateDefendant(deletedOffences.get(0).getDefendantId(), deletedOffences.get(0).getProsecutionCaseId(), offencesToDelete);
        }

        return defendantList;
    }

    private void addOrCreateDefendant(final UUID defendantId, final UUID prosecutionCaseId, List<Offence> offences) {
        final Optional<Defendant> defendant = getSameDefendant(defendantId);
        if (defendant.isPresent()) {
            addOffenceToDefendant(defendant.get(), offences);
        } else {
            defendantList.add(Defendant.defendant().withProsecutionCaseId(prosecutionCaseId).withId(defendantId).withOffences(offences).build());
        }
    }
}
