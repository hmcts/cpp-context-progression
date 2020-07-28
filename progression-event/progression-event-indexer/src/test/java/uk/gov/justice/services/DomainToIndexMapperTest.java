package uk.gov.justice.services;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class DomainToIndexMapperTest {

    private DomainToIndexMapper domainToIndexMapper;

    @Before
    public void setUp() {
        domainToIndexMapper = new DomainToIndexMapper();
    }

    @Test
    public void shouldFormatCourtProceedingsInitiated() {
        final String expectedCourtProceedingsInitiated = "2020-06-26T07:51:45.330Z";
        final Defendant defendant = createDefendant(expectedCourtProceedingsInitiated);
        final Party party = domainToIndexMapper.party(defendant);
        assertThat(party.getCourtProceedingsInitiated(), is(expectedCourtProceedingsInitiated));
    }

    @Test
    public void shouldFormatCourtProceedingsInitiatedWithZeroMilliSecond() {
        final String expectedCourtProceedingsInitiated = "2020-06-26T07:51:45.000Z";
        final Defendant defendant = createDefendant(expectedCourtProceedingsInitiated);
        final Party party = domainToIndexMapper.party(defendant);
        assertThat(party.getCourtProceedingsInitiated(), is(expectedCourtProceedingsInitiated));
    }

    @Test
    public void shouldFormatCourtProceedingsInitiatedLongerMilliSecondPart() {
        final Defendant defendant = createDefendant("2020-06-26T07:51:45.339999999Z");
        final Party party = domainToIndexMapper.party(defendant);
        assertThat(party.getCourtProceedingsInitiated(), is("2020-06-26T07:51:45.339Z"));
    }

    @Test
    public void shouldFormatCourtProceedingsInitiatedWithoutMilliSecond() {
        final Defendant defendant = createDefendant("2020-06-26T07:51:45Z");
        final Party party = domainToIndexMapper.party(defendant);
        assertThat(party.getCourtProceedingsInitiated(), is("2020-06-26T07:51:45.000Z"));
    }

    @Test
    public void shouldFormatCourtProceedingsInitiatedWithoutSecond() {
        final Defendant defendant = createDefendant("2020-06-26T07:51Z");
        final Party party = domainToIndexMapper.party(defendant);
        assertThat(party.getCourtProceedingsInitiated(), is("2020-06-26T07:51:00.000Z"));
    }

    private Defendant createDefendant(final String courtProceedingsInitiated) {
        return Defendant.defendant()
                .withId(UUID.randomUUID())
                .withCourtProceedingsInitiated(ZonedDateTime.parse(courtProceedingsInitiated))
                .build();
    }
}