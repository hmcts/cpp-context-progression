package uk.gov.moj.cpp.progression.service.payloads;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("squid:S2384")
public class PublishCourtListHearing {

    private final String caseReference;
    private final String hearingType;
    private final List<PublishCourtListDefendant> defendants;

    private PublishCourtListHearing(final String caseReference, final String hearingType, final List<PublishCourtListDefendant> defendants) {
        this.caseReference = caseReference;
        this.hearingType = hearingType;
        this.defendants = defendants;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public String getHearingType() {
        return hearingType;
    }

    public List<PublishCourtListDefendant> getDefendants() {
        return isNull(defendants) ? ImmutableList.of() : copyOf(defendants);
    }

    public PublishCourtListHearing addDefendant(final PublishCourtListDefendant defendant) {
        this.defendants.add(defendant);
        this.defendants.sort(new SortDefendantComparator());
        return this;
    }

    public static PublishCourtListHearingBuilder publishCourtListHearingBuilder() {
        return new PublishCourtListHearingBuilder();
    }

    public static final class PublishCourtListHearingBuilder {
        private String caseReference;
        private String hearingType;
        private List<PublishCourtListDefendant> defendants;

        private PublishCourtListHearingBuilder() {
        }

        public PublishCourtListHearingBuilder withCaseReference(final String caseReference) {
            this.caseReference = caseReference;
            return this;
        }

        public PublishCourtListHearingBuilder withHearingType(final String hearingType) {
            this.hearingType = hearingType;
            return this;
        }

        public PublishCourtListHearingBuilder addDefendant(final PublishCourtListDefendant defendant) {
            if (isNull(this.defendants)) {
                this.defendants = new ArrayList<>();
            }
            this.defendants.add(defendant);
            this.defendants.sort(new SortDefendantComparator());
            return this;
        }

        public PublishCourtListHearing build() {
            return new PublishCourtListHearing(caseReference, hearingType, defendants);
        }
    }

    static class SortDefendantComparator implements Comparator<PublishCourtListDefendant> {

        @Override
        public int compare(final PublishCourtListDefendant o1, final PublishCourtListDefendant o2) {
            if (isNotBlank(o1.getDefendantLastName()) && isNotBlank(o2.getDefendantLastName())) {
                return o1.getDefendantLastName().compareToIgnoreCase(o2.getDefendantLastName());
            } else if (isBlank(o1.getDefendantLastName()) && isNotBlank(o2.getDefendantLastName())) {
                return 1;
            } else if (isNotBlank(o1.getDefendantLastName())) {
                return -1;
            }
            return o1.getDefendantName().compareToIgnoreCase(o2.getDefendantName());
        }
    }

}
