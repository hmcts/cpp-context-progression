package uk.gov.moj.cpp.progression.query.view.service.transformer;



import uk.gov.moj.cpp.progression.domain.pojo.Direction;
import uk.gov.moj.cpp.progression.domain.pojo.RefDataDirection;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.progression.domain.pojo.RefData.refData;
import static uk.gov.moj.cpp.progression.domain.pojo.RefDataDirection.refDataDirection;

public class DirectionMapper {

    private DirectionMapper() {
    }

    public static RefDataDirection mapFrom(final Direction direction) {
        final RefDataDirection.Builder builder = refDataDirection()
                .withRefData(refData().withDirectionRefDataId(direction.getDirectionId()).build())
                .withStatus(direction.getStatus())
                .withSubDirection(direction.getSubDirection())
                .withText(direction.getDisplayText())
                .withPrompts(direction.getPrompts())
                .withDefaultDirection(direction.getDefaultDirection())
                .withDueDate(direction.getDueDate())
                .withNonPublishable(direction.getNonPublishable());

        if (!isEmpty(direction.getAssignee())) {
            builder.withAssignee(direction.getAssignee());
        }

        return builder.build();
    }
}
