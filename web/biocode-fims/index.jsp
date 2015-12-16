<%@ include file="header-home.jsp" %>

<div id="validation" class="section">
    <div id="warning"></div>
    <div class="sectioncontent" id="mainpage">

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
    <div id=dialogContainer></div>
</div>

<script>
// function to show incoming failure Messages, its only used on HOME page since it is the only place
// that accepts redirects with error messages
window.onload = function checkForFailMessageInURL(){
    var match = RegExp('[?&]error=([^&]*)').exec(window.location.search);
    if (match !=null) {
        var results = decodeURIComponent(match[1].replace(/\+/g, ' '));
        if (results!=null) {
            alert(results);
        }
    }
};
</script>

<script>
    $(document).ready(function() {
       fimsBrowserCheck($('#warning'));
    });
</script>

<%@ include file="footer.jsp" %>
