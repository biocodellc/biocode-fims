<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
    <title>Biocode FIMS</title>
    <link rel="stylesheet" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/flick/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="css/biscicol.css"/>
    <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script src="js/jquery.form.js"></script>
    <script src="js/distal.js"></script>
        <script>
            jQuery.fn.distal = function (json) {
                return this.each( function () { distal(this, json) } )
            };
        </script>
    <script type="text/javascript" src="js/biocode-fims.js"></script>
</head>

<body>

<div id="container">

    <div id="header">

        <div style='float:left'><h1>Biocode FIMS</h1></div>

        <div style='float:right' id="loginLink">
            <c:if test="${user == null}">
                <a id="login" href="rest/authenticationService/login">Login</a>
            </c:if>
            <c:if test="${user != null}">
                <a href="/bcid/secure/profile.jsp">${user}</a> | <a id="logout" href="/biocode-fims/rest/authenticationService/logout/">Logout</a>
            </c:if>
            <!--| <div class="link"><a href='/bcid/concepts.jsp'>Concepts</a></div>-->
            | <a href="https://code.google.com/p/biocode-fims/">Help</a>
        </div>

        <div style="clear: both;"></div>

        <div style="overflow: auto;width: 100%;">
            <div class="link"><a href='/biocode-fims/uploader.jsp'>Uploader</a></div>
        </div>
    </div>
