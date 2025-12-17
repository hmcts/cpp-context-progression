package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.core.courts.Defendant.defendant;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.moj.cpp.progression.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.utils.FileUtil.jsonFromString;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.moj.cpp.progression.domain.pojo.PrisonCustodySuite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class CustodialEstablishmentUpdateHelperTest {

    private CustodialEstablishmentUpdateHelper custodialEstablishmentUpdateHelper = new CustodialEstablishmentUpdateHelper();

    @Test
    public void shouldReturnNoDefendantWithCustodialEstablishmentToUpdateWhenDefendantsWithNullOffences() {
        final UUID defendant1 = randomUUID();
        final List<PrisonCustodySuite> prisonCustodySuites = emptyList();

        final Defendant defendant = defendant().withId(defendant1).build();

        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(false));
    }

    @Test
    public void shouldReturnNoDefendantWithCustodialEstablishmentToUpdateWhenDefendantsWithOffencesEmpty() {
        final UUID defendant1 = randomUUID();
        final List<PrisonCustodySuite> prisonCustodySuites = emptyList();

        final Defendant defendant = defendant().withId(defendant1).withOffences(emptyList()).build();

        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(false));
    }

    @Test
    public void shouldReturnNoDefendantWithCustodialEstablishmentToUpdateWhenDefendantsWithOffenceJRsEmpty() {
        final UUID defendant1 = randomUUID();
        final List<PrisonCustodySuite> prisonCustodySuites = emptyList();

        final Defendant defendant = defendant().withId(defendant1)
                .withOffences(singletonList(offence().withId(randomUUID()).withJudicialResults(emptyList()).build()))
                .build();

        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(false));
    }

    @Test
    public void shouldReturnNoDefendantWithCustodialEstablishmentToUpdateWhenDefendantsWithOffenceJRWithEmptyPrompts() {
        final UUID defendant1 = randomUUID();
        final List<PrisonCustodySuite> prisonCustodySuites = emptyList();

        final Defendant defendant = defendant().withId(defendant1)
                .withOffences(singletonList(offence().withId(randomUUID())
                        .withJudicialResults(singletonList(judicialResult().withJudicialResultTypeId(randomUUID()).withJudicialResultPrompts(emptyList()).build())).build()))
                .build();

        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(false));
    }

    @Test
    public void shouldReturnDefendantWithCustodialEstablishmentToUpdateWhenJudicialResultPromptPrisonCustody() {
        final UUID defendant1 = randomUUID();
        final PrisonCustodySuite prisonCustodySuite = PrisonCustodySuite.prisonCustodySuite().withId(randomUUID())
                .withName("HMP/YOI Hatfield")
                .withType("Prison")
                .build();
        final List<PrisonCustodySuite> prisonCustodySuites = singletonList(prisonCustodySuite);

        final String prisonCustodyOrgName = "HMP/YOI Hatfield";
        final Defendant defendant = defendant().withId(defendant1)
                .withOffences(asList(offence().withId(randomUUID())
                                .withJudicialResults(asList(judicialResult()
                                                .withJudicialResultTypeId(randomUUID())
                                                .withJudicialResultPrompts(asList(judicialResultPrompt().withPromptReference("somePrompt").withValue("someValue").build(),
                                                        judicialResultPrompt().withPromptReference("prisonOrganisationName").withValue(prisonCustodyOrgName).build())).build(),
                                        judicialResult().withJudicialResultTypeId(randomUUID()).build())).build(),
                        offence().build()))
                .build();

        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(true));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getName(), is(prisonCustodySuite.getName()));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getCustody(), is(prisonCustodySuite.getType()));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getId(), is(prisonCustodySuite.getId()));
    }

    @Test
    public void shouldReturnDefendantWithCustodialEstablishmentToUpdateWhenJudicialResultPromptHospitalCustody() {

        final UUID defendant1 = randomUUID();
        final PrisonCustodySuite prisonCustodySuite = PrisonCustodySuite.prisonCustodySuite().withId(randomUUID())
                .withName("HMP/YOI Hatfield")
                .withType("Prison")
                .build();
        final List<PrisonCustodySuite> prisonCustodySuites = singletonList(prisonCustodySuite);

        final String hospitalCustodyOrgName = "Camden NHS Hospital";
        final Defendant defendant = defendant().withId(defendant1)
                .withId(defendant1)
                .withOffences(asList(offence()
                                .withId(randomUUID())
                                .withJudicialResults(asList(judicialResult()
                                                .withJudicialResultTypeId(randomUUID())
                                                .withJudicialResultPrompts(asList(judicialResultPrompt().build(),
                                                        judicialResultPrompt().withPromptReference("hospitalOrganisationName").withValue(hospitalCustodyOrgName).build())).build(),
                                        judicialResult().withJudicialResultTypeId(randomUUID()).build())).build(),
                        offence().withId(randomUUID()).build()))
                .build();

        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getDefendantsResultedWithCustodialEstablishmentUpdate(defendant, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(true));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getCustody(), is(hospitalCustodyOrgName));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getName(), is(hospitalCustodyOrgName));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getId(), is(notNullValue()));
    }


    @Test
    public void shouldReturnCustodialEstablishmentUpdateFromJudicialResults() {
        final PrisonCustodySuite prisonCustodySuite = PrisonCustodySuite.prisonCustodySuite().withId(randomUUID())
                .withName("HMP Channings Wood")
                .withType("Prison")
                .build();
        final List<PrisonCustodySuite> prisonCustodySuites = singletonList(prisonCustodySuite);

        final UUID caseId = randomUUID();
        final String masterDefendantId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final JsonObject applicationJsonObject = jsonFromString(getPayload("stub-data/progression.event.hearing-application-link-created-single-application.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%MASTER_DEFENDANT_ID%",masterDefendantId)
                .replaceAll("%DEFENDANT_ID%",defendantId)
        );


        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getCustodialEstablishmentUpdateFromJudicialResults(applicationJsonObject, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(true));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getName(), is(prisonCustodySuite.getName()));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getCustody(), is(prisonCustodySuite.getType()));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getId(), is(prisonCustodySuite.getId()));
    }

    @Test
    void shouldReturnCustodialEstablishmentUpdateFromCortOrderJudicialResults() {
        final PrisonCustodySuite prisonCustodySuite = PrisonCustodySuite.prisonCustodySuite().withId(randomUUID())
                .withName("HMP Ashfield")
                .withType("Prison")
                .build();
        final List<PrisonCustodySuite> prisonCustodySuites = singletonList(prisonCustodySuite);

        final UUID caseId = randomUUID();
        final String masterDefendantId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final JsonObject applicationJsonObject = jsonFromString(getPayload("stub-data/progression.event.hearing-application-link-created-single-application_with_court_order.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%MASTER_DEFENDANT_ID%",masterDefendantId)
                .replaceAll("%DEFENDANT_ID%",defendantId)
        );


        final Optional<CustodialEstablishment> defendantsResultedWithCustodialEstablishmentUpdate = custodialEstablishmentUpdateHelper.getCustodialEstablishmentUpdateFromJudicialResults(applicationJsonObject, prisonCustodySuites);
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.isPresent(), is(true));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getName(), is(prisonCustodySuite.getName()));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getCustody(), is(prisonCustodySuite.getType()));
        assertThat(defendantsResultedWithCustodialEstablishmentUpdate.get().getId(), is(prisonCustodySuite.getId()));
    }

}