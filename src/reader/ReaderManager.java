package reader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reader.plugins.TabularDataReader;
import fimsExceptions.FIMSRuntimeException;


/**
 * Provides high-level access to the Triplifier's data reader plugin system.
 * Includes methods for opening data files and getting instances of particular
 * reader plugins.  There should generally be few situations where you might
 * need to manually instantiate plugin classes.  Using the methods in
 * ReaderManager is much simpler, less error-prone, and recommended whenever
 * possible.
 */
public class ReaderManager implements Iterable<TabularDataReader> {
    private LinkedList<TabularDataReader> readers;
    private static Logger logger = LoggerFactory.getLogger(ReaderManager.class);

    /**
     * Initializes a new ReaderManager.  No plugins are loaded by default.  The
     * LoadReaders() method must be called to find and load reader plugins.
     */
    public ReaderManager() {
        readers = new LinkedList<TabularDataReader>();
    }

    /**
     * Load all reader plugins.  All compiled class files in the
     * reader/plugins directory will be examined to see if they implement the
     * TabularDataReader interface.  If so, they will be loaded as valid reader
     * plugins for use by the ReaderManager.
     */
    public void loadReaders() {
        URL resource = getClass().getResource("plugins");
        if(resource != null && resource.getFile().contains(".jar!")) {
            loadReaderPluginsFromJar(resource);
        } else {
            loadReaderPluginsFromDirectory();
        }
    }

    private void loadReaderPluginsFromDirectory() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // get location of the plugins package
        URL rsc = cl.getResource(PACKAGE_PATH);
        if (rsc == null)
            throw new FIMSRuntimeException("Could not locate plugins directory.", 500);

        File pluginsdir = new File(rsc.getFile());

        // make sure the location is a valid directory
        if (!pluginsdir.exists() || !pluginsdir.isDirectory())
            throw new FIMSRuntimeException("Could not locate plugins directory.", 500);

