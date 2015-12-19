package unit_tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import run.ConfigurationFileTester;
import tools.SiConverter;
import tools.SiProjects;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertTrue;


/**
 * Test SI configuration files that were automatically generated using tools.SiConverter Class
 */
@RunWith(value = Parameterized.class)
public class SIValidationTesterRunner {

    private ConfigurationFileTester tester;
     String name;

    /**
     * Setup the tester framework by creating a ConfigurationFileTester object and calling its initializer
     *
     * @param file
     */
    public SIValidationTesterRunner(String name, File file) {
        tester = new ConfigurationFileTester();
        tester.init(file);
        this.name = name;
    }

    /**
     * Build parameterized tests so we can call each of the SI configuration files in turn
     *
     * @return
     */
    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new ArrayList<Object[]>();

        // Create a converter object so we can initialize each of the SI projects
        SiConverter si = null;
        si = new SiConverter();
        si.init();
        // Loop each of the projects
        Iterator projectsIt = si.projects.iterator();
        int count = 0;
        while (projectsIt.hasNext()) {
            SiProjects project = (SiProjects) projectsIt.next();
            // Write the actual file
            File configFile = new File(
                    si.outputDirectory.getAbsolutePath() +
                            System.getProperty("file.separator") +
                            project.abbreviation +
                            ".xml");
            data.add(new Object[]{configFile.getName(),configFile});
        }

        return data;
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