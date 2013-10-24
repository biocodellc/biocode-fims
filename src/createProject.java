import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.api.representation.Form;

import javax.ws.rs.core.Cookie;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: jdeck
 * Date: 10/23/13
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class createProject {

    //String curlCxn = "curl --data 'j_username=USER&j_password=PSWD' http://biscicol.org/bcid/j_spring_security_check --location --cookie-jar cookies.txt";
    static String test = "http://biscicol.org/id/groupService/list";
    static String cxn = "http://biscicol.org/bcid/j_spring_security_check";

    public createProject(String username, String password) {

        /*
        Client client = Client.create();
        final HTTPBasicAuthFilter authFilter = new HTTPBasicAuthFilter(username, password);
        client.addFilter(authFilter);
        client.addFilter(new LoggingFilter());

        WebResource service = client.resource(cxn);
        Cookie cookie = new Cookie("hellocookie", "Hello Cookie");
        final ClientResponse blogResponse = service.cookie(cookie).post(ClientResponse.class);
        final String response = blogResponse.getEntity(String.class);

        System.out.println(response);
        */
    }

    public static void main(String[] args) {
        //createProject createProject = new createProject("demo", "demo");


        //String URL_LOGIN = "http://localhost:9080/foo/j_security_check";
        String URL_LOGIN = cxn;
        String URL_DATA = test;
        Client client = Client.create();

        // add a filter to set cookies received from the server and to check if login has been triggered
        client.addFilter(new ClientFilter() {
            private ArrayList<Object> cookies;

            @Override
            public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
                if (cookies != null) {
                    request.getHeaders().put("Cookie", cookies);
                }
                ClientResponse response = getNext().handle(request);
                // copy cookies
                if (response.getCookies() != null) {
                    if (cookies == null) {
                        cookies = new ArrayList<Object>();
                    }
                    // A simple addAll just for illustration (should probably check for duplicates and expired cookies)
                    cookies.addAll(response.getCookies());
                }
                return response;
            }
        });

        String username = "demo";
        String password = "demo";

        // Login:
        WebResource webResource = client.resource(URL_LOGIN);

        com.sun.jersey.api.representation.Form form = new Form();
        form.putSingle("j_username", username);
        form.putSingle("j_password", password);
        webResource.type("application/x-www-form-urlencoded").post(form);

        System.out.println(webResource.get(String.class));

        // Get the protected web page:
        webResource = client.resource(URL_DATA);
        String response = webResource.get(String.class);

        System.out.println("response = " + response + webResource.toString());
    }

}
