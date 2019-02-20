package uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping;

import static uk.gov.moj.cpp.progression.domain.constant.ProsecutingAuthority.CPS;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Person;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils.SearchCaseBuilder;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655", "squid:S2259", "squid:S2629", "squid:S00115", "pmd:BeanMembersShouldSerialize", "squid:CallToDeprecatedMethod"})
public class SearchProsecutionCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchProsecutionCase.class);
    private static final DateTimeFormatter DOB_FORMATER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Inject
    private SearchProsecutionCaseRepository searchRepository;

    public SearchProsecutionCaseEntity makeSearchable(final ProsecutionCase prosecutionCase, final Defendant defendant) {
        final SearchCaseBuilder builder = prepareSearch(prosecutionCase, defendant);
        SearchProsecutionCaseEntity searchEntity = searchRepository.findBy(defendant.getId());
        if (searchEntity == null) {
            searchEntity = new SearchProsecutionCaseEntity();
        }
        if (StringUtils.isNotEmpty(builder.getReference()) && Objects.nonNull(defendant.getPersonDefendant())) {
            searchEntity.setCaseId(builder.getCaseId());
            searchEntity.setDefendantId(builder.getDefendantId());
            searchEntity.setReference(builder.getReference());
            searchEntity.setDefendantFirstName(safeUnbox(builder.getDefendantFirstName(), searchEntity.getDefendantFirstName()));
            searchEntity.setDefendantMiddleName(safeUnbox(builder.getDefendantMiddleName(), searchEntity.getDefendantMiddleName()));
            searchEntity.setDefendantLastName(safeUnbox(builder.getDefendantLastName(), searchEntity.getDefendantLastName()));
            final String dob = safeUnbox(builder.getDefendantDob(), searchEntity.getDefendantDob());
            searchEntity.setDefendantDob(StringUtils.isNotEmpty(dob) ? DOB_FORMATER.format(LocalDate.parse(dob)) : StringUtils.EMPTY);
            searchEntity.setProsecutor(safeUnbox(builder.getProsecutor(), searchEntity.getProsecutor()));
            searchEntity.setStatus(builder.getStatus());
            searchEntity.setSearchTarget(SearchCaseBuilder.searchCaseBuilder()
                    .withSearchCaseEntity(searchEntity)
                    .withDefendantFullName()
                    .withSearchTarget()
                    .build().getSearchTarget());
            searchRepository.save(searchEntity);
            LOGGER.info("Non CPS Search target created : {}", searchEntity.getSearchTarget());
        } else {
            LOGGER.error("Unable to create a non CPS search for defendant: {}, missing case reference: {}", defendant.getId(), builder.getReference());
        }
        return searchEntity;
    }

    private SearchCaseBuilder prepareSearch(final ProsecutionCase prosecutionCase, final Defendant defendant) {
        return SearchCaseBuilder.searchCaseBuilder()
                .withCaseId(prosecutionCase.getId().toString())
                .withDefendantId(defendant.getId())
                .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier())
                .withPersonDefendant(defendant.getPersonDefendant())
                .build();
    }

    private String safeUnbox(final String value, final String existingValue) {
        final String cleanValue = StringUtils.isEmpty(existingValue) ? StringUtils.EMPTY : existingValue;
        return StringUtils.isEmpty(value) ? cleanValue : value;
    }

    @SuppressWarnings("deprecation")
    public SearchProsecutionCaseEntity makeSearchable(
            final CaseProgressionDetail caseProgressionDetail,
            final uk.gov.moj.cpp.progression.persistence.entity.Defendant defendant) {

        SearchProsecutionCaseEntity searchEntity = searchRepository.findBy(defendant.getDefendantId());
        if (searchEntity == null) {
            searchEntity = new SearchProsecutionCaseEntity();
        }
        if (StringUtils.isNotEmpty(caseProgressionDetail.getCaseUrn())) {
            searchEntity.setDefendantId(defendant.getDefendantId());
            searchEntity.setCaseId(caseProgressionDetail.getCaseId().toString());
            searchEntity.setReference(safeUnbox(caseProgressionDetail.getCaseUrn(), searchEntity.getReference()));
            buildDefendantName(defendant, searchEntity);
            final String defendantDob = safeUnbox(getDob(defendant.getPerson()), searchEntity.getDefendantDob());
            searchEntity.setDefendantDob(StringUtils.isNotEmpty(defendantDob)
                    ? DOB_FORMATER.format(LocalDate.parse(defendantDob)) : StringUtils.EMPTY);
            searchEntity.setProsecutor(CPS.getDescription());
            searchEntity.setStatus(caseProgressionDetail.getStatus() != null ?
                    caseProgressionDetail.getStatus().name() : searchEntity.getStatus());
            searchEntity.setSearchTarget(SearchCaseBuilder.searchCaseBuilder()
                    .withSearchCaseEntity(searchEntity)
                    .withDefendantFullName()
                    .withSearchTarget()
                    .build().getSearchTarget());
            searchRepository.save(searchEntity);
            LOGGER.info("CPS search target: {}", searchEntity.getSearchTarget());
        } else {
            LOGGER.error("Unable to create a CPS search for defendant: {}, missing case urn: {}",
                    defendant.getDefendantId(), caseProgressionDetail.getCaseUrn());
        }
        return searchEntity;
    }

    @SuppressWarnings("deprecation")
    private void buildDefendantName(final uk.gov.moj.cpp.progression.persistence.entity.Defendant defendant, final SearchProsecutionCaseEntity searchEntity) {
        if (defendant.getPerson() != null) {
            final uk.gov.moj.cpp.progression.persistence.entity.Person person = defendant.getPerson();
            searchEntity.setDefendantFirstName(safeUnbox(person.getFirstName(), searchEntity.getDefendantFirstName()));
            searchEntity.setDefendantMiddleName(StringUtils.EMPTY);
            searchEntity.setDefendantLastName(safeUnbox(person.getLastName(), searchEntity.getDefendantLastName()));
        }
    }

    @SuppressWarnings("deprecation")
    private String getDob(final Person person) {
        return person != null ? person.getDateOfBirth().toString() : StringUtils.EMPTY;
    }
}
