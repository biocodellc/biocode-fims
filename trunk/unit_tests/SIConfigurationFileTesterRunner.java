package unit_tests;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import run.configurationFileTester;
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
@RunWith(value = Parameterized.class)
public class SIConfigurationFileTesterRunner {

    private configurationFileTester tester;
     String name;

    /**
     * Setup the tester framework by creating a configurationFileTester object and calling its initializer
     *
     * @param file
     */
    public SIConfigurationFileTesterRunner(String name, File file) {
        tester = new configurationFileTester();
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
        siConverter si = null;
        try {
            si = new siConverter();
            si.init();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
        // Loop each of the projects
        Iterator projectsIt = si.projects.iterator();
        int count = 0;
        while (projectsIt.hasNext()) {
            siProjects project = (siProjects) projectsIt.next();
            // Write the actual file
            File configFile = new File(
                    si.output_directory.getAbsolutePath() +
                            System.getProperty("file.separator") +
                            project.abbreviation +
                            ".xml");
            data.add(new Object[]{configFile.getName(),configFile});
        }

        return data;
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