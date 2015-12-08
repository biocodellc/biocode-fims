package services.rest;

import com.sun.jersey.api.core.ExtendedUriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.processController;
import fimsExceptions.FIMSRuntimeException;
import settings.errorInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * class to catch an exception thrown from a rest service and map the necessary information to a request
 */
@Provider
public class exceptionMapper implements ExceptionMapper<Exception> {
    @Context
    static HttpServletRequest request;
    @Context
    static ExtendedUriInfo uriInfo;
    @Context
    static HttpHeaders httpHeaders;

    private static Logger logger = LoggerFactory.getLogger(exceptionMapper.class);

    @Override
    public Response toResponse( Exception e) {
        e.printStackTrace();
        logger.error(null, e);
        errorInfo errorInfo = getErrorInfo(e);
        String mediaType;

        HttpSession session = request.getSession();
        if (session != null) {
            processController pc = (processController) session.getAttribute("processController");
            if (pc != null) {
                //delete any tmp files that were created
                new File(pc.getInputFilename()).delete();

                //remove processController from session
                session.removeAttribute("processController");
            }
        }

        // check if the called service is expected to return HTML of JSON
        // try to get the mediaType of the matched method. If an exception was thrown before the resource was constructed
        // then getMatchedMethod will return null. If that's the case then we should look to the accept header for the
        // correct response type.
        try {
            mediaType = uriInfo.getMatchedMethod().getSupportedOutputTypes().get(0).toString();
        } catch(NullPointerException ex) {
            List<MediaType> accepts = httpHeaders.getAcceptableMediaTypes();
            logger.warn("NullPointerException thrown while retrieving mediaType in exceptionMapper.java");
            // if request accepts JSON, return the error in JSON, otherwise use html
            if (accepts.contains(MediaType.APPLICATION_JSON_TYPE)) {
                mediaType = MediaType.APPLICATION_JSON;
            } else {
                mediaType = MediaType.TEXT_HTML;
            }
        }

        if (mediaType.contains( MediaType.APPLICATION_JSON )) {
            return Response.status(errorInfo.getHttpStatusCode())
                    .entity(errorInfo.toJSON())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } else {
            try {
//              send the user to error.jsp to display info about the exception/error
                URI url = new URI("error.jsp");
                session.setAttribute("errorInfo", errorInfo);

                return Response.status(errorInfo.getHttpStatusCode())
                        .location(url)
                        .build();
            } catch (URISyntaxException ex) {
                logger.error("URISyntaxException forming url for bcid error page.", ex);
                return Response.status(errorInfo.getHttpStatusCode())
                        .entity(errorInfo.toJSON())
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

        }
    }

    // method to set the relevent information in errorInfo
    private errorInfo getErrorInfo(Exception e) {
        String usrMessage;
        String developerMessage = null;
        Integer httpStatusCode = getHttpStatus(e);

        if (e instanceof FIMSRuntimeException) {
            usrMessage = ((FIMSRuntimeException) e).getUsrMessage();
            developerMessage = ((FIMSRuntimeException) e).getDeveloperMessage();
        } else {
            usrMessage = "Server Error";
        }

        return new errorInfo(usrMessage, developerMessage, httpStatusCode, e);

    }

    private Integer getHttpStatus(Exception e) {
        // if the throwable is an instance of WebApplicationException, get the status code
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse().getStatus();
        } else if (e instanceof FIMSRuntimeException) {
            return ((FIMSRuntimeException) e).getHttpStatusCode();
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
    }

}
