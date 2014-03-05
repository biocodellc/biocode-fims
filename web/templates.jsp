<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>FIMS Spreadsheet Customization</title>

    <link rel="stylesheet" href="css/jNotify.jquery.css" type="text/css"/>

    <!-- Bootstrap -->
    <link href="css/templates.css" rel="stylesheet">
    <link href="css/bootstrap.css" rel="stylesheet">

    <script src="http://code.jquery.com/jquery-1.10.2.min.js"></script>
    <script src="js/bootstrap.js"></script>
    <script src="js/templates.js"></script>

    <script src="js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };
        // handle selecting a new project and then populating columns
        $(function () {
            $('#selectProject').on('click', function () {
                populateColumns('#cat1');
            });
        });

    </script>
    <script src="js/biocode-fims.js"></script>
    <script src="js/jNotify.jquery.js"></script>
</head>

<body onload="populateProjects();">

<h1>Biocode-FIMS Spreadsheet Customization <a href='javascript:showVersion();'>v0.2</a></h1>

<div class='container-fluid'>

    <div class='row-fluid'>
        <div class="span7">
            <h2>Choose a Project</h2>

            <select width=20 id=projects>
                <option qdup=1 value=0>Select an project ...</option>
                <option data-qrepeat="e projects" data-qattr="value e.project_id; text e.project_title">
                    Loading Projects ...
                </option>
            </select>

            <button class='btn btn-primary' id='selectProject'>Go</button>

       </div>
    </div>

    <div class='row-fluid'>
        <div class="span7">
            <h2>Available Columns</h2>

            <p><strong>Below, you will find all available column headings that you can include in your customized FIMS
                spreadsheet. </strong></p>

            <div class='tab-content'>
                <div class="tab-pane active" id="cat1">
                </div>

            </div>
        </div>

        <div class="span5">
            <h2>Definition</h2>

            <p><strong>Click on the "DEF" link next to any of the headings to see its definition in this pane.</strong>
            </p>

            <div id='definition'></div>
        </div>
    </div>

    <button class='btn btn-primary pull-left' type='button' id='excel_button'>Export Excel</button>

</div>


</body>
</html>