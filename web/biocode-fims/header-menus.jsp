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
                <a class="navbar-brand" href="/biocode-fims/index.jsp">Biocode Field Information Management System</a>
            </div>

            <!-- Collect the nav links, forms, and other content for toggling -->
            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav navbar-right">
                    <li class="dropdown">
                        <a href="#" data-toggle="dropdown" class="dropdown-toggle">Tools<b class="caret"></b></a>
                        <ul class="dropdown-menu">
                        <c:if test="${user != null}">
                            <li><a href='/biocode-fims/templates.jsp' class='enabled'>Generate Template</a></li>
                            <li><a href='/biocode-fims/validation.jsp' class='enabled'>Validation</a></li>
                            <li><a href='/biocode-fims/query.jsp' class='enabled'>Query</a></li>
                            <li><a href='/biocode-fims/secure/profile.jsp' class='enabled'>User Profile</a></li>
                        </c:if>

                        <c:if test="${user == null}">
                            <li><a href='/biocode-fims/templates.jsp' class='enabled'>Generate Template</a></li>
                            <li><a href='/biocode-fims/validation.jsp' class='enabled'>Validation</a></li>
                            <li><a href='/biocode-fims/query.jsp' class='enabled'>Query</a></li>
                            <li><a href='#' class='disabled'>User Profile</a></li>
                        </c:if>
                        </ul>
                    </li>

                    <c:if test="${projectAdmin != null}">
                        <li class="dropdown">
                            <a href="#" data-toggle="dropdown" class="dropdown-toggle">Admin<b class="caret"></b></a>

                            <ul class="dropdown-menu">
                                <li><a href='/biocode-fims/secure/projects.jsp' class='enabled'>Manage Projects</a></li>
                            </ul>
                        </li>
                    </c:if>

                    <c:if test="${user == null}">
                            <li><a id="login" href="login.jsp">Login</a></li>
                    </c:if>
                    <c:if test="${user != null}">
                            <li><a href="/biocode-fims/secure/profile.jsp">${user}</a></li>
                            <li><a id="logout" href="/id/authenticationService/logout/">Logout</a></li>
                    </c:if>
                    <li><a href="https://github.com/biocodellc/biocode-fims/wiki/WebVersion">Help</a></li>
                </ul>
            </div><!-- /.navbar-collapse -->
        </div>
    </nav>
<div class="alert-container"><div id="alerts"></div></div>
