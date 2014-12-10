package unit_tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import run.configurationFileTester;

import java.io.File;


/**
 * Test SI configuration files that were automatically generated using tools.siConverter Class
 */

public class configurationFileTesterRunner {

    private configurationFileTester tester;
     String name;

    /**
     * Setup the tester framework by creating a configurationFileTester object and calling its initializer
     *
     */
    public configurationFileTesterRunner() {
        tester = new configurationFileTester();
        tester.init( new File("http://ucjeps.berkeley.edu/ucjeps_fims.xml"));
    }

    @Test
    public void parse() throws Exception {
        boolean result = tester.parse();
        assertTrue("Unabled to parse " + tester.fileToTest.getName(), result);
    }

    @Test
    public void checkUniqueURIValues() {
        boolean result = tester.checkUniqueKeys();
        assertTrue(tester.getMessages(), result);
    }

        @Test
    public void checkRuleFormation() {
        boolean result = tester.checkRuleFormation();
        assertTrue(tester.getMessages(), result);
    }
    @Test
    public void checkLists() {
        boolean result = tester.checkLists();
        assertTrue(tester.getMessages(), result);
    }





}
