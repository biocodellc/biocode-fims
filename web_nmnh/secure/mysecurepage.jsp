<%@ include file="header-home.jsp" %>

<div id="validation" class="section">
    <div id="warning"></div>

    <div style="margin: 0 auto;width: 100%;">

        <p style='margin-bottom: 2cm;'></p>

<!-- Save for Web Slices (Workflow_simple.jpeg) -->
<table id="Table_01" width="1200" height="108" border="0" cellpadding="0" cellspacing="0" border=0>
<tbody style='border:none' border=0>
        <tr>
                <td colspan="5">
                        <img src="docs/images/Workflow_simple_01.jpg" width="1200" height="18" alt=""></td>
        </tr>
        <tr>
                <td rowspan="2">
                        <img src="docs/images/Workflow_simple_02.jpg" width="218" height="90" alt=""></td>
                <td>
                        <c:if test="${user == null}">
                                <img src="docs/images/GenerateTemplate.jpg" width="143" height="77" border="0" alt="Generate Spreadsheet Template"></td>
                        </c:if>
                        <c:if test="${user != null}">
                            <a href="/fims/templates.jsp"
                                onmouseover="window.status='Generate Spreadsheet Template';  return true;"
                                onmouseout="window.status='';  return true;">
                                <img src="docs/images/GenerateTemplate.jpg" width="143" height="77" border="0" alt="Generate Spreadsheet Template"></a></td>
                        </c:if>
                <td rowspan="2">
                        <img src="docs/images/Workflow_simple_04.jpg" width="281" height="90" alt=""></td>
                <td>
                    <c:if test="${user == null}">
                        <img src="docs/images/Validation.jpg" width="142" height="77" border="0" alt="Run Validation"></td>
                    </c:if>
                    <c:if test="${user != null}">
                        <a href="/fims/validation.jsp"
                                onmouseover="window.status='Run Validation';  return true;"
                                onmouseout="window.status='';  return true;">
                                <img src="docs/images/Validation.jpg" width="142" height="77" border="0" alt="Run Validation"></a></td>
                    </c:if>
                <td rowspan="2">
                        <img src="docs/images/Workflow_simple_06.jpg" width="416" height="90" alt=""></td>
        </tr>
        <tr>
                <td>
                        <img src="docs/images/Workflow_simple_07.jpg" width="143" height="13" alt=""></td>
                <td>
                        <img src="docs/images/Workflow_simple_08.jpg" width="142" height="13" alt=""></td>
        </tr>
        </tbody>
</table>

<div class="span4 collapse-group">

    <p>
Welcome to the Smithsonian Institution's National Museum of Natural History Field Information
Management System (FIMS). The FIMS is designed for NMNH researchers to digitally capture
data while on field expeditions, automatically check and correct these data against controlled
lists and accepted values, and then upload these data to NMNH storage where it can later be
imported into the Museum's collection information system (EMu). The above image outlines the
workflow steps
<b><a id="details" href="#">More &raquo;</a></b>
    </p>

    <div class="collapse">

<h2>Pre-Registration</h2>

Pre-registration in EMu is a requirement for the field collecting event and will insure that the
project will have an acquisition transaction record in EMu for later referencing and appending
and scientists can populate their field templates with these acquisition numbers before going
into the field.
Pre-registration involves the creation of a new In Process Acquisition transaction in EMu.
Subtype of this acquisition should be Collected for Museum with Primary Sponsor or Collector
recorded as the Primary Transactor. Other collectors or collaborating institutions can be
included as secondary transactors. General information about where and what will be
collected should be included in the material description field of the transaction. Any permits or
agreements received prior to the trip should be scanned and referenced via Rights records and
linked to the transaction. Item level information will not be flushed out until after return from the
trip. The data spreadsheet itself should be loaded in as a multimedia asset associated with the
transaction upon return and final validation.

<h2>Generate Template</h2>

Users can generate a unit-specific Excel template spreadsheet to be used as the primary
method of recording field collection information in the field.
To create the Excel template, a user will select from a robust set of standardized metadata
fields, with the core set EMu, Darwin Core, MIxS, and the ABCDDNA/DwC DNA and Tissue
extension for GGBN. Each unit has designated mandatory, desirable, and optional fields,
based on specific unit collection requirements. Field definitions, synonymies, mappings to
EMu, crosswalks between terms, and validation rules are available to the user while selecting
metadata fields, so that the user can select the best fields for his or her collecting trip.

<h2>Enter Data in Spreadsheet</h2>

Once in the field, researchers can use the pre-generated spreadsheet to record data.

<h2>Validation</h2>

Researchers can validate recorded data against quality assurance validation scripts. These
scripts will show errors (where incorrect values or used) or warnings when data might need
modification. Users will use errors and warnings to edit spreadsheet data until it passes
validation (an iterative process of checking and correcting).

<h2>Upload</h2>

Once a spreadsheet has passed validation, it can be uploaded to NMNH storage where it can
later be imported into the Museum’s collection information system (EMu).

<!--End collapse-->
</div>

<!--End collapse-group-->
</div>



<!-- End Save for Web Slices -->
    </div>
</div>

<script>
// function to show incoming failure Messages, only used on HOME page
window.onload = function checkForFailMessageInURL(){
    var match = RegExp('[?&]error=([^&]*)').exec(window.location.search);
    if (match !=null) {
        var results = decodeURIComponent(match[1].replace(/\+/g, ' '));
        if (results!=null) {
            alert(results);
        }
    }


};

$(document).ready(function() {
    // Run our Browser Check
    fimsBrowserCheck($('#warning'));

      // View Details Function
    $('#details').on('click', function(e) {
        e.preventDefault();
        var $this = $(this);
        var $collapse = $this.closest('.collapse-group').find('.collapse');
        $collapse.collapse('toggle');
    });

});
</script>

<%@ include file="footer.jsp" %>
