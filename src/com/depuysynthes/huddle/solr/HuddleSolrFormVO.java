package com.depuysynthes.huddle.solr;

import com.depuysynthes.huddle.HuddleUtils;
import com.siliconmtn.annotations.SolrField;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: FBSolrDocumentVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 16, 2016
 ****************************************************************************/
public class HuddleSolrFormVO extends SolrDocumentVO {

	private String specialty;
	
	/**
	 * @param solrIndex
	 */
	public HuddleSolrFormVO(String solrIndex) {
		super(solrIndex);
	}
	
	public void setSpecialty(String s) {
		this.specialty = s;
	}
	
	
	@SolrField(name=HuddleUtils.SOLR_OPCO_FIELD)
	public String getSpecialty() {
		return specialty;
	}
	
	@SolrField(name="assetType_s")
	public String getAssetType() {
		//this allows us to marry with XLS and PDF forms (non-interactive) stored in the CMS, on the /forms page
		return "Form";
	}

}
