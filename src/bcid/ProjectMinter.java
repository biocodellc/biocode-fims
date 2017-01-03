package bcid;

import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import biocode.fims.SettingsManager;

import java.sql.*;
import java.util.*;
import java.util.Hashtable;

/**
 * Mint new expeditions.  Includes the automatic creation of a core set of entity types
 */
public class ProjectMinter {
    protected Connection conn;
    Database db;
    public ArrayList<Integer> expeditionResources;
    private SettingsManager sm;

    private static Logger logger = LoggerFactory.getLogger(ProjectMinter.class);


    /**
     * The constructor defines the class-level variables used when minting Expeditions.
     * It defines a generic set of entities (process, information content, objects, agents)
     * that can be used for any expedition.
     */
    public ProjectMinter() {
        db = new Database();
        conn = db.getConn();

        // Initialize settings manager
        sm = SettingsManager.getInstance();
    }

    public void close() {
        db.close();
    }
    /**
     * Find the BCID that denotes the validation file location for a particular expedition
     *
     * @param projectId defines the projectId to lookup
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public String getValidationXML(Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String query = "select \n" +
                    "validationXml\n" +
                    "from \n" +
                    " projects\n" +
                    "where \n" +
                    "projectId=?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getString("validationXml");
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting Configuration File", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * List all the defined projects
     *
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public String listProjects(Integer userId) {
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String query = "SELECT \n" +
                    "\tp.projectId,\n" +
                    "\tp.projectCode,\n" +
                    "\tp.projectTitle,\n" +
                    "\tp.validationXml\n" +
                    " FROM \n" +
                    "\tprojects p\n" +
                    " WHERE \n" +
                    "\tp.public = true\n";


            if (userId != null) {
                query += " UNION \n" +
                        " SELECT \n" +
                        "\tp.projectId,\n" +
                        "\tp.projectCode,\n" +
                        "\tp.projectTitle, \n" +
                        "\tp.validationXml\n" +
                        " FROM \n" +
                        "\tprojects p, userProjects u\n" +
                        " WHERE \n" +
                        "\t(p.projectId = u.projectId AND u.userId = ?)\n" +
                        " ORDER BY \n" +
                        "\tprojectId";
            }
            stmt = conn.prepareStatement(query);

            if (userId != null) {
                stmt.setInt(1, userId);
            }
            rs = stmt.executeQuery();

            sb.append("{\n");
            sb.append("\t\"projects\": [\n");
            while (rs.next()) {
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"projectId\":\"" + rs.getString("projectId") + "\",\n");
                sb.append("\t\t\t\"projectCode\":\"" + rs.getString("projectCode") + "\",\n");
                sb.append("\t\t\t\"projectTitle\":\"" + rs.getString("projectTitle") + "\",\n");
                sb.append("\t\t\t\"validationXml\":\"" + rs.getString("validationXml") + "\"\n");
                sb.append("\t\t}");
                if (!rs.isLast())
                    sb.append(",\n");
                else
                    sb.append("\n");
            }
            sb.append("\t]\n}");

            return sb.toString();

        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting list of all projects.", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
        * List all the defined projects
        *
        * @return returns the BCID for this expedition and conceptURI combination
        */
       public ArrayList<Integer> getAllProjects() {
           ArrayList<Integer> projects = new ArrayList<Integer>();
           PreparedStatement stmt = null;
           ResultSet rs = null;

           try {
               String sql = "SELECT projectId FROM projects";
               stmt = conn.prepareStatement(sql);
               rs = stmt.executeQuery();
               while (rs.next()) {
                   projects.add(rs.getInt("projectId"));
               }
               return projects;
           } catch (SQLException e) {
               throw new ServerErrorException("Trouble getting project List", e);
           } finally {
               db.close(stmt, rs);
           }
       }

