    function showVersion() {
        var text ='<strong>Version 0.2 January 11, 2014</strong>' +
              "<p>This is the first mock-up of a FIMS spreadsheet customization tool, based on Mike Trizna's "+
               '0.1 Spreadsheet customization tool. This version reads FIMS XML Configuration Files to '+
               'to generate the available mappings.  In addition, it also now reads the RequiredColumns rule ' +
               'and automatically checks those boxes for the user (and disabling unchecking).' +
               '</p>';

        showMessage(text);
    }

	function populate_bottom(){
			var selected = new Array();
			var listElement = document.createElement("ul");
			listElement.className = 'picked_tags';
			$("#checked_list").html(listElement);
			$("input:checked").each(function() {
				var listItem = document.createElement("li");
				listItem.className = 'picked_tags_li';
				listItem.innerHTML = ($(this).val());
				listElement.appendChild(listItem);
				//selected.push($(this).val());
			});
		}

    function download_file(){
        isNMNHProject(getProjectID()).done(function(accessionNumber, datasetCode, operation) {

		    // TODO: create a single place for our fims service calls
			var url = '/fims/rest/templates/createExcel/';
			var input_string = '';
			// Loop through CheckBoxes and find ones that are checked
			$(".check_boxes").each(function(index) {
                if ($(this).is(':checked'))
				    //input_string+='<input type="hidden" name="field'+index + '" value="' + $(this).val() + '" />';
				    input_string+='<input type="hidden" name="fields" value="' + $(this).val() + '" />';
			});
			input_string+='<input type="hidden" name="project_id" value="' + getProjectID() + '" />';

			if (accessionNumber != null) {
			    input_string += '<input type="hidden" name="accession_number" value="' + accessionNumber + '" />' +
			        '<input type="hidden" name="dataset_code" value="' + datasetCode + '" />';
			}

            // is this an update or insert operation (no need to check this again on server)
            input_string += '<input type="hidden" name="operation" value="' + operation +'" />';

			// Pass the form to the server and submit
			$('<form action="'+ url +'" method="post">'+input_string+'</form>').appendTo('body').submit().remove();
        });
    }

    // Processing functions
	$(function () {
		$('input').click(populate_bottom);

		$('#default_bold').click(function() {
		    $('.check_boxes').prop('checked',true);
			populate_bottom();
	    });
		$('#excel_button').click(function() {
			var li_list = new Array();
			$(".check_boxes").each(function() {
				li_list.push($(this).text() );
			});
			if(li_list.length > 0){
				download_file();
			}
			else{
				showMessage('You must select at least 1 field in order to export a spreadsheet.');
			}
	    });
    })

    // show a dialog to get the user's accession number and unique collection number
    function NMNHDialog() {

        var d = new $.Deferred();
        var title = "NMNH Project Additional Information"
        var message = "This is a NMNH project. Please enter:<br>" +
            "Accession Number: <input type='text' id='accession_number' /><br>" +
            "Dataset Code: <input type='text' id='dataset_code' />";

        var buttons = {
            "Create": function() {
                // Verify in Jscript that accession number is valid
                var digitRegExp = /^\d+$/;
                var alNumRegExp = /^\w{4,16}$/;
                if (!digitRegExp.test($("#accession_number").val()) || !alNumRegExp.test($("#dataset_code").val())) {
                    var error = "<br><p class=error>Make sure your Accession Number is an integer and the Dataset Code is " +
                        "an alphanumeric between 4 and 16 chars long!</p>";
                    dialog(message + error, title, buttons);
                // Call
                } else {

                    // Save the context of this so it can be used inside the POST
                    var $this = $(this);

                    $.getJSON("/fims/rest/utils/validateExpedition/" + $("#projects").val() + "/" + $("#dataset_code").val())
                        .done(function(data) {
                            if (data.update) {
                                var buttons = {
                                    "Continue": function() {
                                        d.resolve($("#accession_number").val(), $("#dataset_code").val(),"update");
                                        $(this).dialog("close");
                                    },
                                    "Cancel": function() {
                                        d.reject();
                                        $(this).dialog("close");
                                    }
                                }
                                // remember accession_number, dataset_code values using hidden form elements
                                dialog("Warning: Dataset Code '" + $("#dataset_code").val() + "' already exists." +
                                 "<br>The Dataset Code designates a globally unique key for this dataset. " +
                                "<br>Select CONTINUE only if you wish to update column names for a spreadsheet you recently downloaded, otherwise " +
                                "<br>select CANCEL to type in a new Dataset Code" +
                                "<input type=hidden id='accession_number' value='"+$("#accession_number").val()+"' />" +
                                "<input type=hidden id='dataset_code' value='"+$("#dataset_code").val()+"' />"
                                , "Dataset Code", buttons);
                            } else {
                                d.resolve($("#accession_number").val(), $("#dataset_code").val(), "insert");
                                $this.dialog("close");
                            }
                        }).fail(function(jqxhr) {
                            var cancelbutton = { "Cancel": function() {
                                    d.reject();
                                    $(this).dialog("close");
                                }
                            }
                            // Process status codes from server
                            if (jqxhr.status == 401) {
                                var message = "Server message<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).error);
                                dialog(message, "Dataset Error", cancelbutton);
                            } else if (jqxhr.status != 404) {
                                var message = "Server responded with HTTP status code = "+ jqxhr.status;
                                dialog(message, "Dataset Error", cancelbutton);
                            } else {
                                var message = "Dataset validation failed.<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).error);
                                dialog(message, "Dataset Error", cancelbutton);
                            }
                        });
                }
            },
            "Cancel": function() {
                d.reject();
                $(this).dialog("close");
            }
        }
        dialog(message, title, buttons);
        return d.promise();
    }

    // check if the project is an NMNHProject. If it is, we get the user's accession number and
    // datasetCode
    function isNMNHProject(projectId) {
        var d = new $.Deferred();
        $.getJSON("/fims/rest/utils/isNMNHProject/" + projectId)
            .done(function(data) {
                if (data.isNMNHProject == "true") {
                    NMNHDialog().then(function(accessionNumber, datasetCode,operation) {
                        d.resolve(accessionNumber, datasetCode, operation);
                    });
                } else {
                    d.resolve();
                }
            }).fail(function() {
                d.resolve;
        });
        return d.promise();
    }