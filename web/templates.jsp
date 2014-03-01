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

<h1>Biocode-FIMS Spreadsheet Customization</h1>

<div class='container-fluid'>
    <div class='row-fluid'>
        <div class='span8'>
            <div class='accordion' id='version_info'>
                <div class='accordion-group'>
                    <div class='accordion-heading'>
                        <a class='accordion-toggle' data-toggle='collapse' data-parent='version_info'
                           href='#collapse_info'>
                            VERSION INFORMATION (click here to collapse or expand)
                        </a>
                    </div>
                    <div id='collapse_info' class='accordion-body collapse'>
                        <div class='accordion-inner'>
                            <strong>Version 0.1 January 11, 2013</strong>

                            <p>This is the first mock-up of a FIMS spreadsheet customization tool, with very basic
                                functionality. The available headings is populated with headings from the BOLD upload
                                spreadsheet, and the Definition pane is generated from a hard-coded (not
                                database-sourced) set of definitions. Excel spreadsheet and Biovalidator file generation
                                is functional at a basic level.</p>
                        </div>
                    </div>
                </div>
                <div class='accordion-group'>
                    <div class='accordion-heading'>
                        <a class='accordion-toggle' data-toggle='collapse' data-parent='version_info'
                           href='#collapse_info2'>
                            FUTURE ENHANCEMENTS (click here to collapse or expand)
                        </a>
                    </div>
                    <div id='collapse_info2' class='accordion-body collapse'>
                        <div class='accordion-inner'>
                            <ul>
                                <li>Import Jamie's gigantic spreadsheet into a database, and use as source for
                                    headings.
                                </li>
                                <li>Beef up the bottom pane with functionality like being able to re-order headings,
                                    change names, etc.
                                </li>
                                <li>Enable ability to save heading groups as a new "template".</li>
                                <li>Get BioValidator XML generator working.</li>
                                <li>Allow users more control over BioValidator restrictions.</li>
                                <li>Automatically mint EZIDs using the <a href='http://n2t.net/ezid/doc/apidoc.html'
                                                                          target='_blank'>EZID API</a>.
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>


    <div class='row-fluid'>
        <div class="span7">
            <h2>Projects</h2>

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
            <h2>Available Headings</h2>

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

    <button class='btn btn-primary pull-right' type='button' id='excel_button'>Export Excel</button>

</div>


</body>
</html>