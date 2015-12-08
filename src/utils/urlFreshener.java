package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fimsExceptions.FIMSRuntimeException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Always ensure a fresh URL with this nifty little class
 */
public class urlFreshener {
    private static Logger logger = LoggerFactory.getLogger(urlFreshener.class);
    public static void main(String[]  args) {
        urlFreshener t = new urlFreshener();

        String url = "https://biocode-fims.googlecode.com/svn/trunk/TestResultsSITester.html";
        try {
            URL u = new URL(url);
            System.out.println(t.forceLatestURL(u));

        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * pass in a URL as a string
     * @param url
     * @return
     */
    public String forceLatestURL(String url) {
        return url + "#" +System.currentTimeMillis() / 1000L;
    }

    /**
     * pass in a URL as a URL
     * @param url
     * @return
     */
    public URL forceLatestURL(URL url) {
        try {
            return new URL(forceLatestURL(url.toString()));
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

    /**
     * Use the proper query delimiter ? or & depending on whether query conditions exist already or not
     * @param urlString
     * @return
     */
    private String queryDelimiter(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            logger.warn("MalformedURLException", e);
            return "&";
        }
        if (url.getQuery() != null && !url.getQuery().equals("")) {
            return "&";
        } else {
            return "?";
        }
    }


}
