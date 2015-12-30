package utils;


import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class that handles getting configuration files.  Configuration files are stored as BCID/ARKs and thus this class
 * needs to handle redirection when fetching appropriate configuration files.
 */
public class EncodeURIcomponent {

    private static Logger logger = LoggerFactory.getLogger(EncodeURIcomponent.class);

    public static void main(String[] args) {
        if (!testURIcomponent()) {
            logger.warn("Failed tests!");
        } else {
            logger.info("Passed tests!");
        }


    }

    public static boolean testURIcomponent() {
        EncodeURIcomponent e = new EncodeURIcomponent();
        ArrayList tests = new ArrayList();
        tests.add("dfofo&&");
        tests.add("JD\\");
        tests.add("Franks's 1");

        Iterator i = tests.iterator();
        while (i.hasNext()) {
            String value = (String) i.next();
             String encoded = e.encode(value);
            try {
                String decoded = URLDecoder.decode(encoded, "utf-8");
                System.out.println(value + ";" + encoded + ";" + decoded);
                if (!value.equals(decoded))
                    return false;
            } catch (UnsupportedEncodingException ex) {
                throw new FimsRuntimeException(null, 500, ex);
            }
        }
        return true;
    }
    /**
     * Converts a string into something you can safely insert into a URL.
     */
    public  String encode(String s) {
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                o.append('%');
                o.append(toHex(ch / 16));
                o.append(toHex(ch % 16));
            } else o.append(ch);
        }
        return o.toString();
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0)
            return true;
        return " %$&+,/:;=?@<>#%\\".indexOf(ch) >= 0;
    }

}
