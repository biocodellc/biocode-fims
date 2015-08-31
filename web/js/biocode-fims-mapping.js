(function(undefined) {
    'use strict';
    // Check if dependecies are available.
    if (typeof XLSXReader === 'undefined') {
        console.log('xlsx-reader.js is required. Get it from https://gist.github.com/psjinx/8002786');
        return;
    }

    if (typeof L === 'undefined') {
        console.log('mapbox.js is required. Get it from https://www.mapbox.com/mapbox.js/api/v2.1.9/');
        return;
    }

    if (typeof L.MarkerClusterGroup === 'undefined') {
        console.log('leaflet.markercluster.js is required. Get it from https://www.mapbox.com/mapbox.js/plugins/#leaflet-markercluster');
        return;
    }

    if (typeof Papa === 'undefined') {
        console.log('papaparser.js is required. Get it from http://papaparse.com/');
        return;
    }

    $.getJSON("rest/utils/getMapboxToken", function(data) {
        L.mapbox.accessToken = data.accessToken;
    }).fail(function() {
        console.log("Failed to retrieve mapbox accessToken. Mapping features will not work.");
    });
}).call(this);

// function to parse the sample coordinates from the spreadsheet
function getSampleCoordinates(configData) {
    try {
        var reader = new FileReader();
    } catch(err) {
        return -1
    }

    // older browsers don't have a FileReader
    if (reader != null) {
        var deferred = new $.Deferred();
        var inputFile= $('#dataset')[0].files[0];

        var splitFileName = $('#dataset').val().split('.');
        if ($.inArray(splitFileName[splitFileName.length - 1], XLSXReader.exts) > -1) {
            XLSXReader(inputFile, true, false, function(reader) {
                // get the data from the sample collection sheet
                var data  = reader.sheets[configData.data_sheet].data;

                // return the geoJSON data
                deferred.resolve(parseGeoJSONData(data, configData.lat_column, configData.long_column));
            })
        } else {
            Papa.parse(inputFile, {
                complete: function(results) {
                    if (!results.meta.aborted)
                        deferred.resolve(parseGeoJSONData(results.data, configData.lat_column, configData.long_column));
                    else
                        deferred.fail();
                }
            });
        }
        return deferred.promise();
    }
    return -1;
}

function parseGeoJSONData(data, lat_column, long_column) {
    var featureTemplate = {
                            "type": "Feature",
                            "geometry": {"type": "Point", "coordinates": []},
                            "properties": {
                                            "description": "",
                                           }
                           };
    var geoJSONData = {"type": "FeatureCollection",
                       "features": []};

    // find the index of the lat and long columns
    var latColumn = data[0].indexOf(lat_column);
    var longColumn = data[0].indexOf(long_column);

    data.forEach(function(element, index, array) {
        // 0 index is the column headers, so skip
        if (index != 0) {
            f = $.extend(true, {}, featureTemplate);

            // only add coordinates if we find them
            if ((element[longColumn] != null) && (element[latColumn] != null) ) {
                // add the coordinates to the feature object
                f.geometry.coordinates.push(element[longColumn]);
                f.geometry.coordinates.push(element[latColumn]);

                var featureIndex = findFeature(f, geoJSONData.features);

                // add feature object to feature collection object only if a feature doesn't already exist
                if (featureIndex == -1) {
                    f.properties.description = "Sample ID: " + element[0] + " (Row " + (index + 1) + ")";
                    geoJSONData.features.push(f);
                } else {
                    geoJSONData.features[featureIndex].properties.description += ", Sample ID: " + element[0] +
                                                                                 " (Row " + (index + 1) + ")";
                }
            }
        }
    });

    return geoJSONData;
}

// function to check if the feature with the same coordinates already exists in the array
function findFeature(feature, featuresArray) {
    var index = -1;
    featuresArray.some(function(element, i) {
        if (element.geometry.coordinates.toString() == feature.geometry.coordinates.toString()) {
            index = i;
            return true;
        }
    });

    return index;
}

var map;

//function to display the map given the div id and the geoJSONData
function displayMap(id, geoJSONData) {
    // create the map
    map = L.mapbox.map(id, 'mapbox.streets');

    // create the data points
    var geoJSONLayer = L.geoJson(geoJSONData, {
        onEachFeature: function (feature, layer) {
            layer.bindPopup(L.mapbox.sanitize(feature.properties.description),{maxHeight: 175});
        }
    });

    // use marker clusters if there are more then 1000 different points
    if (geoJSONData.features.length > 1000) {
        var markers = new L.MarkerClusterGroup()
        markers.addLayer(geoJSONLayer);
        map.addLayer(markers);
    } else {
        map.addLayer(geoJSONLayer);
    }

    // zoom the map to the data points
    map.fitBounds(geoJSONLayer.getBounds());
}

function generateMap(id, project_id) {
    if (map != undefined) {
        map.remove();
        map = undefined;
    }
    $('#' + id).html('Loading map...');
    // generate a map with markers for all sample points
    $.getJSON("rest/utils/getLatLongColumns/" + project_id
        ).done(function(data) {
            getSampleCoordinates(data).done(function(geoJSONData) {
                if (geoJSONData.features.length == 0) {
                    $('#' + id).html('We didn\'t find any lat/long coordinates for your collection samples.');
                } else {
                    displayMap(id, geoJSONData);
                    }
                // remove the refresh map link if there is one
                $("#refresh_map").remove();
            });
        }).fail(function(jqXHR) {
            $('#' + id).html('Failed to load map.');
        });
}
