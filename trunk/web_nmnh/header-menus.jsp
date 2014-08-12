<div id="container">
    <nav id="myNavbar" class="navbar navbar-default" role="navigation">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div>
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="/biocode-fims/index.jsp">NMNH Field Information Management System (FIMS)</a>
            </div>

            <!-- Collect the nav links, forms, and other content for toggling -->
            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav navbar-right">
                    <li class="dropdown">
                        <a href="#" data-toggle="dropdown" class="dropdown-toggle">Tools<b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <c:if test="${user != null}">
                            <ul>
                                <li><a href='/fims/templates.jsp' class='enabled'>Generate Template</a></li>
                                <li><a href='/fims/validation.jsp' class='enabled'>Validation</a></li>
                            </ul>
                        </c:if>

                        <c:if test="${user == null}">
                            <ul>
                                <li><a href='/fims/templates.jsp' class='enabled'>Generate Template</a></li>
                                <li><a href='/fims/validation.jsp' class='enabled'>Validation</a></li>
                            </ul>
                            </c:if>
                        </ul>
                    </li>
                    <c:if test="${user == null}">
                        <li><a id="login" href="rest/authenticationService/login">Login</a></li>
                    </c:if>
                    <c:if test="${user != null}">
                        <li><a href="/bcid/secure/profile.jsp">${user}</a></li>
                        <li><a id="logout" href="/biocode-fims/rest/authenticationService/logout/">Logout</a></li>
                    </c:if>
                    <li><a href="https://code.google.com/p/biocode-fims/">Help</a></li>
                </ul>
            </div><!-- /.navbar-collapse -->
        </div>
    </nav>
<div class="alert-container"><div id="alerts"></div></div>