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
            // NOTE: Turning OFF RDF/XML Header resolution and instead opting to use this for
            // RDF/XML Data
            //if (accept.equalsIgnoreCase("application/rdf+xml")) {
            //    return Response.ok(r.printMetadata(new RDFRenderer())).build();
            // All other Accept Headers, or none specified, then attempt a redirect

            // Use this for accept header = "text/tab-separated-values"
            //} else if {

            //} else {
            URI seeOtherUri = null;
            try {
                // Resolve data as RDF+XML
                //System.out.println("accept = " + accept);
                if (accept.equalsIgnoreCase("rdf+xml") ||
                        accept.equalsIgnoreCase("application/rdf+xml")) {
                    seeOtherUri = r.resolveARK();
                }
                // This is the default mechanism
                else {

                    seeOtherUri = r.resolveARK();
                    // If graph not null and no forwarding resolution we can return the dataset
                    if (r.graph != null && !r.forwardingResolution) {
                        seeOtherUri = r.resolveArkAs("tab");
                    }
                }
                //System.out.println("seeotheruri = " + seeOtherUri);

            } catch (URISyntaxException e) {
                logger.warn("URISyntaxException while trying to resolve ARK for element: {}", element, e);
                throw new BadRequestException("Server error while trying to resolve ARK. Did you supply a valid naan?");
            }
            // if the URI is null just print metadata
                /*System.out.println("value of seeOtherUri:" + seeOtherUri.toString() + "END");
                if (seeOtherUri == null || seeOtherUri.toString().contains("null")) {
                    try {
                        return Response.ok(new resolver(element).printMetadata(new RDFRenderer())).build();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Response.serverError().entity(new errorInfo(e, request).toJSON()).build();
                    }
                } */

            // The expected response for IDentifiers without a URL
            return Response.status(Response.Status.SEE_OTHER).location(seeOtherUri).build();
            //}
        } finally {
            r.close();
        }
    }
}
