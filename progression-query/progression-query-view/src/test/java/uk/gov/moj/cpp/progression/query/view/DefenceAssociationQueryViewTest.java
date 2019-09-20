package uk.gov.moj.cpp.progression.query.view;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationHistory;
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

    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANISATION_ID = UUID.randomUUID();

    @InjectMocks
    private DefenceAssociationQueryView defenceAssociationQueryView;

    @Mock
    private DefenceAssociationRepository defenceAssociationRepository;


    @Test
    public void shouldReturnDefenceAssociationHistory() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(stubbedCurrentDefenceAssociation());

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getDefendantRequest(stubbedQueryObject());

        //Then
        JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association, notNullValue());
        assertThat(getValue(association, "organisationId"), equalTo(ORGANISATION_ID.toString()));
        assertThat(getValue(association, "status"), equalTo("ASSOCIATED"));
    }

    @Test
    public void shouldReturnEmptyDataWhenNoAssociationExist() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(stubbedEmptyDefenceAssociationHistory());

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getDefendantRequest(stubbedQueryObject());

        //Then
        JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association, notNullValue());
        assertThat(getValue(association, "organisationId"), equalTo(""));
        assertThat(getValue(association, "status"), equalTo(""));

    }

    @Test
    public void shouldReturnCurrentAssociationGivenExpiredAssociationExist() {

        //Given
        when(defenceAssociationRepository.findByDefendantId(DEFENDANT_ID)).thenReturn(stubbedExpiredAssociation());

        //When
        final JsonEnvelope defenceAssociationResponse = defenceAssociationQueryView.getDefendantRequest(stubbedQueryObject());

        //Then
        JsonObject association = defenceAssociationResponse.payloadAsJsonObject().getJsonObject("association");
        assertThat(association, notNullValue());
        assertThat(getValue(association, "organisationId"), equalTo(ORGANISATION_ID.toString()));
        assertThat(getValue(association, "status"), equalTo("ASSOCIATED"));
    }

    private DefenceAssociation stubbedEmptyDefenceAssociationHistory() {
        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setDefendantId(DEFENDANT_ID);
        final DefenceAssociationHistory defenceAssociationHistory = new DefenceAssociationHistory();
        defenceAssociation.setDefenceAssociationHistories(new HashSet<>());
        return defenceAssociation;
    }

    private String getValue(final JsonObject associationsJsonObject, String key) {
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

    private DefenceAssociation stubbedCurrentDefenceAssociation() {
        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setDefendantId(DEFENDANT_ID);
        final DefenceAssociationHistory defenceAssociationHistory = stubbedAssociation(ZonedDateTime.now(), null, defenceAssociation, USER_ID, ORGANISATION_ID);
        defenceAssociation.setDefenceAssociationHistories(new HashSet<>());
        defenceAssociation.getDefenceAssociationHistories().add(defenceAssociationHistory);
        return defenceAssociation;
    }

    private DefenceAssociation stubbedExpiredAssociation() {
        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setDefendantId(DEFENDANT_ID);
        DefenceAssociationHistory defenceAssociationHistory = stubbedAssociation(ZonedDateTime.now(), ZonedDateTime.now(), defenceAssociation, randomUUID(), randomUUID());
        defenceAssociation.setDefenceAssociationHistories(new HashSet<>());
        defenceAssociation.getDefenceAssociationHistories().add(defenceAssociationHistory);
        defenceAssociationHistory = stubbedAssociation(ZonedDateTime.now(), null, defenceAssociation, USER_ID, ORGANISATION_ID);
        defenceAssociation.getDefenceAssociationHistories().add(defenceAssociationHistory);
        assertThat(defenceAssociation.getDefenceAssociationHistories().size(), equalTo(2));
        return defenceAssociation;
    }

    private DefenceAssociationHistory stubbedAssociation(final ZonedDateTime startDate, final ZonedDateTime endDate,
                                                         final DefenceAssociation defenceAssociation,
                                                         final UUID grantorUserId,
                                                         final UUID grantorOrgId) {

        final DefenceAssociationHistory defenceAssociationHistory = new DefenceAssociationHistory();
        defenceAssociationHistory.setId(UUID.randomUUID());
        defenceAssociationHistory.setDefenceAssociation(defenceAssociation);
        defenceAssociationHistory.setGrantorUserId(grantorUserId);
        defenceAssociationHistory.setGrantorOrgId(grantorOrgId);
        defenceAssociationHistory.setStartDate(startDate);
        defenceAssociationHistory.setEndDate(endDate);
        defenceAssociationHistory.setAgentFlag(false);
        return defenceAssociationHistory;
    }

}
