// Must set global variable naan here to check a spreadsheet's naan
var naan = 99999

// for template generator, get the definitions when the user clicks on DEF
function populateDefinitions(column) {
 var e = document.getElementById('projects');
    var project_id = e.options[e.selectedIndex].value;

    theUrl = "/fims/rest/templates/definition/?project_id=" + project_id + "&column_name=" + column;

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

    theUrl = "/fims/rest/templates/attributes/?project_id=" + project_id;

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
function populateAbstract(targetDivId) {
    $(targetDivId).html("Loading ...");

    var e = document.getElementById('projects');
    var project_id = e.options[e.selectedIndex].value;

    theUrl = "/fims/rest/templates/abstract/?project_id=" + project_id;

    var jqxhr = $.ajax( {
        url: theUrl,
        async: false,
        dataType : 'html'
    }).done(function(data) {
        $(targetDivId).html(data +"<p>");
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response!");
        } else {
                showMessage ("Error completing request!" );
        }
    });
}

// Get the available projects
function populateProjects() {
    // We assume that BCID is on the same server... not always safe
    // TODO: read properties to figure out location
    theUrl = "/id/projectService/list";
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
    theUrl = "/id/projectService/graphs/" + project_id;
    var jqxhr = $.getJSON( theUrl, function(data) {
    // Check for empty object in response
    if (typeof data['data'][0] === "undefined") {
	graphsMessage('No datasets found for this project');
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
function queryJSON(params) {
   // serialize the params object using a shallow serialization
    var jqxhr = $.post("/fims/rest/query/json/", $.param(params, true))
        .done(function(data) {
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
function queryExcel(params) {
    showMessage ("Downloading results as an Excel document<br>this will appear in your browsers download folder.");
    download("/fims/rest/query/excel/", params);
}

// Get results as Excel
function queryKml(params) {
    showMessage ("Downloading results as an KML document<br>If Google Earth does not open you can point to it directly");
    download("/fims/rest/query/kml/", params);
}

// create a form and then submit that form in order to download files
function download(url, data) {
    //url and data options are required
    if (url && data) {
        var form = $('<form />', { action: url, method: 'POST'});
        $.each(data, function(key, value) {
            // if the value is an array, we need to create an input element for each value
            if (value instanceof Array) {
                $.each(value, function(i, v) {
                 var input = $('<input />', {
                     type: 'hidden',
                     name: key,
                     value: v
                 }).appendTo(form);
                });
            } else {
                var input = $('<input />', {
                    type: 'hidden',
                    name: key,
                    value: value
                }).appendTo(form);
            }
        });

        return form.appendTo('body').submit().remove();
    }
    throw new Error("url and data required");
}

// Get results as Excel
function queryGoogleMaps() {
    theUrl = "/fims/rest/query/kml/" +encodeURIComponent("?") + getGraphsKeyValue() + encodeURIComponent("&") + getProjectKeyValue();
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

// Get the query graph URIs
function getGraphURIs() {
    var graphs = [];
    $( "select#graphs option:selected" ).each(function() {
        graphs.push($(this).val());
    });
    return graphs;
}

// Get the URL key/value for the graphs by parsing return from the BCID service
function getGraphsKeyValue() {
    var str = "";
    var separator = "";
    $( "select#graphs option:selected" ).each(function() {
        str += separator + encodeURIComponent($( this ).val());
        separator = ",";
    });
    return "graphs=" + str;
}

// Uses jNotify to display messages
// To re-configure this boxes behaviour and style, goto http://demos.myjqueryplugins.com/jnotify/
function showMessage(message) {
$('#alerts').append(
        '<div class="alert">' +
            '<button type="button" class="close" data-dismiss="alert">' +
            '&times;</button>' + message + '</div>');
/*
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
*/
}


// handle displaying messages/results in the graphs(spreadsheets) select list
function graphsMessage(message) {
        $('#graphs').empty();
        $('#graphs').append('<option data-qrepeat="g data" data-qattr="value g.graph; text g.expedition_title"></option>');
        $('#graphs').find('option').first().text(message);
}

// function to open an new or update an already open jquery ui dialog box
function dialog(msg, title, buttons) {
    var dialogContainer = $("#dialogContainer");
    if (dialogContainer.html() != msg) {
        dialogContainer.html(msg);
    }

    if (!$(".ui-dialog").is(":visible") || (dialogContainer.dialog("option", "title") != title ||
        dialogContainer.dialog("option" , "buttons") != buttons)) {
        dialogContainer.dialog({
            modal: true,
            autoOpen: true,
            title: title,
            resizable: false,
            width: 'auto',
            draggable: false,
            buttons: buttons,
            position: { my: "center top", at: "top", of: window}
        });
    }

    return;
}

// write results to the resultsContainer
function writeResults(message) {
    $("#resultsContainer").show();
    // Add some nice coloring
    message= message.replace(/Warning:/g,"<span style='color:orange;'>Warning:</span>");
    message= message.replace(/Error:/g,"<span style='color:red;'>Error:</span>");
    // set the project key for any project_id expressions... these come from the validator to call REST services w/ extra data
    message= message.replace(/project_id=/g,getProjectKeyValue());
    $("#resultsContainer").html("<table><tr><td>" + message + "</td></tr></table>");
}

// If the user wants to create a new expedition, get the expedition code
function createExpedition() {
    var d = new $.Deferred();
    var message = "<input type='text' id='new_expedition' />";
    var buttons = {
        "Create": function() {
            d.resolve($("#new_expedition").val());
        },
        "Cancel": function() {
            d.reject();
            $(this).dialog("close");
        }
    }
    dialog(message, "Dataset Code", buttons);
    return d.promise();
}

// function to submit the validation form using jquery form plugin to handle the file uploads
function submitForm(){
    var de = new $.Deferred();
    var promise = de.promise();
    var options = {
        url: "/fims/rest/validate/",
        type: "POST",
        contentType: "multipart/form-data",
        beforeSerialize: function(form, options) {
            $('#projects').prop('disabled', false);
        },
        beforeSubmit: function(form, options) {
            $('#projects').prop('disabled', true);
            dialog("Loading ...", "Validation Results", null);
            // For browsers that don't support the upload progress listener
            var xhr = $.ajaxSettings.xhr();
            if (!xhr.upload) {
                loopStatus(promise)
            }
        },
        error: function(jqxhr) {
            de.reject();
        },
        success: function(data) {
            de.resolve(data);
        },
        fail: function(jqxhr) {
            de.reject(jqxhr);
        },
        uploadProgress: function(event, position, total, percentComplete) {
            // For browsers that do support the upload progress listener
            if (percentComplete == 100) {
            loopStatus(promise)
            }
        }
    }

    $('form').ajaxSubmit(options);
    return promise;
}

// function to show the user an error occurred if an ajax call failed
function failError(jqxhr) {
    var buttons = {
        "OK": function(){
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
          }
    }
    $("#dialogContainer").addClass("error");

    var message;
    if (jqxhr.responseText != null) {
        message = jqxhr.responseText;
    } else {
        message = "Server Error!";
    }
    dialog(message, "Error", buttons);
}

// Check that the validation form has a project id and if uploading, has an expedition code
function validForm() {
    if ($('#projects').val() == 0 || $("#upload").is(":checked")) {
        var message;
        var error = false;
        if ($('#projects').val() == 0) {
            message = "Please select a project.";
            error = true;
        } else if ($("#upload").is(":checked") && ($('#expedition_code').val() == null ||
            $('#expedition_code').val().length < 4)) {
            message = "Dataset code is too short. Must be between 4 and 16 characters.";
            error = true;
        } else if ($("#upload").is(":checked") && ($('#expedition_code').val().length > 16)) {
            message = "Dataset code is too long. Please limit to 16 characters.";
            error = true;
        }
        if (error) {
            $('#resultsContainer').html(message);
            var buttons = {
                "OK": function(){
                    $(this).dialog("close");
                  }
            }
            dialog(message, "Validation Results", buttons);
            return false;
        }
    }
    return true;
}

// submit dataset to be validated/uploaded
function validatorSubmit() {
    // User wants to create a new expedition
   /* if ($("#upload").is(":checked") && $("#expedition_code").val() == 0) {
        createExpedition().done(function (e) {
            $("#expedition_code").replaceWith("<input name='expedition_code' id='expedition_code' type='text' value=" + e + " />");
            if (validForm()) {
                submitForm().done(function(data) {
                    validationResults(data);
                }).fail(function(jqxhr) {
                    failError(jqxhr);
                });
            }
        })
    } else if (validForm()) {
    */
    if (validForm()) {
        submitForm().done(function(data) {
            validationResults(data);
        }).fail(function(jqxhr) {
            failError(jqxhr);
        });
    }
}

// keep looping pollStatus every second until results are returned
function loopStatus(promise) {
    setTimeout( function() {
        pollStatus()
            .done(function(data) {
                if (promise.state() == "pending") {
                    if (data.error != null) {
                        dialog(data.error, "Validation Results");
                    } else {
                        dialog(data.status, "Validation Results");
                    }
                    loopStatus(promise);
                }
            });
    }, 1000);
}

// poll the server to get the validation/upload status
function pollStatus() {
    var def = new $.Deferred();
    $.getJSON("/fims/rest/validate/status")
        .done(function(data) {
            def.resolve(data);
        }).fail(function() {
            def.reject();
        });
    return def.promise();
}

// Continue the upload process after getting user consent if there were warnings during validation or if we are creating
// a new expedition
function continueUpload(createExpedition) {
    var d = new $.Deferred();
    var url = "/fims/rest/validate/continue_spreadsheet";
    if (createExpedition) {
        url += "?createExpedition=true";
    }
    $.getJSON(url)
        .done(function(data) {
            d.resolve();
            uploadResults(data);
        }).fail(function(jqxhr) {
            d.reject();
            failError(jqxhr);
        });
    loopStatus(d.promise());
}

// function to handle the results from the rest service /fims/rest/validate
function validationResults(data) {
    var title = "Validation Results";
    if (data.done != null) {
        var buttons = {
            "Ok": function() {
                $(this).dialog("close");
            }
        }
        $("#dialogContainer").dialog("close");
        writeResults(data.done);
//        dialog(data.done, title, buttons);
    } else {
        if (data.continue.message == null) {
            continueUpload(false);
        } else {
            // ask user if want to proceed
            var buttons = {
                "Continue": function() {
                      continueUpload(false);
                },
                "Cancel": function() {
                    writeResults(data.continue.message);
                    $(this).dialog("close");
                }
            }
            dialog(data.continue.message, title, buttons);
        }
    }
}

// function to handle the results from the rest service /fims/rest/validate/continue
function uploadResults(data) {
    var title = "Upload Results";
    if (data.done != null || data.error != null) {
        var message;
        if (data.done != null) {
            message = data.done;
            writeResults(message);
        } else {
            $("#dialogContainer").addClass("error");
            message = data.error;
        }
        var buttons = {
            "Ok": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close");
            }
        }
        dialog(message, title, buttons);
        // reset the form to default state
        $('form').clearForm();
        $('.toggle-content#projects_toggle').hide(400);
        $('.toggle-content#expedition_code_toggle').hide(400);

    } else {
        // ask user if want to proceed
        var buttons = {
            "Continue": function() {
                continueUpload(true);
            },
            "Cancel": function() {
                $(this).dialog("close");
            }
        }
        dialog(data.continue, title, buttons);
    }
}

// function to extract the project_id from a dataset to be uploaded
function extractNAAN() {
    var f = new FileReader();
    // older browsers don't have a FileReader
    if (f != null) {
        var deferred = new $.Deferred();
        var file = $('#dataset')[0].files[0];
        // after file has been read, extract the naan if present
        f.onload = function () {
            var fileContents = f.result;
            var re = "~naan=[0-9]+~";
            try {
                var results = fileContents.match(re)[0];

                if (results != null) {
                    var project_id = results.split('=')[1].slice(0, -1);
                    if (project_id > 0) {
                        deferred.resolve(project_id);
                    }
                } else {
                    deferred.resolve(-1);
                }
            } catch (e) {
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
            try {
                var results = fileContents.match(re)[0];

                if (results != null) {
                    var project_id = results.split('=')[1].slice(0, -1);
                    if (project_id > 0) {
                        deferred.resolve(project_id);
                    }
                } else {
                    deferred.resolve(-1);
                }
            } catch (e) {
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

// function to extract the dataset_code from a dataset to be uploaded
function extractDatasetCode() {
    var f = new FileReader();
    // older browsers don't have a FileReader
    if (f != null) {
        var deferred = new $.Deferred();
        var file = $('#dataset')[0].files[0];
        // after file has been read, extract the project_id if present
        f.onload = function () {
            var fileContents = f.result;
            var re = "~dataset_code=[a-zA-Z0-9-_]{4,16}~";
            try {
                var results = fileContents.match(re)[0];

                if (results != null) {
                    var dataset_code = results.split('=')[1].slice(0, -1);
                    if (dataset_code != null && dataset_code.length > 0) {
                        deferred.resolve(dataset_code);
                    }
                } else {
                    deferred.resolve(null);
                }
            } catch (e) {
                deferred.resolve(null);
            }
        };
        f.readAsText(file);
        return deferred.promise();
    } else {
        // can't find the dataset_code, so return null
        return null;
    }
}

// function to toggle the project_id and expedition_code inputs of the validation form
function validationFormToggle() {
    $('#dataset').change(function() {

        // Check NAAN
        $.when(extractNAAN()).done(function(spreadsheetNaan) {
            if (spreadsheetNaan > 0) {
                if (spreadsheetNaan != naan) {
			    var buttons = {
            			"Ok": function() {
                		$("#dialogContainer").removeClass("error");
                		$(this).dialog("close");
            			}
        		}
			    var message = "Spreadsheet appears to have been created using a different FIMS/BCID system.<br>";
			    message += "Spreadsheet says NAAN = " + spreadsheetNaan + "<br>";
			    message += "System says NAAN = " + naan + "<br>";
			    message += "Proceed only if you are SURE that this spreadsheet is being called.<br>";
			    message += "Otherwise, re-load the proper FIMS system or re-generate your spreadsheet template.";

                dialog(message, "NAAN check", buttons);
                }
            }
        });

        $.when(extractProjectId()).done(function(project_id) {
            if (project_id > 0) {
                $('#projects').val(project_id);
                $('#projects').prop('disabled', true);
                $('#projects').trigger("change");
                if ($('.toggle-content#projects_toggle').is(':hidden')) {
                    $('.toggle-content#projects_toggle').show(400);
                }
            } else {
                $('#projects').prop('disabled', false);
                if ($('.toggle-content#projects_toggle').is(':hidden')) {
                    $('.toggle-content#projects_toggle').show(400);
                }
            }
        });
    });
    $('#upload').change(function() {
        if ($('.toggle-content#expedition_code_toggle').is(':hidden') && $('#upload').is(":checked")) {
            $('.toggle-content#expedition_code_toggle').show(400);
        } else {
            $('.toggle-content#expedition_code_toggle').hide(400);
        }
    });
    $("#projects").change(function() {
        // only get expedition codes if a user is logged in
        if ($('*:contains("Logout")').length > 0) {
            $("#expedition_code").replaceWith("<p id='expedition_code'>Loading ... </p>");
            $.when(extractDatasetCode()).done(function(dataset_code) {
                if (dataset_code != null) {
                    //$("#expedition_code").replaceWith('<input type="hidden" name="expedition_code" id="expedition_code">' + dataset_code);
                    $("#expedition_code_container").html('<input type="hidden" name="expedition_code" id="expedition_code">' + dataset_code);
                    $("#expedition_code").val(dataset_code);
                } else {
                    // getExpeditionCodes();
                    alert("Problem reading dataset code from spreadsheet");
                }
            });
        }
    });
}

// get the expeditions codes a user owns for a project
function getExpeditionCodes() {
    var projectID = $("#projects").val();
    $.getJSON("/fims/rest/utils/expeditionCodes/" + projectID)
        .done(function(data) {
            var select = "<select name='expedition_code' id='expedition_code' style='max-width:199px'>" +
                "<option value='0'>Create New Dataset</option>";
            $.each(data.expeditions, function(key, e) {
                select += "<option value=" + e.expedition_code + ">" + e.expedition_code + " (" + e.expedition_title + ")</option>";
            });

            select += "</select>";
            $("#expedition_code").replaceWith(select);
        }).fail(function(jqxhr) {
            $("#expedition_code").replaceWith('<input type="text" name="expedition_code" id="expedition_code" />');
            $("#dialogContainer").addClass("error");
            var buttons = {
                "Ok": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close");
                }
            }
            dialog("Error fetching expeditions!<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).error), "Error!", buttons)
        });
}

// a select element with all of the filterable options. Used to add additional filter statements
var filterSelect = null;

// populate a select with the filter values of a given project
function getFilterOptions(projectId) {
    var jqxhr = $.getJSON("rest/mapping/filterOptions/" + projectId)
        .done(function(data) {
            if (data.error != null) {
                return;
            }
            filterSelect = "<select id='uri' style='max-width:100px;'>";
            $.each(data.attributes, function(k, v) {
                filterSelect += "<option value=" + v + ">" + k + "</option>";
            });

            filterSelect += "</select>";
        });
    return jqxhr;
}

// add additional filters to the query
function addFilter() {
    // change the method to post
    $("form").attr("method", "POST");

    var tr = "<tr>\n<td align='right'>AND</td>\n<td>\n";
    tr += filterSelect;
    tr += "<p style='display:inline;'>=</p>\n";
    tr += "<input type='text' name='filter_value' style='width:285px;' />\n";
    tr += "</td>\n";

    // insert another tr after the last filter option, before the submit buttons
    $("#uri").parent().parent().siblings(":last").before(tr);
}

// prepare a json object with the query POST params by combining the text and select inputs for each filter statement
function getQueryPostParams() {
    var params = {
        graphs: getGraphURIs(),
        project_id: getProjectID()
    }

    var filterKeys = $("select[id=uri]");
    var filterValues = $("input[name=filter_value]");

    // parse the filter keys and values and add them to the post params
    $.each(filterKeys, function(index, e) {
        if (filterValues[index].value != "") {
            params[e.value] = filterValues[index].value;
        }
    });

    return params;
}
