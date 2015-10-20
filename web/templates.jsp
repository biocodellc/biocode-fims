<%@ include file="header-templates.jsp" %>

<div id="template" class="section">
<div class="sectioncontent">

<h2>Generate Template</h2>

    <form>
        <table border=0 class="table" style="width:600px;">
            <tr>
                <td align=right>&nbsp;&nbsp;Choose Project&nbsp;&nbsp;</td>
                <td align=left>
                <select width=20 id=projects onChange="populateColumns('#cat1');populateAbstract('#abstract');">
                        <option value=0>Loading projects ...</option>
                </select>

                </td>
            </tr>

            <tr  class="toggle-content" id="config_toggle">
                <td align=right>&nbsp;&nbsp;Choose Template Config&nbsp;&nbsp;</td>
                <td align=left id="config_container">
                    <select width=20 id="configs" onChange="updateCheckedBoxes();">
                        <option value=0>Select a Project</option>
                    </select>
                    <a class="toggle-content" id="remove_config_toggle" href="#" onclick="removeConfig();">remove</a>
                </td>
            </tr>
        </table>
    </form>

    <div style='min-height:200px;'>

        <div style="width: 45%; float:left;border-right: 1px solid gray; ">

            <h2>Available Columns</h2>

            <p>Check available column headings to include in your customized FIMS
                spreadsheet.</p>

            <div id="cat1"></div>

        </div>


        <div style="width: 45%; float:left;margin-left:5px;">

            <div id='abstract'></div>

            <h2>Definition</h2>

            <p>Click on the "DEF" link next to any of the headings to see its definition in this pane.</p>

            <div id='definition'></div>
        </div>

    </div>

    <div style="clear:both;"></div>

    <p>

    <button type='button' id='excel_button' class="btn btn-default">Export Excel</button>

    <div id=dialogContainer></div>

</div>
</div>

<%@ include file="footer.jsp" %>
