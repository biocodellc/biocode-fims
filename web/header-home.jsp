<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML>

<html>
<head>
    <title>Biocode Field Information Management System</title>

    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/alerts.css"/>
    <link rel="stylesheet" type="text/css" href="/biocode-fims/css/biscicol.css"/>

    <script type="text/javascript" src="/biocode-fims/js/jquery.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/jquery.form.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/BrowserDetect.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/lodash.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/xlsx.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/papaparse.min.js"></script>

    <script src='https://api.tiles.mapbox.com/mapbox.js/v2.1.9/mapbox.js'></script>
    <link href='https://api.tiles.mapbox.com/mapbox.js/v2.1.9/mapbox.css' rel='stylesheet' />

    <script src='https://api.tiles.mapbox.com/mapbox.js/plugins/leaflet-markercluster/v0.4.0/leaflet.markercluster.js'></script>
    <link href='https://api.tiles.mapbox.com/mapbox.js/plugins/leaflet-markercluster/v0.4.0/MarkerCluster.css' rel='stylesheet' />
    <link href='https://api.tiles.mapbox.com/mapbox.js/plugins/leaflet-markercluster/v0.4.0/MarkerCluster.Default.css' rel='stylesheet' />

    <script type="text/javascript" src="/biocode-fims/js/biocode-fims-xlsx-reader.js"></script>
    <script type-"text/javascript" src="/biocode-fims/js/biocode-fims-mapping.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/biocode-fims.js"></script>
    <script type="text/javascript" src="/biocode-fims/js/bootstrap.min.js"></script>

    <link rel="short icon" href="/biocode-fims/docs/fimsicon.png" />

</head>


<body>
<%@ include file="header-menus.jsp" %>
