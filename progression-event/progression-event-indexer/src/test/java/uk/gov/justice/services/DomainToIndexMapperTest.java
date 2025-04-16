package uk.gov.justice.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.justice.services.DomainToIndexMapper.addressLines;
import static java.util.Arrays.asList;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.Verdict;
import uk.gov.justice.core.courts.VerdictType;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DomainToIndexMapperTest {

    private DomainToIndexMapper domainToIndexMapper;

    @BeforeEach
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

    @Test
    public void shouldFormatAddressLines() {
        final String actualAddressLines = addressLines(Address.address().withAddress1("address1").withAddress3("address5").build());
        assertThat(actualAddressLines, is("address1 address5"));
    }

    @Test
    public void shouldFormatAddressLinesRemovingExtraSpace() {
        final String actualAddressLines = addressLines(Address.address().withAddress1("  address1 ").withAddress3("  address5  ").build());
        assertThat(actualAddressLines, is("address1 address5"));
    }

    @Test
    public void shouldFormatAddressLinesWithoutBlankSpace() {
        final String actualAddressLines = addressLines(Address.address().withAddress1("  ").withAddress3("  ").build());
        assertThat(actualAddressLines, is(""));
    }

    @Test
    public void shouldFormatAddressLinesToEmptyString() {
        final String actualAddressLines = addressLines(Address.address().build());
        assertThat(actualAddressLines, is(""));
    }

    @Test
    public void shouldConverVerdictWhenThereAreOnlyMandatoryFields() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withVerdict(Verdict.verdict()
                                .withOffenceId(UUID.randomUUID())
                                .withVerdictDate(LocalDate.now())
                                .withVerdictType(VerdictType.verdictType()
                                        .withId(UUID.randomUUID())
                                        .withCategory("Category")
                                        .withCategoryType("CategoryType")
                                        .build())
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getVerdictTypeId(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getId().toString()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getCategory(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getCategory()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getCategoryType(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getCategoryType()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getSequence(), is(0));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictDate(), is(defendant.getOffences().get(0).getVerdict().getVerdictDate().toString()));
    }

    @Test
    public void shouldConverVerdictWhenThereAreAllFields() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withVerdict(Verdict.verdict()
                                .withOffenceId(UUID.randomUUID())
                                .withVerdictDate(LocalDate.now())
                                .withOriginatingHearingId(UUID.randomUUID())
                                .withVerdictType(VerdictType.verdictType()
                                        .withId(UUID.randomUUID())
                                        .withCategory("Category")
                                        .withCategoryType("CategoryType")
                                        .withDescription("Description")
                                        .withSequence(1)
                                        .build())
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getVerdictTypeId(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getId().toString()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getCategory(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getCategory()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getCategoryType(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getCategoryType()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getDescription(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getDescription()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictType().getSequence(), is(defendant.getOffences().get(0).getVerdict().getVerdictType().getSequence()));
        assertThat(party.getOffences().get(0).getVerdict().getVerdictDate(), is(defendant.getOffences().get(0).getVerdict().getVerdictDate().toString()));
        assertThat(party.getOffences().get(0).getVerdict().getOriginatingHearingId(), is(defendant.getOffences().get(0).getVerdict().getOriginatingHearingId().toString()));
    }

    @Test
    public void shouldConvertPlea() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withPlea(Plea.plea()
                                .withOriginatingHearingId(UUID.randomUUID())
                                .withPleaValue("GUILTY")
                                .withPleaDate(LocalDate.now())
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertThat(party.getOffences().get(0).getPleas().get(0).getOriginatingHearingId(), is(defendant.getOffences().get(0).getPlea().getOriginatingHearingId().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaValue(), is(defendant.getOffences().get(0).getPlea().getPleaValue().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaDate(), is(defendant.getOffences().get(0).getPlea().getPleaDate().toString()));
    }

    @Test
    public void shouldConvertIndicatedGuiltyPleaToActualGuiltyPleaIfActualGuiltyPleaIsNotAvailable() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                .withIndicatedPleaDate(LocalDate.now())
                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                .withOriginatingHearingId(UUID.randomUUID())
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertThat(party.getOffences().get(0).getPleas().get(0).getOriginatingHearingId(), is(defendant.getOffences().get(0).getIndicatedPlea().getOriginatingHearingId().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaValue(), is("INDICATED_GUILTY"));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaDate(), is(defendant.getOffences().get(0).getIndicatedPlea().getIndicatedPleaDate().toString()));
    }

    @Test
    public void shouldConvertIndicatedGuiltyPleaToActualGuiltyPleaIfDefendantHasOldIndicatedPlea() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(asList(Offence.offence()
                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                .withIndicatedPleaDate(LocalDate.now())
                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                .withOriginatingHearingId(UUID.randomUUID())
                                .build())
                        .build(), Offence.offence()
                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                .withIndicatedPleaDate(LocalDate.now())
                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertThat(party.getOffences().get(0).getPleas().get(0).getOriginatingHearingId(), is(defendant.getOffences().get(0).getIndicatedPlea().getOriginatingHearingId().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaValue(), is("INDICATED_GUILTY"));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaDate(), is(defendant.getOffences().get(0).getIndicatedPlea().getIndicatedPleaDate().toString()));
    }

    @Test
    public void shouldIgnoreIndicatedNotGuiltyPleaIfActualPleaIsPresent() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                .withIndicatedPleaDate(LocalDate.now())
                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_NOT_GUILTY)
                                .withOriginatingHearingId(UUID.randomUUID())
                                .build())
                        .withPlea(Plea.plea()
                                .withOriginatingHearingId(UUID.randomUUID())
                                .withPleaValue("GUILTY")
                                .withPleaDate(LocalDate.now())
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertThat(party.getOffences().get(0).getPleas().get(0).getOriginatingHearingId(), is(defendant.getOffences().get(0).getPlea().getOriginatingHearingId().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaValue(), is(defendant.getOffences().get(0).getPlea().getPleaValue().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaDate(), is(defendant.getOffences().get(0).getPlea().getPleaDate().toString()));
    }

    @Test
    public void shouldIgnoreIndicatedGuiltyPleaIfActualPleaIsPresent() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                .withIndicatedPleaDate(LocalDate.now())
                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                .withOriginatingHearingId(UUID.randomUUID())
                                .build())
                        .withPlea(Plea.plea()
                                .withOriginatingHearingId(UUID.randomUUID())
                                .withPleaValue("NOT_GUILTY")
                                .withPleaDate(LocalDate.now())
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertThat(party.getOffences().get(0).getPleas().get(0).getOriginatingHearingId(), is(defendant.getOffences().get(0).getPlea().getOriginatingHearingId().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaValue(), is(defendant.getOffences().get(0).getPlea().getPleaValue().toString()));
        assertThat(party.getOffences().get(0).getPleas().get(0).getPleaDate(), is(defendant.getOffences().get(0).getPlea().getPleaDate().toString()));
    }

    @Test
    public void shouldIgnoreIndicatedGuiltyPleaIfOriginatingHearingIdIsNotPresent() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                .withIndicatedPleaDate(LocalDate.now())
                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertNull(party.getOffences().get(0).getPleas());
    }

    @Test
    public void shouldIgnoreIndicatedGuiltyPleaIfPleaDateIsNotPresent() {
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.randomUUID())
                .withOffences(Collections.singletonList(Offence.offence()
                        .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                                .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                                .withOriginatingHearingId(UUID.randomUUID())
                                .build())
                        .build()))
                .build();

        final Party party = domainToIndexMapper.party(defendant);

        assertNull(party.getOffences().get(0).getPleas());
    }


    private Defendant createDefendant(final String courtProceedingsInitiated) {
        return Defendant.defendant()
                .withId(UUID.randomUUID())
                .withCourtProceedingsInitiated(ZonedDateTime.parse(courtProceedingsInitiated))
                .build();
    }
}
