package com.depuysynthes.huddle;

import java.util.Collections;
import java.util.Comparator;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;

import com.siliconmtn.data.Node;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.DateUtil;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: HTMLFiltersBean.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 1, 2016
 ****************************************************************************/
public class HTMLSolrFiltersBean {
	
	/**
	 * turns a Solr Hierarchy Node into rendered HTML.
	 * Abstracts the filter HTML to keep reusable code in one place, and our Views lean.
	 * @param n
	 * @return
	 */
	public static String getFilterSimple(Count c, String filterNm, String onclick) {
		String uuid = RandomAlphaNumeric.generateRandom(5);
		StringBuilder sb = new StringBuilder(250);
		sb.append("<div class=\"collapse-panel collapse-panel-filter\">");
		sb.append("<div class=\"collapse-panel-header\">");
		sb.append("<span class=\"count\">").append(c.getCount()).append("</span>");
		sb.append("<input type=\"checkbox\" id=\"filter_simple_").append(uuid);
		sb.append("\" data-filter-nm=\"").append(filterNm).append("\" value=\"");
		sb.append(c.getName()).append("\" onclick=\"").append(onclick).append("\">");
		sb.append("<label class=\"checkbox\" for=\"filter_simple_").append(uuid).append("\">");
		sb.append(c.getName()).append("</label></div></div>\r");
		return sb.toString();
	}
	
	
	/**
	 * Create an html structure from a parsed hierarchy field.
	 * @param c
	 * @param filterNm
	 * @param onclick
	 * @param classes List of classes that each cause the creation of a new containing div for the html element
	 * @return
	 */
	public static String getHierarchyFilterSimple(Node c, String filterNm, String onclick, String... classes) {
		String uuid = RandomAlphaNumeric.generateRandom(5);
		StringBuilder sb = new StringBuilder(250);
		for (String s : classes) {
			sb.append("<div class=\"").append(s).append("\">");
		}
		if (c.getNumberChildren() > 0) {
			sb.append("<span class=\"count fa fa-caret-down collapse-toggle\"");
			sb.append("data-toggle='collapse' data-target='#");
			sb.append(c.getNodeId().replace("~", "-"));
			sb.append("' aria-expanded='false' aria-controls='");
			sb.append(c.getNodeId().replace("~", "-"));
			sb.append("'></span>");
			
		} else {
			sb.append("<span class=\"count\">").append(c.getUserObject()).append("</span>");
		}
		sb.append("<input type=\"checkbox\" id=\"filter_simple_").append(uuid);
		sb.append("\" data-filter-nm=\"").append(c.getNodeName()).append("\" value=\"");
		sb.append(c.getNodeName()).append("\" onclick=\"").append(onclick).append("\">");
		sb.append("<label class=\"checkbox\" for=\"filter_simple_").append(uuid).append("\">");
		sb.append(c.getNodeName()).append(c.isLeaf()).append("</label>");
		for (String s : classes) {
			sb.append("</div>");
		}
		sb.append("\r");
		return sb.toString();
	}
	
	
	public static String getPivotFilterOneLevel(PivotField pf, boolean childIsMonth, String onclick) {
		String uuid = RandomAlphaNumeric.generateRandom(5);
		StringBuilder sb = new StringBuilder(250);
		sb.append("<div class=\"collapse-panel collapse-panel-filter\">");
		sb.append("<div class=\"collapse-panel-header\">");
		sb.append("<a class=\"caret_wrap collapsed\" data-toggle=\"collapse\" href=\"#filter_onelevel_").append(uuid).append("\">");
		sb.append("<span class=\"caret\"></span></a>\n");
		sb.append("<input type=\"checkbox\" class=\"parChkbx\" id=\"main_check_").append(uuid);
		sb.append("\" data-filter-nm=\"").append(pf.getField()).append("\" value=\"");
		sb.append(pf.getValue()).append("\" onclick=\"").append(onclick).append("\">");
		sb.append("<label class=\"checkbox\" for=\"main_check_").append(uuid).append("\">").append(pf.getValue()).append("</label>");
		sb.append("</div>\n");
		sb.append("<div class=\"collapse-panel-body collapse\" id=\"filter_onelevel_").append(uuid).append("\">");
		sb.append("<ul class=\"list-unstyled sub-filters\">");
		
		
		//sort the child data 

		//turn the month 
		if (childIsMonth) { //as ints, then turn the values into month names
			Collections.sort(pf.getPivot(), new IntPivotSort());
		} else { //lexicographically
			Collections.sort(pf.getPivot(), new AlphabeticalPivotSort());
		}
		
		int x = 0;
		for (PivotField child : pf.getPivot()) {
			String label = StringUtil.checkVal(child.getValue());
			if (childIsMonth)
				label = DateUtil.getMonthNameLong(Convert.formatInteger(label));
			
			String filterNm = pf.getField() + ":" + pf.getValue() + "&fq=" + child.getField(); //preserve the parent as part of the constraint
			sb.append("<li>");
			sb.append("<input id=\"nested_list_item_").append(uuid).append(x).append("\" type=\"checkbox\"");
			sb.append("class=\"childChkbx\" data-filter-nm=\"").append(filterNm).append("\" value=\"");
			sb.append(child.getValue()).append("\" onclick=\"").append(onclick).append("\">");
			sb.append("<label for=\"nested_list_item_").append(uuid).append(x).append("\">").append(label).append("</label>");
			sb.append("<span class=\"count\">").append(child.getCount()).append("</span>");
			sb.append("</li>\n");
			x++;
		}
		sb.append("</ul></div></div>\n");
		return sb.toString();
	}
	
	
	

}



class IntPivotSort implements Comparator<PivotField> {
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(PivotField o1, PivotField o2) {
		if (o1 == null || o1.getValue() == null) return -1;
		if (o2 == null || o2.getValue() == null) return 1;
		return Convert.formatInteger(o1.getValue().toString()).compareTo(Convert.formatInteger(o2.getValue().toString()));
	}
}


class AlphabeticalPivotSort implements Comparator<PivotField> {
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(PivotField o1, PivotField o2) {
		if (o1 == null || o1.getValue() == null) return -1;
		if (o2 == null || o2.getValue() == null) return 1;
		return o1.getValue().toString().compareTo(o2.getValue().toString());
	}
}