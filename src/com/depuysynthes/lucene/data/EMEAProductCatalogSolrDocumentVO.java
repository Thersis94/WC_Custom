package com.depuysynthes.lucene.data;

import java.util.Calendar;

import org.apache.log4j.Logger;

import com.depuysynthes.lucene.EMEAProductCatalogSolrIndex;

/****************************************************************************
 * <b>Title</b>: EMEAProductCatalogSolrDocumentVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Aug 24, 2015
 ****************************************************************************/
public class EMEAProductCatalogSolrDocumentVO extends ProductCatalogSolrDocumentVO {
	
	protected static final Logger log = Logger.getLogger(EMEAProductCatalogSolrDocumentVO.class);
	
	/**
	 * 
	 */
	public EMEAProductCatalogSolrDocumentVO() {
		this(EMEAProductCatalogSolrIndex.INDEX_TYPE);
		super.setUpdateDt(Calendar.getInstance().getTime());
	}
	
	/**
	 * @param solrIndex
	 */
	public EMEAProductCatalogSolrDocumentVO(String solrIndex) {
		super(solrIndex);
	}
}