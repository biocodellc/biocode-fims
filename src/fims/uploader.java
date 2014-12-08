package fims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import settings.FIMSRuntimeException;

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
public class uploader {

    private File file;
    private String endpoint;
    //private String graph;
    private String content_type = "text/turtle;charset=utf-8;";
    private String method = "POST";
    private String encoding = "UTF-8";
    private String service;
    private String connectionPoint;
    private String graphID;

    private static Logger logger = LoggerFactory.getLogger(uploader.class);

    public String getConnectionPoint() {
        return this.getService() + "?graph="+graphID;
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
     * @param fuseki_service
     * @param file
     */
    public uploader(String fuseki_service, File file) {
        setGraphID();

        this.service = fuseki_service;
        try {
            this.endpoint = service + "?graph=" + URLEncoder.encode(graphID, encoding);
            //System.out.println(endpoint);
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException", e);
        }
        this.file = file;
    }

    /**
     * @method execute the update
     */
    public String execute() {
        try {
            // Setup URL connection to this endpoint
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", content_type);
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
            throw new FIMSRuntimeException(500, e);
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
        return graphID;
    }

    public String getMethod() {
        return method;
    }

    public String getContent_type() {
        return content_type;
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
     * A convenient place to upload n-triple files to the data.biscicol.org database... this was
     * started as a temporary back-end to load data files coming from the triplifier project into the
     * biscicol database.
     * @param args
     */
    public static void main (String[] args) {
        File file = new File("/Users/jdeck/Downloads/triplifierTest.n3");
        //File file = new File("/Users/jdeck/IdeaProjects/biocode-fims/tripleOutput/DEMOH_output.31.n3");
        //http://data.biscicol.org/ds/data?graph=urn%3Auuid%3A37797bda-7602-42af-82a5-c8a3827d1c61
        //String uuid = "urn%3Auuid%3A2eddf62e-a58a-11e3-aae7-d4c45d837ce1";
        //uploader u = new uploader("http://data.biscicol.org/ds/data",file);
        uploader u = new uploader("http://data.biscicol.org/ds/data",file);
       System.out.println( u.execute());
    }
}

