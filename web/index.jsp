<%@ include file="header-home.jsp" %>

<div id="validation" class="section">
    <div class="sectioncontent">

        <h2>Validation</h2>

        <c:if test="${param.error != null}">
        <script>
        $(document).ready(function(){
            $("#dialogContainer").addClass("error");
            dialog("Authentication Error!", "Error", {"OK": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close"); }
            });
        });</script>
        </c:if>

        <form method="POST">
            <table>
                <tr>
                    <td align="right">FIMS Spreadsheet</td>
                    <td><input type="file" name="dataset" id="dataset" /></td>
                </tr>

                <tr class="toggle-content" id="projects_toggle">
                    <td align="right">Project</td>
                    <td>
                        <select width=20 name="project_id" id="projects">
                            <option qdup=1 value=0>Select a project ...</option>
                            <option data-qrepeat="e projects" data-qattr="value e.project_id; text e.project_title">
                                Loading Projects ...
                            </option>
                        </select>
                    </td>
                </tr>

                <tr>
                    <td align="right">Upload</td>
                    <td style="font-size:11px;">
                        <c:if test="${user == null}">
                            <input type="checkbox" id="upload" disabled="disabled" /> (login to upload)
                        </c:if>
                        <c:if test="${user != null}">
                            <input type="checkbox" id="upload" name="upload" />
                        </c:if>
                    </td>
                </tr>

                <tr class="toggle-content" id="expedition_code_toggle">
                    <td align="right">Dataset Code</td>
                    <td><input type="text" name="expedition_code" id="expedition_code" /></td>
                </tr>

                <tr>
                    <td></td>
                    <td><input type="button" value="Submit"</td>
                </tr>
            </table>
        </form>

        <div id=dialogContainer></div>

        <div id=resultsContainer style='overflow:auto; display:none;'>
        </div>

        <p>
        <h2>Workflow</h2>
        <!--Biocode FIMS is a field information management system that enables data collection at the source (in the field),
                validates data, and assigns persistent identifiers by Project, Dataset, and locally unique identifiers
                (see <a href="http://biscicol.org/bcid" target="_blank">BCID system</a>).
                <p>
         -->
        <img id='workflowImage' src='docs/Workflow_simple.jpeg'>
        <br><a id='workflowControl' onclick='workflowImageSwap();'>Details</a>
    </div>
</div>

<script>
    function workflowImageSwap() {
        if ($("#workflowImage").attr("src") == 'docs/Workflow.jpeg') {
            $("#workflowImage").attr("src",'docs/Workflow_simple.jpeg');
            $("#workflowControl").text('Details');
        } else {
            $("#workflowImage").attr("src",'docs/Workflow.jpeg');
            $("#workflowControl").text('Simple');
        }
    }
    $(document).ready(function() {
        validationFormToggle();
        populateProjects();
        // call validatorSubmit if the enter key was pressed in an input
        $("input").keydown( function(event) {
            if (event.which == 13) {
            event.preventDefault();
            validatorSubmit();
            }
        });
        $("input[type=button]").click(function() {
            validatorSubmit();
        });
    });
</script>

<%@ include file="footer.jsp" %>
