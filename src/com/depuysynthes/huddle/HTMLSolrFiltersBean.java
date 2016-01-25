package com.depuysynthes.huddle;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.PivotField;

import com.depuysynthes.huddle.HuddleUtils.IndexType;
import com.siliconmtn.data.Node;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.DateUtil;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: HTMLFiltersBean.java<p/>
 * <b>Description: Reusable HTML generating class for Huddle Solr Facets & filter boxes.</b> 
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
	public static String getFilterSimple(Count c, String filterNm, String onclick, String selectedItem, boolean formatName) {
		String uuid = RandomAlphaNumeric.generateRandom(5);
		StringBuilder sb = new StringBuilder(250);
		sb.append("<div class=\"collapse-panel collapse-panel-filter\">");
		sb.append("<div class=\"collapse-panel-header\">");
		sb.append("<span class=\"count\">").append(formatCount(c.getCount())).append("</span>");
		sb.append("<input type=\"checkbox\" id=\"filter_simple_").append(uuid);
		sb.append("\" data-filter-nm=\"").append(filterNm).append("\" value=\"");
		sb.append(c.getName()).append("\" onclick=\"").append(onclick).append("\"");
		if (selectedItem != null && c.getName().equals(selectedItem)) sb.append("checked=\"checked\" ");
		sb.append(">");
		sb.append("<label class=\"checkbox\" for=\"filter_simple_").append(uuid).append("\">");
		if (formatName) {
			sb.append(IndexType.valueOf(c.getName()).getName());
		} else {
			sb.append(c.getName());
		}
		sb.append("</label></div></div>\r");
		return sb.toString();
	}
	

	public static String getFilterSimple(Count c, String filterNm, String onclick, String selectedItem) {
		return getFilterSimple(c, filterNm, onclick, selectedItem, false);
	}
	
	public static String getFilterSimple(Count c, String filterNm, String onclick) {
		return HTMLSolrFiltersBean.getFilterSimple(c, filterNm, onclick, null);
	}
	
	
	/**
	 * Takes calls from the jsp, creates the stringbuilder that will hold the 
	 * html and passes it on to the main method.
	 * @param c
	 * @param filterNm
	 * @param onclick
	 * @param invertColor
	 * @return
	 */
	public static String getHierarchyFilterSimple(Node c, String filterNm, String onclick, boolean invertColor) {
		return getHierarchyFilterSimple(c, filterNm, onclick, new StringBuilder(), invertColor);
	}
	
	
	/**
	 * Create an html structure from a parsed hierarchy field.
	 * @param rootNode
	 * @param filterNm
	 * @param onclick
	 * @param classes List of classes that each cause the creation of a new containing div for the html element
	 * @return
	 */
	public static String getHierarchyFilterSimple(Node rootNode, String filterNm, String onclick, StringBuilder sb, boolean invertColor) {
		if (rootNode == null) return "";
		for (Node n : rootNode.getChildren()) {
			if ((Long)n.getUserObject() == 0) continue;
			
			String uuid = RandomAlphaNumeric.generateRandom(5);
			sb.append("<div class='collapse-panel collapse-panel-filter");
			if (invertColor) sb.append(" collapse-panel-nested");
			sb.append("'>");
			sb.append("<div class='collapse-panel-header'>");
			if (n.getNumberChildren() > 0) {
				sb.append("<a class=\"caret_wrap collapsed\"");
				sb.append(" data-toggle='collapse' href='#");
				sb.append(n.getNodeId().replace("~", "-").replace(" ", "-"));
				sb.append("' aria-expanded='false' aria-controls='");
				sb.append(n.getNodeId().replace("~", "-").replace(" ", "-"));
				sb.append("'><span class=\"caret\"></span></a>");
			} else {
				sb.append("<span class=\"count\">").append(formatCount(n.getUserObject())).append("</span>");
			}
			sb.append("<input type=\"checkbox\" class=\"parChkbx\" id=\"filter_simple_").append(uuid);
			sb.append("\" data-filter-nm=\"").append(filterNm).append("\" value=\"");
			sb.append(n.getNodeId()).append("\" onclick=\"").append(onclick);
			sb.append("\" data-search-name=\"").append(StringUtil.removeNonAlpha((n.getNodeId()))).append("\">");
			sb.append("<label class=\"checkbox\" for=\"filter_simple_").append(uuid).append("\">");
			sb.append(n.getNodeName()).append("</label>");
			sb.append("</div>");
			
			if (n.getNumberChildren() > 0) {
				boolean isLeaf = true;
				// If this item is inverted its children will 
				// never be displayed as the end of a branch.
				if (invertColor) {
					isLeaf = false;
				} else {
					// If any children have children of their own this this
					// section should not be treated as the end of a branch
					for (Node g : n.getChildren()) {
						if (g.getNumberChildren() > 0)
							isLeaf = false;
					}
				}
				if (isLeaf) {
					sb.append("<div id='").append(n.getNodeId().replace("~", "-").replace(" ", "-"));
					sb.append("' class='collapse-panel-body collapse' aria-expanded='true'>");
					getHierarchyLeaf(n, filterNm, onclick, sb);
					sb.append("</div>");
				} else {
					sb.append("<div id='").append(n.getNodeId().replace("~", "-").replace(" ", "-"));
					sb.append("' class='collapse-panel-body collapse' aria-expanded='true'>");
					getHierarchyFilterSimple(n, filterNm, onclick, sb, !invertColor);
					sb.append("</div>");
				}
			}
			
			sb.append("</div>");
		}
		sb.append("\r");
		return sb.toString();
	}
	
	
	/**
	 * Build the hierarchy leaf html
	 * @param rootNode
	 * @param filterNm
	 * @param onclick
	 * @param sb
	 */
	private static void getHierarchyLeaf(Node rootNode, String filterNm, String onclick, StringBuilder sb) {
		sb.append("<ul class='list-unstyled sub-filters'>");
		for (Node n : rootNode.getChildren()) {
			if ((Long)n.getUserObject() == 0) continue;
			String uuid = RandomAlphaNumeric.generateRandom(5);
			sb.append("<li>");
			sb.append("<span class=\"count\">").append(n.getUserObject()).append("</span>");
			sb.append("<input type=\"checkbox\" class=\"parChkbx\" id=\"filter_simple_").append(uuid);
			sb.append("\" data-filter-nm=\"").append(filterNm).append("\" value=\"");
			sb.append(n.getNodeId()).append("\" onclick=\"").append(onclick);
			sb.append("\" data-search-name=\"").append(StringUtil.removeNonAlpha(n.getNodeId())).append("\">");
			sb.append("<label class=\"checkbox\" for=\"filter_simple_").append(uuid).append("\">");
			sb.append(n.getNodeName()).append("</label>");
			sb.append("</li>");
		}
		sb.append("</ul>");
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
			sb.append("<span class=\"count\">").append(formatCount(child.getCount())).append("</span>");
			sb.append("</li>\n");
			x++;
		}
		sb.append("</ul></div></div>\n");
		return sb.toString();
	}
	
	
	/**
	 * format a long for UI display
	 * @param l
	 * @return
	 */
	private static String formatCount(long l) {
		return formatCount(Long.valueOf(l));
	}
	/**
	 * format an int for UI display
	 * @param l
	 * @return
	 */
	private static String formatCount(int i) {
		return formatCount(Integer.valueOf(i));
	}
	/**
	 * format an object for UI display
	 * @param l
	 * @return
	 */
	private static String formatCount(Object o) {
		try {
			return NumberFormat.getNumberInstance(Locale.US).format(o);
		} catch (Exception e) {
			//System.err.println(e.getMessage() + " could not format " + o);
			if (o != null) return o.toString();
		}
		return "0";
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