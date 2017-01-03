package services.id;

import bcid.Resolver;
import biocode.fims.fimsExceptions.BadRequestException;
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
 * This is the core Resolver Service for BCIDs.  It returns URIs
 */
@Path("ark:")
public class ResolverService {

    String scheme = "ark:";
    @Context
    ServletContext context;
    @Context
    HttpServletRequest request;

    private static Logger logger = LoggerFactory.getLogger(ResolverService.class);

    /**
     * User passes in an Bcid of the form scheme:/naan/shoulder_identifier
     *
     * @param naan
     * @param shoulderPlusIdentifier
     *
     * @return
     */
    @GET
    @Path("/{naan}/{shoulderPlusIdentifier}")
    @Produces({MediaType.APPLICATION_JSON, "application/rdf+xml"})
    public Response run(
            @PathParam("naan") String naan,
            @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier,
            @HeaderParam("accept") String accept) {

        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the Bcid element from path parameters
        String element = scheme + "/" + naan + "/" + shoulderPlusIdentifier;

        // When the Accept Header = "application/rdf+xml" return Metadata as RDF
        Resolver r = new Resolver(element);
        try {
            URI seeOtherUri = null;
            try {
                    seeOtherUri = r.resolveIdentifier();

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
