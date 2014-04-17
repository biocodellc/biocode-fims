<%@ include file="header-query.jsp" %>

<div id="query" class="section">
<div class="sectioncontent">

<h2>Query</h2>

    <form>
        <table border=0>
            <tr>
                <td align=right>Choose Project</td>
                <td><select width=20 id=projects onchange='populateGraphs(this.options[this.selectedIndex].value);' style="display:inline-block;width:400px;text-align:left;">
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

            <tr>
                <td align=right>Filter</td>
                <td><input type=text id=filter style="display:inline-block;width:400px;text-align:left;"></td>
            </tr>

            <tr>
                <td colspan=2>
                    <input type=button onclick="javascript:queryJSON();" value=table>
                    <input type=button onclick="javascript:queryExcel();" value=excel>
                    <input type=button onclick="javascript:queryKml();" value=kml>
                </td>
            </tr>
        </table>
    </form>

    <div id=resultsContainer style='overflow:auto;'>
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
<%@ include file="footer.jsp" %>
