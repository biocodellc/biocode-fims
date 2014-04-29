// for template generator, get the definitions when the user clicks on DEF
function populateDefinitions(column) {
 var e = document.getElementById('projects');
    var project_id = e.options[e.selectedIndex].value;

    theUrl = "/biocode-fims/rest/templates/definition/?project_id=" + project_id + "&column_name=" + column;

    $.ajax({
        type: "GET",
        url: theUrl,
        dataType: "html",
        success: function(data) {
            $("#definition").html(data);
        }
    });
}

function populateColumns(targetDivId) {
    $(targetDivId).html("Loading ...");

    var e = document.getElementById('projects');
    var project_id = e.options[e.selectedIndex].value;

    theUrl = "/biocode-fims/rest/templates/attributes/?project_id=" + project_id;

    var jqxhr = $.ajax( {
        url: theUrl,
        async: false,
        dataType : 'html'
    }).done(function(data) {
        $(targetDivId).html(data);
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response!");
        } else {
                showMessage ("Error completing request!" );
        }
    });

     $(".def_link").click(function () {
        populateDefinitions($(this).attr('name'));
     });
}
// Get the available projects
function populateProjects() {
    theUrl = "http://biscicol.org/id/projectService/list";
    var jqxhr = $.getJSON( theUrl, function(data) {
	    // Call distal to load the projects data
        distal(projects,data);
	    // Set to the first value in the list which should be "select one..."
	    $("#projects").val($("#projects option:first").val());
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
	     showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
	    showMessage ("Error completing request!");
        }
    });
}

// Get the graphs for a given project_id
function populateGraphs(project_id) {
    $("#resultsContainer").hide();
    // Don't let this progress if this is the first option, then reset graphs message
    if ($("#projects").val() == 0)  {
	    graphsMessage('Choose an project to see loaded spreadsheets');
	    return;
    }
    theUrl = "http://biscicol.org/id/projectService/graphs/" + project_id;
    var jqxhr = $.getJSON( theUrl, function(data) {
    // Check for empty object in response
    if (typeof data['data'][0] === "undefined") {
	graphsMessage('No expeditions found for this project');
    } else {
	// Call distal to load the graphs data
        distal(graphs,data);
    }
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
	     showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
	    showMessage ("Error completing request!");
        }
    });
}

// Get results as JSON
function queryJSON() {
   // theUrl = "/biocode-fims/rest/query/json/?" + getGraphsKeyValue() + "&" + getProjectKeyValue() + "&" +  getFilterKeyValue();
    theUrl = "/biocode-fims/rest/query/json/?" + getGraphsKeyValue() + "&" + getProjectKeyValue();
    var jqxhr = $.getJSON( theUrl, function(data) {
        $("#resultsContainer").show();
        distal(results,data);
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
	     showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
	    showMessage ("Error completing request!");
        }
    });
}

// Get results as Excel
function queryExcel() {
    //theUrl = "/biocode-fims/rest/query/excel/?" + getGraphsKeyValue() + "&" + getProjectKeyValue() + "&" +  getFilterKeyValue();
    theUrl = "/biocode-fims/rest/query/excel/?" + getGraphsKeyValue() + "&" + getProjectKeyValue();
    window.location = theUrl;
    showMessage ("Downloading results as an Excel document<br>this will appear in your browsers download folder.");
}

// Get results as Excel
function queryKml() {
    //theUrl = "/biocode-fims/rest/query/kml/?" + getGraphsKeyValue() + "&" + getProjectKeyValue() + "&" +  getFilterKeyValue();
    theUrl = "/biocode-fims/rest/query/kml/?" + getGraphsKeyValue() + "&" + getProjectKeyValue();
    window.location = theUrl;
    showMessage ("Downloading results as an KML document<br>If Google Earth does not open you can point to it directly");
}

