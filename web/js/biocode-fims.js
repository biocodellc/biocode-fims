// Global variables
var configuration="https://biocode-fims.googlecode.com/svn/trunk/Documents/IndoPacific/indoPacificConfiguration.xml";
var expedition_id = 1;



// Get results as JSON
function queryJSON() {
    theUrl = "/biocode-fims/query/json/?" + getGraphs() + "&" + getConfiguration() + "&" +  getFilter();

    var jqxhr = $.getJSON( theUrl, function(data) {
        distal(results,data);
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
	     showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
	    showMessage ("Error completing request!");
        }
    });
}

// Get the graphs
function populateGraphs() {
    theUrl = "http://biscicol.org/id/projectService/expedition/" + expedition_id;
    var jqxhr = $.getJSON( theUrl, function(data) {
        distal(document.body,data);
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
    theUrl = "/biocode-fims/query/excel/?" + getGraphs() + "&" + getConfiguration() + "&" +  getFilter();
    window.location = theUrl;
    showMessage ("Downloading results as an Excel document<br>this will appear in your browsers download folder.");
}

// Get the URL key/value for the filter specified by the user
function getFilter() {
    var filter = document.getElementById("filter").value;
    if (filter != "") {
     return "filter=" + document.getElementById("filter").value;
    }
    return "";
}

// Get the URL key/value for configuration
function getConfiguration() {
    return "configuration=" + encodeURIComponent(configuration);
}

// Get the URL key/value for the graphs by parsing return from the BCID service
function getGraphs() {
    var str = "";
    var separator = "";
    $( "select option:selected" ).each(function() {
        str += separator + encodeURIComponent($( this ).text());
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