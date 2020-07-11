package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterRecipient;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.progression.courts.InformantRegisterGenerated;
import uk.gov.justice.progression.courts.InformantRegisterNotificationIgnored;
import uk.gov.justice.progression.courts.InformantRegisterNotified;
import uk.gov.justice.progression.courts.NotifyInformantRegister;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProsecutionAuthorityAggregate implements Aggregate {
    private static final long serialVersionUID = 101L;
    private List<InformantRegisterRecipient> informantRegisterRecipients;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(InformantRegisterRecorded.class).apply(e -> {
                }),
                when(InformantRegisterGenerated.class).apply(e -> {
                    final List<InformantRegisterDocumentRequest> informantRegisterWithRecipients = e.getInformantRegisterDocumentRequests().stream().filter(
                            informantRegisterDocumentRequest -> nonNull(informantRegisterDocumentRequest.getRecipients()) && !informantRegisterDocumentRequest.getRecipients().isEmpty())
                            .collect(Collectors.toList());
                    if (isNotEmpty(informantRegisterWithRecipients)) {
                        this.informantRegisterRecipients = informantRegisterWithRecipients.get(0).getRecipients();
                    }
                }),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> createInformantRegister(final UUID prosecutionAuthorityId, final InformantRegisterDocumentRequest informantRegisterRequest) {
        return Stream.of(new InformantRegisterRecorded(informantRegisterRequest, prosecutionAuthorityId));
    }

    public Stream<Object> generateInformantRegister(final List<InformantRegisterDocumentRequest> informantRegisterRequests, final boolean systemGenerated) {
        return apply(Stream.of(InformantRegisterGenerated.informantRegisterGenerated()
                .withInformantRegisterDocumentRequests(informantRegisterRequests)
                .withSystemGenerated(systemGenerated)
                .build()));
    }

    public Stream<Object> notifyProsecutingAuthority(final NotifyInformantRegister notifyInformantRegister) {

        if (isEmpty(informantRegisterRecipients) || isBlank(notifyInformantRegister.getTemplateId()) || isNull(notifyInformantRegister.getFileId())) {
            return apply(Stream.of(InformantRegisterNotificationIgnored.informantRegisterNotificationIgnored()
                    .withFileId(notifyInformantRegister.getFileId())
                    .withTemplateId(notifyInformantRegister.getTemplateId())
                    .withProsecutionAuthorityId(notifyInformantRegister.getProsecutionAuthorityId()).build()));
        }

        return apply(Stream.of(InformantRegisterNotified.informantRegisterNotified().withRecipients(informantRegisterRecipients)
                .withFileId(notifyInformantRegister.getFileId())
                .withTemplateId(notifyInformantRegister.getTemplateId())
                .withProsecutionAuthorityId(notifyInformantRegister.getProsecutionAuthorityId()).build()));
    }

    public void setInformantRegisterRecipients(List<InformantRegisterRecipient> informantRegisterRecipients) {
        this.informantRegisterRecipients = Collections.unmodifiableList(informantRegisterRecipients);
    }

}
