package services.id;

import bcid.Renderer.*;
import bcid.resolver;
import com.sun.jersey.api.view.Viewable;
import utils.SettingsManager;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * The resolver service searches for identifiers in the BCID system and in EZID and returns a JSON
 * representation of results containing identifier metadata.
 * One can parse the JSON result for "Error" for non-responses or bad identifiers.
 * This service is open to ALL and does not require authentication.
 * <p/>
 * Resolution determines if this is a Data Group, a Data Element with an encoded ID, or a
 * Data Element with a suffix.
 */
@Path("metadata")
public class resolverMetadataService {

    static SettingsManager sm;
    @Context
    static ServletContext context;

    /**
     * Load settings manager
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();
    }

    /**
     * User passes in an identifier of the form scheme:/naan/shoulder_identifier
     *
     * @param naan
     * @param shoulderPlusIdentifier
     * @return
     */
    @GET
    @Path("/{scheme}/{naan}/{shoulderPlusIdentifier}")
    @Produces({MediaType.TEXT_HTML, "application/rdf+xml"})
    public Response run(@PathParam("scheme") String scheme,
                        @PathParam("naan") String naan,
                        @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier,
                        @HeaderParam("Accept") String accept) {
        // Clean up input
        scheme = scheme.trim();
        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the identifier element from path parameters
        String element = scheme + "/" + naan + "/" + shoulderPlusIdentifier;

        // Setup ezid account/login information
       // NOTE: i don't think i need to call the EZID service function here
       /*
       EZIDService ezidService = new EZIDService();
        try {
            ezidService.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));
        } catch (EZIDException e) {
            // For now, just print stack trace here and proceed.
            e.printStackTrace();
        }

        System.out.println("eziduser = " + sm.retrieveValue("eziduser"));
        System.out.println("shoulderPlusIdentifier = " + shoulderPlusIdentifier);

*/
        // Return an appropriate response based on the Accepts header that was passed in.
        //
        resolver r = new resolver(element);
        if (accept.equalsIgnoreCase("application/rdf+xml")) {
            // Return RDF when the Accepts header specifies rdf+xml
            String response = r.printMetadata(new RDFRenderer());
            r.close();
            return Response.ok(response).build();
        } else {
            // This next section uses the Jersey Viewable class, which is a type of Model, View, Controller
            // construct, enabling us to pass content JSP code to a JSP template.  We do this in this section
            // so we can have a REST style call and provide human readable content with BCID header/footer
            String response = r.printMetadata(new HTMLTableRenderer());
            r.close();
            return Response.ok(response).build();
        }
    }
}
