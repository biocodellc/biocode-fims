package rest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.IOException;

/**
 * Created by rjewing on 4/12/14.
 */
@Path("authenticationService")
public class authenticationService {

    @GET
    @Path("access_token")
    public void access_token(@QueryParam("code") String code,
                             @Context HttpServletResponse response) throws IOException {
        System.out.print(code);
        response.sendRedirect("/index");
    }
}
