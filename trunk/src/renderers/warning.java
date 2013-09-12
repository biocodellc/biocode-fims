package renderers;

import digester.Rule;
import renderers.Message;

public class warning extends Message {
    public warning(int row, Rule r, String message) {
        this.row = row;
        this.r = r;
        this.message = message;
    }

    public warning(Rule r, String message) {
        this.r = r;
        this.message = message;
        this.row = 0;
    }


}
