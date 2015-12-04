<%@ include file="header-home.jsp" %>

div class="section">
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
                    <td><input type="password" name="password" autocomplete="off"></td>
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
    <p>
    For assistance with your account login or challenge questions call (202) 633-4000 or
    visit <a href="http://myid.si.edu/">http://myid.si.edu/</a>
    </div>
</div>

<%@ include file="footer.jsp" %>
