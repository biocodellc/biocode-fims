package unit_tests;

import com.hp.hpl.jena.tdb.migrate.A2;
import org.junit.Test;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import run.*;
import settings.FIMSException;
import settings.bcidConnector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The publicly accessible tests return or true or false, with true indicating success and false indicating that
 * the test was failed.  All messages are managed by the configurationFileErrorMessager class and can be
 * retrieved at any point to display any explanatory information regarding why a particular test failed.   If all
 * tests pass then no messages are written to the configurationFileErrorMessager
 */
public class SIValidationTester {

    private StringBuilder expectedSB = new StringBuilder();
    private StringBuilder actualSB = new StringBuilder();

    private configurationFileErrorMessager messages = new configurationFileErrorMessager();
    String output_directory = System.getProperty("user.dir") + File.separator + "sampledata" + File.separator;


    /**
     * Return all the messages from this Configuration File Test
     */
    public String getMessages() {
        return messages.printMessages();
    }

    public void init(String inputFile, File configFile, File testFile) throws FIMSException, IOException {
        processController processController = new processController(0, null);

        process p = new process(
                inputFile,
                output_directory,
                new bcidConnector(),
                processController,
                configFile);

        // Run the validation processor
        p.runAllLocally(false, false);

        // Generate output results from the Validation engine
        actualSB = processController.getCommandLineSB();

        // Read TEST file
        BufferedReader br = new BufferedReader(new FileReader(testFile));
        try {
            String line = br.readLine();
            while (line != null) {
                expectedSB.append(line);
                expectedSB.append('\n');
                line = br.readLine();
            }
        } finally {
            br.close();
        }
    }

    /**
     * For testing purposes ONLY, call this script from unit_tests directory
     *
     * @param args
     *
     * @throws run.configurationFileError
     */
    public static void main(String[] args) throws Exception {

        SIValidationTester tester = new SIValidationTester();
        tester.init(
                "/Users/jdeck/IdeaProjects/biocode-fims/sampledata/SIENT_error.xlsx",
                new File("/Users/jdeck/IdeaProjects/biocode-fims/web_nmnh/docs/SIENT.xml"),
                new File("/Users/jdeck/IdeaProjects/biocode-fims/web_nmnh/docs/SIENT.test")
        );

        // Lines below this point are TEST lines.
        // Check for unexpected lines in the validation method
        ArrayList unexpected = tester.unexpectedLines();
        if (unexpected.size() > 0) {
            System.out.println("Unexpected lines in output!" + unexpected);
        }

        // Check for missing lines in the validation method
        ArrayList missing = tester.missingLines();
        if (missing.size() > 0) {
            System.out.println("Missing lines in output!" + unexpected);
        }
    }

    /**
     * Check for unexpected results being returned from the validation process
     *
     * @return
     */
    public ArrayList unexpectedLines() {
        ArrayList unexpectedLines = new ArrayList();
        String[] actuallines = actualSB.toString().split("\n");
        for (int i = 0; i < actuallines.length; i++) {
            if (!checkline(actuallines[i], expectedSB)) {
                unexpectedLines.add(actuallines[i]);
            }
        }
        return unexpectedLines;
    }

    /**
     * Check for missing results being returned from the validation process
     *
     * @return
     */
    public ArrayList missingLines() {
        ArrayList missingLines = new ArrayList();
        String[] expectedLines = expectedSB.toString().split("\n");

        for (int i = 0; i < expectedLines.length; i++) {
            if (!checkline(expectedLines[i], actualSB)) {
                missingLines.add(expectedLines[i]);
            }
        }
        return missingLines;
    }

    /**
     * Run the "inside-loop" checking for existince of line in array
     *
     * @param line
     * @param chunkToCheck
     *
     * @return
     */
    private static Boolean checkline(String line, StringBuilder chunkToCheck) {
        // Strip tabs off this string
        line = line.replace("\t", "");
        // Effectively ignore the Warning/Error Messages, which are group level
        if (line.contains("Warning:") || line.contains(("Error:"))) return true;

        String[] chunkedlines = chunkToCheck.toString().split("\n");

        for (int j = 0; j < chunkedlines.length; j++) {
            // Strip tabs off this string
            String expected = chunkedlines[j].replace("\t", "");

            if (expected.equals(line))
                return true;
        }
        return false;
    }

}
