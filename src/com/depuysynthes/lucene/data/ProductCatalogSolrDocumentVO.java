package com.depuysynthes.lucene.data;

import java.util.Calendar;

import org.apache.log4j.Logger;

import com.depuysynthes.lucene.ProductCatalogSolrIndex;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: ProductCatalogSolrDocumentVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Aug 24, 2015
 ****************************************************************************/
public class ProductCatalogSolrDocumentVO extends SolrDocumentVO {

	protected static final Logger log = Logger.getLogger(ProductCatalogSolrDocumentVO.class);

	private String thumbImage = null;
	private int dsOrderNo;
	private String specialty = null;

	/**
	 * 
	 */
	public ProductCatalogSolrDocumentVO() {
		this(ProductCatalogSolrIndex.INDEX_TYPE);
		super.setUpdateDt(Calendar.getInstance().getTime());
	}

	/**
	 * @param solrIndex
	 */
	public ProductCatalogSolrDocumentVO(String solrIndex) {
		super(solrIndex);
	}

	/**
	 * Captures DS Product Catalog data fields into the SolrDocument.
	 */
	public void setData(Object o) {
		Node n = (Node) o;
		super.setLanguage("en");
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		super.setDocumentId(n.getNodeId());
		super.setTitle(n.getNodeName());
	}

	/**
	 * Overloaded to allow additional data passed from multiple objects/VOs.
	 * @param n
	 * @param vo
	 */
	public void setData(Node n, ProductCategoryVO vo) {
		this.setData(n);
		super.setDocumentUrl(vo.getCategoryUrl());
		super.setSummary(StringUtil.checkVal(vo.getCategoryDesc()));
	}



	// ------- Getters and Setters Below This Line -------

	@SolrField(name=SearchDocumentHandler.THUMBNAIL_IMAGE)
	public String getThumbImage() {
		return thumbImage;
	}

	public void setThumbImage(String thumbImage) {
		this.thumbImage = thumbImage;
	}

	@SolrField(name="dsOrderNo_i")
	public int getDsOrderNo() {
		return dsOrderNo;
	}

	public void setDsOrderNo(int dsOrderNo) {
		this.dsOrderNo = dsOrderNo;
	}

	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}

	@SolrField(name="opco_ss")
	public String getSpecialty(){
		return specialty;
	}
}