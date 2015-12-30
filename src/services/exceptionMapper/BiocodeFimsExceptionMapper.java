package services.exceptionMapper;

import biocode.fims.ErrorInfo;
import biocode.fims.FimsExceptionMapper;
import run.ProcessController;

import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * class to catch an exception thrown from a rest service and map the necessary information to a request
 */
@Provider
public class BiocodeFimsExceptionMapper extends FimsExceptionMapper {

    @Override
    public Response toResponse(Exception e) {
        //TODO figure out how to extend the FimsExceptionMapper toResponse method and only delete the the tmp files here
        logException(e);
        ErrorInfo errorInfo = getErrorInfo(e);
        String mediaType;

        HttpSession session = request.getSession();
        if (session != null) {
            ProcessController pc = (ProcessController) session.getAttribute("processController");
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
            logger.warn("NullPointerException thrown while retrieving mediaType in BiocodeFimsExceptionMapper.java");
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
}
