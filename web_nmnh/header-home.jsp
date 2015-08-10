<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
   response.setHeader( "Pragma", "no-cache" );
   response.setHeader( "Cache-Control", "no-Store,no-Cache" );
   response.setDateHeader( "Expires", 0 );
%>

<html>
<head>
    <title>NMNH FIMS</title>

    <link rel="stylesheet" type="text/css" href="/fims/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/fims/css/alerts.css"/>
    <link rel="stylesheet" type="text/css" href="/fims/css/biscicol.css"/>

    <script type="text/javascript" src="/fims/js/jquery.js"></script>
    <script type="text/javascript" src="/fims/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/fims/js/jquery.form.js"></script>
    <script type="text/javascript" src="/fims/js/BrowserDetect.js"></script>

    <script>var sessionMaxInactiveInterval = ${pageContext.session.maxInactiveInterval}</script>
    <script type="text/javascript" src="/fims/js/lodash.js"></script>
    <script type="text/javascript" src="/fims/js/xlsx.js"></script>
    <script type="text/javascript" src="/fims/js/biocode-fims-xlsx-reader.js"></script>
    <script type="text/javascript" src="/fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/fims/js/bootstrap.min.js"></script>

    <link rel="short icon" href="/fims/docs/images/fimsicon.png" />
</head>

<body>
<%@ include file="header-menus.jsp" %>
