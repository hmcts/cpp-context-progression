package uk.gov.moj.cpp.progression.service.amp.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterCaseOrApplication;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendantCases;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.moj.cpp.progression.service.amp.dto.PcrEventType.PRISON_COURT_REGISTER_GENERATED;

@ExtendWith(MockitoExtension.class)
class AmpPcrMapperTest {

    @InjectMocks
    AmpPcrMapper mapper;

    PrisonCourtRegisterCaseOrApplication caseOrApplication = PrisonCourtRegisterCaseOrApplication.prisonCourtRegisterCaseOrApplication()
            .withCaseOrApplicationReference("SJ54CYRNYB")
            .build();
    PrisonCourtRegisterDefendant defendant = PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant()
            .withMasterDefendantId(UUID.fromString("d78e8cac-991c-43fa-86a7-8fc6b857308a"))
            .withName("Defendant Name")
            .withDateOfBirth("2000-01-31")
            .withProsecutionCasesOrApplications(List.of(caseOrApplication))
            .build();

    PrisonCourtRegisterGeneratedV2 pcr = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
            .withId(UUID.randomUUID())
            .withMaterialId(UUID.randomUUID())
            .withDefendant(defendant)
            .build();

    @Test
    void mapperShouldCreateAmpPayload() {
        Instant createdAt = Instant.now();
        PcrEventPayload payload = mapper.mapPcrForAmp(pcr, "wandsworth@example.com", createdAt);

        assertThat(payload.getEventId(), equalTo(pcr.getId()));
        assertThat(payload.getMaterialId(), equalTo(pcr.getMaterialId()));
        assertThat(payload.getEventType(), equalTo(PRISON_COURT_REGISTER_GENERATED));
        assertThat(payload.getTimestamp(), is(notNullValue()));
        assertDefendant(payload.getDefendant());
    }

    @Test
    void mapperShouldBeNullSafe() {
        PrisonCourtRegisterGeneratedV2 emptyPcr = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                .build();
        PcrEventPayload payload = mapper.mapPcrForAmp(emptyPcr, null, null);

        assertNull(payload.getEventId());
    }

    private void assertDefendant(PcrEventPayloadDefendant defendant) {
        assertThat(defendant.getMasterDefendantId(), equalTo(UUID.fromString("d78e8cac-991c-43fa-86a7-8fc6b857308a")));
        assertThat(defendant.getName(), equalTo("Defendant Name"));
        assertThat(defendant.getDateOfBirth(), equalTo(LocalDate.of(2000, 1, 31)));
        assertThat(defendant.getCustodyEstablishmentDetails().getEmailAddress(), equalTo("wandsworth@example.com"));

        assertThat(defendant.getCases(), hasSize(1));
        PcrEventPayloadDefendantCases case0 = defendant.getCases().get(0);
        assertThat(case0.getUrn(), equalTo("SJ54CYRNYB"));
    }
}