// Get results as Excel
function queryGoogleMaps() {
    //theUrl = "http://biscicol.org/biocode-fims/rest/query/kml/" +encodeURIComponent("?") + getGraphsKeyValue() + encodeURIComponent("&") + getProjectKeyValue() + encodeURIComponent("&") +  getFilterKeyValue
    theUrl = "http://biscicol.org/biocode-fims/rest/query/kml/" +encodeURIComponent("?") + getGraphsKeyValue() + encodeURIComponent("&") + getProjectKeyValue();
    mapsUrl = "http://maps.google.com/maps?q=" + theUrl;
    window.open(
        mapsUrl,
        '_blank'
    );
}
// Get the URL key/value for the filter specified by the user
function getFilterKeyValue() {
    var filter = document.getElementById("filter").value;
    if (filter != "") {
     return "filter=" + document.getElementById("filter").value;
    }
    return "";
}



// Get the projectID
function getProjectID() {
    var e = document.getElementById('projects');
    return  e.options[e.selectedIndex].value;
}

// Get the project_id for a key/value expression
function getProjectKeyValue() {
    return "project_id=" + getProjectID();
}

// Get the URL key/value for the graphs by parsing return from the BCID service
function getGraphsKeyValue() {
    var str = "";
    var separator = "";
    $( "select option:selected" ).each(function() {
        str += separator + encodeURIComponent($( this ).val());
        separator = ",";
    });
    return "graphs="+str;
}

// Uses jNotify to display messages
// To re-configure this boxes behaviour and style, goto http://demos.myjqueryplugins.com/jnotify/
function showMessage(message) {
      jNotify(
        message,
        {
          autoHide : false, // added in v2.0
          clickOverlay : false, // added in v2.0
          MinWidth : 250,
          TimeShown : 3000,
          ShowTimeEffect : 200,
          HideTimeEffect : 200,
          LongTrip :20,
          HorizontalPosition : 'center',
          VerticalPosition : 'top',
          ShowOverlay : true,
          ColorOverlay : '#000',
          OpacityOverlay : 0.3,
          onClosed : function(){ // added in v2.0

          },
          onCompleted : function(){ // added in v2.0

          }
        });
}


// handle displaying messages/results in the graphs(spreadsheets) select list
function graphsMessage(message) {
        $('#graphs').empty();
        $('#graphs').append('<option data-qrepeat="g data" data-qattr="value g.graph; text g.expedition_title"></option>');
        $('#graphs').find('option').first().text(message);
}

// submit dataset to be validated/uploaded
function validatorSubmit() {
    $("#uploaderResults").html("validating...")
    var options = {
        beforeSerialize: function(form, options) {
            $('#projects').prop('disabled', false);
        },
        beforeSubmit: function(form, options) {
            $('#projects').prop('disabled', true);
        },
        url: "/biocode-fims/rest/validate/validate/",
        type: "POST",
        resetForm: true
        }

    $(this).ajaxSubmit(options)
        .done(function(data) {
            var t = true;
        });
}

// function to extract the project_id from a dataset to be uploaded
function extractProjectId() {
    var f = new FileReader();
    // older browsers don't have a FileReader
    if (f != null) {
        var deferred = new $.Deferred();
        var file = $('#dataset')[0].files[0];
        // after file has been read, extract the project_id if present
        f.onload = function () {
            var fileContents = f.result;
            var re = "~project_id=[0-9]+~";
            var results = fileContents.match(re)[0];

            if (results != null) {
                var project_id = results.split('=')[1].slice(0, -1);
                if (project_id > 0) {
                    deferred.resolve(project_id);
                }
            } else {
                deferred.resolve(-1);
            }
        };
        f.readAsText(file);
        return deferred.promise();
    } else {
        // can't find the project_id, so return -1
        return -1;
    }
}

function uploader() {
    $('#dataset').change(function() {
        $.when(extractProjectId()).done(function(project_id) {
            if (project_id > 0) {
                $('#projects').val(project_id);
                $('#projects').prop('disabled', true);
                if ($('.toggle-content#projects_toggle').is(':hidden')) {
                    $('.toggle-content#projects_toggle').show(400);
                }
            } else {
                if ($('.toggle-content#projects_toggle').is(':hidden')) {
                    $('.toggle-content#projects_toggle').show(400);
                }
            }
        });
    });
    $('#upload').change(function() {
        if ($('.toggle-content#expedition_code_toggle').is(':hidden')) {
            $('.toggle-content#expedition_code_toggle').show(400);
        } else {
            $('.toggle-content#expedition_code_toggle').hide(400);
        }
    });
}