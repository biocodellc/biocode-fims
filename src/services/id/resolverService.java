package services.id;

import bcid.resolver;
import fimsExceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This is the core resolver Service for BCIDs.  It returns URIs
 */
@Path("ark:")
public class resolverService {

    String scheme = "ark:";
    @Context
    static ServletContext context;
    @Context
    static HttpServletRequest request;

    private static Logger logger = LoggerFactory.getLogger(resolverService.class);

    /**
     * User passes in an identifier of the form scheme:/naan/shoulder_identifier
     *
     * @param naan
     * @param shoulderPlusIdentifier
     *
     * @return
     */
    @GET
    @Path("/{naan}/{shoulderPlusIdentifier}")
    @Produces({MediaType.TEXT_HTML, "application/rdf+xml"})
    public Response run(
            @PathParam("naan") String naan,
            @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier,
            @HeaderParam("accept") String accept) {

        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the identifier element from path parameters
        String element = scheme + "/" + naan + "/" + shoulderPlusIdentifier;

        // When the Accept Header = "application/rdf+xml" return Metadata as RDF
        resolver r = new resolver(element);
        try {
            URI seeOtherUri = null;
            try {
                    seeOtherUri = r.resolveARK();

            } catch (URISyntaxException e) {
                logger.warn("URISyntaxException while trying to resolve ARK for element: {}", element, e);
                throw new BadRequestException("Server error while trying to resolve ARK. Did you supply a valid naan?");
            }

            // The expected response for IDentifiers without a URL
            return Response.status(Response.Status.SEE_OTHER).location(seeOtherUri).build();
        } finally {
            r.close();
        }
    }
}
