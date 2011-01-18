<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil"%>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1"
  prefix="str"%>


<tiles:importAttribute />

<link rel="stylesheet" href="model/css/projects.css" type="text/css" media="screen" title="no title" charset="utf-8">

<html:xhtml />

<table cellpadding="0" cellspacing="0" border="0" class="topBar hints" width="95%">
<tr><td align="right"><a href="/${WEB_PROPERTIES['webapp.path']}/projects.do?">Switch to Projects View</a></td></tr>
</table>

<div class="body">

<div align="center">

<div style="width: 90%; text-align: left">
<p>Data generated by modENCODE is grouped by category based on the genomic or biological element tested.
Each category contains sets of experiments, with each set varying by the assay method, submitting group, and/or experimental condition tested.
An individual 'submission' is a single instance of an experiment which tests varying factors, such as life stage, genetic background, or antibody, and consists of a package of results, experimental protocols and other metadata.</p>
</div>
<br/>
<table cellpadding="0" cellspacing="0" border="0" class="projects" id="projects">
<tr><a name="index">
<c:forEach items="${catExp}" var="catInd" varStatus="catInd_status">
<td><a href="/${WEB_PROPERTIES['webapp.path']}/projectsSummary.do#${catInd_status.count}"  title="Go to category: ${catInd.key}" >${catInd.key}
<img src="images/right-arrow.gif" /></a></td>
</c:forEach>
</a></tr>
</table>

<table cellpadding="0" cellspacing="0" border="0" class="projects" id="projects">
<c:forEach items="${catExp}" var="cat" varStatus="cat_status">
  <tr><th>&nbsp;</th><th><h4><a name="${cat_status.count}">${cat.key}</a></h4></th><th class="lastcol">&nbsp;</th></tr>

 <c:forEach items="${cat.value}" var="exp"  varStatus="status">
<c:set var="expCount" value="${fn:length(proj.value)}"></c:set>

  <tr>

  <td >
    <c:forEach items="${exp.organisms}" var="organism" varStatus="orgStatus">
      <c:if test="${organism eq 'D. melanogaster'}">
        <img border="0" class="arrow" src="model/images/f_vvs.png" title="fly"/><br>
      </c:if>
      <c:if test="${organism eq 'C. elegans'}">
        <img border="0" class="arrow" src="model/images/w_vvs.png" title="worm"/><br>
      </c:if>
    </c:forEach>
  </td>

  <td><h4><html:link href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${exp.name}">${exp.name}</html:link></h4>

<%-- LABS Note: linking with surname only, 2 Green and Kim--%>
Project:${exp.projectName } &nbsp;&nbsp;(${exp.pi })&nbsp;&nbsp;
Labs:
    <c:forEach items="${exp.labs}" var="lab" varStatus="labStatus"><c:if test="${!labStatus.first}">, </c:if><b>${lab}</b></c:forEach>

<br>
<%-- SUBMISSIONS --%>
  <c:choose>
    <c:when test="${exp.submissionCount == 0}">
      This experiment has <b>no data submissions yet</b>.
    </c:when>
    <c:when test="${exp.submissionCount == 1}">
      This experiment has <b><c:out value="${exp.submissionCount}"></c:out> data submission</b>.
    </c:when>
    <c:otherwise>
      This experiment has <b><c:out value="${exp.submissionCount}"></c:out> data submissions</b>.
    </c:otherwise>
  </c:choose>

<%-- REPOSITORY ENTRIES --%>
    <c:if test="${exp.repositedCount == 1}">
       It has produced <b>${exp.repositedCount} entry in public repositories</b>.
    </c:if>
    <c:if test="${exp.repositedCount > 1}">
       It has produced <b>${exp.repositedCount} entries in public repositories</b>.
    </c:if>



<%-- EXPERIMENTAL FACTORS --%>
     <c:if test="${fn:length(exp.factorTypes) > 0 }">
       <c:choose>
         <c:when test="${ fn:length(exp.factorTypes) == 1}">
           <c:out value="The experimental factor is"/>
         </c:when>
         <c:otherwise>
           <c:out value="The experimental factors are"/>
         </c:otherwise>
       </c:choose>
       <c:forEach items="${exp.factorTypes}" var="ft" varStatus="ft_status"><c:if test="${ft_status.count > 1 && !ft_status.last }">, </c:if><c:if test="${ft_status.count > 1 && ft_status.last }"> and </c:if><b>${ft}</b></c:forEach>.
     </c:if>
