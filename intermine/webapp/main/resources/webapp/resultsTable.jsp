<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<html:xhtml/>

<tiles:importAttribute name="invalid" ignore="true"/>
<tiles:importAttribute name="bag" ignore="true"/>
<tiles:importAttribute name="cssClass" ignore="true"/>
<tiles:importAttribute name="pageSize" ignore="true"/>
<tiles:importAttribute name="query" ignore="true"/>

<c:if test="${empty query}">
    <c:set var="query" value="${QUERY}"/>
</c:if>

<c:if test="${empty pageSize}">
    <c:set var="pageSize" value="25"/>
</c:if>
  

<c:set var="initValue" value="0"/>

<c:if test="${empty currentUniqueId}">
    <c:set var="currentUniqueId" value="${initValue}" scope="application"/>
</c:if>

<c:set var="tableContainerId" value="_unique_id_${currentUniqueId}" scope="request"/>

<c:set var="currentUniqueId" value="${currentUniqueId + 1}" scope="application"/>

<c:if test="${! empty query.title}">
    <c:set var="templateQuery" value="${query}"/>
    <tiles:insert template="templateTitle.jsp"/>
</c:if>

<c:choose>
    <c:when test="${not empty query.json}">
        <c:set var="queryJson" value="${query.json}"/>
    </c:when>
    <c:otherwise>
        <c:set var="queryJson" value="{}"/>
    </c:otherwise>
</c:choose>

<div id="${tableContainerId}" class="${cssClass}"></div>

<script type="text/javascript">
jQuery(function() {
    intermine.css.headerIcon = "fm-header-icon";
    var opts = {
        type: 'table',
        url: "${WEB_PROPERTIES['webapp.baseurl']}/${WEB_PROPERTIES['webapp.path']}",
        token: "${PROFILE.dayToken}",
        error: FailureNotification.notify,
        query: ${queryJson},
        events: LIST_EVENTS,
        properties: { pageSize: ${pageSize} }
    };
    jQuery('#${tableContainerId}').imWidget(opts);
});
</script>


