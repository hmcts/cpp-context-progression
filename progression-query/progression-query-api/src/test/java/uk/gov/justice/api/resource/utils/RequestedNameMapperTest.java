package uk.gov.justice.api.resource.utils;


import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RequestedNameMapperTest {

    private static final String REQUESTED_NAME = "requestedName";
    private static final String TITLE_PREFIX = "titlePrefix";
    private static final String SURNAME = "surname";
    private static final String TITLE_JUDICIAL_PREFIX = "titleJudicialPrefix";
    private static final String TITLE_SUFFIX = "titleSuffix";

    private RequestedNameMapper requestedNameMapper;

    @BeforeEach
    public void init() {
        requestedNameMapper = new RequestedNameMapper();
    }

    @Test
    public void shouldReturnRequestedNameAsJudgeName() {

        final String judgeName = requestedNameMapper.getRequestedJudgeName(createJudiciaryWithRequestedName());
        assertThat(judgeName, is(REQUESTED_NAME));
    }

    @Test
    public void shouldNotReturnRequestedNameAsJudgeName() {

        final JsonObject judiciary = createJudiciaryWithoutRequestedName();
        final String formattedName = format("%s %s %s", judiciary.getString(TITLE_JUDICIAL_PREFIX, judiciary.getString(TITLE_PREFIX, EMPTY)), judiciary.getString(SURNAME), judiciary.getString(TITLE_SUFFIX, EMPTY)).trim();
        final String judgeName = requestedNameMapper.getRequestedJudgeName(createJudiciaryWithoutRequestedName());
        assertThat(judgeName, is(formattedName));
    }


    private JsonObject createJudiciaryWithRequestedName() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(REQUESTED_NAME, REQUESTED_NAME);
        judiciaryBuilder.add(SURNAME, SURNAME);
        judiciaryBuilder.add(TITLE_SUFFIX, TITLE_SUFFIX);
        judiciaryBuilder.add(TITLE_JUDICIAL_PREFIX, TITLE_JUDICIAL_PREFIX);
        judiciaryBuilder.add(TITLE_PREFIX, TITLE_PREFIX);
        return judiciaryBuilder.build();
    }

    private JsonObject createJudiciaryWithoutRequestedName() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add(SURNAME, SURNAME);
        judiciaryBuilder.add(TITLE_SUFFIX, TITLE_SUFFIX);
        judiciaryBuilder.add(TITLE_JUDICIAL_PREFIX, TITLE_JUDICIAL_PREFIX);
        judiciaryBuilder.add(TITLE_PREFIX, TITLE_PREFIX);
        return judiciaryBuilder.build();
    }

}