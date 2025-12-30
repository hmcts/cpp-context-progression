package uk.gov.moj.cpp.progression.service.amp.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendantsInner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.progression.service.amp.dto.EventType.PCR;

@ExtendWith(MockitoExtension.class)
class AmpPcrMapperTest {

    @InjectMocks
    AmpPcrMapper mapper;

    PrisonCourtRegisterCaseOrApplication pcase = PrisonCourtRegisterCaseOrApplication.prisonCourtRegisterCaseOrApplication()
            // we need to set cases.urn, cases.docs.url, cases.docs.timestamp from here ??
            .build();
    PrisonCourtRegisterDefendant defendant = PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
            .withMasterDefendantId(UUID.fromString("d78e8cac-991c-43fa-86a7-8fc6b857308a"))
            .withName("Defendant Name")
            .withDateOfBirth("2000-01-31")
            .withProsecutionCasesOrApplications(List.of())
            .build();

    PrisonCourtRegisterGeneratedV2 pcr = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
            .withId(UUID.randomUUID())
            .withDefendant(defendant)
            .build();

    @Test
    void mapperShouldCreateAmpPayload() {
        PcrEventPayload payload = mapper.mapPcrForAmp(pcr);

        assertThat(payload.getEventId(), equalTo(pcr.getId()));
        assertThat(payload.getEventType(), equalTo(PCR));
        assertThat(payload.getTimestamp(), is(notNullValue()));
        assertThat(payload.getDefendants(), hasSize(1));
        assertDefendant(payload.getDefendants().get(0));
    }

    private void assertDefendant(PcrEventPayloadDefendantsInner defendant) {
        assertThat(defendant.getMasterDefendantId(), equalTo(UUID.fromString("d78e8cac-991c-43fa-86a7-8fc6b857308a")));
        assertThat(defendant.getName(), equalTo("Defendant Name"));
        assertThat(defendant.getDateOfBirth(), equalTo(LocalDate.of(2000, 1, 31)));
        assertThat(defendant.getCustodyEstablishmentDetails().getEmailAddress(), equalTo("TODO"));

        // TODO needs define from here
//        assertThat(defendant.getCases(), hasSize(1));
//        PcrEventPayloadDefendantsInnerCasesInner case0 = defendant.getCases().get(0);
//        assertThat(case0.getUrn(), equalTo("http://xxx"));
//
//        assertThat(case0.getDocuments(), hasSize(1));
//        PcrEventPayloadDefendantsInnerCasesInnerDocumentsInner document0 = case0.getDocuments().get(0);
//        assertThat(document0.getUrl(), equalTo(""));
//        assertThat(document0.getTimestamp(), is(notNullValue()));
    }
}