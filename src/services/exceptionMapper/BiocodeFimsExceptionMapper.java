package services.exceptionMapper;

import biocode.fims.FimsExceptionMapper;
import run.ProcessController;

import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.File;

/**
 * class to catch an exception thrown from a rest service and map the necessary information to a request
 */
@Provider
public class BiocodeFimsExceptionMapper extends FimsExceptionMapper {

    @Override
    public Response toResponse(Exception e) {
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
        return super.toResponse(e);
    }
}
