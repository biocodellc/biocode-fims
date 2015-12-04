package util;

import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import utils.SettingsManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;

/**
 * A data class to provide information to the user about exceptions thrown in a service
 */
public class errorInfo {
    private Integer httpStatusCode;
    private Object usrMessage;
    private Object developerMessage;
    private Exception e;
    private Timestamp ts;

    public errorInfo(String usrMessage, String developerMessage, int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
        this.ts = new Timestamp(new java.util.Date().getTime());
    }

    public errorInfo(String usrMessage,String developerMessage, int httpStatusCode, Exception e) {
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
        this.e = e;
        this.ts = new Timestamp(new java.util.Date().getTime());
    }

    public errorInfo(String usrMessage, int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.ts = new Timestamp(new java.util.Date().getTime());
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getMessage() {
        return usrMessage.toString();
    }

    public String toJSON() {
        JSONObject obj = new JSONObject();
        convertToNull();

        obj.put("usrMessage", usrMessage);
        obj.put("developerMessage", developerMessage);
        obj.put("httpStatusCode", httpStatusCode);
        obj.put("time", ts.toString());

        if (e != null) {
            SettingsManager sm = SettingsManager.getInstance();
            sm.loadProperties();
            String debug = sm.retrieveValue("debug", "false");

            if (debug.equalsIgnoreCase("true")) {
                obj.put("exceptionMessage", e.getMessage());
                obj.put("stackTrace", getStackTraceString());
            }
        }
        return obj.toString(4);
    }

    // JSONObject doesn't accept regular null object, so must convert them to JSONNull
    private void convertToNull() {
        if (usrMessage == null) usrMessage = JSONNull.getInstance();
        if (developerMessage == null) developerMessage = JSONNull.getInstance();
    }

    public String toHTMLTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<th colspan=2><font color=#d73027>Server Error</font></th>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Message:</td>\n");
        sb.append("\t\t<td>" + usrMessage + "</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Status Code:</td>\n");
        sb.append("\t\t<td>" + httpStatusCode + "</td>\n");
        sb.append("\t</tr>\n");

        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        String debug = sm.retrieveValue("debug", "false");

        if (debug.equalsIgnoreCase("true")) {

            sb.append("\t<tr>\n");
            sb.append("\t\t<td>time:</td>\n");
            sb.append("\t\t<td>" + ts + "</td>\n");
            sb.append("\t</tr>\n");

            if (e != null) {
                sb.append("\t<tr>\n");
                sb.append("\t\t<td>Exception Message:</td>\n");
                sb.append("\t\t<td>" + e.getMessage() + "</td>\n");
                sb.append("\t</tr>\n");

                sb.append("\t<tr>\n");
                sb.append("\t\t<td>Stacktrace:</td>\n");
                sb.append("\t\t<td>" + getStackTraceString() + "</td>\n");
                sb.append("\t</tr>\n");
            }
        }

        sb.append("</table>");

        return sb.toString();

    }

    // returns the full stackTrace as a string
    private String getStackTraceString() {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}
