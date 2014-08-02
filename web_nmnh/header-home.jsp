<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
    <title>Smithsonian National Museum of Natural History Field Information Management System</title>

    <link rel="stylesheet" type="text/css" href="/fims/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/fims/css/biscicol.css"/>
    <link rel="stylesheet" type="text/css" href="/fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/fims/css/alerts.css"/>

    <script type="text/javascript" src="/fims/js/jquery.js"></script>
    <script type="text/javascript" src="/fims/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/fims/js/jquery.form.js"></script>
    <script type="text/javascript" src="/fims/js/dropit.js"></script>

    <script src="/fims/js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };
    </script>

    <script type="text/javascript" src="/fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/fims/js/bootstrap.min.js"></script>

    <script>$(document).ready(function() {$('.menu').dropit();});</script>




</head>

<body>
<%@ include file="header-menus.jsp" %>
