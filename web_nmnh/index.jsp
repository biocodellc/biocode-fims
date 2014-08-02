<%@ include file="header-home.jsp" %>

<div id="validation" class="section">
    <div class="sectioncontent">

        <p style='margin-bottom: 2cm;'></p>

        <img id='workflowImage' src='docs/Workflow_simple.jpeg'>

        <p style='margin-bottom: 2cm;'>
            <ul>
                <li style="margin-bottom: 10px;"><a href='/fims/templates.jsp'><b>Generate Template</b></a> to create spreadsheet template</li>
                <li style="margin-bottom: 10px;">Fill in your spreadsheet</li>
                <li style="margin-bottom: 10px;">Validate and Upload using <a href='/fims/validation.jsp'><b>Validation</b></a></li>
            </ul>
        </p>

    </div>
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

<%@ include file="footer.jsp" %>
