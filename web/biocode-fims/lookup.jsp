<%@ include file="header-home.jsp" %>

<div id="resolver" class="section">
    <div class="sectioncontent row">

        <h1>Lookup</h1>

        <form>
            <table border=0>
            <tr>
                <td>Identifier</td>
                <td>
                    <input
                        type=text
                        name="identifier"
                        id="identifier"
                        placeholder="ark:/21547/R2MBIO56"
                        size="40"
                        onkeypress="if(event.keyCode==13) {resolverResults(); return false;}" />
                </td>
                <td>
                    <input
                        type="button"
                        onclick="resolverResults();"
                        name="Submit"
                        value="Submit" />
                </td>
            </tr>
            </table>
        </form>

    </div>
    <div class="sectioncontent row" id="results"></div>
</div>

<script>
    /* parse input parameter -- ARKS must be minimum length of 12 characters*/
    var a = '<%=request.getParameter("id")%>';
    if (a.length > 12) {
        $("#identifier").val(a);
        resolverResults();
    }
</script>
<%@ include file="footer.jsp" %>
