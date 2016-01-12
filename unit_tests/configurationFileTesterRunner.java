package unit_tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;


/**
 * Test SI configuration files that were automatically generated using tools.SiConverter Class
 */

public class configurationFileTesterRunner {

    private ConfigurationFileTester tester;
     String name;

    /**
     * Setup the tester framework by creating a ConfigurationFileTester object and calling its initializer
     *
     */
    public configurationFileTesterRunner() {
        tester = new ConfigurationFileTester();
        tester.init( new File("http://ucjeps.berkeley.edu/ucjeps_fims.xml"));
    }

    @Test
    public void parse() {
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
