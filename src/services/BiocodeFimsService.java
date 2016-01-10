package services;

import biocode.fims.SettingsManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.File;

/**
 * An abstract class that sets the necessary information when communicating with Biocode-Fims services
 */
public abstract class BiocodeFimsService {
    @Context
    protected ServletContext context;
    @Context
    protected HttpServletResponse response;
    @Context
    protected HttpServletRequest request;
    protected HttpSession session;

    @QueryParam("access_token")
    protected String accessToken;

    protected static SettingsManager sm;
    protected static String appRoot;

    static {
        sm = SettingsManager.getInstance();
        appRoot = sm.retrieveValue("appRoot", null);
    }

    @Context
    public void setSessionVariables(HttpServletRequest request) {
        session = request.getSession();
    }

    /**
     * Get real path of the uploads folder from context.
     * Needs context to have been injected before.
     *
     * @return Real path of the uploads folder with ending slash.
     */
    public String uploadPath() {
        return context.getRealPath("tripleOutput") + File.separator;
    }
}
