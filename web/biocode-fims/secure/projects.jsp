<%@ include file="../header-home.jsp" %>

<div class="section">
    <div class="sectioncontent">Loading Projects...</div>
</div>
<div id="confirm">Are you sure you wish to remove {user}?</div>

<script>
    $(document).ready(function() {
        populateProjectPage("${user}");
    });
</script>

<%@ include file="../footer.jsp" %>