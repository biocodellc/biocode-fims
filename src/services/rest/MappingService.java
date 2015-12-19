package services.rest;

import digester.Attribute;
import digester.Mapping;
import org.apache.commons.digester3.Digester;
import run.ConfigurationFileFetcher;
import run.Process;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.util.*;

/**
 * MappingService rest interface for Biocode-fims expedition
 */
@Path("mapping")
public class MappingService {
    @Context
    static ServletContext context;

    /**
     * return the column name uri's for a given project
     *
     * @param projectID
     * @return
     */
    @GET
    @Path("/filterOptions/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getFilterOptions(@PathParam("projectId") Integer projectID) {

        File configFile = new ConfigurationFileFetcher(projectID, uploadPath(), true).getOutputFile();

        // Create a process object
        Process p = new Process(
                projectID,
                uploadPath(),
                configFile
        );

        Mapping mapping = new Mapping();
        p.addMappingRules(new Digester(), mapping);
        ArrayList<Attribute> attributeArrayList = mapping.getAllAttributes(mapping.getDefaultSheetName());

        StringBuilder json = new StringBuilder();
        json.append("{\n\"attributes\": {\n");

        Iterator it = attributeArrayList.iterator();
        while (it.hasNext()) {
            Attribute a = (Attribute) it.next();
            json.append("\t\"" + a.getColumn() + "\":");
            json.append("\"" + a.getUri() + "\"");
            if (it.hasNext()) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("\t}\n}");

        return json.toString();
    }

    /**
     * Get real path of the uploads folder from context.
     * Needs context to have been injected before.
     *
     * @return Real path of the uploads folder with ending slash.
     */
    private static String uploadPath() {
        return context.getRealPath("tripleOutput") + File.separator;
    }

}