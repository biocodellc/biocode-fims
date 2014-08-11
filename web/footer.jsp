    <div class="clearfooter"></div>
</div> <!—-End Container—>

<div id="footer">
    <div>
    <a href="https://code.google.com/p/bcid/">Biocode FIMS</a> is supported by the NSF-funded
        <a href="http://biscicol.blogspot.com/">BiSciCol project</a> and
        <a href="http://www.nescent.org/">NESCENT</a>, in collaboration with the
        <a href="http://www.barcodeofwildlife.org/">Barcode of Wildlife Project</a> and
        <a href="http://bnhm.berkeley.edu/">Berkeley Natural History Museums</a>, with initial support from the
        <a href="http://www.moore.org/">Gordon and Betty Moore Foundation</a>
    </div>
</div>


<script>
$(document).ready(function() {
   if (BrowserDetect.browser = "Explorer" &&
        BrowserDetect.version <=9) {
     alert("FIMS does not fully support your browser, please try IE 11, or a recent version of Firefox, Chrome, or Safari");
     $('#warning').html("<b>NOTE:</b>Your browser may not be supported by this FIMS");
   }
});
</script>
</body>

</html>