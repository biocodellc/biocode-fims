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

        // Only try to use zip functions if this an XLSX file
        if (getExtension(inputFile.name) == "xlsx") {
            zipmodel.getEntries(inputFile, function(entries) {
                entries.forEach(function(entry) {
                        entry.getData(new zip.TextWriter(), function(text) {
                                // text contains the entry data as a String
                                var results = text.match(regExpression);
                                if (!!results) {
                                        var myResult = results.toString().split('=')[1].slice(0, -1);
                                        deferred.resolve(myResult);
                                }
                        });
                });
            });
        }
        // All other cases besides XLSX use plain file reader
        else {
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
        }

        return deferred.promise();
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

// Return file extension
function getExtension(filename) {
    var a = filename.split(".");
    if( a.length === 1 || ( a[0] === "" && a.length === 2 ) ) {
        return "";
    }
    return a.pop();
}
