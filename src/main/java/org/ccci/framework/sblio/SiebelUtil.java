package org.ccci.framework.sblio;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SiebelUtil
{

    private static final String SIEBEL_DATE_TIME_PATTERN = "MM/dd/yyyy HH:mm:ss";

    private static final String SIEBEL_DATE_PATTERN = "MM/dd/yyyy";

    /** prints and parses dates using the format that Siebel uses for dates without a time component: {@code MM/dd/yyyy} */
    public static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormat.forPattern(SIEBEL_DATE_PATTERN);
    
    /** prints and parses dates using the format that Siebel uses for dates with a time component: {@code MM/dd/yyyy hh:mm:ss} */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(SIEBEL_DATE_TIME_PATTERN);

}
