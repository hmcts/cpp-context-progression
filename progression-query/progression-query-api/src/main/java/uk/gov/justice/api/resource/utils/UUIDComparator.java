package uk.gov.justice.api.resource.utils;

import static java.util.Collections.unmodifiableList;

import uk.gov.justice.api.resource.dto.ResultPrompt;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

public class UUIDComparator implements Comparator<ResultPrompt> {

    static final String PROMPT_MISSING_FROM_REFERENCE_DATA = "Prompt missing from reference data";

    @Inject
    private Logger LOGGER;

    private final List<UUID> referenceList;

    public UUIDComparator(final List<UUID> referenceList) {
        this.referenceList = unmodifiableList(referenceList);
    }

    @Override
    public int compare(final ResultPrompt left, final ResultPrompt right) {

        final Integer indexOf1 = referenceList.indexOf(left.getPromptId());

        final Integer indexOf2 = referenceList.indexOf(right.getPromptId());

        if (indexOf1 == -1 || indexOf2 == -1) {
            LOGGER.error(PROMPT_MISSING_FROM_REFERENCE_DATA);
            return 0;
        }

        return indexOf1.compareTo(indexOf2);
    }
}