    /**
     * A utility function to get the very latest graph loads for each expedition
     * This is a public accessible function from the REST service so it only returns results that are declared as public
     *
     * @param projectId pass in an project Bcid to limit the set of expeditions we are looking at
     * @return
     */
    public JSONArray getLatestGraphs(int projectId, String username) {
        StringBuilder sb = new StringBuilder();
        JSONArray graphs = new JSONArray();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Construct the query
            // This query is built to give us a groupwise maximum-- we want the graphs that correspond to the
            // maximum timestamp (latest) loaded for a particular expedition.
            // Help on solving this problem came from http://jan.kneschke.de/expeditions/mysql/groupwise-max/
            String sql = "select p.expeditionCode as expeditionCode,p.expeditionTitle,b1.graph as graph,b1.ts as ts, b1.webAddress as webAddress, b1.identifier as identifier, b1.bcidId as id, p.projectId as projectId \n" +
                    "from bcids as b1, \n" +
                    "(select p.expeditionCode as expeditionCode,b.graph as graph,max(b.ts) as maxts, b.webAddress as webAddress, b.identifier as identifer, b.bcidId as id, p.projectId as projectId \n" +
                    "    \tfrom bcids b,expeditions p, expeditionBcids eB\n" +
                    "    \twhere eB.bcidId=b.bcidId\n" +
                    "    \tand eB.expeditionId=p.expeditionId\n" +
                    " and b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    and p.projectId = ?\n" +
                    "    \tgroup by p.expeditionCode) as  b2,\n" +
                    "expeditions p,  expeditionBcids eB\n" +
                    "where p.expeditionCode = b2.expeditionCode and b1.ts = b2.maxts\n" +
                    " and eB.bcidId=b1.bcidId\n" +
                    " and eB.expeditionId=p.expeditionId\n" +
                    " and b1.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    and p.projectId =?";

            // Enforce restriction on viewing particular bcids -- this is important for protected bcids
            if (username != null) {
                sql += "    and (p.public = 1 or p.userId = ?)";
            } else {
                sql += "    and p.public = 1";
            }

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setInt(2, projectId);

            // Enforce restriction on viewing particular bcids -- this is important for protected bcids
            if (username != null) {
                Integer userId = db.getUserId(username);
                stmt.setInt(3, userId);
            }

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject graph = new JSONObject();
                // Grap the prefixes and concepts associated with this
                sb.append("\t\t{\n");
                graph.put("expeditionCode", rs.getString("expeditionCode"));
                graph.put("expeditionTitle", rs.getString("expeditionTitle"));
                graph.put("ts", rs.getString("ts"));
                graph.put("identifier", rs.getString("identifier"));
                graph.put("bcidId", rs.getString("id"));
                graph.put("projectId", rs.getString("projectId"));
                graph.put("webAddress", rs.getString("webAddress"));
                graph.put("graph", rs.getString("graph"));

                graphs.add(graph);
            }
            return graphs;
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting latest graphs.", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    public static void main(String args[]) {
        // See if the user owns this expedition or no
        ProjectMinter project = new ProjectMinter();
        //System.out.println(project.listProjects());
//        System.out.println("datasets = \n" + project.getMyTemplatesAndDatasets("demo"));
        System.out.println("datasets = \n" + project.getMyLatestGraphs("demo"));
        project.close();
    }

    /**
     * Return a JSON representation of the projects a user is an admin for
     *
     * @param username
     * @return
     */
    public String listUserAdminProjects(String username) {
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Integer userId = db.getUserId(username);

            String sql = "SELECT projectId, projectCode, projectTitle, projectTitle, validationXml FROM projects WHERE userId = \"" + userId + "\"";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            sb.append("{\n");
            sb.append("\t\"projects\": [\n");
            while (rs.next()) {
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"projectId\":\"" + rs.getString("projectId") + "\",\n");
                sb.append("\t\t\t\"projectCode\":\"" + rs.getString("projectCode") + "\",\n");
                sb.append("\t\t\t\"projectTitle\":\"" + rs.getString("projectTitle") + "\",\n");
                sb.append("\t\t\t\"validationXml\":\"" + rs.getString("validationXml") + "\"\n");
                sb.append("\t\t}");
                if (!rs.isLast())
                    sb.append(",\n");
                else
                    sb.append("\n");
            }
            sb.append("\t]\n}");

            return sb.toString();
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * return a JSON representation of the projects that a user is a member of
     * @param username
     * @return
     */
    public JSONArray listUsersProjects(String username) {
        JSONArray projects = new JSONArray();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Integer userId = db.getUserId(username);

            String sql = "SELECT p.projectId, p.projectCode, p.projectTitle, p.validationXml FROM projects p, userProjects u WHERE p.projectId = u.projectId && u.userId = \"" + userId + "\"";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            while (rs.next()) {
                JSONObject project = new JSONObject();
                project.put("projectId", rs.getString("projectId"));
                project.put("projectCode", rs.getString("projectCode"));
                project.put("projectTitle", rs.getString("projectTitle"));
                project.put("validationXml", rs.getString("validationXml"));
                projects.add(project);
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        return projects;
    }

    /**
     * return an HTML table of a project's configuration.
     * @param projectId
     * @param username
     * @return
     */
    public String getProjectConfigAsTable(Integer projectId, String username) {
        StringBuilder sb = new StringBuilder();
        Hashtable<String, String> config = getProjectConfig(projectId, username);

        sb.append("<table>\n");
        sb.append("\t<tbody>\n");
        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Title:</td>\n");
        sb.append("\t\t\t<td>");
        sb.append(config.get("title"));
        sb.append("</td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Configuration File:</td>\n");
        sb.append("\t\t\t<td>");
        sb.append(config.get("validationXml"));
        sb.append("</td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Public Project</td>\n");
        sb.append("\t\t\t<td>\n");
        sb.append(config.get("public"));
        sb.append("</td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td><a href=\"javascript:void()\" id=\"edit_config\">Edit Configuration</a></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t</tbody>\n</table>\n");

        return sb.toString();
    }

    /**
     * return an HTML table in order to edit a project's configuration
     * @param projectId
     * @param username
     * @return
     */
    public String getProjectConfigEditorAsTable(Integer projectId, String username) {
        StringBuilder sb = new StringBuilder();
        Hashtable<String, String> config = getProjectConfig(projectId, username);

        sb.append("<form id=\"submitForm\" method=\"POST\">\n");
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");
        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Title</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" class=\"project_config\" name=\"title\" value=\""));
        sb.append(config.get("title"));
        sb.append("\"></td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Configuration File</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" class=\"project_config\" name=\"validationXml\" value=\""));
        sb.append(config.get("validationXml"));
        sb.append("\"></td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Public Project</td>\n");
        sb.append("\t\t\t<td><input type=\"checkbox\" name=\"public\"");
        if (config.get("public").equalsIgnoreCase("true")) {
            sb.append(" checked=\"checked\"");
        }
        sb.append("></td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td class=\"error\" align=\"center\">");
        sb.append("</td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append(("\t\t\t<td><input id=\"configSubmit\" type=\"button\" value=\"Submit\">"));
        sb.append("</td>\n\t\t</tr>\n");
        sb.append("\t</tbody>\n");
        sb.append("</table>\n");
        sb.append("</form>\n");

        return sb.toString();
    }

    /**
     * Update the project's configuration with the values in the Hashtable.
     * @param updateTable
     * @param projectId
     * @return
     */
    public Boolean updateConfig(Hashtable<String, String> updateTable, Integer projectId) {
        String updateString = "UPDATE projects SET ";

        // Dynamically create our UPDATE statement depending on which fields the user wants to update
        for (Enumeration e = updateTable.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement().toString();
            updateString += key + " = ?";

            if (e.hasMoreElements()) {
                updateString += ", ";
            } else {
                updateString += " WHERE projectId =\"" + projectId + "\";";
            }
        }
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(updateString);

            // place the parametrized values into the SQL statement
            {
                int i = 1;
                for (Enumeration e = updateTable.keys(); e.hasMoreElements(); ) {
                    String key = e.nextElement().toString();
                    if (key.equals("public")) {
                        if (updateTable.get(key).equalsIgnoreCase("true")) {
                            stmt.setBoolean(i, true);
                        } else {
                            stmt.setBoolean(i, false);
                        }
                    } else if (updateTable.get(key).equals("")) {
                        stmt.setString(i, null);
                    } else {
                        stmt.setString(i, updateTable.get(key));
                    }
                    i++;
                }
            }

            Integer result = stmt.executeUpdate();

            // result should be '1', if not, an error occurred during the UPDATE statement
            if (result == 1) {
                return true;
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, null);
        }
        return false;
    }

    /**
     * Return a hashTable of project configuration options for a given projectId and userId
     * @param projectId
     * @param username
     * @return
     */
    public Hashtable<String, String> getProjectConfig(Integer projectId, String username) {
        Hashtable<String, String> config = new Hashtable<String, String>();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Integer userId = db.getUserId(username);

            String sql = "SELECT projectTitle as title, public, validationXml as validationXml FROM projects WHERE projectId=?"
                    + " AND userId= ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setInt(2, userId);

            rs = stmt.executeQuery();
            if (rs.next()) {
                config.put("title", rs.getString("title"));
                config.put("public", String.valueOf(rs.getBoolean("public")));
                if (rs.getString("validationXml") != null) {
                    config.put("validationXml", rs.getString("validationXml"));
                }
            } else {
                throw new BadRequestException("You must be this project's admin in order to view its configuration.");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException retrieving project configuration for projectID: " +
                    projectId, e);
        } finally {
            db.close(stmt, rs);
        }
        return config;
    }

    /**
     * Check if a user belongs to a project
     */
    public Boolean userProject(Integer userId, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT count(*) as count " +
                    "FROM users u, projects p, userProjects uP " +
                    "WHERE u.userId=uP.userId and uP.projectId = p.projectId and u.userId = ? and p.projectId=?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();

            // If the user belongs to this project then there will be a >=1 value and returns true, otherwise false.
            return rs.getInt("count") >= 1;
        }  catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * Check if a user is a given project's admin
     * @param userId
     * @param projectId
     * @return
     */
    public Boolean userProjectAdmin(Integer userId, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT count(*) as count FROM projects WHERE userId = ? AND projectId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();

            return rs.getInt("count") >= 1;
        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * Remove a user from a project. Once removed, a user can no longer create/view expeditions in the project.
     * @param userId
     * @param projectId
     * @return
     */
    public void removeUser(Integer userId, Integer projectId) {
        PreparedStatement stmt = null;
        try {
            String sql = "DELETE FROM userProjects WHERE userId = ? AND projectId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while removing user", e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * Add a user as a member to the project. This user can then create expeditions in this project.
     * @param userId
     * @param projectId
     * @return
     */
    public void addUserToProject(Integer userId, Integer projectId) {
        PreparedStatement stmt = null;

        try {
            String insertStatement = "INSERT INTO userProjects (userId, projectId) VALUES(?,?)";
            stmt = conn.prepareStatement(insertStatement);

            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while adding user to project.", e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * return an HTML table of all the members of a given project.
     * @param projectId
     * @return
     */
    public String listProjectUsersAsTable(Integer projectId) {
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String userProjectSql = "SELECT userId FROM userProjects WHERE projectId = \"" + projectId + "\"";
            String userSql = "SELECT username, userId FROM users";
            String projectSql = "SELECT projectTitle FROM projects WHERE projectId = \"" + projectId + "\"";
            List projectUsers = new ArrayList();
            stmt = conn.prepareStatement(projectSql);

            rs = stmt.executeQuery();
            rs.next();
            String projectTitle = rs.getString("projectTitle");

            db.close(stmt, rs);

            sb.append("\t<form method=\"POST\">\n");

            sb.append("<table data-projectId=\"" + projectId + "\" data-projectTitle=\"" + projectTitle + "\">\n");
            sb.append("\t<tr>\n");
            stmt = conn.prepareStatement(userProjectSql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer userId = rs.getInt("userId");
                String username = db.getUserName(userId);
                projectUsers.add(userId);
                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append(username);
                sb.append("</td>\n");
                sb.append("\t\t<td><a id=\"remove_user\" data-userId=\"" + userId + "\" data-username=\"" + username + "\" href=\"javascript:void();\">(remove)</a> ");
                sb.append("<a id=\"edit_profile\" data-username=\"" + username + "\" href=\"javascript:void();\">(edit)</a></td>\n");
                sb.append("\t</tr>\n");
            }

            sb.append("\t<tr>\n");
            sb.append("\t\t<td>Add User:</td>\n");
            sb.append("\t\t<td>");
            sb.append("<select name=userId>\n");
            sb.append("\t\t\t<option value=\"0\">Create New User</option>\n");

            db.close(stmt, rs);

            stmt = conn.prepareStatement(userSql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer userId = rs.getInt("userId");
                if (!projectUsers.contains(userId)) {
                    sb.append("\t\t\t<option value=\"" + userId + "\">" + db.getUserName(userId) + "</option>\n");
                }
            }
            db.close(stmt, rs);

            sb.append("\t\t</select></td>\n");
            sb.append("\t</tr>\n");
            sb.append("\t<tr>\n");
            sb.append("\t\t<td></td>\n");
            sb.append("\t\t<td><div class=\"error\" align=\"center\"></div></td>\n");
            sb.append("\t</tr>\n");
            sb.append("\t<tr>\n");
            sb.append("\t\t<td><input type=\"hidden\" name=\"projectId\" value=\"" + projectId + "\"></td>\n");
            sb.append("\t\t<td><input type=\"button\" value=\"Submit\" onclick=\"projectUserSubmit(\'" + projectTitle.replaceAll(" ", "_") + '_' + projectId + "\')\"></td>\n");
            sb.append("\t</tr>\n");

            sb.append("</table>\n");
            sb.append("\t</form>\n");

            return sb.toString();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error retrieving project users.", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * A utility function to get all the datasets that belong to a user
     * This function returns only Expeditions that have loaded Datasets.
     *
     * @return
     */
    public String getMyTemplatesAndDatasets(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        HashMap projectMap = new HashMap();

        // We need a username
        if (username == null) {
            throw new ServerErrorException("server error", "username can't be null");
        }
        Integer userId = db.getUserId(username);


        try {
            String sql1 = "select e.expeditionTitle, p.projectTitle from expeditions e, projects p " +
                    "where e.userId = ? and p.projectId = e.projectId";
            stmt = conn.prepareStatement(sql1);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                String projectTitle = rs.getString("projectTitle");
                String expeditionTitle = rs.getString("expeditionTitle");

                if (projectTitle != null && !projectTitle.isEmpty()) {
                    // if the project isn't in the map, then add it
                    if (!projectMap.containsKey(projectTitle)) {
                        projectMap.put(projectTitle, new JSONObject());
                    }

                    // if the expedition isn't in the project then add it
                    JSONObject p = (JSONObject) projectMap.get(projectTitle);
                    if (!p.containsKey(expeditionTitle)) {
                        p.put(expeditionTitle, new JSONArray());
                    }

                }

            }

            String sql2 = "select e.expeditionCode, e.expeditionTitle, b.ts, b.identifier as identifier, b.bcidId as id, b.finalCopy, e.projectId, p.projectTitle\n" +
                    "from bcids b, expeditions e,  expeditionBcids eB, projects p\n" +
                    "where b.userId = ? and b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    " and eB.bcidId=b.bcidId\n" +
                    " and e.expeditionId=eB.expeditionId\n" +
                    " and p.projectId=e.projectId\n" +
                    " order by projectId, expeditionCode, ts desc";

            stmt = conn.prepareStatement(sql2);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject dataset = new JSONObject();
                String projectTitle = rs.getString("projectTitle");
                String expeditionTitle = rs.getString("expeditionTitle");

                // Grap the prefixes and concepts associated with this
                dataset.put("ts", rs.getString("ts"));
                dataset.put("bcidsId", rs.getString("id"));
                dataset.put("identifier", rs.getString("identifier"));
                dataset.put("finalCopy", rs.getString("finalCopy"));

                JSONObject p = (JSONObject) projectMap.get(projectTitle);
                ((JSONArray) p.get(expeditionTitle)).add(dataset);
            }

            return JSONValue.toJSONString(projectMap);
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting users datasets.", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * A utility function to get the very latest graph loads that belong to a user
     *
     * @return
     */
    public String getMyLatestGraphs(String username) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        JSONObject response = new JSONObject();
        HashMap projectMap = new HashMap();
        JSONArray projectDatasets;

        JSONArray projects = listUsersProjects(username);

        for (Object p:  projects) {
            JSONObject project = (JSONObject) p;
            projectMap.put(project.get("projectTitle"), new JSONArray());
        }

        // We need a username
        if (username == null) {
            throw new ServerErrorException("server error", "username can't be null");
        }

        try {
            // Construct the query
            // This query is built to give us a groupwise maximum-- we want the graphs that correspond to the
            // maximum timestamp (latest) loaded for a particular expedition.
            // Help on solving this problem came from http://jan.kneschke.de/expeditions/mysql/groupwise-max/
            String sql = "select e.expeditionCode, e.expeditionTitle, b1.graph, b1.ts, b1.bcidId as id, b1.webAddress as webAddress, b1.identifier as identifier, e.projectId, e.public, p.projectTitle\n" +
                    "from bcids as b1, \n" +
                    "(select e.expeditionCode as expeditionCode,b.graph as graph,max(b.ts) as maxts, b.webAddress as webAddress, b.identifier as identifier, b.bcidId as id, e.projectId as projectId \n" +
                    "    \tfrom bcids b,expeditions e, expeditionBcids eB\n" +
                    "    \twhere eB.bcidId=b.bcidId\n" +
                    "    \tand eB.expeditionId=e.expeditionId\n" +
                    " and b.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    \tgroup by e.expeditionCode) as  b2,\n" +
                    "expeditions e,  expeditionBcids eB, projects p\n" +
                    "where e.expeditionCode = b2.expeditionCode and b1.ts = b2.maxts\n" +
                    " and eB.bcidId=b1.bcidId\n" +
                    " and eB.expeditionId=e.expeditionId\n" +
                    " and p.projectId=e.projectId\n" +
                    " and b1.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    and e.userId = ?";

            stmt = conn.prepareStatement(sql);
            Integer userId = db.getUserId(username);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject dataset = new JSONObject();
                String projectTitle = rs.getString("projectTitle");

                // Grap the prefixes and concepts associated with this
                dataset.put("expeditionCode", rs.getString("expeditionCode"));
                dataset.put("expeditionTitle", rs.getString("expeditionTitle"));
                dataset.put("ts", rs.getString("ts"));
                dataset.put("bcidsId", rs.getString("id"));
                dataset.put("projectId", rs.getString("projectId"));
                dataset.put("graph", rs.getString("graph"));
                dataset.put("public", rs.getString("public"));
                dataset.put("identifier", rs.getString("identifier"));
                dataset.put("webAddress", rs.getString("webAddress"));


                if (projectTitle != null && !projectTitle.isEmpty()) {
                    projectDatasets = (JSONArray) projectMap.get(projectTitle);
                    // TODO What should we do if a projectTitle shows up that wasn't before fetched? ignore it? or add the project?
                    if (projectDatasets == null) {
                        projectDatasets = new JSONArray();
                    }
                    projectDatasets.add(dataset);
                    projectMap.put(projectTitle, projectDatasets);
                }
            }

            return JSONValue.toJSONString(projectMap);
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting latest graphs.", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * save a template generator configuration
     * @param configName
     * @param projectId
     * @param userId
     * @param checkedOptions
     */
    public void saveTemplateConfig(String configName, Integer projectId, Integer userId, List<String> checkedOptions) {
        PreparedStatement stmt = null;

        try {
            String insertStatement = "INSERT INTO templateConfigs (userId, projectId, configName, config) " +
                    "VALUES(?,?,?,?)";
            stmt = conn.prepareStatement(insertStatement);

            stmt.setInt(1, userId);
            stmt.setInt(2, projectId);
            stmt.setString(3, configName);
            stmt.setString(4, JSONValue.toJSONString(checkedOptions));

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while saving template config.", e);
        } finally {
            db.close(stmt, null);
        }
    }

    /**
     * check if a config exists
     * @param configName
     * @param projectId
     * @return
     */
    public boolean configExists(String configName, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT count(*) as count " +
                    "FROM templateConfigs " +
                    "WHERE configName = ? and projectId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, configName);
            stmt.setInt(2, projectId);

            rs = stmt.executeQuery();
            rs.next();

            if (rs.getInt("count") > 0) {
                return true;
            }
            return false;
        }  catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * check if a user owns the config
     */
    public boolean usersConfig(String configName, Integer projectId, Integer userId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT count(*) as count " +
                    "FROM templateConfigs " +
                    "WHERE configName = ? and projectId = ? and userId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, configName);
            stmt.setInt(2, projectId);
            stmt.setInt(3, userId);

            rs = stmt.executeQuery();
            rs.next();

            if (rs.getInt("count") > 0) {
                return true;
            }
            return false;
        }  catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }
    }

    /**
     * get the template generator configuration for the given project
     * @param projectId
     * @return
     */
    public JSONArray getTemplateConfigs(Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        JSONArray configNames = new JSONArray();

        configNames.add("Default");

        try {
            String sql = "SELECT configName FROM templateConfigs WHERE projectId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                configNames.add(rs.getString("configName"));
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException retrieving template configurations for projectID: " +
                    projectId, e);
        } finally {
            db.close(stmt, rs);
        }
        return configNames;
    }

    /**
     * get a specific template generator configuration
     * @param configName
     * @param projectId
     * @return
     */
    public String getTemplateConfig(String configName, Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        JSONObject obj = new JSONObject();

        try {
            String sql = "SELECT config FROM templateConfigs WHERE projectId = ? AND configName = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setString(2, configName);

            rs = stmt.executeQuery();
            if (rs.next()) {
                obj.put("checkedOptions", JSONValue.parse(rs.getString("config")));
            } else {
                obj.put("error", configName + " template configuration not found.");
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException retrieving template config.", e);
        } finally {
            db.close(stmt, rs);
        }

        return obj.toJSONString();
    }

    public void updateTemplateConfig(String configName, Integer projectId, Integer userId, List<String> checkedOptions) {
        PreparedStatement stmt = null;

        try {
            String updateStatement = "UPDATE templateConfigs SET config = ? WHERE " +
                    "userId = ? AND projectId = ? and configName = ?";
            stmt = conn.prepareStatement(updateStatement);

            stmt.setString(1, JSONValue.toJSONString(checkedOptions));
            stmt.setInt(2, userId);
            stmt.setInt(3, projectId);
            stmt.setString(4, configName);

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while updating template config.", e);
        } finally {
            db.close(stmt, null);
        }
    }

    public void removeTemplateConfig(String configName, Integer projectId) {
        PreparedStatement stmt = null;

        try {
            String sql = "DELETE FROM templateConfigs WHERE projectId = ? AND configName = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setString(2, configName);

            stmt.execute();
        } catch (SQLException e) {
            throw new ServerErrorException("Server error while removing template config.");
        } finally {
            db.close(stmt, null);
        }
    }
}

