<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
    <head>
    <title>Biocode FIMS Template Generator</title>

    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/biscicol.css"/>
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/alerts.css"/>

    <script type="text/javascript" src="/biocode-fims/js/jquery.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/templates.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/BrowserDetect.js"></script>

    <script type="text/javascript" src="/biocode-fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/bootstrap.min.js"></script>

</head>

<body onload="populateProjects();">

<%@ include file="header-menus.jsp" %>
