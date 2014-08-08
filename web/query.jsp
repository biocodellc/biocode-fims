<%@ include file="header-query.jsp" %>

<div id="query" class="section">
<div class="sectioncontent">

<h2>Query</h2>

    <form method="POST">
        <table border=0 class="table" style="width:800px;">
            <tr>
                <td align=right>&nbsp;&nbsp;Choose Project&nbsp;&nbsp;</td>
                <td><select width=20 id=projects style="display:inline-block;width:400px;text-align:left;">
                    <option value=0>Loading projects ...</option>
                </select></td>
            </tr>

            <tr>
                <td align=right>&nbsp;&nbsp;Choose Dataset(s)&nbsp;&nbsp;&nbsp;</td>
                <td><select id=graphs multiple style="display:inline-block;width:400px;text-align:left;"></select></td>
            </tr>

            <tr class=toggle-content id=filter-toggle>
                <td align=right>Filter</td>
                <td>
                    <select id="uri" style="max-width:100px;"><option value="0">Loading ...</option></select>
                    <p style='display:inline;'>=</p>
                    <input type="text" name="filter_value" style="width:250px;"/>
                    <input type=button value=+ id=add_filter  class="btn btn-default btn-sm"/>
                </td>
            </tr>

            <tr>
                <td colspan=2>
                    <input type=button id="submit" value=table class="btn btn-default btn-sm">
                    <input type=button id="submit" value=excel class="btn btn-default btn-sm">
                    <input type=button id="submit" value=kml class="btn btn-default btn-sm">
                </td>
            </tr>
        </table>
    </form>

    <div id=resultsContainer style='overflow:auto; display:none;'>
        <table id=results border=0>
            <tr>
                <th data-qrepeat="m header"><b data-qtext="m"></b></th>
            </tr>
            <tr data-qrepeat="m data">
                <td data-qrepeat="i m.row"><i data-qtext="i"></i>
            </tr>
        </table>
    </div>

</div>
</div>
<script>
    $(document).ready(function() {
        graphsMessage('Choose a project to see loaded spreadsheets');
        populateProjects();

        $("#projects").change(function() {
            if ($(this).val() == 0) {
                $(".toggle-content#filter-toggle").hide(400);
            } else {
                $(".toggle-content#filter-toggle").show(400);
            }
            populateGraphs(this.options[this.selectedIndex].value);
            getFilterOptions(this.value).done(function() {
                $("#uri").replaceWith(filterSelect);
            });
        });

        $("#add_filter").click(function() {
            addFilter();
        });

        $("input[id=submit]").click(function(e) {
            e.preventDefault();

            var params = getQueryPostParams();
            switch (this.value)
            {
                case "table":
                    queryJSON(params);
                    break;
                case "excel":
                    queryExcel(params);
                    break;
                case "kml":
                    queryKml(params);
                    break;
            }
        });
    });
</script>
<%@ include file="footer.jsp" %>
