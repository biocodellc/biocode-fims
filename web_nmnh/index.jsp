<%@ include file="header-home.jsp" %>

<div id="validation" class="section">
    <div style="margin: 0 auto;width: 100%;">

        <p style='margin-bottom: 2cm;'></p>

<!-- Save for Web Slices (Workflow_simple.jpeg) -->
<table id="Table_01" width="935" height="129" border="0" cellpadding="0" cellspacing="0">
<tbody style='border:none'>
        <tr>
                <td colspan="5">
                        <img src="docs/images/Workflow_simple_01.jpg" width="935" height="22" alt="" border="0" ></td>
        </tr>
        <tr>
                <td rowspan="2">
                        <img src="docs/images/Workflow_simple_02.jpg" width="10" height="107" alt="" border="0" ></td>
                <td>
                        <a href="/fims/templates.jsp"
                                onmouseover="window.status='Generate Spreadsheet Template';  return true;"
                                onmouseout="window.status='';  return true;">
                                <img src="docs/images/GenerateTemplate.jpg" width="171" height="92" border="0" alt="Generate Spreadsheet Template"></a></td>
                <td rowspan="2">
                        <img src="docs/images/Workflow_simple_04.jpg" width="337" height="107" alt=""></td>
                <td>
                        <a href="/fims/validation.jsp"
                                onmouseover="window.status='Run Validation';  return true;"
                                onmouseout="window.status='';  return true;">
                                <img src="docs/images/Validation.jpg" width="171" height="92" border="0" alt="Run Validation"></a></td>
                <td rowspan="2">
                        <img src="docs/images/Workflow_simple_06.jpg" width="246" height="107" alt="" border="0" ></td>
        </tr>
        <tr>
                <td>
                        <img src="docs/images/Workflow_simple_07.jpg" width="171" height="15" alt="" border="0" ></td>
                <td>
                        <img src="docs/images/Workflow_simple_08.jpg" width="171" height="15" alt="" border="0" ></td>
        </tr>
	<tr>
                <td colspan="5">
    The image above describes the workflow for working with the FIMS.  Blue boxes are clickable.
    Begin with generating a spreadsheet template. You will then need to fill out your data in the provided spreadsheet.
    Once you have completed filling out your spreadsheet, you can validate your spreadsheet data.  When validation is
    passed you will be given the option to upload your file for ingestion into RCIS.
	</td>

	</tr>
        </tbody>
</table>
<!-- End Save for Web Slices -->
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
