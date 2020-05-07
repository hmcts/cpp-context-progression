package uk.gov.moj.cpp.progression.handler.courts.document;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;

import uk.gov.justice.core.courts.CourtDocument;


@RunWith(MockitoJUnitRunner.class)
public class DefaultCourtDocumentFactoryTest {

    @InjectMocks
    private DefaultCourtDocumentFactory defaultCourtDocumentFactory;

    @Test
    public void shouldCreateCourtDocumentWithFinancialMeansFalseIfNotSet() throws Exception {

        final CourtDocument courtDocument = courtDocument().build();
        assertThat(courtDocument.getContainsFinancialMeans(), is(nullValue()));

        final CourtDocument defaultCourtDocument = defaultCourtDocumentFactory.createDefaultCourtDocument(courtDocument);
        assertThat(defaultCourtDocument, is(notNullValue()));
        assertThat(defaultCourtDocument.getContainsFinancialMeans(), is(false));
    }
}
