// Function to display a simple list in a message
function list(url) {
    $.ajax({
        type: "GET",
        url: url,
        dataType: "html",
        success: function(data) {
                if (data.split("\n").length > 5) {
                        showBigMessage(data);
                } else {
                        showMessage(data);
                }
        }
   });

}

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

    if (project_id != 0) {
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
}

function populateAbstract(targetDivId) {
    $(targetDivId).html("Loading ...");

    var e = document.getElementById('projects');
    var project_id = e.options[e.selectedIndex].value;

    theUrl = "rest/templates/abstract/?project_id=" + project_id;

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

function populateProjects() {
    theUrl = "rest/utils/listProjects";
    var jqxhr = $.getJSON( theUrl, function(data) {
        var listItems = "";
        listItems+= "<option value='0'>Select a project ...</option>";
        $.each(data.projects,function(index,project) {
            listItems+= "<option value='" + project.project_id + "'>" + project.project_title + "</option>";
        });
        $("#projects").html(listItems);
        // Set to the first value in the list which should be "select one..."
        $("#projects").val($("#projects option:first").val());
        $('.toggle-content#projects_toggle').show(400);

        $("#projects").on("change", function() {
            if ($('.toggle-content#config_toggle').is(':hidden')) {
                $('.toggle-content#config_toggle').show(400);
            }
        });
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
    theUrl = "rest/utils/graphs/" + project_id;
    var jqxhr = $.getJSON( theUrl, function(data) {
        // Check for empty object in response
        if (typeof data['data'][0] === "undefined") {
	        graphsMessage('No datasets found for this project');
        } else {
	        var listItems = "";
             $.each(data.data,function(index,graph) {
                listItems+= "<option value='" + graph.graph + "'>" + graph.expedition_title + "</option>";
            });
            $("#graphs").html(listItems);
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
    var jqxhr = $.post("rest/query/json/", $.param(params, true))
        .done(function(data) {
            $("#resultsContainer").show();
           //alert('debugging queries now, will fix soon!');
           //alert(data);
            // TODO: remove distal from this spot
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
    download("rest/query/excel/", params);
}

// Get results as Excel
function queryKml(params) {
    showMessage ("Downloading results as an KML document<br>If Google Earth does not open you can point to it directly");
    download("rest/query/kml/", params);
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
    //theUrl = "http://biscicol.org/biocode-fims/rest/query/kml/" +encodeURIComponent("?") + getGraphsKeyValue() + encodeURIComponent("&") + getProjectKeyValue() + encodeURIComponent("&") +  getFilterKeyValue
    theUrl = "rest/query/kml/" +encodeURIComponent("?") + getGraphsKeyValue() + encodeURIComponent("&") + getProjectKeyValue();
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

// A short message
function showMessage(message) {
$('#alerts').append(
        '<div class="alert">' +
            '<button type="button" class="close" data-dismiss="alert">' +
            '&times;</button>' + message + '</div>');
}
// A big message
function showBigMessage(message) {
$('#alerts').append(
        '<div class="alert" style="height:400px">' +
            '<button type="button" class="close" data-dismiss="alert">' +
            '&times;</button>' + message + '</div>');
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

    //$("#resultsContainer").html("<table><tr><td>" + message + "</td></tr></table>");
    $("#resultsContainer").html(message);
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
        url: "rest/validate/",
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
            de.reject(jqxhr);
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
        message = JSON.stringify($.parseJSON(jqxhr.responseText).usrMessage);
    } else {
        message = "Server Error!";
    }
    dialog(message, "Error", buttons);
}

// Check that the validation form has a project id and if uploading, has an expedition code
function validForm(dataset_code) {
    if ($('#projects').val() == 0 || $("#upload").is(":checked")) {
        var message;
        var error = false;
        var dRE = /^[a-zA-Z0-9_-]{4,50}$/

        if ($('#projects').val() == 0) {
            message = "Please select a project.";
            error = true;
        } else if ($("#upload").is(":checked")) {
            // get the dataset code value
            //var datasetcodeval = $("#expedition_code").val();
            // if it doesn't pass the regexp test, then set error message and set error to true
            if (!dRE.test(dataset_code)) {
                message = "<b>Dataset Code</b> must contain only numbers, letters, or underscores and be 4 to 50 characters long";
                error = true;
            }
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
    if ($("#upload").is(":checked") && $("#expedition_code").val() == 0) {
        createExpedition().done(function (e) {

            if (validForm(e)) {
                // if the form is valid, update the dataset code value
                $("#expedition_code").replaceWith("<input name='expedition_code' id='expedition_code' type='text' value=" + e + " />");

                // Submit form
                submitForm().done(function(data) {
                    validationResults(data);
                }).fail(function(jqxhr) {
                    failError(jqxhr);
                });
            }
        })
    } else if (validForm($("#expedition_code").val())) {
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
    $.getJSON("rest/validate/status")
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
    var url = "rest/validate/continue";
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

// function to handle the results from the rest service /biocode-fims/rest/validate
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
        if (data.continue_message.message == null) {
            continueUpload(false);
        } else {
            // ask user if want to proceed
            var buttons = {
                "Continue": function() {
                      continueUpload(false);
                },
                "Cancel": function() {
                    writeResults(data.continue_message.message);
                    $(this).dialog("close");
                }
            }
            dialog(data.continue_message.message, title, buttons);
        }
    }
}

// function to handle the results from the rest service /biocode-fims/rest/validate/continue
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
        $('.toggle-content#expedition_public_toggle').hide(400);

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
        dialog(data.continue_message, title, buttons);
    }
}

// function to verify naan's
function checkNAAN(spreadsheetNaan, naan) {
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

// function to toggle the project_id and expedition_code inputs of the validation form
function validationFormToggle() {
    $('#dataset').change(function() {
        // Clear the resultsContainer
        $("#resultsContainer").empty();

        // Check NAAN
        $.when(parseSpreadsheet("~naan=[0-9]+~", "Instructions")).done(function(spreadsheetNaan) {
            if (spreadsheetNaan > 0) {
                $.getJSON("rest/utils/getNAAN")
                        .done(function(data) {
                    checkNAAN(spreadsheetNaan, data.naan);
                });
            }
        });

        $.when(parseSpreadsheet("~project_id=[0-9]+~", "Instructions")).done(function(project_id) {
            if (project_id > 0) {
                $('#projects').val(project_id);
                $('#projects').prop('disabled', true);
                $('#projects').trigger("change");
                if ($('.toggle-content#projects_toggle').is(':hidden')) {
                    $('.toggle-content#projects_toggle').show(400);
                }
            } else {
                var p = $("#projects");
                // add a refresh map link incase the new dataset has the same project as the previous dataset. In that
                // case, the user won't change projects and needs to manually refresh the map
                if (p.val() != 0) {
                    p.parent().append("<a id='refresh_map' href='#' onclick=\"generateMap('map', " + p.val() + ")\">Refresh Map</a>");
                }
                p.prop('disabled', false);
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
        if ($('.toggle-content#expedition_public_toggle').is(':hidden') && $('#upload').is(":checked")) {
            $('.toggle-content#expedition_public_toggle').show(400);
        } else {
            $('.toggle-content#expedition_public_toggle').hide(400);
        }
    });
    $("#projects").change(function() {
        // generate the map if the dataset isn't empty
        if ($("#dataset").val() != "") {
            generateMap('map', this.value);
        }

        // only get expedition codes if a user is logged in
        if ($('*:contains("Logout")').length > 0) {
            $("#expedition_code").replaceWith("<p id='expedition_code'>Loading ... </p>");
            getExpeditionCodes();
        }
    });
}

// update the checkbox to reflect the expedition's public status
function updateExpeditionPublicStatus(expeditionList) {
    $('#expedition_code').change(function() {
        var code = $('#expedition_code').val();
        var public;
        $.each(expeditionList.expeditions, function(key, e) {
            if (e.expedition_code == code) {
                public = e.public;
                return false;
            }
        });
        if (public == 'true') {
            $('#public_status').prop('checked', true);
        } else {
            $('#public_status').prop('checked', false);
        }
    });
}


// get the expeditions codes a user owns for a project
function getExpeditionCodes() {
    var projectID = $("#projects").val();
    $.getJSON("rest/utils/expeditionCodes/" + projectID)
        .done(function(data) {
            var select = "<select name='expedition_code' id='expedition_code' style='max-width:199px'>" +
                "<option value='0'>Create New Dataset</option>";
            $.each(data.expeditions, function(key, e) {
                select += "<option value=" + e.expedition_code + ">" + e.expedition_code + " (" + e.expedition_title + ")</option>";
            });

            select += "</select>";
            $("#expedition_code").replaceWith(select);
            updateExpeditionPublicStatus(data);
        }).fail(function(jqxhr) {
            $("#expedition_code").replaceWith('<input type="text" name="expedition_code" id="expedition_code" />');
            $("#dialogContainer").addClass("error");
            var buttons = {
                "Ok": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close");
                }
            }
            dialog("Error fetching datasets!<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).usrMessage), "Error!", buttons)
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

// function to retrieve the user's datasets
function getDatasetDashboard() {
    theUrl = "rest/utils/getDatasetDashboard";
    var jqxhr = $.getJSON( theUrl, function(data) {
        $("#dashboard").html(data.dashboard);
        // attach toggle function to each project
        $(".expand-content").click(function() {
            projectToggle(this.id)
        });
    }).fail(function() {
        $("#mainpage").show();
    });
}

// function to apply the jquery slideToggle effect.
function projectToggle(id) {
    /*if ($('.toggle-content#'+id).is(':hidden')) {
        $('.img-arrow', '#'+id).attr("src","images/down-arrow.png");
    } else {
        $('.img-arrow', '#'+id).attr("src","images/right-arrow.png");
    }
    $('.toggle-content#'+id).slideToggle('slow');   */
     // escape special characters in id field
        id = id.replace(/([!@#$%^&*()+=\[\]\\';,./{}|":<>?~_-])/g, "\\$1");
        // store the element value in a field
        var idElement = $('.toggle-content#'+id);
        if (idElement.is(':hidden')) {
            $('.img-arrow', '#'+id).attr("src","../images/down-arrow.png");
        } else {
            $('.img-arrow', '#'+id).attr("src","../images/right-arrow.png");
        }
        $(idElement).slideToggle('slow');
}

// function to edit a dataset
function editDataset(project_id, expedition_code, e) {
    var currentPublic;
    var title = "Editing " + $(e.parentElement).siblings()[0].textContent;

    if ($(e.parentElement).siblings()[1].textContent == "yes") {
        currentPublic = true;
    } else {
        currentPublic = false;
    }

    var message = "<table><tr><td>Public:</td><td><input type='checkbox' name='public'";
    if (currentPublic) {
        message += " checked='checked'";
    }
    message += "></td></tr></table>";

    var buttons = {
        "Update": function() {
            var public = $("[name='public']")[0].checked;

            $.post("rest/utils/updatePublicStatus", { project_id: project_id, expedition_code: expedition_code, public: public}
            ).done(function() {
                var b = {
                    "Ok": function() {
                        $(this).dialog("close");
                        location.reload();
                    }
                }
                dialog("Successfully updated the public status.", "Success!", b);
            }).fail(function(jqXHR) {
                $("#dialogContainer").addClass("error");
                var b= {
                    "Ok": function() {
                    $("#dialogContainer").removeClass("error");
                    $(this).dialog("close");
                    }
                }
                dialog("Error updating dataset public status!<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).usrMessage), "Error!", buttons)
            });
        },
        "Cancel": function() {
            $(this).dialog("close");
        }
    }
    dialog(message, title, buttons);
}

function parseSpreadsheet(regExpression, sheetName) {
    try {
        f = new FileReader();
    } catch(err) {
        return null;
    }
    var deferred = new $.Deferred();
    // older browsers don't have a FileReader
    if (f != null) {
        var inputFile= $('#dataset')[0].files[0];

        var splitFileName = $('#dataset').val().split('.');
        if ($.inArray(splitFileName[splitFileName.length - 1], XLSXReader.exts) > -1) {
            $.when(XLSXReader.utils.findCell(inputFile, regExpression, sheetName)).done(function(match) {
                if (match) {
                    deferred.resolve(match.toString().split('=')[1].slice(0, -1));
                } else {
                    deferred.resolve(null);
                }
            });
            return deferred.promise();
        }
    }
    setTimeout(function(){deferred.resolve(null)}, 100);
    return deferred.promise();

}

var savedConfig;
function saveTemplateConfig() {
    var message = "<table><tr><td>Configuration Name:</td><td><input type='text' name='config_name' /></td></tr></table>";
    var title = "Save Template Generator Configuration";
    var buttons = {
        "Save": function() {
            var checked = [];
            var configName = $("input[name='config_name']").val();

            if (configName.toUpperCase() == "Default".toUpperCase()) {
                $("#dialogContainer").addClass("error");
                dialog("Talk to the project admin to change the default configuration.<br><br>" + message, title, buttons);
                return;
            }

            $("#cat1 input[type='checkbox']:checked").each(function() {
                checked.push($(this).data().uri);
            });

            savedConfig = configName;
            $.post("/biocode-fims/rest/templates/saveConfig/" + $("#projects").val(), $.param(
                                                            {"configName": configName, "checkedOptions": checked}, true)
            ).done(function(data) {
                if (data.error != null) {
                    $("#dialogContainer").addClass("error");
                    var m = data.error + "<br><br>" + message;
                    dialog(m, title, buttons);
                } else {
                    $("#dialogContainer").removeClass("error");
                    populateConfigs();
                    var b = {
                        "Ok": function() {
                            $(this).dialog("close");
                        }
                    }
                    dialog(data.success + "<br><br>", "Success!", b);
                }
            }).fail(function(jqXHR) {
                failError(jqXHR);
            });
        },
        "Cancel": function() {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}



function populateConfigs() {
    var project_id = $("#projects").val();
    if (project_id == 0) {
        $("#configs").html("<option value=0>Select a Project</option>");
    } else {
        var el = $("#configs");
        el.empty();
        el.append($("<option></option>").attr("value", 0).text("Loading configs..."));
        $.getJSON("/biocode-fims/rest/templates/getConfigs/" + project_id).done(function(data) {
            var listItems = "";

            el.empty();
            data.configNames.forEach(function(configName) {
                el.append($("<option></option>").
                    attr("value", configName).text(configName));
            });

            if (savedConfig != null) {
                $("#configs").val(savedConfig);
            }

            // if there are more then the default config, show the remove link
            if (data.configNames.length > 1) {
                if ($('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').show(400);
                }
            } else {
                if (!$('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').hide();
                }
            }

        }).fail(function(jqXHR,textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage ("Error fetching template configurations!");
            }
        });
    }
}

function updateCheckedBoxes() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        populateColumns("#cat1");
    } else {
        $.getJSON("/biocode-fims/rest/templates/getConfig/" + $("#projects").val() + "/" + configName.replace(/\//g, "%2F")).done(function(data) {
            if (data.error != null) {
                showMessage(data.error);
                return;
            }
            // deselect all unrequired columns
            $(':checkbox').not(":disabled").each(function() {
                this.checked = false;
            });

            data.checkedOptions.forEach(function(uri) {
                $(':checkbox[data-uri="' + uri + '"]')[0].checked = true;
            });
        }).fail(function(jqXHR, textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage ("Error fetching template configuration!");
            }
        });
    }
}

function removeConfig() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        var buttons = {
            "Ok": function() {
                $(this).dialog("close");
            }
        }
        dialog("You can not remove the Default configuration", title, buttons);
        return;
    }

    var message = "Are you sure you want to remove "+ configName + " configuration?";
    var title = "Warning";
    var buttons = {
        "OK": function() {
            var buttons = {
                "Ok": function() {
                    $(this).dialog("close");
                }
            }
            var title = "Remove Template Generator Configuration";

            $.getJSON("/biocode-fims/rest/templates/removeConfig/" + $("#projects").val() + "/" + configName.replace(/\//g, "%2F")).done(function(data) {
                if (data.error != null) {
                    showMessage(data.error);
                    return;
                }

                populateConfigs();
                dialog(data.success, title, buttons);
            });
        },
        "Cancel": function() {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}

// function to apply the jquery slideToggle effect.
function projectToggle(id) {
    // escape special characters in id field
    id = id.replace(/([!@#$%^&*()+=\[\]\\';,./{}|":<>?~_-])/g, "\\$1");
    // store the element value in a field
    var idElement = $('.toggle-content#'+id);
    if (idElement.is(':hidden')) {
        $('.img-arrow', '#'+id).attr("src","../images/down-arrow.png");
    } else {
        $('.img-arrow', '#'+id).attr("src","../images/right-arrow.png");
    }
    $(idElement).slideToggle('slow');
}


