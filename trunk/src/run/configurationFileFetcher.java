package run;

import org.jsoup.Jsoup;
import settings.PathManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class that handles getting configuration files.  Configuration files are stored as BCID/ARKs and thus this class
 * needs to handle redirection when fetching appropriate configuration files.
 */
public class configurationFileFetcher {
    private String expedition_code;
    private File outputFile;
    // TODO: Fix biscicol.org resolution -- can't see itself!
    private String projectLookup = "http://biscicol.org:8080/id/projectService/validation/";

    public File getOutputFile() {
        return outputFile;
    }

    public String getExpedition_code() {
        return expedition_code;
    }

    /**
     * in this constructor we already know the URL of the configuration file
     * @param url
     * @param defaultOutputDirectory
     * @throws Exception
     */
    public configurationFileFetcher(URL url, String defaultOutputDirectory) throws Exception {
        init(url, defaultOutputDirectory);
    }

    /**
     * Create the class object given a particular expedition code and a default Output Directory
     *
     * @param defaultOutputDirectory
     * @throws IOException
     */
    public configurationFileFetcher(Integer project_id, String defaultOutputDirectory) throws Exception {
        this.expedition_code = expedition_code;

        // Get the URL for this configuration File
        String expeditionServiceString = projectLookup + project_id;
        // Set a 10 second timeout on this connection
        String urlString = Jsoup.connect(expeditionServiceString).timeout(10000).get().body().html();
        // Setup connection
        URL url = new URL(urlString);
        init(new URL(urlString), defaultOutputDirectory);
    }

    private void init(URL url, String defaultOutputDirectory) throws Exception {
        boolean redirect = false;

        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(5000);
        connection.setUseCaches(false);
        connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
        connection.addRequestProperty("User-Agent", "Mozilla");
        connection.addRequestProperty("Referer", "google.com");

        // Handle response Codes, Normally, 3xx is redirect, setting redirect boolean variable if it is a redirect
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }

        // Handle redirects
        if (redirect) {

            // get redirect url from "location" header field
            String newUrl = connection.getHeaderField("Location");

            // get the cookie if need, for login
//            String cookies = connection.getHeaderField("Set-Cookie");
            // open the new connnection again

            connection = (HttpURLConnection) new URL(newUrl ).openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("Cache-Control", "no-cache");

//            connection.setRequestProperty("Cookie", cookies);
            connection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            connection.addRequestProperty("User-Agent", "Mozilla");
            connection.addRequestProperty("Referer", "google.com");

        }
        InputStream inputStream = connection.getInputStream();

        // Write configuration file to output directory
        try {
            outputFile = PathManager.createUniqueFile("config.xml", defaultOutputDirectory);
        } catch (Exception e) {
            throw new IOException("Unable to create configuration file", e);
        }
        FileOutputStream os = new FileOutputStream(outputFile);
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from is to buffer
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            //flush OutputStream to write any buffered data to file
            os.flush();
            os.close();
        } catch (IOException e) {
            throw new Exception("Unable to get configuration file, server down or network error ", e);
        }
    }


    /**
     * Readfile method -- used as a convenience in this class for testing.
     *
     * @param file
     * @return
     * @throws IOException
     */
    private static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        return stringBuilder.toString();
    }

    public static void main(String[] args) {
        configurationFileFetcher cFF = null;
        String defaultOutputDirectory = System.getProperty("user.dir") + File.separator + "tripleOutput";

        try {
            cFF = new configurationFileFetcher(1, defaultOutputDirectory);
            System.out.println(readFile(cFF.getOutputFile()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
