<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<head>
<html>
    <title>Biocode FIMS</title>

    <!-- Javascript -->
    <!-- JQuery -->
    <script src="http://code.jquery.com/jquery-1.10.2.min.js"></script>
    <!-- Distal populates menu items based on JSON responses -->
    <script src="js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
        return this.each( function () { distal(this, json) } )
        };
    </script>
    <!-- Customized Javasript for our biocode-fims -->
    <script src="js/biocode-fims.js"></script>
    <!-- jNotify is for messaging-->
    <script src="js/jNotify.jquery.js"></script>
    <!-- Bootstrap provides snazzy looking buttons and select lists-->
    <script src="js/bootstrap.js"></script>
    <script src="js/templates.js"></script>

    <!-- Style Sheets-->
    <!-- jNotify -->
    <link rel="stylesheet" href="css/jNotify.jquery.css" type="text/css"/>
    <!-- Bootstrap -->
    <link href="css/templates.css" rel="stylesheet">
    <link href="css/bootstrap.css" rel="stylesheet">

</head>

<body onload="graphsMessage('Choose an project to see loaded spreadsheets');populateProjects();">

<h1>Biocode-FIMS Query</h1>

    <div class='row-fluid'>
        <div class="span7">
            <b class="lead" style="display:inline-block;width:200px;text-align:right;">Choose Project:</b>

            <select width=20 id=projects onchange='populateGraphs(this.options[this.selectedIndex].value);' style="display:inline-block;width:400px;text-align:left;">
                <option qdup=1 value=0>Select an project ...</option>
                <option data-qrepeat="e projects" data-qattr="value e.project_id; text e.project_title">
                    Loading Projects ...
                </option>
            </select>

       </div>
    </div>

    <div class='row-fluid'>
        <div class="span7">
            <b class="lead" style="display:inline-block;width:200px;text-align:right;">Choose Dataset(s):</b>
            <select id=graphs multiple style="display:inline-block;width:400px;text-align:left;"></select>
       </div>
    </div>

    <div class='row-fluid'>
        <div class="span7">
            <b class="lead" style="display:inline-block;width:200px;text-align:right;">Filter:</b>
            <input type=text id=filter style="display:inline-block;width:400px;text-align:left;">
        </div>
    </div>

    <div class='row-fluid'>
        <div class="span7">
            <b class="lead" style="display:inline-block;width:200px;text-align:right;">Output:</b>

            <input class='btn btn-primary' type=button onclick="javascript:queryJSON();" value=table>
            <input class='btn btn-primary' type=button onclick="javascript:queryExcel();" value=excel>
            <input class='btn btn-primary' type=button onclick="javascript:queryKml();" value=kml>
        </div>
    </div>

    <div class='row-fluid'>
        <div id=resultsContainer>
            <table id=results border=0>
                <tr>
                    <th data-qrepeat="m header"><b data-qtext="m"></b></th>
                </tr>
                <tr data-qrepeat="m data">
                    <td data-qrepeat="i m.row"><i data-qtext="i"></i>
                </tr>
            </table>
        </div>
    </div>

</body>
</html>