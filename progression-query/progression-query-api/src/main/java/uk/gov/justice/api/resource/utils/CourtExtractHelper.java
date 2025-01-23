package uk.gov.justice.api.resource.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;

import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.service.UsersAndGroupsService;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.CommittedForSentence;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;
import uk.gov.moj.cpp.progression.query.api.UserGroupsDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class CourtExtractHelper {

    @Inject
    private UsersAndGroupsService usersAndGroupsService;

    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";


    public Map<UUID, CommittedForSentence> getOffencesResultedWithCommittedForSentence(final UUID defendantId, final Hearing hearingFromListing, final List<Hearings> hearingsList, final List<ResultDefinition> filteredResultDefinitions) {
        final Map<UUID, SeedingHearing> offenceIdSeedingHearingMap = getOffenceIdSeedingHearingMap(defendantId, hearingFromListing);
        final List<UUID> committedToCCORSentToCCResultDefinitionsIds = filteredResultDefinitions.stream().map(ResultDefinition::getId).toList();

        final Map<UUID, CommittedForSentence> offenceIdCommittedForSentenceMap = new HashMap<>();
        offenceIdSeedingHearingMap.forEach((offenceId, seedingHearingMags) -> {
            final Optional<Hearings> matchingSeedingHearing = hearingsList.stream().filter(h -> h.getId().equals(seedingHearingMags.getSeedingHearingId())).findFirst();

            if (matchingSeedingHearing.isPresent()) {
                final Hearings magsHearing = matchingSeedingHearing.get();
                if (nonNull(magsHearing.getDefendants())) {
                    magsHearing.getDefendants().stream()
                            .filter(hd -> hd.getId().equals(defendantId))
                            .filter(hd -> nonNull(hd.getOffences()))
                            .flatMap(hd -> hd.getOffences().stream())
                            .filter(hdo -> nonNull(hdo.getJudicialResults()))
                            .filter(hdo -> hdo.getJudicialResults().stream().anyMatch(jr -> committedToCCORSentToCCResultDefinitionsIds.contains(jr.getJudicialResultTypeId())))
                            .forEach(hdo -> {
                                final List<String> resultTextList = nonNull(hdo.getJudicialResults()) && !hdo.getJudicialResults().isEmpty() ?
                                        hdo.getJudicialResults().stream().map(JudicialResult::getResultText).filter(StringUtils::isNotEmpty).toList() : emptyList();
                                offenceIdCommittedForSentenceMap.put(hdo.getId(), CommittedForSentence.committedForSentence()
                                        .withResultTextList(resultTextList)
                                        .withCourtName(magsHearing.getCourtCentre().getName())
                                        .withSittingDay(seedingHearingMags.getSittingDay())
                                        .build());

                            });
                }
            }
        });

        return offenceIdCommittedForSentenceMap;
    }

    public Map<UUID, SeedingHearing> getOffenceIdSeedingHearingMap(final UUID defendantId, final Hearing hearingFromListing) {
        final Map<UUID, SeedingHearing> offenceSeedingHeadingMap = new HashMap<>();
        if (nonNull(hearingFromListing) && nonNull(hearingFromListing.getListedCases())) {
            hearingFromListing.getListedCases().stream()
                    .filter(listedCase -> nonNull(listedCase.getDefendants()))
                    .flatMap(listedCase -> listedCase.getDefendants().stream())
                    .filter(defendant -> defendant.getId().equals(defendantId) && nonNull(defendant.getOffences()))
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(dOffence -> dOffence.getSeedingHearing().isPresent()
                            && dOffence.getSeedingHearing().get().getJurisdictionType() == JurisdictionType.MAGISTRATES)
                    .forEach(dOffence -> offenceSeedingHeadingMap.put(dOffence.getId(), dOffence.getSeedingHearing().get()));
        }

        return offenceSeedingHeadingMap;
    }

    public List<Offences> getOffencesFromSeedingHearings(final UUID defendantId, final Hearing hearingFromListing, final List<Hearings> hearingsList) {

        final List<Offences> offencesOfSeedingHeadingMap = new ArrayList<>();

        if (nonNull(hearingFromListing) && nonNull(hearingFromListing.getListedCases())) {
            hearingFromListing.getListedCases().stream()
                    .filter(listedCase -> nonNull(listedCase.getDefendants()))
                    .flatMap(listedCase -> listedCase.getDefendants().stream())
                    .filter(defendant -> defendant.getId().equals(defendantId) && nonNull(defendant.getOffences()))
                    .flatMap(defendant -> defendant.getOffences().stream())
                    .filter(dOffence -> dOffence.getSeedingHearing().isPresent()
                            && dOffence.getSeedingHearing().get().getJurisdictionType() == JurisdictionType.MAGISTRATES)
                    .forEach(dOffence -> {
                        List<Offences> defendantOffences = getOffencesFromHearingForMatchingSeedingHearing(dOffence.getSeedingHearing().get().getSeedingHearingId(), defendantId, dOffence.getId(), hearingsList);
                        offencesOfSeedingHeadingMap.addAll(defendantOffences);
                    });
        }

        return offencesOfSeedingHeadingMap;
    }

    public List<DelegatedPowers> getAuthorisedLegalAdvisors(final List<uk.gov.justice.progression.courts.exract.Offences> offences) {
        return offences.stream()
                .filter(o -> nonNull(o.getResults()))
                .flatMap(o -> o.getResults().stream())
                .map(JudicialResult::getDelegatedPowers)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .filter(delegatedPowers -> isLegalAdvisor(delegatedPowers.getUserId()))
                .collect(Collectors.toList());
    }

    private List<Offences> getOffencesFromHearingForMatchingSeedingHearing(final UUID seedingHearingId, final UUID defendantId, final UUID offenceIdFromSeedingHearing, final List<Hearings> hearingsList) {

        final List<Offences> matchingOffences = new ArrayList<>();

        hearingsList.stream()
                .filter(hearings -> hearings.getId().equals(seedingHearingId))
                .filter(hearings -> nonNull(hearings.getDefendants()))
                .flatMap(hearings -> hearings.getDefendants().stream())
                .filter(defendants -> defendants.getId().equals(defendantId))
                .filter(defendants -> nonNull(defendants.getOffences()))
                .flatMap(defendants -> defendants.getOffences().stream())
                .filter(offences -> offences.getId().equals(offenceIdFromSeedingHearing))
                .forEach(matchingOffences::add);

        return matchingOffences;
    }

    private Boolean isLegalAdvisor(final UUID userId) {
        final List<UserGroupsDetails> groupsUserBelongsTo = usersAndGroupsService.getUserGroups(userId);
        return groupsUserBelongsTo.stream().anyMatch(group -> GROUP_LEGAL_ADVISERS.equals(group.getGroupName()));
    }

}
