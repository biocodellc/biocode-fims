    function showVersion() {
        var text ='<strong>Version 0.2 January 11, 2014</strong>' +
              "<p>This is the first mock-up of a FIMS spreadsheet customization tool, based on Mike Trizna's "+
               '0.1 Spreadsheet customization tool. This version reads Biocode-FIMS XML Configuration Files to '+
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
        isNMNHProject(getProjectID()).done(function(accessionNumber, collectionNumber) {
		    // TODO: create a single place for our biocode-fims service calls
			var url = '/biocode-fims/rest/templates/createExcel/';
			var input_string = '';
			// Loop through CheckBoxes and find ones that are checked
			$(".check_boxes").each(function(index) {
                if ($(this).is(':checked'))
				    //input_string+='<input type="hidden" name="field'+index + '" value="' + $(this).val() + '" />';
				    input_string+='<input type="hidden" name="fields" value="' + $(this).val() + '" />';
			});
			input_string+='<input type="hidden" name="project_id" value="' + getProjectID() + '" />';

			if (accessionNumber != null) {
			    input_string += '<input type="hidden" name="accession_number" value="' + accessionNumber + '"/>' +
			        '<input type="hidden" name="collection_number" value="' + collectionNumber + '"/>';
			}

			// Pass the form to the server and submit
			//showMessage("STILL TO CODE, CALL: " + url + " with input_string = " + input_string);
			// Get the form parameter correct.
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
        var message = "This is an NMNH project. Please enter:<br>" +
            "Accession Number: <input type='text' id='accession_number' /><br>" +
            "Unique Collection Number: <input type='text' id='unique_collection_number' />";

        var buttons = {
            "Create": function() {
                var digitRegExp = /^\d+$/;
                var alNumRegExp = /^\w+$/;
                if (!digitRegExp.test($("#accession_number").val()) || !alNumRegExp.test($("#unique_collection_number").val())) {
                    var error = "<br><p class=error>Make sure you Accession Number is an integer and you Unique Collection Number is an alphanumeric!</p>";
                    dialog(message + error, title, buttons);
                } else {
                    d.resolve($("#accession_number").val(), $("#unique_collection_number").val());
                    $(this).dialog("close");
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

    // check if the project is an NMNHProject. If it is, we get the user's accession number and unique collection number
    function isNMNHProject(projectId) {
        var d = new $.Deferred();
        $.getJSON("/biocode-fims/rest/utils/isNMNHProject/" + projectId)
            .done(function(data) {
                if (data.isNMNHProject == "true") {
                    NMNHDialog().then(function(accessionNumber, collectionNumber) {
                        d.resolve(accessionNumber, collectionNumber);
                    });
                } else {
                    d.resolve();
                }
            }).fail(function() {
                d.resolve;
        });
        return d.promise();
    }