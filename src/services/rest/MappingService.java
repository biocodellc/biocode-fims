package services.rest;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import digester.*;
import digester.List;
import org.apache.commons.digester3.Digester;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import run.Process;
import services.BiocodeFimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.util.*;

/**
 * MappingService rest interface for Biocode-fims expedition
 */
@Path("mapping")
public class MappingService extends BiocodeFimsService {

    /**
     * return the column name uri's for a given project
     *
     * @param projectID
     * @return
     */
    @GET
    @Path("/filterOptions/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilterOptions(@PathParam("projectId") Integer projectID) {

        File configFile = new ConfigurationFileFetcher(projectID, uploadPath(), true).getOutputFile();

        // Create a process object
        Process p = new Process(
                projectID,
                uploadPath(),
                configFile
        );

        Mapping mapping = new Mapping();
        mapping.addMappingRules(new Digester(), configFile);
        ArrayList<Attribute> attributeArrayList = mapping.getAllAttributes(mapping.getDefaultSheetName());

        JSONArray attributes = new JSONArray();

        Iterator it = attributeArrayList.iterator();
        while (it.hasNext()) {
            Attribute a = (Attribute) it.next();
            JSONObject attribute = new JSONObject();
            attribute.put("column", a.getColumn());
            attribute.put("uri", a.getUri());

            attributes.add(attribute);
        }

        return Response.ok(attributes.toJSONString()).build();
    }

    @GET
    @Path("/attributes/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAttributes(@PathParam("projectId") Integer projectID) {
        File configFile = new ConfigurationFileFetcher(projectID, uploadPath(), true).getOutputFile();

        // Create a process object
        Process p = new Process(
                projectID,
                uploadPath(),
                configFile
        );

        Mapping mapping = new Mapping();
        mapping.addMappingRules(new Digester(), configFile);

        JSONObject response = new JSONObject();
        response.put("sheetName", mapping.getDefaultSheetName());
        response.put("attributes", mapping.getAllAttributesJSON(mapping.getDefaultSheetName()));
        return Response.ok(response.toJSONString()).build();
    }

    @GET
    @Path("/validation/lists/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getValidationLists(@PathParam("projectId") Integer projectId) {
        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        // Create a process object
        Process p = new Process(
                projectId,
                uploadPath(),
                configFile
        );

        Validation validation = new Validation();
        validation.addValidationRules(new Digester(), configFile);

        LinkedList<List> lists = validation.getLists();
        JSONArray response = new JSONArray();
        for (Object l: lists) {
            List list = (List) l;
            JSONObject listObject = new JSONObject();
            java.util.List<Field> fields = list.getFields();
            JSONArray fieldsArray = new JSONArray();
            for (Object f: fields) {
                Field field = (Field) f;
                JSONObject fd = new JSONObject();
                fd.put("uri", field.getUri());
                fd.put("value", field.getValue());
                fieldsArray.add(fd);
            }
            listObject.put("alias", list.getAlias());
            listObject.put("fields", fieldsArray);
            response.add(listObject);
        }

        return Response.ok(response.toJSONString()).build();
    }
}