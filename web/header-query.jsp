<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html>
    <head>
    <title>FIMS Spreadsheet Customization</title>

    <link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/flick/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/biscicol.css"/>

    <script src="http://code.jquery.com/jquery-1.11.0.min.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/dropit.js"></script>

    <script>$(document).ready(function() {$('.menu').dropit();});</script>

    <script src="js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };

    </script>
    <script src="js/biocode-fims.js"></script>
    <script src="js/jNotify.jquery.js"></script>
</head>

<body onload="graphsMessage('Choose an project to see loaded spreadsheets');populateProjects();">

<%@ include file="header-menus.jsp" %>
