package fims;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.*;
import fimsExceptions.FimsRuntimeException;
import org.openjena.atlas.lib.StrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

/**
 * Uploader sends files to a particular service and a particular graph
 */
public class Uploader {

    private File file;
    private String endpoint;
    //private String graph;
    private String contentType = "text/turtle;charset=utf-8;";
    private String method = "POST";
    private String encoding = "UTF-8";
    private String service;
    private String connectionPoint;
    private String graphID;

    private static Logger logger = LoggerFactory.getLogger(Uploader.class);

    public String getConnectionPoint() {
        return this.getService() + "?graph=" + graphID;
    }

    /**
     * Use a UUID as the graph ID
     */
    private void setGraphID() {
        graphID = "urn:uuid:" + UUID.randomUUID();
    }

    public String getGraphID() {
        return graphID;
    }

    /**
     * Constructor called without a graph specification uses a UUID for the graph
     * TODO: fetch an ARK for this graph-name and make the fuseki service the target, add expedition code etc...
     * following on this, need to think about pointing the ARK to the RDF end-point
     *
     * @param fusekiService
     * @param file
     */
    public Uploader(String fusekiService, File file) {
        setGraphID();

        this.service = fusekiService;
        try {
            this.endpoint = service + "?graph=" + URLEncoder.encode(graphID, encoding);
            //System.out.println(endpoint);
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException", e);
        }
        this.file = file;
    }

    /**
     * Execute the data update.  This method is NOT transaction safe and if it is used, reccomend backing up loaded data
     * in case of data corruption
     *
     * @method execute the update
     */
    public String execute() {
        try {
            // Setup URL connection to this endpoint
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", contentType);
            conn.setRequestMethod(method);

            // Write the file to the connection, sending contents directly to the service
            WritableByteChannel channel = Channels.newChannel(conn.getOutputStream());
            new FileInputStream(file).getChannel().transferTo(0, 9999999, channel);

            // Get the response from the service
            BufferedReader rd = new BufferedReader(new
                    InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                // Process line...
            }
            rd.close();
        } catch (MalformedURLException e) {
            // throw a general exception here since we want to inform the call application of any mis-deeds
            // typically this will be the service being down
            throw new FimsRuntimeException(500, e);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        }
        return graphID;
    }

    /**
     * This is a Transaction-Safe execute that updates data using Write-Ahead-Logging on
     * Fuseki system.  This will prevent corruption in dataset if server goes down during aload.
     *
     * @return
     */
    public String safeExecute()  {
        // Dummy insert statement
        String insert =
                //"INSERT DATA INTO <" + this.getGraphID() + "> {  <http://example/book3> <http://example/book4> \"newValue1\"}\n";
                null;
        try {
            insert = //"PREFIX ark: <http://n2t.net/ark:> \n" +
                    "INSERT DATA INTO <" + this.getGraphID() + "> {  " + readFile(file.getAbsolutePath()) + "}\n";
        } catch (IOException e) {
            throw new FimsRuntimeException(500,e);
        }

        Dataset dataset = TDBFactory.createDataset();

        try {
            dataset.begin(ReadWrite.WRITE);

            String sparqlUpdateString = StrUtils.strjoinNL(insert);
            UpdateRequest request = UpdateFactory.create(sparqlUpdateString);

            // Specify Update service.  Swaps "data" with "update" so we can maintain code consistency until we can
            // refactor later, after we implement this in production
            String updateService =  this.getService().replace("data","update");
            UpdateProcessor proc = UpdateExecutionFactory.createRemote(request,updateService);

            proc.execute();

            // Finally, commit the transaction.
            dataset.commit();
            // Or call .abort()
        } finally {
            dataset.end();
        }
        return graphID;
    }

    public String getMethod() {
        return method;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getService() {
        return service;
    }

    /**
     * Return an encoding of this graph so it can be packed up and sent to a service
     *
     * @return
     */
    public String getEncodedGraph(boolean brackets) {
        try {
            String toEncode = "";
            if (brackets) toEncode += "<";
            toEncode += graphID;
            if (brackets) toEncode += ">";
            return URLEncoder.encode(toEncode, encoding);
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException", e);
            return null;
        }
    }

    /**
     * A convenient place to upload n-triple files to the data.biscicol.org Database... this was
     * started as a temporary back-end to load data files coming from the triplifier project into the
     * biscicol Database.
     *
     * @param args
     */
    public static void main(String[] args) {
        File file = new File("/Users/jdeck/IdeaProjects/biocode-fims/tripleOutput/acapla_CR_all_output.n3");
        //File file = new File("/Users/jdeck/IdeaProjects/biocode-fims/tripleOutput/DEMOH_output.31.n3");
        //http://data.biscicol.org/ds/data?graph=urn%3Auuid%3A37797bda-7602-42af-82a5-c8a3827d1c61
        //String uuid = "urn%3Auuid%3A2eddf62e-a58a-11e3-aae7-d4c45d837ce1";
        //uploader u = new uploader("http://data.biscicol.org/ds/data",file);
        //uploader u = new uploader("http://data.biscicol.org/ds/data",file);
        Uploader u = new Uploader("http://localhost:3030/ds/data", file);
        long startTime = System.currentTimeMillis();

        u.safeExecute();

        long endTime = System.currentTimeMillis();

        System.out.println("That took " + (endTime - startTime) + " milliseconds");

        System.out.println(u.getService() + "?graph="+ u.getGraphID());
    }
    private String readFile( String file ) throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader (file));
        String         line = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls = System.getProperty("line.separator");

        while( ( line = reader.readLine() ) != null ) {
            stringBuilder.append( line );
            stringBuilder.append( ls );
        }

        return stringBuilder.toString();
    }
}

