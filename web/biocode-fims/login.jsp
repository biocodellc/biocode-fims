<%@ include file="header-home.jsp" %>

<div class="section">
    <div class="sectioncontent" id="login">
        <h2>Login</h2>

        <form method="POST" autocomplete="off">
            <table>
                <tr>
                    <td align="right">Username</td>
                    <td><input type="text" name="username" autofocus></td>
                </tr>
                <tr>
                    <td align="right">Password</td>
                    <td><input type="password" name="password" autocomplete="off" onkeypress="if(event.keyCode==13) {login();}"></td>
                </tr>
                <tr>
                    <td></td>
                    <td align="center"><a href="reset.jsp">(forgot password)</a></td>
                </tr>
                <tr></tr>
                <tr>
                    <td></td>
                    <td class="error" align="center"></td>
                </tr>
                <tr>
                    <td></td>
                    <td ><input type="button" value="Submit" onclick="login();"></td>
                </tr>
            </table>
        </form>

    </div>
</div>

<%@ include file="footer.jsp" %>
