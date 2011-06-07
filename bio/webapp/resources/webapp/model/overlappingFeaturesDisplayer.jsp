<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- overlappingFeaturesDisplayer.jsp -->

<div class="custom-displayer" id="overlapping-features">
  <h3>Overlapping Features</h3>
  <p class="desc">
    <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
    Genome features that overlap coordinates of this ${reportObject.type}
  </p>
  <p class="switchers">
    <c:forEach items="${featureCounts}" var="entry" varStatus="status"><c:if test="${status.count > 1}">, </c:if>
    <%-- TODO: potential fail if key has spaces --%>
    <a href="#" id="${fn:toLowerCase(entry.key)}" class="switcher">${entry.key}</a>: ${entry.value}</c:forEach>
  </p>

  <c:if test="${!empty featureTables}">
    <c:forEach items="${featureTables}" var="entry">
      <div class="table" id="${fn:toLowerCase(entry.key)}" style="display:none;">
        <h3>${entry.key}</h3>
        <div class="clear"></div>

        <c:set var="inlineResultsTable" value="${entry.value}" />
        <tiles:insert page="/reportCollectionTable.jsp">
           <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
           <tiles:put name="object" beanName="reportObject.object" />
           <tiles:put name="fieldName" value="${entry.key}" />
        </tiles:insert>
        <p class="toggle">
          <a href="#" style="float:right;" class="collapser"><span>Hide</span></a>
        </p>
        <p class="in_table">
          <html:link action="/collectionDetails?id=${object.id}&amp;field=overlappingFeatures&amp;trail=${param.trail}">
            Show all in a table »
          </html:link>
        </p>
      <br/>
      </div>
      <div class="clear"></div>
    </c:forEach>
    <p class="in_table outer">
      <html:link action="/collectionDetails?id=${object.id}&amp;field=overlappingFeatures&amp;trail=${param.trail}">
        Show all in a table »
      </html:link>
    </p>
  </c:if>

  <script type="text/javascript">
    // apply different class to h3 so tables are not so separate
    jQuery("#overlapping-features.custom-displayer div.table h3").each(function(i) {
        jQuery(this).toggleClass('someclass');
        jQuery(this).toggleClass('someclass');
    });

    // switcher between tables this displayer haz
    jQuery("#overlapping-features.custom-displayer a.switcher").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#overlapping-features.custom-displayer div.table:visible").each(function(j) {
              jQuery(this).hide();
              // hide more toggler
              jQuery(this).parent().find('p.toggle a.toggler').remove();
            });

            // show the one we want
            jQuery("#overlapping-features.custom-displayer #" + jQuery(this).attr('id') + ".table").show();

            // show only 10 rows
            var rows = jQuery("#overlapping-features.custom-displayer #" + jQuery(this).attr('id') + ".table tbody tr");
            var count = 10;
            rows.each(function(index) {
                count--;
                if (count < 0) {
                    jQuery(this).hide();
                }
            });
            var attrId = jQuery(this).attr('id');
            // add a show next link
            if (count < 0) {
                var a = "<a href='#' style='float:right;margin-right:20px;' class='toggler'><span>Show more rows</span></a>";
                jQuery("#overlapping-features.custom-displayer #" + attrId + ".table p.toggle").append(a);
                jQuery("#overlapping-features.custom-displayer #" + attrId + ".table p.toggle a.toggler").bind(
                    "click",
                    function(f) {
                        // show another 10 rows
                        count = 11;
                        rows = jQuery("#overlapping-features.custom-displayer #" + attrId + ".table tbody tr:hidden");
                        // #overlapping-features.custom-displayer #.table tbody tr:hidden
                        rows.each(function(index) {
                            count--;
                            if (count > 0) {
                                jQuery(this).show();
                            }
                        });

                        // we have no more rows to show
                        if (jQuery("#overlapping-features.custom-displayer #" + attrId + ".table tbody tr:hidden").length == 0) {
                            // hide the link to more
                            jQuery("#overlapping-features.custom-displayer #" + attrId + ".table p.toggle a.toggler").remove();
                        }

                        // no linking on my turf
                        f.preventDefault();
                    });
            }

            // switchers all off
            jQuery("#overlapping-features.custom-displayer a.switcher.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

            // we are active
            jQuery(this).toggleClass('active');

            // hide the global show all in a table
            jQuery(this).parent().parent().find('p.in_table.outer').hide();

            // no linking on my turf
            e.preventDefault();
        }
      );
    });

    // table hider
    jQuery("#overlapping-features.custom-displayer p.toggle a").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#overlapping-features.custom-displayer div.table:visible").each(function(j) {
              jQuery(this).hide();
              // hide more toggler
              jQuery(this).parent().find('p.toggle a.toggler').remove();
            });

            // switchers all off
            jQuery("#overlapping-features.custom-displayer a.switcher.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

            // show the global show all in a table
            jQuery(this).parent().parent().parent().find('p.in_table').show();

            // scroll to the top of the displayer (inlinetemplate.js)
            jQuery("#overlapping-features.custom-displayer").scrollTo('fast', 'swing', -30);

            // no linking on my turf
            e.preventDefault();
        }
      );
    });
  </script>

</div>

<!-- /overlappingFeaturesDisplayer.jsp -->
