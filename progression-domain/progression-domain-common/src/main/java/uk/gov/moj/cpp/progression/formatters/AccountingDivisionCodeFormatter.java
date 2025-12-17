package uk.gov.moj.cpp.progression.formatters;
import static org.apache.commons.lang3.StringUtils.leftPad;

public class AccountingDivisionCodeFormatter {

    private static final int SIZE_OF_STRING = 3;
    private static final char PADDED_CHAR = '0';

    private AccountingDivisionCodeFormatter(){
    }

    public static String formatAccountingDivisionCode(final String accountingDivisionCode){
        return leftPad(accountingDivisionCode, SIZE_OF_STRING, PADDED_CHAR);
    }
}
