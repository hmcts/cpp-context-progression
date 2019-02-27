package uk.gov.moj.cpp.progression.query.utils;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({"squid:S3655","squid:S00115","squid:S1166"})
public class SearchQueryUtils {
    private static final DateTimeFormatter dobFormat = ofPattern("yyyy-MM-dd");
    private static final String searchDelimiter = "% %";
    private static final String singleDelimiter="%";
    private static final List<String> allowedPatterns =
            Arrays.asList("dd/MM/yy","d/MM/yy","dd/MM/yyyy","d/M/yyyy","d/MMM/yyyy","dd/MMM/yyyy",
                    "dd-MM-yy", "d-MM-yy", "d-M-yyyy", "dd-MM-yyyy", "d-MMM-yyyy", "dd-MMM-yyyy");

    private SearchQueryUtils() {
    }

    public static String tryParseDate(final String param) {
        return allowedPatterns.stream()
                .map(allowedPattern -> tryPattern(param, ofPattern(allowedPattern)))
                .filter(Optional::isPresent)
                .findFirst().orElse(Optional.of(param)).get();
    }

    private static Optional<String> tryPattern(final String searchParam, final DateTimeFormatter allowedPattern) {
        try {
            return Optional.of(dobFormat.format(LocalDate.parse(searchParam, allowedPattern)));
        } catch (DateTimeParseException e) {
            //Ignore non date search parameters
        }
        return Optional.empty();
    }

    public static String prepareSearch(final String searchParams) {
        final List<String> searchParamList =
                Arrays.asList(searchParams.split(" ")).stream()
                        .filter(StringUtils::isNotEmpty)
                        .map(SearchQueryUtils::tryParseDate)
                        .collect(Collectors.toList());

        final String transformedSearchParameters = searchParamList.stream()
                .collect(Collectors.joining(searchDelimiter));

        return new StringBuilder().append(singleDelimiter).
                append(transformedSearchParameters).append(singleDelimiter).toString();
    }
}

