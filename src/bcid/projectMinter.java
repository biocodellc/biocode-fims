package bcid;

import fimsExceptions.BadRequestException;
import fimsExceptions.ServerErrorException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import java.sql.*;
import java.util.*;
import java.util.Hashtable;

/**
 * Mint new expeditions.  Includes the automatic creation of a core set of entity types
 */
public class projectMinter {
    protected Connection conn;
    database db;
    public ArrayList<Integer> expeditionResources;
    private SettingsManager sm;

    private static Logger logger = LoggerFactory.getLogger(projectMinter.class);


    /**
     * The constructor defines the class-level variables used when minting Expeditions.
     * It defines a generic set of entities (process, information content, objects, agents)
     * that can be used for any expedition.
     */
    public projectMinter() {
        db = new database();
        conn = db.getConn();

        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();
    }

    public void close() {
        db.close();
    }
    /**
     * Find the BCID that denotes the validation file location for a particular expedition
     *
     * @param project_id defines the project_id to lookup
     * @return returns the BCID for this expedition and conceptURI combination
     */
    public String getValidationXML(Integer project_id) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String query = "select \n" +
                    "biovalidator_validation_xml\n" +
                    "from \n" +
                    " projects\n" +
                    "where \n" +
                    "project_id=?";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, project_id);

            rs = stmt.executeQuery();
            rs.next();
            return rs.getString("biovalidator_validation_xml");
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
                    "\tp.project_id,\n" +
                    "\tp.project_code,\n" +
                    "\tp.project_title,\n" +
                    "\tp.biovalidator_validation_xml\n" +
                    " FROM \n" +
                    "\tprojects p\n" +
                    " WHERE \n" +
                    "\tp.public = true\n";


            if (userId != null) {
                query += " UNION \n" +
                        " SELECT \n" +
                        "\tp.project_id,\n" +
                        "\tp.project_code,\n" +
                        "\tp.project_title, \n" +
                        "\tp.biovalidator_validation_xml\n" +
                        " FROM \n" +
                        "\tprojects p, usersProjects u\n" +
                        " WHERE \n" +
                        "\t(p.project_id = u.project_id AND u.users_id = ?)\n" +
                        " ORDER BY \n" +
                        "\tproject_id";
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
                sb.append("\t\t\t\"project_id\":\"" + rs.getString("project_id") + "\",\n");
                sb.append("\t\t\t\"project_code\":\"" + rs.getString("project_code") + "\",\n");
                sb.append("\t\t\t\"project_title\":\"" + rs.getString("project_title") + "\",\n");
                sb.append("\t\t\t\"biovalidator_validation_xml\":\"" + rs.getString("biovalidator_validation_xml") + "\"\n");
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
               String sql = "SELECT project_id FROM projects";
               stmt = conn.prepareStatement(sql);
               rs = stmt.executeQuery();
               while (rs.next()) {
                   projects.add(rs.getInt("project_id"));
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
     * @param project_id pass in an project identifier to limit the set of expeditions we are looking at
     * @return
     */
    public String getLatestGraphs(int project_id, String username) {
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Construct the query
            // This query is built to give us a groupwise maximum-- we want the graphs that correspond to the
            // maximum timestamp (latest) loaded for a particular expedition.
            // Help on solving this problem came from http://jan.kneschke.de/expeditions/mysql/groupwise-max/
            String sql = "select p.expedition_code as expedition_code,p.expedition_title,d1.graph as graph,d1.ts as ts, d1.webaddress as webaddress, d1.prefix as ark, d1.datasets_id as id, p.project_id as project_id \n" +
                    "from datasets as d1, \n" +
                    "(select p.expedition_code as expedition_code,d.graph as graph,max(d.ts) as maxts, d.webaddress as webaddress, d.prefix as ark, d.datasets_id as id, p.project_id as project_id \n" +
                    "    \tfrom datasets d,expeditions p, expeditionsBCIDs pB\n" +
                    "    \twhere pB.datasets_id=d.datasets_id\n" +
                    "    \tand pB.expedition_id=p.expedition_id\n" +
                    " and d.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    and p.project_id = ?\n" +
                    "    \tgroup by p.expedition_code) as  d2,\n" +
                    "expeditions p,  expeditionsBCIDs pB\n" +
                    "where p.expedition_code = d2.expedition_code and d1.ts = d2.maxts\n" +
                    " and pB.datasets_id=d1.datasets_id \n" +
                    " and pB.expedition_id=p.expedition_id\n" +
                    " and d1.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    and p.project_id =?";

            // Enforce restriction on viewing particular datasets -- this is important for protected datasets
            if (username != null) {
                sql += "    and (p.public = 1 or p.users_id = ?)";
            } else {
                sql += "    and p.public = 1";
            }

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, project_id);
            stmt.setInt(2, project_id);

            // Enforce restriction on viewing particular datasets -- this is important for protected datasets
            if (username != null) {
                Integer userId = db.getUserId(username);
                stmt.setInt(3, userId);
            }

            sb.append("{\n\t\"data\": [\n");
            rs = stmt.executeQuery();
            while (rs.next()) {
                // Grap the prefixes and concepts associated with this
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"expedition_code\":\"" + rs.getString("expedition_code") + "\",\n");
                sb.append("\t\t\t\"expedition_title\":\"" + rs.getString("expedition_title") + "\",\n");
                sb.append("\t\t\t\"ts\":\"" + rs.getString("ts") + "\",\n");
                sb.append("\t\t\t\"ark\":\"" + rs.getString("ark") + "\",\n");
                sb.append("\t\t\t\"dataset_id\":\"" + rs.getString("id") + "\",\n");
                sb.append("\t\t\t\"project_id\":\"" + rs.getString("project_id") + "\",\n");
                sb.append("\t\t\t\"webaddress\":\"" + rs.getString("webaddress") + "\",\n");
                sb.append("\t\t\t\"graph\":\"" + rs.getString("graph") + "\"\n");
                sb.append("\t\t}");
                if (!rs.isLast())
                    sb.append(",");

                sb.append("\n");
            }
            sb.append("\t]\n}\n");
            return sb.toString();
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "Trouble getting latest graphs.", e);
        } finally {
            db.close(stmt, rs);
        }
    }

    public static void main(String args[]) {
        // See if the user owns this expedition or no
        projectMinter project = new projectMinter();
        //System.out.println(project.listProjects());
        System.out.println("datasets = \n" + project.getMyTemplatesAndDatasets("demo"));
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
            Integer user_id = db.getUserId(username);

            String sql = "SELECT project_id, project_code, project_title, project_title, biovalidator_validation_xml FROM projects WHERE users_id = \"" + user_id + "\"";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            sb.append("{\n");
            sb.append("\t\"projects\": [\n");
            while (rs.next()) {
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"project_id\":\"" + rs.getString("project_id") + "\",\n");
                sb.append("\t\t\t\"project_code\":\"" + rs.getString("project_code") + "\",\n");
                sb.append("\t\t\t\"project_title\":\"" + rs.getString("project_title") + "\",\n");
                sb.append("\t\t\t\"biovalidator_validation_xml\":\"" + rs.getString("biovalidator_validation_xml") + "\"\n");
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
    public String listUsersProjects(String username) {
        StringBuilder sb = new StringBuilder();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Integer userId = db.getUserId(username);

            String sql = "SELECT p.project_id, p.project_code, p.project_title, p.biovalidator_validation_xml FROM projects p, usersProjects u WHERE p.project_id = u.project_id && u.users_id = \"" + userId + "\"";
            stmt = conn.prepareStatement(sql);

            rs = stmt.executeQuery();

            sb.append("{\n");
            sb.append("\t\"projects\": [\n");
            while (rs.next()) {
                sb.append("\t\t{\n");
                sb.append("\t\t\t\"project_id\":\"" + rs.getString("project_id") + "\",\n");
                sb.append("\t\t\t\"project_code\":\"" + rs.getString("project_code") + "\",\n");
                sb.append("\t\t\t\"project_title\":\"" + rs.getString("project_title") + "\",\n");
                sb.append("\t\t\t\"biovalidator_validation_xml\":\"" + rs.getString("biovalidator_validation_xml") + "\"\n");
                sb.append("\t\t}");
                if (!rs.isLast())
                    sb.append(",\n");
                else
                    sb.append("\n");
            }
            sb.append("\t]\n}");

        } catch (SQLException e) {
            throw new ServerErrorException(e);
        } finally {
            db.close(stmt, rs);
        }

        return sb.toString();
    }

    /**
     * return an HTML table of a project's configuration.
     * @param project_id
     * @param username
     * @return
     */
    public String getProjectConfigAsTable(Integer project_id, String username) {
        StringBuilder sb = new StringBuilder();
        Hashtable<String, String> config = getProjectConfig(project_id, username);

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
        sb.append(config.get("validation_xml"));
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
        sb.append(("\t\t\t<td><input type=\"text\" class=\"project_config\" name=\"validation_xml\" value=\""));
        sb.append(config.get("validation_xml"));
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
                updateString += " WHERE project_id =\"" + projectId + "\";";
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
     * Return a hashTable of project configuration options for a given project_id and user_id
     * @param projectId
     * @param username
     * @return
     */
    public Hashtable<String, String> getProjectConfig(Integer projectId, String username) {
        Hashtable<String, String> config = new Hashtable<String, String>();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            Integer user_id = db.getUserId(username);

            String sql = "SELECT project_title as title, public, bioValidator_validation_xml as validation_xml FROM projects WHERE project_id=?"
                    + " AND users_id= ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);
            stmt.setInt(2, user_id);

            rs = stmt.executeQuery();
            if (rs.next()) {
                config.put("title", rs.getString("title"));
                config.put("public", String.valueOf(rs.getBoolean("public")));
                if (rs.getString("validation_xml") != null) {
                    config.put("validation_xml", rs.getString("validation_xml"));
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
                    "FROM users u, projects p, usersProjects uP " +
                    "WHERE u.user_id=uP.users_id and uP.project_id = p.project_id and u.user_id = ? and p.project_id=?";
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
            String sql = "SELECT count(*) as count FROM projects WHERE users_id = ? AND project_id = ?";
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
            String sql = "DELETE FROM usersProjects WHERE users_id = ? AND project_id = ?";
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
            String insertStatement = "INSERT INTO usersProjects (users_id, project_id) VALUES(?,?)";
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
            String userProjectSql = "SELECT users_id FROM usersProjects WHERE project_id = \"" + projectId + "\"";
            String userSql = "SELECT username, user_id FROM users";
            String projectSql = "SELECT project_title FROM projects WHERE project_id = \"" + projectId + "\"";
            List projectUsers = new ArrayList();
            stmt = conn.prepareStatement(projectSql);

            rs = stmt.executeQuery();
            rs.next();
            String project_title = rs.getString("project_title");

            db.close(stmt, rs);

            sb.append("\t<form method=\"POST\">\n");

            sb.append("<table data-project_id=\"" + projectId + "\" data-project_title=\"" + project_title + "\">\n");
            sb.append("\t<tr>\n");
            stmt = conn.prepareStatement(userProjectSql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer userId = rs.getInt("users_id");
                String username = db.getUserName(userId);
                projectUsers.add(userId);
                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append(username);
                sb.append("</td>\n");
                sb.append("\t\t<td><a id=\"remove_user\" data-user_id=\"" + userId + "\" data-username=\"" + username + "\" href=\"javascript:void();\">(remove)</a> ");
                sb.append("<a id=\"edit_profile\" data-username=\"" + username + "\" href=\"javascript:void();\">(edit)</a></td>\n");
                sb.append("\t</tr>\n");
            }

            sb.append("\t<tr>\n");
            sb.append("\t\t<td>Add User:</td>\n");
            sb.append("\t\t<td>");
            sb.append("<select name=user_id>\n");
            sb.append("\t\t\t<option value=\"0\">Create New User</option>\n");

            db.close(stmt, rs);

            stmt = conn.prepareStatement(userSql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Integer userId = rs.getInt("user_id");
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
            sb.append("\t\t<td><input type=\"hidden\" name=\"project_id\" value=\"" + projectId + "\"></td>\n");
            sb.append("\t\t<td><input type=\"button\" value=\"Submit\" onclick=\"projectUserSubmit(\'" + project_title.replaceAll(" ", "_") + '_' + projectId + "\')\"></td>\n");
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
            String sql1 = "select e.expedition_title, p.project_title from expeditions e, projects p " +
                    "where e.users_id = ? and p.project_id = e.project_id";
            stmt = conn.prepareStatement(sql1);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();

            while (rs.next()) {
                String project_title = rs.getString("project_title");
                String expedition_title = rs.getString("expedition_title");

                if (project_title != null && !project_title.isEmpty()) {
                    // if the project isn't in the map, then add it
                    if (!projectMap.containsKey(project_title)) {
                        projectMap.put(project_title, new JSONObject());
                    }

                    // if the expedition isn't in the project then add it
                    JSONObject p = (JSONObject) projectMap.get(project_title);
                    if (!p.containsKey(expedition_title)) {
                        p.put(expedition_title, new JSONArray());
                    }

                }

            }

            String sql2 = "select e.expedition_code, e.expedition_title, d.ts, d.prefix as ark, d.datasets_id as id, d.finalCopy, e.project_id, p.project_title\n" +
                    "from datasets d, expeditions e,  expeditionsBCIDs pB, projects p\n" +
                    "where d.users_id = ? and d.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    " and pB.datasets_id=d.datasets_id \n" +
                    " and e.expedition_id=pB.expedition_id\n" +
                    " and p.project_id=e.project_id\n" +
                    " order by project_id, expedition_code, ts desc";

            stmt = conn.prepareStatement(sql2);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject dataset = new JSONObject();
                String project_title = rs.getString("project_title");
                String expedition_title = rs.getString("expedition_title");

                // Grap the prefixes and concepts associated with this
                dataset.put("ts", rs.getString("ts"));
                dataset.put("dataset_id", rs.getString("id"));
                dataset.put("ark", rs.getString("ark"));
                dataset.put("finalCopy", rs.getString("finalCopy"));

                JSONObject p = (JSONObject) projectMap.get(project_title);
                ((JSONArray) p.get(expedition_title)).add(dataset);
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

        JSONObject projectResponse = ((JSONObject) JSONValue.parse(listUsersProjects(username)));
        JSONArray projects = ((JSONArray) projectResponse.get("projects"));

        for (Object p:  projects) {
            JSONObject project = (JSONObject) p;
            projectMap.put(project.get("project_title"), new JSONArray());
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
            String sql = "select e.expedition_code, e.expedition_title, d1.graph, d1.ts, d1.datasets_id as id, d1.webaddress as webaddress, d1.prefix as ark, e.project_id, e.public, p.project_title\n" +
                    "from datasets as d1, \n" +
                    "(select e.expedition_code as expedition_code,d.graph as graph,max(d.ts) as maxts, d.webaddress as webaddress, d.prefix as ark, d.datasets_id as id, e.project_id as project_id \n" +
                    "    \tfrom datasets d,expeditions e, expeditionsBCIDs pB\n" +
                    "    \twhere pB.datasets_id=d.datasets_id\n" +
                    "    \tand pB.expedition_id=e.expedition_id\n" +
                    " and d.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    \tgroup by e.expedition_code) as  d2,\n" +
                    "expeditions e,  expeditionsBCIDs pB, projects p\n" +
                    "where e.expedition_code = d2.expedition_code and d1.ts = d2.maxts\n" +
                    " and pB.datasets_id=d1.datasets_id \n" +
                    " and pB.expedition_id=e.expedition_id\n" +
                    " and p.project_id=e.project_id\n" +
                    " and d1.resourceType = \"http://purl.org/dc/dcmitype/Dataset\"\n" +
                    "    and e.users_id = ?";

            stmt = conn.prepareStatement(sql);
            Integer userId = db.getUserId(username);
            stmt.setInt(1, userId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                JSONObject dataset = new JSONObject();
                String project_title = rs.getString("project_title");

                // Grap the prefixes and concepts associated with this
                dataset.put("expedition_code", rs.getString("expedition_code"));
                dataset.put("expedition_title", rs.getString("expedition_title"));
                dataset.put("ts", rs.getString("ts"));
                dataset.put("dataset_id", rs.getString("id"));
                dataset.put("project_id", rs.getString("project_id"));
                dataset.put("graph", rs.getString("graph"));
                dataset.put("public", rs.getString("public"));
                dataset.put("ark", rs.getString("ark"));
                dataset.put("webaddress", rs.getString("webaddress"));


                if (project_title != null && !project_title.isEmpty()) {
                    projectDatasets = (JSONArray) projectMap.get(project_title);
                    // TODO What should we do if a project_title shows up that wasn't before fetched? ignore it? or add the project?
                    if (projectDatasets == null) {
                        projectDatasets = new JSONArray();
                    }
                    projectDatasets.add(dataset);
                    projectMap.put(project_title, projectDatasets);
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
            String insertStatement = "INSERT INTO templateConfigs (users_id, project_id, config_name, config) " +
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
                    "WHERE config_name = ? and project_id = ?";
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
                    "WHERE config_name = ? and project_id = ? and users_id = ?";
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
    public String getTemplateConfigs(Integer projectId) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        JSONObject obj = new JSONObject();
        JSONArray configNames = new JSONArray();

        configNames.add("Default");

        try {
            String sql = "SELECT config_name FROM templateConfigs WHERE project_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, projectId);

            rs = stmt.executeQuery();
            while (rs.next()) {
                configNames.add(rs.getString("config_name"));
            }
        } catch (SQLException e) {
            throw new ServerErrorException("Server Error", "SQLException retrieving template configurations for projectID: " +
                    projectId, e);
        } finally {
            db.close(stmt, rs);
        }
        obj.put("configNames", configNames);
        return obj.toJSONString();
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
            String sql = "SELECT config FROM templateConfigs WHERE project_id = ? AND config_name = ?";
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
                    "users_id = ? AND project_id = ? and config_name = ?";
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
            String sql = "DELETE FROM templateConfigs WHERE project_id = ? AND config_name = ?";

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

