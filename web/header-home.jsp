<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
<head>
    <title>Biocode Field Information Management System</title>

    <link rel="stylesheet" href="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/flick/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/biscicol.css"/>
    <link href="css/bootstrap.min.css" rel="stylesheet">
    <link href="css/alerts.css" rel="stylesheet"/>

    <script src="http://code.jquery.com/jquery-1.11.0.min.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/dropit.js"></script>
    <script src="js/jquery.form.js"></script>

    <script src="js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };
    </script>

    <script type="text/javascript" src="js/biocode-fims.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/jquery-ui.min.js"></script>
    <script type='text/javascript' src="js/bootstrap.min.js"></script>

    <script>$(document).ready(function() {$('.menu').dropit();});</script>

</head>

<body>
<%@ include file="header-menus.jsp" %>
