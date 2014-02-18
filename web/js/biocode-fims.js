// Get the available projects
function populateProjects() {
    theUrl = "http://biscicol.org/id/projectService/list";
    var jqxhr = $.getJSON( theUrl, function(data) {
	// Initialize the graphs option
	graphsMessage('Choose an project to see loaded spreadsheets');
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

// Get the graphs
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
    theUrl = "/biocode-fims/query/json/?" + getGraphsKeyValue() + "&" + getProjectKeyValue() + "&" +  getFilterKeyValue();
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
    theUrl = "/biocode-fims/query/excel/?" + getGraphsKeyValue() + "&" + getProjectKeyValue() + "&" +  getFilterKeyValue();
    window.location = theUrl;
    showMessage ("Downloading results as an Excel document<br>this will appear in your browsers download folder.");
}

// Get results as Excel
function queryKml() {
    theUrl = "/biocode-fims/query/kml/?" + getGraphsKeyValue() + "&" + getProjectKeyValue() + "&" +  getFilterKeyValue();
    window.location = theUrl;
    showMessage ("Downloading results as an KML document<br>If Google Earth does not open you can point to it directly");
}

// Get results as Excel
function queryGoogleMaps() {
    theUrl = "http://biscicol.org/biocode-fims/query/kml/" +encodeURIComponent("?") + getGraphsKeyValue() + encodeURIComponent("&") + getProjectKeyValue() + encodeURIComponent("&") +  getFilterKeyVal
ue
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

// Get the project_id
function getProjectKeyValue() {
    var e = document.getElementById('projects');
    return "project_id=" + e.options[e.selectedIndex].value
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