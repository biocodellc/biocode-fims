<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
   response.setHeader( "Pragma", "no-cache" );
   response.setHeader( "Cache-Control", "no-cache" );
   response.setDateHeader( "Expires", 0 );
%>

<!DOCTYPE HTML>

<html>
    <head>
    <title>NMNH FIMS Template Generator</title>

    <link rel="stylesheet" type="text/css" href="/fims/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/fims/css/alerts.css"/>
    <link rel="stylesheet" type="text/css" href="/fims/css/biscicol.css"/>

    <script>var sessionMaxInactiveInterval = ${pageContext.session.maxInactiveInterval}</script>
    <script type="text/javascript" src="/fims/js/jquery.js"></script>
    <script type="text/javascript" src="/fims/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/fims/js/jquery.form.js"></script>
    <script type="text/javascript" src="/fims/js/templates.js"></script>
    <script type="text/javascript" src="/fims/js/BrowserDetect.js"></script>

    <script type="text/javascript" src="/fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/fims/js/bootstrap.min.js"></script>

    <link rel="short icon" href="/fims/docs/images/fimsicon.png" />

</head>

<body onload="populateProjects();">

<%@ include file="header-menus.jsp" %>
