package uk.gov.moj.cpp.progression.query.view.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.moj.cpp.progression.query.utils.SearchQueryUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SearchQueryUtilsTest {

    @Test
    public void shouldReturnNonDateParamsAsItIs() {
        assertEquals("defendant name", SearchQueryUtils.tryParseDate("defendant name"));
        assertEquals("TFL12345", SearchQueryUtils.tryParseDate("TFL12345"));
        assertEquals("1-13-2001", SearchQueryUtils.tryParseDate("1-13-2001"));
    }

    @Test
    public void shouldTransformSearchParamsToJPAFormat() {
        String searchString = "firstName lastName 6-Jan-2007";
        final String transformedParams = SearchQueryUtils.prepareSearch(searchString);
        assertNotNull(transformedParams);
        assertEquals("%firstName% %lastName% %2007-01-06%", transformedParams);
    }

    @Test
    public void acceptPatternDmYyyy() {
        final String formatedDateIfExists = SearchQueryUtils.tryParseDate("01-Jan-1977");
        assertNotNull(formatedDateIfExists);
        assertEquals("1977-01-01", formatedDateIfExists);
    }

    @Test
    public void acceptPatternDdMmYy() {
        final String formatedDateIfExists = SearchQueryUtils.tryParseDate("06/01/07");
        assertNotNull(formatedDateIfExists);
        assertEquals("2007-01-06", formatedDateIfExists);
    }

    @Test
    public void acceptPatternDMmYy() {
        final String formatedDateIfExists = SearchQueryUtils.tryParseDate("6/01/07");
        assertNotNull(formatedDateIfExists);
        assertEquals("2007-01-06", formatedDateIfExists);
    }

    @Test
    public void acceptPatternDMmYyyy() {
        final String formatedDateIfExists = SearchQueryUtils.tryParseDate("06/01/2007");
        assertNotNull(formatedDateIfExists);
        assertEquals("2007-01-06", formatedDateIfExists);
    }


    @Test
    public void acceptPatternDdMmYyyy() {
        final String formatedDateIfExists = SearchQueryUtils.tryParseDate("01-01-1977");
        assertNotNull(formatedDateIfExists);
        assertEquals("1977-01-01", formatedDateIfExists);
    }

    @Test
    public void acceptPatternDMMMyyyy() {
        final String formatedDateIfExists = SearchQueryUtils.tryParseDate("1-Jan-2007");
        assertNotNull(formatedDateIfExists);
        assertEquals("2007-01-01", formatedDateIfExists);
    }

    @Test
    public void acceptPatternDdMmyy() {
        final String formatedDateIfExists = SearchQueryUtils.tryParseDate("06-01-01");
        assertNotNull(formatedDateIfExists);
        assertEquals("2001-01-06", formatedDateIfExists);
    }

}

