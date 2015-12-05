<%@ include file="../header-menus.jsp" %>

<div id="user" class="section">

    <div class="sectioncontent">
        <h2>User Profile (${user})</h2>

        <br>

        <div>
            <div id=listUserProfile>Loading profile...</div>
        </div>
    </div>
</div>

<script>
    $(document).ready(function() {
        // Populate User Profile
        if ("${param.error}" == "") {
            var jqxhr = populateDivFromService(
                "/id/userService/profile/listAsTable",
                "listUserProfile",
                "Unable to load this user's profile from the Server")
                .done(function() {
                    $("a", "#profile").click( function() {
                        getProfileEditor();
                    });
                });
            loadingDialog(jqxhr);
        } else {
            getProfileEditor();
        }
    });
</script>

<%@ include file="../footer.jsp" %>