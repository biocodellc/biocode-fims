import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import org.apache.log4j.Level;


/**
 * Created by IntelliJ IDEA.
 * User: jdeck
 * Date: 8/9/13
 * Time: 4:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class triplify {

    public triplify(String mappingFile, String outputFile) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);


        try {
            // Construct a fileOutputStream to hold the output
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            // Construct the modeld
            Model model = new ModelD2RQ(FileUtils.toURL(mappingFile), FileUtils.langN3, "urn:x-biscicol:");

            // Write output
            System.out.println("Writing output to " + outputFile);
            model.write(fileOutputStream, FileUtils.langN3);

            // Finish up
            fileOutputStream.close();
            System.out.println("Done!");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows us to triplify from the command line
     *
     * @param args
     */
    public static void main(String args[]) {
        String D2RQmappingfile = "file:mappings/biocode_dbtest_mapping.n3";
        String outputfile = "/tmp/biocode_dbtest.n3";


        triplify run = null;
        run = new triplify(D2RQmappingfile, outputfile);
        /*
        if (args.length != 2) {
            System.out.println("Usage: run mapping.n3 output.n3\n");

        } else {
            run = new triplify(args[0], args[1]);
        }
        */
    }
}
