package uk.gov.justice.api.resource.utils;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.progression.courts.exract.Results.results;

import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.service.UsersAndGroupsService;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.progression.courts.Offences;
import uk.gov.justice.progression.courts.exract.ApplicationResults;
import uk.gov.justice.progression.courts.exract.CommittedForSentence;
import uk.gov.justice.progression.courts.exract.CourtApplicationCases;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtOrderOffences;
import uk.gov.justice.progression.courts.exract.CourtOrders;
import uk.gov.justice.progression.courts.exract.Results;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.SeedingHearing;
import uk.gov.moj.cpp.progression.query.view.UserGroupsDetails;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
                .map(Results::getResult)
                .map(JudicialResult::getDelegatedPowers)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .filter(delegatedPowers -> isLegalAdvisor(delegatedPowers.getUserId()))
                .toList();
    }

    public List<DelegatedPowers> getApplicationAuthorisedLegalAdvisors(final List<CourtApplications> courtApplications) {
        return  courtApplications.stream()
                .filter(courtApplication -> nonNull(courtApplication.getApplicationResults()))
                .flatMap(courtApplication -> courtApplication.getApplicationResults().stream())
                .map(ApplicationResults::getResult)
                .map(JudicialResult::getDelegatedPowers)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .filter(dp -> isLegalAdvisor(dp.getUserId()))
                .toList();
    }

    public List<DelegatedPowers> getApplicationAuthorisedLegalAdvisorsForLinkedApplication(final List<CourtApplications> courtApplications, final List<JudicialResult> defendantLevelJudicialResults) {
        final List<JudicialResult> results = new ArrayList<>(courtApplications.stream()
                .filter(courtApplication -> nonNull(courtApplication.getApplicationResults()))
                .flatMap(courtApplication -> courtApplication.getApplicationResults().stream())
                .map(ApplicationResults::getResult).toList());

        results.addAll(courtApplications.stream()
                .map(CourtApplications::getCourtOrders)
                .filter(Objects::nonNull)
                .map(CourtOrders::getCourtOrderOffences)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(CourtOrderOffences::getCourtOrderOffencesOffenceLevelResults)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(offencesOffenceLevelResults -> offencesOffenceLevelResults.getResult())
                .toList());

        results.addAll(courtApplications.stream()
                .map(CourtApplications::getCourtApplicationCases)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(CourtApplicationCases::getOffences)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(uk.gov.justice.progression.courts.exract.Offences::getResults)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(Results::getResult)
                .toList());

        results.addAll(defendantLevelJudicialResults);

        return  results.stream()
                .map(JudicialResult::getDelegatedPowers)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .filter(dp -> isLegalAdvisor(dp.getUserId()))
                .toList();


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
