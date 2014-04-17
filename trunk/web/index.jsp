<%@ include file="header-home.jsp" %>

<div id="validation" class="section">
    <div class="sectioncontent">

        <h2>Validation</h2>

        <form>
            <table border=0>
            <tr><td colspan=2>Online Validation Services not yet implemented, coming by May, 2014.  <br>Meanwhile, you can use the <a href="https://code.google.com/p/biocode-fims/wiki/GeneiousPluginInstallation" target="_blank">Biocode FIMS Geneious Plugin</a></td></tr>
            <tr>
                <td align=right>FIMS Spreadsheet</td>
                <td align=left>
                    <input
                        type=file
                        name="spreadsheet"
                        id="spreadsheet"
                        size="40"/>
                </td>
            </tr>
            <tr>
                <td align=right style="color:gray">Upload</td>
                <td align=left>
                    <input
                        type="checkbox"
                        name="upload"
                        value="upload" disabled/>
                </td>
            </tr>
            <tr>
                <td colspan=2>
                    <input
                        type="button"
                        onclick="runIt();"
                        name="Submit"
                        value="Submit" />
                </td>
            </tr>
            </table>
        </form>

        <p>
        <h2>Biocode FIMS Workflow</h2>
        Biocode FIMS is a field information management system that enables data collection at the source (in the field),
                validates data, and assigns persistent identifiers by Project, Dataset, and locally unique identifiers
                (see <a href="http://biscicol.org/bcid" target="_blank">BCID system</a>).
                <p>
        <img src='docs/Workflow.jpeg'>
    </div>
</div>

<script>
function runIt() {
    alert("yet to implement this part");
}
</script>

<%@ include file="footer.jsp" %>
