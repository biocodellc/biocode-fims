// Get the available expeditions
function populateExpeditions() {
    theUrl = "http://biscicol.org/id/expeditionService/list";
    var jqxhr = $.getJSON( theUrl, function(data) {
        distal(expeditions,data);
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
	     showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
	    showMessage ("Error completing request!");
        }
    });
}

// Get the graphs
function populateGraphs(expedition_id) {
    theUrl = "http://biscicol.org/id/expeditionService/graphs/" + expedition_id;
    var jqxhr = $.getJSON( theUrl, function(data) {
        distal(graphs,data);
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
    theUrl = "/biocode-fims/query/json/?" + getGraphsKeyValue() + "&" + getExpeditionKeyValue() + "&" +  getFilterKeyValue();
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

// Get results as Excel
function queryExcel() {
    theUrl = "/biocode-fims/query/excel/?" + getGraphsKeyValue() + "&" + getExpeditionKeyValue() + "&" +  getFilterKeyValue();
    window.location = theUrl;
    showMessage ("Downloading results as an Excel document<br>this will appear in your browsers download folder.");
}

// Get results as Excel
function queryKml() {
    theUrl = "/biocode-fims/query/kml/?" + getGraphsKeyValue() + "&" + getExpeditionKeyValue() + "&" +  getFilterKeyValue();
    window.location = theUrl;
    showMessage ("Downloading results as an KML document<br>If Google Earth does not open you can point to it directly");
}

// Get results as Excel
function queryGoogleMaps() {
    theUrl = "http://biscicol.org/biocode-fims/query/kml/" +encodeURIComponent("?") + getGraphsKeyValue() + encodeURIComponent("&") + getExpeditionKeyValue() + encodeURIComponent("&") +  getFilterKeyValue
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

// Get the expedition_id
function getExpeditionKeyValue() {
    var e = document.getElementById('expeditions');
    return "expedition_id=" + e.options[e.selectedIndex].value
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