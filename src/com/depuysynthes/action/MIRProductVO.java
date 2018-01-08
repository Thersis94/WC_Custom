package com.depuysynthes.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: MIRProductVO.java<p/>
 * <b>Description: Represents a row in the Excel file for annotation parser, and a SolrDocument for the indexer</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 07, 2017
 ****************************************************************************/
public class MIRProductVO extends SolrDocumentVO implements Comparable<MIRProductVO> {

	private String[] terms;

	public MIRProductVO() {
		super(MIRSubmissionAction.MIR_INDEX_TYPE);
		terms = new String[7];
	}


	/**
	 * Builder VO, used when retrieving from Solr to populate a list of beans
	 * we can sort correctly.
	 * @param sd
	 */
	public MIRProductVO(SolrDocument sd) {
		this();
		parseHierarchies(sd);
	}


	/**
	 * @param sd
	 */
	private void parseHierarchies(SolrDocument sd) {
		Collection<Object> hierarchies = sd.getFieldValues(SearchDocumentHandler.HIERARCHY);
		if (hierarchies == null || hierarchies.isEmpty()) return;

		for (Object o : hierarchies) { //note: this list is only ever one entry deep - otherwise the structure of this bean would be a bust.
			if (o == null) continue;

			terms = ((String)o).split(SearchDocumentHandler.HIERARCHY_DELIMITER);

			setTitle(terms[terms.length -1]);
		}
	}


	/**
	 * Flattens the String[] into a hierarchy, adds it to the superclass' list, then returns the superclass' list.
	 * @return
	 */
	@SolrField(name=SearchDocumentHandler.HIERARCHY)
	@Override
	public List<String> getHierarchies() {
		//ensure the list is only built once.
		if (!super.getHierarchies().isEmpty()) 
			return super.getHierarchies();

		StringBuilder sb = new StringBuilder(100);
		for (String s : terms) {
			if (StringUtil.isEmpty(s)) continue;
			if (sb.length() > 0) sb.append(SearchDocumentHandler.HIERARCHY_DELIMITER);
			sb.append(s);
		}
		super.addHierarchies(sb.toString());

		return super.getHierarchies();
	}


	@SolrField(name=SearchDocumentHandler.TITLE)
	@Override
	public String getTitle() {
		//return the first value from the bottom->up.  This is the product name
		for (int x=terms.length-1; x > -1 ; x--) {
			if (!StringUtil.isEmpty(terms[x])) {
				return terms[x];
			}
		}
		return super.getTitle();
	}


	/**
	 * simple helper to tell the Action we pulled real data out of the Excel file
	 * @return
	 */
	protected boolean hasData() {
		if (terms == null || terms.length == 0) return false;
		for (String s : terms) {
			if (!StringUtil.isEmpty(s)) 
				return true;
		}
		return false;
	}


	/**********************************************************
	 * 
	 * @Importable annotations match the column heading in the incoming Excel file
	 * 
	 **********************************************************/

	@Importable(name = "Level 1 Term", type = DataType.STRING)
	public void setTerm1(String t) {
		terms[0] = t;
	}

	@Importable(name = "Level 2 Term", type = DataType.STRING)
	public void setTerm2(String t) {
		terms[1] = t;
	}

	@Importable(name = "Level 3 Term", type = DataType.STRING)
	public void setTerm3(String t) {
		terms[2] = t;
	}

	@Importable(name = "Level 4 Term", type = DataType.STRING)
	public void setTerm4(String t) {
		terms[3] = t;
	}

	@Importable(name = "Level 5 Term", type = DataType.STRING)
	public void setTerm5(String t) {
		terms[4] = t;
	}

	@Importable(name = "Level 6 Term", type = DataType.STRING)
	public void setTerm6(String t) {
		terms[5] = t;
	}

	@Importable(name = "Level 7 Term", type = DataType.STRING)
	public void setTerm7(String t) {
		terms[6] = t;
	}


	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MIRProductVO o) {
		if (o == null || o.getTerms() == null) return -1;
		return Arrays.toString(terms).compareTo(Arrays.toString(o.getTerms()));
	}


	/**
	 * @return
	 */
	private String[] getTerms() {
		return terms;
	}
}