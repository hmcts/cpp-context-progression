package uk.gov.moj.cpp.progression.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.progression.event.converter.OffenceForDefendantUpdatedToEntity;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.progression.persistence.entity.OffencePlea;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@SuppressWarnings({"WeakerAccess", "squid:S1133"})
@ExtendWith(MockitoExtension.class)
public class OffencesForDefendantUpdatedListenerTest {


    private final UUID defendantId = randomUUID();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final OffenceForDefendantUpdatedToEntity converter = new OffenceForDefendantUpdatedToEntity();

    @Mock
    private DefendantRepository defendantRepository;

    ArgumentCaptor<Defendant> argumentCaptor= ArgumentCaptor.forClass(Defendant.class);


    @InjectMocks
    private OffencesForDefendantUpdatedListener listener;

    private final UUID offence1 = UUID.randomUUID();
    private final UUID offence2 = UUID.randomUUID();
    private final UUID offence3 = UUID.randomUUID();

    @Test
    public void shouldAddOffenceForDefendant() {
        //given
        final JsonEnvelope envelope = getJsonEnvelope(defendantId, offence2, "unique wordings", offence3, "wording 3");
        when(defendantRepository.findBy(defendantId)).thenReturn(new Defendant());
        //when
        listener.updateOffencesForDefendant(envelope);
        //then
        verify(defendantRepository).save(argumentCaptor.capture());
        assertThat(2,is(argumentCaptor.getValue().getOffences().size()));
        assertNull(argumentCaptor.getValue().getOffences().stream().filter(s-> s.getId().equals(offence2)).findFirst().get().getWording(), "wording is null");
        assertThat("wording 3", is(argumentCaptor.getValue().getOffences().stream().filter(s-> s.getId().equals(offence3)).findFirst().get().getWording()));
    }

    @Test
    public void shouldUpdateOffenceForDefendant() {
        final Defendant defendantDetail = getDefendantDetail(offence1, "wording 1", offence2, "wordings-pleas");
        //given
        final JsonEnvelope envelope = getJsonEnvelope(defendantId, offence2, "wordings", offence3, "wording 3");
        when(defendantRepository.findBy(defendantId)).thenReturn(defendantDetail);
        //when
        listener.updateOffencesForDefendant(envelope);
        //then
        verify(defendantRepository).save(argumentCaptor.capture());
        assertThat(2,is(argumentCaptor.getValue().getOffences().size()));
        final List<UUID> ids=argumentCaptor.getValue().getOffences().stream().map(s->s.getId()).collect(Collectors.toList());
        assertThat(ids,hasItem(offence2));
        assertThat(ids,hasItem(offence3));
        assertThat(ids,not(hasItem(offence1)));
        final OffenceDetail offenceDetail=argumentCaptor.getValue().getOffences().stream().filter(s-> s.getId().equals(offence2)).findFirst().get();
        // wordings from persisted offence2 -> i.e. wordings-pleas
        assertThat(null,is(offenceDetail.getWording()));
        assertThat("GUILTY",is(offenceDetail.getOffencePlea().getValue()));
        assertThat(1,is(offenceDetail.getOrderIndex()));
    }


    @Test
    public void shouldDeleteOffenceForDefendant() {
        final Defendant defendantDetail = getDefendantDetail(offence1, "wording 1", offence2, "wordings-pleas");
        //given
        final JsonEnvelope envelope = getJsonEnvelopeForDelete(defendantId, offence2, "wording 1");
        when(defendantRepository.findBy(defendantId)).thenReturn(defendantDetail);
        //when
        listener.updateOffencesForDefendant(envelope);
        //then
        verify(defendantRepository).save(argumentCaptor.capture());
        assertThat(1,is(argumentCaptor.getValue().getOffences().size()));
        final List<UUID> ids=argumentCaptor.getValue().getOffences().stream().map(s->s.getId()).collect(Collectors.toList());
        assertThat(ids,hasItem(offence2));
        assertThat(ids,not(hasItem(offence1)));
        final OffenceDetail offenceDetail=argumentCaptor.getValue().getOffences().stream().filter(s-> s.getId().equals(offence2)).findFirst().get();
        assertThat(null,is(offenceDetail.getWording()));
        assertThat("GUILTY",is(offenceDetail.getOffencePlea().getValue()));
        assertThat(1,is(offenceDetail.getOrderIndex()));
    }

    private Defendant getDefendantDetail(final UUID id1, final String word1, final UUID id2, final String word2) {
        final OffenceDetail offenceDetail1 = new OffenceDetail();
        offenceDetail1.setId(id1);
        offenceDetail1.setWording(word1);
        offenceDetail1.setOffencePlea(getOffencePlea());
        offenceDetail1.setOrderIndex(1);
        offenceDetail1.setCount(1);
        final OffenceDetail offenceDetail2 = new OffenceDetail();
        offenceDetail2.setId(id2);
        offenceDetail2.setWording(word2);
        offenceDetail2.setOffencePlea(getOffencePlea());
        offenceDetail2.setOrderIndex(2);
        offenceDetail2.setCount(2);
        final Defendant defendantDetail = new Defendant();
        defendantDetail.addOffence(offenceDetail1);
        defendantDetail.addOffence(offenceDetail2);
        return defendantDetail;
    }

    private OffencePlea getOffencePlea() {
        return new OffencePlea(UUID.randomUUID(),"GUILTY", LocalDate.now());
    }

    private JsonEnvelope getJsonEnvelope(final UUID defendantId, final UUID id1, final String word1, final UUID id2, final String word2) {
        final JsonObject pleaJson = createObjectBuilder().add("id", id1.toString()).add("pleaDate","2010-08-01").add("value","GUILTY").build();
        final JsonObject jsonObject1 = createObjectBuilder().add("id", id1.toString()).add("startDate", "2010-08-01").add("endDate", "2011-08-01").add("offenceCode", "H8198").add("offencePlea", pleaJson).add("section", "Section 51").add("orderIndex",1).add("count",1).build();
        final JsonObject jsonObject2 = createObjectBuilder().add("id", id2.toString()).add("wording", word2).add("startDate", "2010-08-01").add("endDate", "2011-08-01").add("offenceCode", "H8198").add("offencePlea", pleaJson).add("section", "Section 51").add("orderIndex",2).add("count",1).build();
        return EnvelopeFactory.createEnvelope("name", Json.createObjectBuilder().add("caseId", defendantId.toString()).add("defendantId", defendantId.toString()).add("offences", Json.createArrayBuilder().add(jsonObject1).add(jsonObject2).build()).build());
    }

    private JsonEnvelope getJsonEnvelopeForDelete(final UUID defendantId, final UUID id1, final String word1) {
        final JsonObject pleaJson = createObjectBuilder().add("id", id1.toString()).add("pleaDate","2010-08-01").add("value","GUILTY").build();
        final JsonObject jsonObject1 = createObjectBuilder().add("id", id1.toString()).add("startDate", "2010-08-01").add("endDate", "2011-08-01").add("offenceCode", "H8198").add("offencePlea", pleaJson).add("section", "Section 51").add("orderIndex",1).add("count",1).build();
        return EnvelopeFactory.createEnvelope("name", Json.createObjectBuilder().add("caseId", defendantId.toString()).add("defendantId", defendantId.toString()).add("offences", Json.createArrayBuilder().add(jsonObject1)).build());
    }

}
