package uk.gov.moj.cpp.progression.service.disqualificationreferral;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public abstract class ReferralDisqualifyWarningDataAggregator extends BaseDataAggregator {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public JsonObject aggregateReferralDisqualifyWarningData(final String caseUrn, final CourtCentre organisationUnit, final LjaDetails ljaDetails, final ReferredPerson personDetails) {

        final JsonObjectBuilder payload = createObjectBuilder().add("caseUrn", caseUrn)
                .add("orderingCourt", buildOrderingCourt(organisationUnit, ljaDetails))
                .add("defendant", buildDefendantPerson(personDetails));
        final JsonObjectBuilder orderAddressee = buildOrderAddressee(personDetails);
        orderAddressee.add("name", getFullName(personDetails));
        payload.add("orderAddressee", orderAddressee);
        return payload.build();
    }

    protected JsonObjectBuilder buildOrderingCourt(final CourtCentre organisation, final LjaDetails ljaDetails) {

        return createObjectBuilder()
                .add("ljaCode", ljaDetails.getLjaCode())
                .add("courtCentreName", getCourtHouseName(organisation))
                .add("ljaName", getLjaName(ljaDetails));
    }

    protected JsonObjectBuilder buildOrderAddressee(final ReferredPerson personDetails) {
        final JsonObjectBuilder addressBuilder = createObjectBuilder();
        final Optional<Address> address = Optional.ofNullable(personDetails.getAddress());

        address.ifPresent(address1 -> {
            ofNullable(address1.getAddress1())
                    .ifPresent(value -> addressBuilder.add("line1", value));
            ofNullable(address1.getAddress2())
                    .ifPresent(value -> addressBuilder.add("line2", value));
            ofNullable(address1.getAddress3())
                    .ifPresent(value -> addressBuilder.add("line3", value));
            ofNullable(address1.getAddress4())
                    .ifPresent(value -> addressBuilder.add("line4", value));
            ofNullable(address1.getAddress5())
                    .ifPresent(value -> addressBuilder.add("line5", value));
            ofNullable(address1.getPostcode())
                    .ifPresent(value -> addressBuilder.add("postCode", value));
        });
        addressBuilder.add("address", addressBuilder);
        return addressBuilder;
    }
}
