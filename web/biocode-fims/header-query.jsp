<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML>

<html>
    <head>
    <title>Biocode FIMS Query</title>

    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/alerts.css"/>
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/biscicol.css"/>

    <script type="text/javascript" src="/biocode-fims/js/jquery.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/jquery.form.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/BrowserDetect.js"></script>

    <script src="/biocode-fims/js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };
    </script>

    <script type="text/javascript" src="/biocode-fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/bootstrap.min.js"></script>

    <link rel="short icon" href="/biocode-fims/docs/fimsicon.png" />


</head>

<body>

<%@ include file="header-menus.jsp" %>
