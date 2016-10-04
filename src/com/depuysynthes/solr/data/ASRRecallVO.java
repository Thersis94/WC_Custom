package com.depuysynthes.solr.data;

import java.util.Map.Entry;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.solr.CMSSolrIndexer;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: ASRRecallVO.java<p/>
 * <b>Description: Used to create solr documents for the ASR Recall  pages in 
 * depuy synthes site. Due to the larger number of pages and the lack of
 * coherent patterns in the naming structure of the files special rules are in 
 * place to prevent incorrect files from being put into solr</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @since Oct 3, 2016
 ****************************************************************************/

public class ASRRecallVO extends SolrDocumentVO {
	
	private final String[] organizations = {"DPY_SYN"};
	
	public ASRRecallVO() {
		this(CMSSolrIndexer.INDEX_TYPE);
	}

	public ASRRecallVO(String solrIndex) {
		super(solrIndex);
	}
	
	/**
	 * Build a proper solr document vo for the ASR Recall pages
	 */
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		// If the Object is not of the correct type return
		if (!(o instanceof Entry<?, ?>)) return;
		Entry<String, byte[]> vo = (Entry<String, byte[]>)o;
		
		String fileName = StringUtil.checkVal(vo.getKey());
		super.setDocumentId("ASR_" + fileName.replace(".html", ""));
		super.setFileName((String) vo.getKey());
		super.addSection("Other");
		super.setDocumentUrl("/asrrecall/" + vo.getKey());
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		for (String s : organizations) {
			super.addOrganization(s);
		}
		super.addAttribute("ASR", "true");
	}
}
