<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
    <title>Biocode Field Information Management System</title>

    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/biscicol.css"/>
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/alerts.css"/>

    <script type="text/javascript" src="/biocode-fims/js/jquery.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/dropit.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/jquery.form.js"></script>

    <script src="/biocode-fims/js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };
    </script>

    <script type="text/javascript" src="/biocode-fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/jquery-ui.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/bootstrap.min.js"></script>

    <script>$(document).ready(function() {$('.menu').dropit();});</script>

</head>

<body>
<%@ include file="header-menus.jsp" %>
