<%@ include file="header-query.jsp" %>

<div id="query" class="section">
<div class="sectioncontent">

<h2>Query</h2>

    <form method="POST">
        <table border=0>
            <tr>
                <td align=right>Choose Project</td>
                <td><select width=20 id=projects style="display:inline-block;width:400px;text-align:left;">
                    <option qdup=1 value=0>Select an project ...</option>
                    <option data-qrepeat="e projects" data-qattr="value e.project_id; text e.project_title">
                        Loading Projects ...
                    </option>
                </select></td>
            </tr>

            <tr>
                <td align=right>Choose Dataset(s)</td>
                <td><select id=graphs multiple style="display:inline-block;width:400px;text-align:left;"></select></td>
            </tr>

            <tr class=toggle-content id=filter-toggle>
                <td align=right>Filter</td>
                <td>
                    <select id="uri" style="max-width:100px;"><option value="0">Loading ...</option></select>
                    <p style='display:inline;'>=</p>
                    <input type="text" name="filter_value" style="width:250px;"/>
                    <input type=button value=+ id=add_filter />
                </td>
            </tr>

            <tr>
                <td colspan=2>
                    <input type=button id="submit" value=table>
                    <input type=button id="submit" value=excel>
                    <input type=button id="submit" value=kml>
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
        graphsMessage('Choose an project to see loaded spreadsheets');
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