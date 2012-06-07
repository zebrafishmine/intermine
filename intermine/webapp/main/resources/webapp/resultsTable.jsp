<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<html:xhtml/>

<%-- Required for displaying the contents of invalid bags --%>
<tiles:importAttribute name="invalid" ignore="true"/>
<tiles:importAttribute name="bag" ignore="true"/>
<tiles:importAttribute name="cssClass" ignore="true"/>
<tiles:importAttribute name="pageSize" ignore="true"/>

<c:if test="${empty pageSize}">
    <c:set var="pageSize" value="25"/>
</c:if>
  

<c:set var="initValue" value="0"/>

<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>

<c:set var="tableContainerId" value="_unique_id_${currentUniqueId}" scope="request"/>

<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>

<script>
(function() {
    intermine.css.headerIcon = "fm-header-icon";
    <c:choose>
        <c:when test="${not empty QUERY.json}">
            var query = ${QUERY.json};
        </c:when>
        <c:otherwise>
            var query = {}; // QUERY.json is empty
        </c:otherwise>
    </c:choose>
    var service = new intermine.Service({
        "root": "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}",
        "token": "${PROFILE.dayToken}"
    });

    if (query && query.select.length > 0) {
        jQuery(function() {
            var view = new intermine.query.results.CompactView(service, query, {}, {pageSize: ${pageSize}});
            view.$el.appendTo('#${tableContainerId}');
            view.render();
        });
    } else {
        jQuery('#${tableContainerId}').html('<p>Query has not been specified, failing...</p>');
    }
})();
</script>

<div id="${tableContainerId}" class="${cssClass}"></div>
