function parseSpreadsheet(regExpression) {
    try {
        f = new FileReader();
    } catch(err) {
        return -1;
    }
    // older browsers don't have a FileReader
    if (f != null) {
        var deferred = new $.Deferred();
        var inputFile= $('#dataset')[0].files[0];
        zipmodel.getEntries(inputFile, function(entries) {
                entries.forEach(function(entry) {
                        entry.getData(new zip.TextWriter(), function(text) {
                                // text contains the entry data as a String
                                //var re = "~" + paramToParse + "=[0-9]+~";
                                var results = text.match(regExpression);
                                if (!!results) {
                                        var myResult = results.toString().split('=')[1].slice(0, -1);
                                        deferred.resolve(myResult);
                                        return deferred.promise();
                                }
                        });
                });
        });

        // If we get to this point we are not a zip archive
                                                           /*
            f.onload = function () {
                var fileContents = f.result;
                try {
                    var results = fileContents.match(regExpression)[0];
                    if (!!results) {
                        var myResult = results.split('=')[1].slice(0, -1);
                        deferred.resolve(myResult);
                    } else {
                        deferred.resolve(-1);
                    }
                } catch (e) {
                    deferred.resolve(-1);
                }
            };
            f.readAsText(inputFile);
            return deferred.promise();
                  */
    }
    return -1;

}

var zipmodel = (function() {
        return {
                getEntries : function(file, onend) {
                        zip.createReader(new zip.BlobReader(file), function(zipReader) {
                                zipReader.getEntries(onend);
                        }, onerror);
                }
        };
})();
