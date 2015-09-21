package com.universal.util.data;

import java.util.Calendar;

import org.apache.log4j.Logger;

import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;
import com.universal.util.ProductSolrIndex;

/****************************************************************************
 * <b>Title</b>: ProductSolrDocumentVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Aug 25, 2015
 ****************************************************************************/
public class ProductSolrDocumentVO extends SolrDocumentVO {
	
	protected static final Logger log = Logger.getLogger(ProductSolrDocumentVO.class);
	
	private String customFieldCatalog = null;
	
	/**
	 * 
	 */
	public ProductSolrDocumentVO() {
		this(ProductSolrIndex.INDEX_TYPE);
		super.setUpdateDt(Calendar.getInstance().getTime());
	}
	
	/**
	 * @param solrIndex
	 */
	public ProductSolrDocumentVO(String solrIndex) {
		super(solrIndex);
	}
	
	/**
	 * Captures DS Product Catalog data fields into the SolrDocument.
	 */
	public void setData(Object o) {
		ProductVO vo = (ProductVO) o;

		super.setLanguage("en");
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		super.setDocumentId(vo.getProductId());
		super.setTitle(vo.getProductName());
		super.setSummary(vo.getDescText());
		
	    /* 2014-03-26: DBargerhuff: 
	     * In order to filter searches by product catalog ID, we are adding product catalog ID to the document using a custom field 
	     * type value that is synonymous with a meta tag in the USA CMS field template.  We will be using a CMS
	     * dynamic list to filter searches, therefore the filter field name must be in all lowercase and the meta tag choice values must
	     * also be in lowercase.
	     * 
	     * If we were to use a DocumentHandler constant (e.g. MODULE_TYPE) for the field type value, and if that constant 
	     * was subsequently added to the 'searchableFields' array in SiteSearchAction, we would lose the ability to filter on 
	     * product catalog ID.  We use a StringField type so that the field value is not tokenized by the Lucene search query parser.
	     */
		this.setCustomFieldCatalog(StringUtil.checkVal(vo.getCatalogId()).toLowerCase());
	}



	// ------- Getters and Setters Below This Line -------
	
	@SolrField(name=ProductSolrIndex.CUSTOM_FIELD_CATALOG)
	public String getCustomFieldCatalog() {
		return customFieldCatalog;
	}

	public void setCustomFieldCatalog(String customFieldCatalog) {
		this.customFieldCatalog = customFieldCatalog;
	}
}