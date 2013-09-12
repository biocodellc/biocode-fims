package renderers;

import digester.Rule;
import renderers.Message;

public class valid extends Message {
    /**
     * @param r Rule
     */
    public valid(Rule r) {
        this.r = r;
        this.row = 0;
    }

    /**
     * @return return message
     */
    public String getLineMessage() {
        return r.getColumn();
    }
}
