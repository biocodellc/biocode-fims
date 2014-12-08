package reader.plugins;


import org.apache.poi.ss.usermodel.Sheet;
import settings.FIMSException;

import java.io.File;
import java.sql.SQLException;

/**
 * The interface for data reader plugins in the triplifier system.  This
 * interface considers a data source to be a set of tables, each of which
 * contains 0 or more rows of data.  The methods hasNextTable() and
 * moveToNextTable() are used to examine each table in the data source, and the
 * methods tableHasNextRow() and tableGetNextRow() are used to iterate through
 * all rows in the active table.
 */
public interface TabularDataReader {
    public java.util.List<String> getColNames();

    public Sheet getSheet();

    public Integer getNumRows();

    public File getInputFile();

    public String getStringValue(String column, int row);

    public String getStringValue(int col, int row);

    public Double getDoubleValue(String column, int row);

    public Integer getColumnPosition(String colName);

    public void setTable(String table) throws FIMSException;
    /**
     * Get a short string identifying the file format(s) supported by this
     * reader.  This string can be treated as a constant that is used to request
     * this reader from a ReaderManager, via ReaderManager's getReader() method.
     *
     * @return A short string that identifies the file format(s) supported by
     *         this reader.
     */
    public String getFormatString();

    /**
     * Get a short, human-friendly description of the file format(s) supported
     * by this reader.  The value returned by this method should be appropriate
     * for use in dialogs, such as file choosers.
     *
     * @return A short, human-readable description of the file format(s)
     *         supported by this reader.
     */
    public String getShortFormatDesc();

    /**
     * Get a human-friendly description of the file format(s) supported by this
     * reader.  This should be a longer, more informative description than the
     * value returned by getFormatString().
     *
     * @return A human-readable description of the file format(s) supported by
     *         this reader.
     */
    public String getFormatDescription();

    /**
     * Get the standard file extension(s) for the file formats supported by this
     * TabularDataReader.
     *
     * @return An array of file extensions (given as Strings) for the file
     *         formats supported by this reader.
     */
    public String[] getFileExtensions();

    /**
     * Test the specified file to see if it is in a format supported by this
     * TabularDataReader.  If so, return true, otherwise, return false.
     *
     * @param filepath The path to a source data file.
     * @return True if the file format is supported by this reader, false
     *         otherwise.
     */
    public boolean testFile(String filepath);

    /**
     * Open the specified file for reading.  Returns true if the file was opened
     * successfully.
     *
     * @param filepath A file from which to read data.
     * @param defaultSheetName A defaultSheetName to Use
     * @param outputFolder the default output folder

     * @return True if the file was opened and is ready to read data from; false
     *         otherwise.
     */
    public boolean openFile(String filepath, String defaultSheetName, String outputFolder);

    /**
     * Test if there is at least one table waiting to be processed in the
     * data source.  This should always return true immediately after a new data
     * source is opened (i.e., before any data has been read).
     *
     * @return True if the data source has at least one more table waiting to be
     *         processed; false otherwise.
     */
    public boolean hasNextTable();

    /**
     * Set the active table to the next table in the data source.  After
     * opening a data source, this method must be called in order to read the
     * first table from the data source, even if it contains only one table
     * (e.g., CSV files).
     */
    public void moveToNextTable();

    /**
     * Get the name of the active table in the data source.
     *
     * @return The name of the active table.
     */
    public String getCurrentTableName();

    /**
     * Test if there is at least one more row of data waiting to be read from
     * the active table of the opened data source.
     *
     * @return True if the data source has at least one more row of data to
     *         read; false otherwise.
     */
    public boolean tableHasNextRow();

    /**
     * Get the next row of data from the active table of the data source.  The
     * row is returned as an array of Strings, where each element of the array
     * represents one column in the source data.
     *
     * @return The next row of data from the data source.
     */
    public String[] tableGetNextRow() throws FIMSException;

    /**
     * Close the open data source, if there is one.
     */
    public void closeFile();
}
