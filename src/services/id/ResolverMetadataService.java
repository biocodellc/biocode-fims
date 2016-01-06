package services.id;

import bcid.Renderer.*;
import bcid.Resolver;
import biocode.fims.SettingsManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The Resolver service searches for identifiers in the BCID system and in EZID and returns a JSON
 * representation of results containing Bcid metadata.
 * One can parse the JSON result for "Error" for non-responses or bad identifiers.
 * This service is open to ALL and does not require authentication.
 * <p/>
 * Resolution determines if this is a Data Group, a Data Element with an encoded ID, or a
 * Data Element with a suffix.
 */
@Path("metadata")
public class ResolverMetadataService {

    static SettingsManager sm;
    @Context
    ServletContext context;

    /**
     * Load settings manager
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
    }

    /**
     * User passes in an Bcid of the form scheme:/naan/shoulder_identifier
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
                        @HeaderParam("Accept") String accept,
                        @Context HttpServletRequest request) {
        // Clean up input
        scheme = scheme.trim();
        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the Bcid element from path parameters
        String identifier = scheme + "/" + naan + "/" + shoulderPlusIdentifier;

        // Return an appropriate response based on the Accepts header that was passed in.
        //
        Resolver r = new Resolver(identifier);
        if (accept.equalsIgnoreCase("application/rdf+xml")) {
            // Return RDF when the Accepts header specifies rdf+xml
            String response = r.printMetadata(new RDFRenderer());
            r.close();
            return Response.ok(response).build();
        } else {
            // Get the username. This is needed to display private datasets
            HttpSession session = request.getSession();
            String username = (String) session.getAttribute("user");

            String response = r.printMetadata(new HTMLTableRenderer(username, r));
            r.close();
            return Response.ok(response).build();
        }
    }
}
