<%@ include file="header-home.jsp" %>
<div class="section">
    <div class="sectioncontent" id="pass_reset">
        <h2>BCID Password Reset</h2>

        <form method="POST">
            <table>
                <tr>
                    <td align="right">Username</td>
                    <td><input type="text" name="username"></td>
                </tr>
                <tr>
                    <td></td>
                    <td class="error" align="center"></td>
                </tr>
                <tr>
                    <td></td>
                    <td><input type="button" value="Submit" onclick="resetPassSubmit();"></td>
                </tr>
            </table>
        </form>

    </div>
</div>

<%@ include file="footer.jsp" %>