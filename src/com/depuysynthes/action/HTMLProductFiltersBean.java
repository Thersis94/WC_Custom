package com.depuysynthes.action;

import java.text.NumberFormat;
import java.util.Locale;

import com.siliconmtn.data.Node;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: HTMLProductFiltersBean.java<p/>
 * <b>Description:</b>  Reusable HTML generating class for DS Product Facets & filter boxes.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 27, 2017
 ****************************************************************************/
public class HTMLProductFiltersBean {

	private static final String CLOSING_DIV = "</div>";

	public HTMLProductFiltersBean() {
		//a public constructor is needed for JSTL!
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
		return getHierarchyFilterSimple(c, filterNm, onclick, new StringBuilder(5000), invertColor);
	}


	/**
	 * Create an html structure from a parsed hierarchy field.
	 * @param rootNode
	 * @param filterNm
	 * @param onclick
	 * @param classes List of classes that each cause the creation of a new containing div for the html element
	 * @return
	 */
	private static String getHierarchyFilterSimple(Node rootNode, String filterNm, String onclick, StringBuilder sb, boolean invertColor) {
		if (rootNode == null) return "";

		for (Node n : rootNode.getChildren()) {
			if (n.getTotalChildren() == 0) continue;

			String cleanNodeId = StringUtil.removeNonAlpha(n.getNodeId());
			String uuid = RandomAlphaNumeric.generateRandom(5);
			//this works around a view issue on /hcp/products (sections not expandable) - caution.
			if (n.getDepthLevel() == 1 && StringUtil.isEmpty(rootNode.getParentId())) n.setLeaf(false);

			sb.append("<div class='collapse-panel collapse-panel-filter");
			if (invertColor) sb.append(" collapse-panel-nested");
			sb.append("'>");
			sb.append("<div class='collapse-panel-header'>");
			if (n.getNumberChildren() > 0 && !n.isLeaf()) {
				sb.append("<a class=\"caret_wrap collapsed\"data-toggle='collapse' href='#");
				sb.append(cleanNodeId).append("' aria-expanded='false' aria-controls='");
				sb.append(cleanNodeId).append("'><span class=\"caret\"></span></a>");
			} else {
				sb.append("<span class=\"count\">").append(formatCount(n.getTotalChildren())).append("</span>");
			}
			sb.append("<input type=\"checkbox\" class=\"").append(n.getNumberChildren() > 0 ? "parChkbx" : "").append("\" id=\"filter_simple_").append(uuid);
			sb.append("\" data-filter-nm=\"").append(filterNm).append("\" value=\"");
			sb.append(n.getFullPath()).append("\" onclick=\"").append(onclick);
			sb.append("\" data-search-name=\"").append(cleanNodeId).append("\">");
			sb.append("<label class=\"checkbox\" for=\"filter_simple_").append(uuid).append("\">");
			sb.append(n.getNodeName()).append("</label>").append(CLOSING_DIV);

			processOffspring(n, cleanNodeId, filterNm, onclick, sb, invertColor);

			sb.append(CLOSING_DIV);
		}
		sb.append("\r");
		return sb.toString();
	}


	/**
	 * separates logic from the invoking method to prevent overcomplicating
	 * @param n
	 * @param cleanNodeId
	 * @param filterNm
	 * @param onclick
	 * @param sb
	 * @param invertColor
	 */
	private static void processOffspring(Node n, String cleanNodeId, String filterNm, 
			String onclick, StringBuilder sb, boolean invertColor) {
		if (n.getNumberChildren() == 0 || n.isLeaf()) return;

		boolean isLeaf = true;
		// If this item is inverted its children will 
		// never be displayed as the end of a branch.
		if (!invertColor) {
			isLeaf = false;
		} else {
			isLeaf =  n.isLeaf();
		}

		if (isLeaf) {
			sb.append("<div id='").append(cleanNodeId);
			sb.append("' class='collapse-panel-body collapse' aria-expanded='true'>");
			getHierarchyLeaf(n, filterNm, onclick, sb);
			sb.append(CLOSING_DIV);
		} else {
			sb.append("<div id='").append(cleanNodeId);
			sb.append("' class='collapse-panel-body collapse' aria-expanded='true'>");
			getHierarchyFilterSimple(n, filterNm, onclick, sb, !invertColor);
			sb.append(CLOSING_DIV);
		}
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
			String uuid = RandomAlphaNumeric.generateRandom(5);
			sb.append("<li>");
			sb.append("<span class=\"count\">").append(formatCount(n.getTotalChildren())).append("</span>");
			sb.append("<input type=\"checkbox\" class=\"").append(n.getNumberChildren() > 0 ? "parChkbx" : "").append("\" id=\"filter_simple_").append(uuid);
			sb.append("\" data-filter-nm=\"").append(filterNm).append("\" value=\"");
			sb.append(n.getFullPath()).append("\" onclick=\"").append(onclick);
			sb.append("\" data-search-name=\"").append(StringUtil.removeNonAlpha(n.getNodeId())).append("\">");
			sb.append("<label class=\"checkbox\" for=\"filter_simple_").append(uuid).append("\">");
			sb.append(n.getNodeName()).append("</label>");
			sb.append("</li>");
		}
		sb.append("</ul>");
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
			if (o != null) return o.toString();
		}
		return "0";
	}
}