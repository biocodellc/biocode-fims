package settings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facilitate regular expressions
 */
public class RegEx {
    /**
     * Method used to emulate Perl type regex's
     *
     * @param pPattern
     * @param pString
     * @return
     */
    public static String run(String pPattern, String pString) {
        Pattern pattern = Pattern.compile(pPattern);
        Matcher matcher = pattern.matcher(pString);

        if (matcher.find()) {
            return matcher.replaceAll("");
        } else {
            return "";
        }
    }

    public static boolean verifyValue(String pPattern, String pString) {
        Pattern pattern = Pattern.compile(pPattern);
        Matcher matcher = pattern.matcher(pString);
        return matcher.matches();
    }

    public static void main(String[] args) {
          FimsPrinter.out.println(run("<.xml version=.1.0. encoding=.utf-8...>","<?xml version=\"1.0\" encoding=\"utf-8\"?>hallo"));
         /*if($well_number96 =~ /(^[A-Ha-h])(\d+)$/) {
            my $letter = $1;
            my $number = $2;
            if ($number !~ /(^0)/ && $number < 10) { $number = "0".$number; }
            $well_number96 = uc($letter).$number;
        } */
    /*    String well = "A01";
        if (verifyValue("(^[A-Ha-h])(\\d+)$", well)) {
            //String letter = well.substring(0,1);
            String number = well.substring(1,3);
            if (checkValidNumber(number)) {
                fimsPrinter.out.println("number is OK");
            } else {
                fimsPrinter.out.println("number is not OK");
            }


            fimsPrinter.out.println("match");
        } else {
            fimsPrinter.out.println("nomatch");
        }
        */
    }


}

