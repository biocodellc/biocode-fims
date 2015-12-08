package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Convenience class for working with dates related to bcids.
 */
public class dates {

    SimpleDateFormat formatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ssz");

    public dates() {
        formatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Record all notions of now() for bcids in a consistent manner
     *
     * @return A string representation of this time (now)
     */
    public String now() {
        return formatUTC.format(new Date());
    }

    /**
     * Convert unix time to UTC...
     * EZID results come back as UnixStamps and we want to display these in our common format.
     * @param timeStamp
     * @return
     */
    public String unixToUTC(long timeStamp) {
        Date date = new Date(timeStamp * 1000);
        return formatUTC.format(date);
    }
}
