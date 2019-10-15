package uk.gov.moj.cpp.progression.query.view;

import static java.time.ZoneId.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;
import uk.gov.moj.cpp.progression.query.DefenceAssociationQueryView;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefenceAssociationQueryViewTest {

    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID ORGANISATION_ID = randomUUID();
    private static final String UTC = "UTC";
    private static final String PRO_BONO = "PRO_BONO";


    @InjectMocks
    private DefenceAssociationQueryView defenceAssociationQueryView;

    @Mock
    private DefenceAssociationRepository defenceAssociationRepository;


    @Test
    public void shouldReturnDefenceAssociation() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(stubbedCurrentDefenceAssociationDefendant());

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getAssociatedOrganisation(stubbedQueryObject());

        //Then
        final JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association, notNullValue());
        assertThat(getValue(association, "organisationId"), equalTo(ORGANISATION_ID.toString()));
        assertThat(getValue(association, "status"), equalTo("Active Barrister/Solicitor of record"));
        assertThat(getValue(association, "representationType"), equalTo(PRO_BONO));
    }

    @Test
    public void shouldReturnEmptyDataWhenNoAssociationExist() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(stubbedEmptyDefenceAssociation());

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getAssociatedOrganisation(stubbedQueryObject());

        //Then
        final JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association.toString(), equalTo("{}"));
    }


    @Test
    public void shouldReturnEmptyDataWhenAssociationIsNull() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(null);

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getAssociatedOrganisation(stubbedQueryObject());

        //Then
        final JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association.toString(), equalTo("{}"));
    }

    @Test
    public void shouldReturnCurrentAssociationGivenExpiredAssociationExist() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(stubbedExpiredAssociationAndCurrentAssociation());

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getAssociatedOrganisation(stubbedQueryObject());

        //Then
        final JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association, notNullValue());
        assertThat(getValue(association, "organisationId"), equalTo(ORGANISATION_ID.toString()));
        assertThat(getValue(association, "status"), equalTo("Active Barrister/Solicitor of record"));
    }

    @Test
    public void shouldReturnEmptyDataWhenOnlyExpiredAssociationEntryExist() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(stubbedOnlyExpiredAssociation());

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getAssociatedOrganisation(stubbedQueryObject());

        //Then
        final JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association.toString(), equalTo("{}"));

    }

    private DefenceAssociationDefendant stubbedEmptyDefenceAssociation() {
        final DefenceAssociationDefendant defenceAssociationDefendant = new DefenceAssociationDefendant();
        defenceAssociationDefendant.setDefendantId(DEFENDANT_ID);
        defenceAssociationDefendant.setDefenceAssociations(new HashSet<>());
        return defenceAssociationDefendant;
    }

    private String getValue(final JsonObject associationsJsonObject, final String key) {
        return associationsJsonObject.getString(key);
    }

    private JsonEnvelope stubbedQueryObject() {
        return JsonEnvelope.envelopeFrom(
                stubbedMetadataBuilder(),
                stubbedQueryObjectPayload());
    }

    private JsonObject stubbedQueryObjectPayload() {
        return Json.createObjectBuilder()
                .add("defendantId", DEFENDANT_ID.toString())
                .build();
    }

    private MetadataBuilder stubbedMetadataBuilder() {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("progression.query.associated-organisation")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(randomUUID().toString());
    }

    private DefenceAssociationDefendant stubbedCurrentDefenceAssociationDefendant() {
        final DefenceAssociationDefendant defenceAssociationDefendant = new DefenceAssociationDefendant();
        defenceAssociationDefendant.setDefendantId(DEFENDANT_ID);
        final DefenceAssociation defenceAssociation = stubbedAssociation(ZonedDateTime.now(of(UTC)), null, defenceAssociationDefendant, USER_ID, ORGANISATION_ID);
        defenceAssociationDefendant.setDefenceAssociations(new HashSet<>());
        defenceAssociationDefendant.getDefenceAssociations().add(defenceAssociation);
        return defenceAssociationDefendant;
    }

    private DefenceAssociationDefendant stubbedOnlyExpiredAssociation() {
        final DefenceAssociationDefendant defenceAssociationDefendant = new DefenceAssociationDefendant();
        defenceAssociationDefendant.setDefendantId(DEFENDANT_ID);
        final DefenceAssociation defenceAssociation = stubbedAssociation(ZonedDateTime.now(of(UTC)), ZonedDateTime.now(of(UTC)), defenceAssociationDefendant, randomUUID(), randomUUID());
        defenceAssociationDefendant.setDefenceAssociations(new HashSet<>());
        defenceAssociationDefendant.getDefenceAssociations().add(defenceAssociation);
        return defenceAssociationDefendant;
    }

    private DefenceAssociationDefendant stubbedExpiredAssociationAndCurrentAssociation() {
        final DefenceAssociationDefendant defenceAssociationDefendant = new DefenceAssociationDefendant();
        defenceAssociationDefendant.setDefendantId(DEFENDANT_ID);
        DefenceAssociation defenceAssociation = stubbedAssociation(ZonedDateTime.now(of(UTC)), ZonedDateTime.now(of(UTC)), defenceAssociationDefendant, randomUUID(), randomUUID());
        defenceAssociationDefendant.setDefenceAssociations(new HashSet<>());
        defenceAssociationDefendant.getDefenceAssociations().add(defenceAssociation);
        defenceAssociation = stubbedAssociation(ZonedDateTime.now(of(UTC)), null, defenceAssociationDefendant, USER_ID, ORGANISATION_ID);
        defenceAssociationDefendant.getDefenceAssociations().add(defenceAssociation);
        assertThat(defenceAssociationDefendant.getDefenceAssociations().size(), equalTo(2));
        return defenceAssociationDefendant;
    }


    private DefenceAssociation stubbedAssociation(final ZonedDateTime startDate,
                                                  final ZonedDateTime endDate,
                                                  final DefenceAssociationDefendant defenceAssociationDefendant,
                                                  final UUID userId,
                                                  final UUID orgId) {
        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setId(randomUUID());
        defenceAssociation.setDefenceAssociationDefendant(defenceAssociationDefendant);
        defenceAssociation.setUserId(userId);
        defenceAssociation.setOrgId(orgId);
        defenceAssociation.setStartDate(startDate);
        defenceAssociation.setEndDate(endDate);
        defenceAssociation.setRepresentationType(PRO_BONO);
        return defenceAssociation;
    }

}
