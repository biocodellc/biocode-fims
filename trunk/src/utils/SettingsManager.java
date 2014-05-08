package utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;


/**
 * util.SettingsManager provides a generic way to configure BiSciCol core classes
 * from a properties file.  The basic idea is that any object that supports
 * the Configurable interface can be passed a util.SettingsManager, and it will then
 * use the util.SettingsManager to configure itself.  util.SettingsManager is implemented
 * as a singleton to ensure that all BiSciCol objects use common configuration
 * information.
 */
public class SettingsManager {
    private static SettingsManager instance = null;

    private Properties props;
    private String propsfile;

    protected SettingsManager(String propsfile) {
        this.propsfile = propsfile;
    }

    /**
     * Get a reference to the global util.SettingsManager instance.  If this is the
     * first request for a util.SettingsManager instance, then a new util.SettingsManager
     * object will be created using the default properties file, which is
     * expected to be located in the "classes" directory of the expedition build
     * directory.
     *
     * @return A reference to the global util.SettingsManager object.
     */
    public static SettingsManager getInstance() {
        return getInstance(Thread.currentThread().getContextClassLoader().getResource("biocode-fims.props").getFile());
    }

    /**
     * Get a reference to the global util.SettingsManager object, specifying a
     * properties file to use.  If this is the first request for a
     * util.SettingsManager instance, then a new util.SettingsManager object will be
     * created using the specified properties file.  Otherwise, the existing
     * util.SettingsManager will be returned and the specified properties file is
     * ignored.
     *
     * @param propsfile A properties file to use in initializing the
     *                  util.SettingsManager.
     * @return A reference to the global util.SettingsManager object.
     */
    public static SettingsManager getInstance(String propsfile) {
        if (instance == null)
            instance = new SettingsManager(propsfile);

        return instance;
    }

    /**
     * Get the path of the properties file associated with this util.SettingsManager.
     *
     * @return The path of the properties file used by this util.SettingsManager.
     */
    public String getPropertiesFile() {
        return propsfile;
    }

    /**
     * Specify a properties file for this util.SettingsManager to use.
     *
     * @param propsfile The path to a properties file.
     */
    public void setPropertiesFile(String propsfile) {
        this.propsfile = propsfile;
    }

    /**
     * Attempt to load the properties file associated with this util.SettingsManager.
     * This method must be called to properly initialize the util.SettingsManager
     * before it can be used by Configurable classes.
     *
     * @throws java.io.FileNotFoundException
     */
    public void loadProperties() throws FileNotFoundException {
        try {
            props = new Properties();
            FileInputStream in = new FileInputStream(propsfile);


            props.load(in);
            in.close();
        } catch (Exception e) {
            throw new FileNotFoundException("Unable to find settings file " + propsfile + ". Make sure you have included this file in the root class of your deployed application!");
        }
    }

    /**
     * Get the value associated with the specified key.  If the key is not found
     * in the properties file, then an empty string is returned.
     *
     * @param key The key to search for in the properties file.
     * @return The value associated with the key if it exists, otherwise, an
     *         empty string.
     */
    public String retrieveValue(String key) {
        return retrieveValue(key, "");
    }

    /**
     * Get the value associated with the specified key; return a default value
     * if the key is not found.  The string specified by defaultval is returned
     * as the default value if the key cannot be found.
     *
     * @param key        The key to search for in the properties file.
     * @param defaultval The default value to return if the key cannot be found.
     * @return The value associated with the key if it exists, otherwise, the
     *         specified default value.
     */
    public String retrieveValue(String key, String defaultval) {
        return props.getProperty(key, defaultval);
    }
}
