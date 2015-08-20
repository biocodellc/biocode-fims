package reader;

import org.apache.commons.lang.ArrayUtils;

import java.io.*;

/**
 * Cleans Files of bad, nasty characters that will cause files to choke in various processing steps.
 * These are typically things that like vertical tabs which are useless for characterizing data but have
 * been found at one point or another to cause systems downstream to fail.
 *
 * Integer values of bad characters are added to the "badAsciiChars" array
 */
public class characterCleaner {

    // A list of ascii characters that we want to just remove.
    static Integer[] badAsciiChars = {-1, 11};

    public static void main(String[] args) throws IOException {

        File f = new File("/Users/jdeck/Google Drive/!DIPnet_DB/Repository/1-cleaned_QC2_mdfasta_files/mdfastaQC2_Eucmet_CO1_HL.txt");
        //File f = new File("/Users/jdeck/IdeaProjects/biocode-fims/sampledata/test");

        FileInputStream fis = new FileInputStream(f);

        //Construct BufferedReader from InputStreamReader
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        String line = null;
        PrintWriter out = new PrintWriter("filename.txt");

        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            String goodLine = getOnlyGoodChars(line);
            if (goodLine != null) {
                sb.append(getOnlyGoodChars(line));
            }
        }

        br.close();

        out.print(sb.toString());
        out.close();

    }

    /**
     * Function that parses a line and returns only its good characters
     * @param input
     * @return
     */
    public static String getOnlyGoodChars(String input) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {

            // Convert each character to its ascii equivalent
            Integer asciiCharInt = 0;
            try {
                asciiCharInt = (int) input.charAt(i);
            } catch (Exception e) {
                e.printStackTrace();
                asciiCharInt = -1;
            }
            if (!ArrayUtils.contains(badAsciiChars, asciiCharInt)) {
                sb.append(input.charAt(i));
            }
        }

        // If nothing on string, don't even get line
        if (sb.toString().equals("")) {
            return null;
        } else {
            return sb.toString() + "\n";
        }
    }
}
