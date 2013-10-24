package settings;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Manage input/output path designations.
 * This was created to handle input from command prompts and tells application where to read and write various files.
 */
public class PathManager {
    public File setFile(String path) throws Exception {
        return setPaths(path, true);
    }

    public File setDirectory(String path) throws Exception {
        return setPaths(path, false);
    }

    /**
     * Set a Directory
     * Handle all special cases here (beginning with File separator, ending with File separator, relative
     * paths, etc...)
     *
     * @param path
     * @return
     * @throws Exception
     */
    private File setPaths(String path, boolean file) throws Exception {
        String fullPath = null;

        // outputPath is specified somehow
        if (path != null && !path.equals("")) {
            String endCharacter = "";
            // Set ending character
            if (!path.endsWith(File.separator)) {
                // only set the end character for directories
                if (!file)
                    endCharacter = File.separator;
            }
            // Test beginning character and determine if this should be relative or not
            String operatingSystemName = System.getProperty("os.name");
            if (path.startsWith(File.separator) ||
                    (operatingSystemName != null && operatingSystemName.toLowerCase().contains("windows") && path.charAt(1) == ':')) {
                fullPath = path + endCharacter;
            } else {
                fullPath = System.getProperty("user.dir") + File.separator + path + endCharacter;
            }

        }
        // no output path specified
        else {
            fullPath = System.getProperty("user.dir") + File.separator;
        }

        File theDir = new File(fullPath);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            boolean result = theDir.mkdirs();

            if (!result) {
                throw new FileNotFoundException("Unable to create directory " + theDir.getAbsolutePath());
            }
        }
        return theDir;
    }

        /**
     * Create new file in given folder, add incremental number to base if filename already exists.
     *
     * @param pFilename Name of the file.
     * @return The new file.
     */
    public static File createUniqueFile(String pFilename, String pOutputFolder) throws Exception {

        // Get just the filename
        File fileFilename = new File(pFilename);
        String fileName = fileFilename.getName();

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1)
            dotIndex = fileName.length();
        String base = fileName.substring(0, dotIndex);
        String ext = fileName.substring(dotIndex);

        File file = new File(pOutputFolder, fileName);
        int i = 1;
        while (file.exists())
            file = new File(pOutputFolder, base + "." + i++ + ext);
        return file;
    }
    public static void main(String args[]) {
        PathManager pm = new PathManager();
        try {
            //fimsPrinter.out.println(pm.setFile("sampledata/biocode_template.xls").getName());
            //fimsPrinter.out.println(pm.setFile("../../../sampledata/biocode_template.xls"));
            fimsPrinter.out.println(pm.setDirectory("/Users/jdeck/tripleOutput/").toString());
            //fimsPrinter.out.println(pm.setDirectory("."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
