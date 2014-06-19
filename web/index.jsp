<%@ include file="header-home.jsp" %>

<div id="validation" class="section">
    <div class="sectioncontent">

        A Field Information Management System (FIMS) enables data collection at the source (in the field) by
        generating spreadsheet templates, validating data, and assigning persistent identifiers for every unique biological sample.
        The following diagram shows how the system works.  The most typical functions are <b>Generating Templates</b> and <b>Validating Data</b>,
              both of which can be found under the Tools menu.
        <!--using
                <a href="http://biscicol.org/bcid" target="_blank">BCIDs</a>, which extends <a href="http://ezid.cdlib.org/">EZIDs</a>.
                <p>-->

<p><img id='workflowImage' src='docs/Workflow.jpeg'>
        <!--<img id='workflowImage' src='docs/Workflow_simple.jpeg'>
        <br><a id='workflowControl' onclick='workflowImageSwap();'>Details</a>-->
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
