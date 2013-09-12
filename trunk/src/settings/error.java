package settings;

import digester.Rule;

public class error extends Message {

    public error(Rule r, String message) {
        this.r = r;
        this.message = message;
        this.row = 0;
    }

    public error(int row, Rule r, String message) {
        this.row = row;
        this.message = message;
        this.r = r;
    }
}
