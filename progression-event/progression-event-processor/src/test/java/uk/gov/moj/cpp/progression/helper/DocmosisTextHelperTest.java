package uk.gov.moj.cpp.progression.helper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DocmosisTextHelperTest {

    @InjectMocks
    DocmosisTextHelper docmosisTextHelper;

    @Test
    public void shouldReturnBackSlashAndDoubleQuoteWhenThereIsIrregularDoubleQuote() {
        final String result = docmosisTextHelper.replaceEscapeCharForDocmosis("This is a “test” string.");
        assertThat("This is a \\\"test\\\" string.", is(result));
    }


    @Test
    public void shouldReturnHypenWhenThereIsIrregularHypen() {
        final String result = docmosisTextHelper.replaceEscapeCharForDocmosis("This is a –test– string.");
        assertThat("This is a -test- string.", is(result));
    }

    @Test
    public void shouldReturnSingleQuoteWhenThereIsIrregularSingleQuote() {
        final String result = docmosisTextHelper.replaceEscapeCharForDocmosis("This is a ’test’ string.");
        assertThat("This is a 'test' string.", is(result));
    }
}