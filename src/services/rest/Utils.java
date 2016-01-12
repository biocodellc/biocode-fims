package services.rest;

import biocode.fims.config.ConfigurationFileFetcher;
import digester.Field;
import digester.Validation;
import org.apache.commons.digester3.Digester;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import run.Process;
import services.BiocodeFimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Iterator;


/**
 * Biocode-FIMS utility services
 */
@Path("utils/")
public class Utils extends BiocodeFimsService {
    /**
     * Retrieve the list of acceptable values for a given column/listName in a project
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/getListFields/{projectId}/{listName}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListFields(@PathParam("projectId") Integer projectId,
                                  @PathParam("listName") String listName,
                                  @QueryParam("column_name") String columnName) {

        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        String name = listName;
        if (columnName != null && !columnName.trim().equals("")) {
            name = columnName;
        }

        // Create a process object
        Process p = new Process(
                projectId,
                uploadPath(),
                configFile
        );

        Validation validation = new Validation();
        validation.addValidationRules(new Digester(), configFile);
        digester.List results = validation.findList(listName);
        JSONObject list = new JSONObject();

        if (results != null) {
            Iterator it = results.getFields().iterator();
            JSONArray fields = new JSONArray();

            // Get field values
            while (it.hasNext()) {
                Field f = (Field)it.next();
                fields.add(f.getValue());
            }

            list.put(name, fields);
        } else {
            // NO results mean no list has been defined!
            list.put("error", JSONValue.escape("No list has been defined for \"" + name + "\" but there is a rule saying it exists.  " +
                    "Please talk to your FIMS data manager to fix this"));
        }

        return Response.ok(list.toJSONString()).build();
    }

    @GET
    @Path("/getNAAN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNAAN() {
        String naan = sm.retrieveValue("naan");

        return Response.ok("{\"naan\": \"" + naan + "\"}").build();
    }
}

