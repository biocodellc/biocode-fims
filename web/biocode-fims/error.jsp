<%@ include file="header-home.jsp" %>
<div class="section">
    <div class="sectioncontent">
        ${errorInfo.toHTMLTable()}
        <c:remove var="errorInfo" scope="session" />
    </div>
</div>

<script>
</script>
<%@ include file="footer.jsp" %>