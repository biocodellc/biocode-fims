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
		function download_file(url){
			var input_string = '';
			$(".picked_tags_li").each(function(index) {
				input_string+='<input type="hidden" name="field'+index + '" value="' + $(this).text() + '" />';
			});
			$('<form action="'+ url +'" method="post">'+input_string+'</form>').appendTo('body').submit().remove();
		}
		$(function () {
			$('#available_tags a:first').tab('show');
			$('input').click(populate_bottom);


/*			$('.def_link').click(function() {
				//var heading_name = $(this).attr('name');
				theUrl = "/biocode-fims/rest/templates/attributes/?project_id=" + project_id + "&column_name=" + $(this).attr('name');
				$.ajax({
					type: "GET",
					url: theUrl,
					dataType: "html",
					//data: {heading: heading_name},
					success: function(data) {
						$("#definition").html(data);
					}
				});
				//var def_html = '<p><strong>Heading Name:</strong> ' + heading_name + '</p>';
				//$("#definition").html(def_html);
				console.log(heading_name);
			});
			*/
			$('#default_bold').click(function() {
				$('.check_boxes').prop('checked',true);
				populate_bottom();
			});
			$('#excel_button').click(function() {
				var li_list = new Array();
				$(".picked_tags_li").each(function() {
					li_list.push($(this).text() );
				});
				if(li_list.length > 0){
					download_file('excel_export.php');
				}
				else{
					alert('You must select at least 1 field in order to export a spreadsheet.');
				}
			});
		  })