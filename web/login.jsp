<%@ include file="header.jsp" %>
<div class="section">
    <div class="sectioncontent" id="login">
        <h2>BCID Login</h2>

        <c:if test="${pageContext.request.getQueryString() != null}">
        <form method="POST" action="/rest/authenticationService/login?${pageContext.request.getQueryString()}">
        </c:if>
        <c:if test="${pageContext.request.getQueryString() == null}">
        <form method="POST" action="/rest/authenticationService/login/">
        </c:if>
            <table>
                <tr>
                    <td align="right">Username</td>
                    <td><input type="text" name="username" autofocus></td>
                </tr>
                <tr>
                    <td align="right">Password</td>
                    <td><input type="password" name="password"></td>
                </tr>
                <tr>
                    <td></td>
                    <td align="center"><a href="/bcid/reset.jsp">(forgot password)</a></td>
                </tr>
                <c:if test="${param['error'] != null}">
                <tr></tr>
                <tr>
                    <td></td>
                    <td class="error" align="center">Bad Credentials</td>
                </tr>
                </c:if>
                <tr>
                    <td></td>
                    <td ><input type="submit" value="Submit"></td>
                </tr>
            </table>
        </form>

    </div>
</div>

<%@ include file="footer.jsp" %>