package unit_tests;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import tools.siConverter;
import tools.siProjects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


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
    public void checkLists() {
        boolean result = tester.checkLists();
        assertTrue(tester.getMessages(), result);
    }





}
