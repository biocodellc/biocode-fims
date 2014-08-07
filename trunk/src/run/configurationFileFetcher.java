package run;

import org.jsoup.Jsoup;
import settings.PathManager;
import utils.SettingsManager;
import utils.urlFreshener;
import java.io.*;
import java.net.*;
import java.util.Date;

/**
 * Class that handles getting configuration files.  Configuration files are stored as BCID/ARKs and thus this class
 * needs to handle redirection when fetching appropriate configuration files.
 */
public class configurationFileFetcher {
    private File outputFile;
    private Integer project_id;
    private String configFileName;
    private Integer hoursToHoldCache = 24;

    public Integer getProject_id() {
        return project_id;
    }

    public File getOutputFile() {
        return outputFile;
    }

    /**
     * If file is more than 24 hours old or does not exist then return false
     *
     * @param defaultOutputDirectory
     *
     * @return
     */
    public Boolean getCachedConfigFile(String defaultOutputDirectory) throws Exception {
        // Create a file reference
        File file = new File(defaultOutputDirectory, configFileName);

        // check for file existing, if not then return false
        if (!file.exists())
            return false;

        // check for files older than 24 hours
        if (new Date().getTime() - file.lastModified() > hoursToHoldCache * 60 * 60 * 1000)
            return false;

        // File exists and is younger than 24 hours old, set the outputFile class variable
        outputFile = new File(defaultOutputDirectory, configFileName);
        return true;
    }

    /**
     * Create the class object given a particular expedition code and a default Output Directory
     *
     * @param defaultOutputDirectory
     *
     * @throws IOException
     */
    public configurationFileFetcher(Integer project_id, String defaultOutputDirectory, Boolean useCache) throws Exception {
        this.project_id = project_id;
        configFileName = "config." + project_id + ".xml";

        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        String projectServiceString = sm.retrieveValue("project_lookup_uri") + project_id;
        Boolean useCacheResults = false;

        // call cache operation if user wants it
        if (useCache) {
            useCacheResults = getCachedConfigFile(defaultOutputDirectory);
        }

        // get a fresh copy if the useCacheResults is false
        if (!useCacheResults) {
            // Get the URL for this configuration File by fetching it from the project service
            String urlString = Jsoup.connect(projectServiceString).timeout(10000).get().body().html();
            // Initialize the connection
            init(new URL(urlString), defaultOutputDirectory);
        }
    }

    /**
     * Download the file!
     * @param url
     * @param defaultOutputDirectory
     * @throws Exception
     */
    private void init(URL url, String defaultOutputDirectory) throws Exception {
        boolean redirect = false;

        // Always ensure we have the freshest copy of a particular URL
        urlFreshener freshener = new urlFreshener();
        url = freshener.forceLatestURL(url);

        HttpURLConnection.setFollowRedirects(true);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setDefaultUseCaches(false);
        conn.setRequestMethod("GET");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setReadTimeout(5000);
        conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
        conn.addRequestProperty("User-Agent", "Mozilla");
        conn.addRequestProperty("Referer", "google.com");
        conn.addRequestProperty("Cache-Control", "no-store,no-cache");


        // Handle response Codes, Normally, 3xx is redirect, setting redirect boolean variable if it is a redirect
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }

        // Handle redirects
        if (redirect) {
            // get redirect url from "location" header field
            String newUrl = freshener.forceLatestURL(conn.getHeaderField("Location"));
            // open the  connnection

            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.addRequestProperty("Cache-Control", "no-store,no-cache");
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("User-Agent", "Mozilla");
            conn.addRequestProperty("Referer", "google.com");
        }

        conn.connect();
        InputStream inputStream = conn.getInputStream();

        // Set outputFile location
        try {
            outputFile = PathManager.createFile(configFileName, defaultOutputDirectory);
        } catch (Exception e) {
            throw new IOException("Unable to create configuration file", e);
        }

        // Write the data using input and output streams
        FileOutputStream os = new FileOutputStream(outputFile);
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from is to buffer
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            inputStream.close();

            // Debugging where file output is stored
            System.out.println("writing " + url + " to " + outputFile.getAbsolutePath());

            //flush OutputStream to write any buffered data to file
            os.flush();
            os.close();
            // Close the connection input stream
            conn.getInputStream().close();

        } catch (IOException e) {
            throw new Exception("Unable to get configuration file, server down or network error ", e);
        } finally {
            // Disconnect at the end
            conn.disconnect();
        }
    }

    public static void main(String[] args) {
        configurationFileFetcher cFF = null;
        String defaultOutputDirectory = System.getProperty("user.dir") + File.separator + "tripleOutput";

        try {
            cFF = new configurationFileFetcher(1, defaultOutputDirectory, false);
            // System.out.println(readFile(cFF.getOutputFile()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}


