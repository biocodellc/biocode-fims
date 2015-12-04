package bcid.testData;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * testDataRow is a data structure to use with the testDataSet class
 */
public class testDataRow {
    public URI webAddress = null;
    public String localID = null;
    public int type;

    public testDataRow(String primaryDigitalResolver, String localID, int type) throws URISyntaxException {
        this.webAddress = new URI(primaryDigitalResolver);
        this.localID = localID;
        this.type = type;
    }
}
