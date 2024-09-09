package uk.gov.moj.cpp.progression.service;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.common.service.ProvisionalBookingService;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jgroups.util.Util.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


@RunWith(MockitoJUnitRunner.class)
public class ProvisionalBookingServiceAdapterTest {

    private final UUID  BOOKINGID_1 = UUID.fromString("00e17481-18d3-4eda-9214-75bdb15abd25");
    private final UUID  COURT_SCHEDULE_ID_1_A = UUID.fromString("9e12bb19-da11-4437-b1a7-5169899f2d26");
    private final UUID  COURT_SCHEDULE_ID_1_B = UUID.fromString("faa612f0-932c-4690-ba20-4272ec037ec5");

    private final UUID  BOOKINGID_2 = UUID.fromString("d977f709-05c7-44d2-a053-404d493454d2");
    private final UUID  COURT_SCHEDULE_ID_2 = UUID.fromString("08420555-727e-47d8-9b06-ffe18d248467");


    @Mock
    private ProvisionalBookingService provisionalBookingService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ProvisionalBookingServiceAdapter provisionalBookingServiceAdapter;

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void getSlots(){
        final Response response = Response.status(Response.Status.OK).entity(getBookingServiceResponse()).build();
        final List<UUID> bookingRefereceList = Arrays.asList(BOOKINGID_1, BOOKINGID_2);
        when(provisionalBookingService.getSlots(anyMap())).thenReturn(response);
        final Map<UUID, Set<UUID>> slotsMap = provisionalBookingServiceAdapter.getSlots(bookingRefereceList);
        assertThat(slotsMap.size(), is(2));
        assertTrue(slotsMap.containsKey(BOOKINGID_1));
        assertTrue(slotsMap.containsKey(BOOKINGID_2));
        final Set<UUID> set1 = slotsMap.get(BOOKINGID_1);
        assertTrue(set1.contains(COURT_SCHEDULE_ID_1_A));
        assertTrue(set1.contains(COURT_SCHEDULE_ID_1_B));
        final Set<UUID> set2 = slotsMap.get(BOOKINGID_2);
        assertTrue(set2.contains(COURT_SCHEDULE_ID_2));
    }

    private JsonObject getBookingServiceResponse() {
        String response = null;
        try {
            response = Resources.toString(getResource("provisionalBookingService-getSlots.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }


}