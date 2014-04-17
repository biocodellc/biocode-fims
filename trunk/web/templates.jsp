<%@ include file="header-templates.jsp" %>

<div id="template" class="section">
<div class="sectioncontent">

<h2>Generate Template</h2>

    <form>
        <table border=0>
            <tr>
                <td align=right>Choose Project</td>
                <td align=left>
                <select width=20 id=projects onChange="populateColumns('#cat1');">
                        <option qdup=1 value=0>Select an project ...</option>
                        <option data-qrepeat="e projects" data-qattr="value e.project_id; text e.project_title">
                            Loading Projects ...
                        </option>
                </select>

                </td>
            </tr>

        </table>
    </form>

    <div style='min-height:200px;'>

        <div style="width: 45%; float:left;border-right: 1px solid gray; ">

            <h2>Available Columns</h2>

            <p>Below, you will find all available column headings that you can include in your customized FIMS
                spreadsheet.</p>

            <div id="cat1"></div>

        </div>


        <div style="width: 45%; float:left;margin-left:5px;"> <h2>Definition</h2>

            <p>Click on the "DEF" link next to any of the headings to see its definition in this pane.</p>

            <div id='definition'></div>
        </div>

    </div>

    <div style="clear:both;"></div>

    <p>

    <button type='button' id='excel_button'>Export Excel</button>

</div>
</div>

<%@ include file="footer.jsp" %>
