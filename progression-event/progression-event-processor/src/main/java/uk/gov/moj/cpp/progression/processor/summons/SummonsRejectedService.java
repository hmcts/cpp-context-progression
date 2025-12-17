package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getFullName;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationService;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummonsRejectedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SummonsRejectedService.class);

    @Inject
    private NotificationService notificationService;

    @Inject
    private SummonsNotificationEmailPayloadService summonsNotificationEmailPayloadService;

    public void sendSummonsRejectionNotification(final JsonEnvelope jsonEnvelope, final CourtApplication courtApplication, final SummonsRejectedOutcome summonsRejectedOutcome) {

        final SummonsTemplateType summonsTemplateType = courtApplication.getType().getSummonsTemplateType();
        if (summonsTemplateType != SummonsTemplateType.BREACH && summonsTemplateType != SummonsTemplateType.GENERIC_APPLICATION && summonsTemplateType != SummonsTemplateType.FIRST_HEARING) {
            LOGGER.info("Application rejection notification not raised for court application '{}' of type '{}'", courtApplication.getId(), summonsTemplateType);
            return;
        }

        final String applicantEmailAddress = summonsRejectedOutcome.getProsecutorEmailAddress();

        final EmailChannel summonsRejectedEmailChannel = summonsNotificationEmailPayloadService.getEmailChannelForSummonsRejected(applicantEmailAddress, courtApplication.getApplicationReference(), getPartyDetails(courtApplication), summonsRejectedOutcome.getReasons());
        notificationService.sendEmail(jsonEnvelope, null, courtApplication.getId(), null, singletonList(summonsRejectedEmailChannel));
    }

    private List<String> getPartyDetails(final CourtApplication courtApplication) {
        final SummonsTemplateType summonsTemplateType = courtApplication.getType().getSummonsTemplateType();
        if (SummonsTemplateType.BREACH == summonsTemplateType || SummonsTemplateType.GENERIC_APPLICATION == summonsTemplateType) {
            return singletonList(getPartyDetails(courtApplication.getSubject()));
        } else {
            return courtApplication.getRespondents().stream().map(this::getPartyDetails).collect(toList());
        }
    }

    private String getPartyDetails(final CourtApplicationParty party) {
        return newArrayList(getPartyName(party), getProsecutionAuthorityReference(party)).
                stream()
                .filter(StringUtils::isNotBlank)
                .collect(joining(", "));
    }

    private String getProsecutionAuthorityReference(final CourtApplicationParty party) {
        if (nonNull(party.getMasterDefendant())) {
            return party.getMasterDefendant().getProsecutionAuthorityReference();
        }

        return null;
    }

    private String getPartyName(final CourtApplicationParty party) {
        final MasterDefendant masterDefendant = party.getMasterDefendant();
        if (nonNull(masterDefendant)) {
            final PersonDefendant personDefendant = masterDefendant.getPersonDefendant();
            if (nonNull(personDefendant)) {
                final Person person = personDefendant.getPersonDetails();
                return getFullName(person.getFirstName(), person.getMiddleName(), person.getLastName());
            }
            final LegalEntityDefendant legalEntityDefendant = masterDefendant.getLegalEntityDefendant();
            if (nonNull(legalEntityDefendant)) {
                return nonNull(legalEntityDefendant.getOrganisation()) ? legalEntityDefendant.getOrganisation().getName() : EMPTY;
            }
        }

        if (nonNull(party.getOrganisation())) {
            return party.getOrganisation().getName();
        }

        if (nonNull(party.getPersonDetails())) {
            final Person person = party.getPersonDetails();
            return getFullName(person.getFirstName(), person.getMiddleName(), person.getLastName());
        }

        if (nonNull(party.getProsecutingAuthority())) {
            return party.getProsecutingAuthority().getName();
        }

        return null;
    }
}
