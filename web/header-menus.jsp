
<div id="container">

    <div id="header">

        <div style='float:left'><h2 style="margin:0px;">Field Information Management System</h2></div>

        <div style='float:right' id="loginLink">
            <c:if test="${user == null}">
                <a id="login" href="rest/authenticationService/login">Login</a>
            </c:if>
            <c:if test="${user != null}">
                <a href="/bcid/secure/profile.jsp">${user}</a> | <a id="logout" href="/biocode-fims/rest/authenticationService/logout/">Logout</a>
            </c:if>
            | <a href="https://code.google.com/p/biocode-fims/">Help</a>
        </div>

        <div style="clear: both;"></div>

        <div style="overflow: auto;width: 100%;">
            <div class="link"><a href='/biocode-fims/index.jsp'>Home</a></div>

            <div class="separator">|</div>

            <ul id="menu2" class="menu">
                <li><a href="#" class="btn">Tools</a>

                    <c:if test="${user != null}">
                        <ul>
                            <li><a href='/biocode-fims/query.jsp' class='enabled'>Query</a></li>
                            <li><a href='/biocode-fims/templates.jsp' class='enabled'>Generate Template</a></li>
                            <li><a href='http://biscicol.org/bcid' class='enabled'>Manage Projects (BCID)</a></li>
                        </ul>
                    </c:if>

                    <c:if test="${user == null}">
                        <ul>
                            <li><a href='/biocode-fims/index.jsp' class='enabled'>Validation</a></li>
                            <li><a href='/biocode-fims/templates.jsp' class='enabled'>Generate Template</a></li>
                            <li><a href='/biocode-fims/query.jsp' class='enabled'>Query</a></li>
                            <li><a href='http://biscicol.org/bcid' class='enabled'>Manage Projects (BCID)</a></li>
                         </ul>
                    </c:if>
                </li>
            </ul>
        </div>
    </div>

<div class="alert-container"><div id="alerts"></div></div>
