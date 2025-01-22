package uk.gov.moj.cpp.progression.query.view;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.Prompt;
import uk.gov.moj.cpp.progression.domain.pojo.RefDataDirection;
import uk.gov.moj.cpp.progression.domain.pojo.ReferenceDataDirectionManagementType;
import uk.gov.moj.cpp.progression.query.view.service.DefendantService;
import uk.gov.moj.cpp.progression.query.view.service.DirectionTransformService;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.progression.domain.pojo.RefDataDirection.refDataDirection;
import static uk.gov.moj.cpp.progression.query.view.service.transformer.DirectionMapper.mapFrom;

public class DirectionQueryView {

    private static final String CASE_ID = "caseId";


    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DirectionTransformService directionTransformService;

    @Inject
    private DefendantService defendantService;

    public RefDataDirection getTransformedDirections(final Direction referenceDirection,
                                                     final ReferenceDataDirectionManagementType referenceDataDirectionManagementTypes,
                                                     final List<Defendant> defendantList,
                                                     Map<UUID, String> witnesses,
                                                     Map<UUID, String> assignees,
                                                     boolean changeWitnessPromptFixListToTxt,
                                                     final String formType) {
        final List<Prompt> prompts = directionTransformService.transform(referenceDirection, defendantList, getDefendantsFullName(defendantList), witnesses, assignees, changeWitnessPromptFixListToTxt, formType);

        return refDataDirection()
                .withValuesFrom(mapFrom(referenceDirection))
                .withPrompts(prompts)
                .withCategories(referenceDataDirectionManagementTypes.getCategory() != null ?Arrays.asList(referenceDataDirectionManagementTypes.getCategory().split(",")): null)
                .withSequenceNumber(Objects.nonNull(referenceDirection.getSequenceNumber()) ? referenceDirection.getSequenceNumber() : referenceDataDirectionManagementTypes.getSequenceNumber())
                .withVariant(referenceDataDirectionManagementTypes.getVariant())
                .build();
    }

    private Map<UUID, String> getDefendantsFullName(final List<Defendant> defendants) {
        if (isNotEmpty(defendants)) {
            return defendants.stream().collect(Collectors.toMap((e -> e.getId()), e -> {
                        String defendantName = null;
                        if (Objects.nonNull(e.getPersonDefendant()) && Objects.nonNull(e.getPersonDefendant().getPersonDetails())) {
                            defendantName = defendantService.getDefendantFullName(e.getPersonDefendant().getPersonDetails());
                        } else if (Objects.nonNull(e.getLegalEntityDefendant()) && Objects.nonNull(e.getLegalEntityDefendant().getOrganisation())) {
                            defendantName = e.getLegalEntityDefendant().getOrganisation().getName();
                        }
                        return defendantName;
                    }
            ));
        }
        return newHashMap();
    }
}