        // createEZID a simple filter to only look at compiled class files
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // only accept class files, but ignore the interface
                return (name.endsWith(".class") && !name.endsWith("TabularDataReader.class"));
            }
        };
        File[] classfiles = pluginsdir.listFiles(filter);

        String classname;
        Class newclass;
        Object newreader;

        // run.process each class file
        for (File classfile : classfiles) {
            classname = classfile.getName();
            classname = classname.substring(0, classname.length() - 6);
            //fimsPrinter.out.println(classname);

            try {
                // load the class file and instantiate the class
                newclass = cl.loadClass(getClassNameWithReaderPluginsPackage(classname));
                newreader = newclass.newInstance();

                // make sure this class implements the reader interface
                if (newreader instanceof TabularDataReader) {
                    // add it to the list of valid readers
                    readers.add((TabularDataReader) newreader);
                }
            } catch (ClassNotFoundException e) {
                throw new FIMSRuntimeException(500, e);
            } catch (InstantiationException e) {
                throw new FIMSRuntimeException(500, e);
            } catch (IllegalAccessException e) {
                throw new FIMSRuntimeException(500, e);
            }
        }
    }

    private static final String PACKAGE_PATH = "reader/plugins";

    private void loadReaderPluginsFromJar(URL resource) {

        try {
            List<String> classNames = new ArrayList<String>();
            String pathToJar = resource.getFile().substring(0, resource.getFile().indexOf("!"));
            ZipInputStream zipStream = new ZipInputStream(new URL(pathToJar).openStream());
            ZipEntry entry = zipStream.getNextEntry();
            while (entry != null) {
                String toFind = PACKAGE_PATH + "/";
                int indexOfPackages = entry.getName().indexOf(toFind);
                if (indexOfPackages != -1) {
                    classNames.add(entry.getName().substring(indexOfPackages + toFind.length()).replace(".class", ""));
                }
                entry = zipStream.getNextEntry();
            }

            for (String classname : classNames) {
                try {
                    Class<?> clazz = Class.forName(getClassNameWithReaderPluginsPackage(classname));
                    Object newInstance = clazz.newInstance();
                    if (newInstance instanceof TabularDataReader) {
                        readers.add((TabularDataReader) newInstance);
                    }
                } catch (ClassNotFoundException e) {
                    throw new FIMSRuntimeException(500, e);
                } catch (InstantiationException e) {
                    throw new FIMSRuntimeException(500, e);
                } catch (IllegalAccessException e) {
                    throw new FIMSRuntimeException(500, e);
                }
            }
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

    private String getClassNameWithReaderPluginsPackage(String classname) {
        return PACKAGE_PATH.replace("/", ".") + "." + classname;
    }

    /**
     * Get all file formats supported by the loaded reader plugins.  The
     * returned strings contain short format identifiers that can be used to
     * request specific readers from the getReader() method.
     *
     * @return An array of file format identifiers for all loaded plugins.
     */
    public String[] getSupportedFormats() {
        String[] formats = new String[readers.size()];

        for (int cnt = 0; cnt < readers.size(); cnt++)
            formats[cnt] = readers.get(cnt).getFormatString();

        return formats;
    }

    /**
     * Get a reader for the specified input file format.  The format string
     * should correspond to the value returned by the getFormatString() method
     * of the reader.  If a reader matching the file format is found, a new
     * instance of the reader is created and returned.
     *
     * @param formatstring The input file format.
     * @return A new instance of the reader supporting the specified format.
     */
    public TabularDataReader getReader(String formatstring) {
        for (TabularDataReader reader : readers) {
            if (reader.getFormatString().equals(formatstring)) {
                try {
                    return reader.getClass().newInstance();
                } catch (IllegalAccessException e){
                    logger.warn("IllegalAccessException", e);
                } catch (InstantiationException e) {
                    logger.warn("InstantiationException", e);
                }
            }
        }

        return null;
    }

    /**
     * Returns an iterator for all reader plugins loaded by this ReaderManager.
     * This should only be used for querying properties of the readers, and not
     * for actually obtaining a reader to use for reading data.  For that
     * purpose, use either getReader() or one of the openFile() methods, since
     * these actually return a new instance of the requested reader.
     *
     * @return An iterator for all loaded reader plugins.
     */
    public Iterator<TabularDataReader> iterator() {
        return new ReaderIterator(readers);
    }

    /**
     * Attempts to open the specified file with an appropriate reader plugin.
     * The testFile() method of the readers is used to find a reader that can
     * open the file.  If a reader for the file type is found, a new instance
     * of the reader is created and returned after opening the file.
     *
     * @param filepath The path of the data file to open.
     * @return A new instance of a reader if an appropriate reader is found that
     *         opens the file successfully. Otherwise, returns null.
     */
    public TabularDataReader openFile(String filepath, String defaultSheetName, String outputFolder) {
        // Check all readers to see if we have one that can read the
        // specified file.
        for (TabularDataReader reader : readers) {
            if (reader.testFile(filepath)) {
                    // A matching reader was found, so create a new instance of
                    // the reader, open the file with it, and return it.
                try{
                    TabularDataReader newreader = reader.getClass().newInstance();
                    newreader.openFile(filepath, defaultSheetName, outputFolder);
                    // Set input file
                    return newreader;
                } catch (InstantiationException e) {
                    throw new FIMSRuntimeException(500, e);
                } catch (IllegalAccessException e) {
                    throw new FIMSRuntimeException(500, e);
                }
            }
        }

        // no matching reader was found
        return null;
    }

    /**
     * Attempts to open a datafile with just the filepath... this works for Spreadsheets only
     * and kept as a legacy.  Other tab formats end up being converted to spreadsheets in the end.
     * @param filepath
     * @return
     */
    public TabularDataReader openFile(String filepath) {
        return openFile(filepath,null,null);
    }

    /**
     * Attempts to open a data file with a specified format.  If a reader
     * supporting the format is found, a new instance of the reader is created
     * and returned after opening the file.
     *
     * @param filepath     The path of the data file to open.
     * @param formatstring The format of the input file.
     * @return A new instance of a reader if a reader for the format is
     *         available and opens the file successfully. Otherwise, returns null.
     */
    public TabularDataReader openFile(String filepath, String formatstring) {
        // get the reader for the specified file format
        TabularDataReader reader = getReader(formatstring);

        if (reader != null) {
            // if a matching reader was found, try to open the specified file
            if (reader.openFile(filepath,null,null)) {
                return reader;
            } else {
                return null;
            }
        } else {
            // no matching reader was found
            return null;
        }
    }


    /**
     * An iterator for all reader plugins loaded by the ReaderManager.  Note
     * that as ReaderManager is currently implemented, defining a separate class
     * here for an iterator over all readers is not strictly necessary.  We
     * could have just as easily returned an iterator for the list used by
     * ReaderManager to keep track of loaded plugins.  The advantage of
     * explicitly defining an iterator class here is that it gives us more
     * flexibility if the ReaderManager implementation changes in the future or
     * if we need to do more complex things within the iterator itself.
     */
    private class ReaderIterator implements Iterator<TabularDataReader> {
        private LinkedList<TabularDataReader> readerlist;
        private int index;

        public ReaderIterator(LinkedList<TabularDataReader> readerlist) {
            this.readerlist = readerlist;
            index = 0;
        }

        public boolean hasNext() {
            return index < readerlist.size();
        }

        public TabularDataReader next() {
            if (hasNext()) {
                index++;
                return readerlist.get(index - 1);
            } else
                throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
