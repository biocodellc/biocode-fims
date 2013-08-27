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
public class simpleTest {

    public simpleTest(String mappingFile, String outputFile) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
        String baseURIForData =  "urn:x-biscicol:";
        String outputLang = FileUtils.langTurtle;

        try {
            // Construct a fileOutputStream to hold the output
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);

            // Construct the modeld
            Model model = new ModelD2RQ(FileUtils.toURL(mappingFile), outputLang, baseURIForData);

            // Write output
            model.write(fileOutputStream, outputLang, baseURIForData);

            // Finish up
            fileOutputStream.close();
            System.out.println("wrote output to " + outputFile);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String args[]) {
        String D2RQmappingfile = "file:mappings/TestMapping1.n3";
        String outputfile = "/tmp/TestMapping1.n3";


        simpleTest run = null;
        run = new simpleTest(D2RQmappingfile, outputfile);
        /*
        if (args.length != 2) {
            System.out.println("Usage: run mapping.n3 output.n3\n");

        } else {
            run = new triplify(args[0], args[1]);
        }
        */
    }
}