</td>

<td>
<%-- FEATURES --%>
      <c:forEach items="${exp.featureCountsRecords}" var="fc" varStatus="fc_status">
     <c:if test="${fc_status.count > 1 }"><br> </c:if>

     <%-- TEMP patch until data is corrected. it should be (otherwise) --%>
     <c:choose>
     <c:when test="${exp.name == 'Genome-wide localization of essential replication initiators'
  && fc.featureType == 'ProteinBindingSite'}">
  ${fc.featureType}:&nbsp;38114
     </c:when>
     <c:otherwise>
      ${fc.featureType}:&nbsp;${fc.featureCounts}
     </c:otherwise>
     </c:choose>
<%-- END --%>

<%-- too crowded: rm here, still available in the experiment page
      <c:if test="${!empty fc.uniqueFeatureCounts && fc.uniqueFeatureCounts != fc.featureCounts}">
      <br>
      <i style="font-size:0.9em;">
      (${fc.uniqueFeatureCounts} unique ${fc.featureType}s)
      </i>
      </c:if>

--%>
     <c:if test="${fc_status.last }"><br> </c:if>
      </c:forEach>

    <c:if test="${exp.expressionLevelCount > 0}">
       with ${exp.expressionLevelCount} expression levels.
    </c:if>

<p/>
<%-- TRACKS --%>
     <c:if test="${!empty tracks[exp.name]}">
       <c:choose>
         <c:when test="${fn:length(tracks[exp.name]) == 1}">
           <c:out value="${fn:length(tracks[exp.name])}"/> GBrowse track
         </c:when>
         <c:otherwise>
           <c:out value="${fn:length(tracks[exp.name])}"/> GBrowse tracks
         </c:otherwise>
       </c:choose>
<br></br>
     </c:if>
<%-- REPOSITORY ENTRIES --%>
     <c:if test="${exp.repositedCount > 0}">

      <c:forEach items="${exp.reposited}" var="rep" varStatus="rep_status">
      ${rep.value}
      <c:choose>
        <c:when test="${rep.value == 1}">
         entry
        </c:when>
        <c:otherwise>
        entries
        </c:otherwise>
      </c:choose>
      in ${rep.key}
      <br></br>
      </c:forEach>
     </c:if>

<%-- GET DATA --%>
<html:link
        href="/${WEB_PROPERTIES['webapp.path']}/experiment.do?experiment=${exp.name}">
        <img src="model/images/get_data_button.png" alt="Get Data" style="align:middle">
        </html:link>

</td>


</tr>
</c:forEach>
</c:forEach>
</table>

</div>


<div align="center">

</div>

<table cellpadding="0" cellspacing="0" border="0" class="topBar hints" width="95%">
<tr><td align="right"><a href="/${WEB_PROPERTIES['webapp.path']}/projects.do?">Switch to Projects View</a></td></tr>
</table>


<!-- links to all subs -->
<table cellspacing="4"><tr>

<td>
<im:querylink text="Fly" showArrow="true" skipBuilder="true">
 <query name="" model="genomic"
   view="Submission.title Submission.DCCid Submission.experimentType "
   sortOrder="Submission.experimentType asc">
  <constraint path="Submission.organism.shortName" op="=" value="D. melanogaster"/>
</query>

</im:querylink>
    </td>

<td>
<im:querylink text="Worm" showArrow="true" skipBuilder="true">
 <query name="" model="genomic"
   view="Submission.title Submission.DCCid Submission.experimentType "
   sortOrder="Submission.experimentType asc">
  <constraint path="Submission.organism.shortName" op="=" value="C. elegans"/>
</query>
</im:querylink>
</td>

<td>
<im:querylink text="All submissions" showArrow="true" skipBuilder="true">
 <query name="" model="genomic"
   view="Submission.title Submission.DCCid Submission.experimentType "
   sortOrder="Submission.experimentType asc">
</query>
</im:querylink>
    </td>
  </tr></table>

