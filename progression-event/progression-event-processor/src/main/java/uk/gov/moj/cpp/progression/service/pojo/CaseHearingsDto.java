package uk.gov.moj.cpp.progression.service.pojo;

import uk.gov.justice.progression.courts.Hearings;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("squid:S2384")
public class CaseHearingsDto {

    private List<Hearings> hearings;

    public CaseHearingsDto() {
        this.hearings = Collections.emptyList();
    }

    public static CaseHearingsDto.Builder builder() {
        return new CaseHearingsDto.Builder();
    }

    public List<Hearings> gethearings() {
        return hearings;
    }

    public void sethearings(final List<Hearings> hearings) {
        this.hearings = hearings;
    }

    public static final class Builder {

        private List<Hearings> hearings;

        public CaseHearingsDto.Builder withhearings(final List<Hearings> hearings) {
            this.hearings = hearings;
            return this;
        }

        public CaseHearingsDto build() {
            final CaseHearingsDto caseHearingsDto = new CaseHearingsDto();
            caseHearingsDto.sethearings(hearings);
            return caseHearingsDto;
        }
    }
}

