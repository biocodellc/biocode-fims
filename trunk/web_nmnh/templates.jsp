<%@ include file="header-templates.jsp" %>

<div id="template" class="section">
<div class="sectioncontent">

<h2>Generate Template</h2>

    <form>
        <table border=0 class="table" style="width:600px;">
            <tr>
                <td align=right>&nbsp;&nbsp;Choose Project&nbsp;&nbsp;</td>
                <td align=left>
                <c:if test="${user != null}">
                    <select width=20 id=projects onChange="populateColumns('#cat1');populateAbstract('#abstract');">
                </c:if>
                <c:if test="${user == null}">
                    <select width=20 id=projects onChange="alert('Must login before generating template');">
                </c:if>
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
