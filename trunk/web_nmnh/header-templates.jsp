<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
    <head>
    <title>Biocode FIMS Template Generator</title>

    <link rel="stylesheet" type="text/css" href="/fims/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/fims/css/biscicol.css"/>
    <link rel="stylesheet" type="text/css" href="/fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/fims/css/alerts.css"/>

    <script type="text/javascript" src="/fims/js/jquery.js"></script>
    <script type="text/javascript" src="/fims/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/fims/js/jquery.form.js"></script>
    <script type="text/javascript" src="/fims/js/dropit.js"></script>
    <script type="text/javascript" src="/fims/js/templates.js"></script>

    <script>$(document).ready(function() {$('.menu').dropit();});</script>

    <script type="text/javascript" src="/fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/fims/js/bootstrap.min.js"></script>

</head>

<body onload="populateProjects();">

<%@ include file="header-menus.jsp" %>
