package com.universal.util.data;

// Java 8
import java.util.Calendar;

// Log4j
import org.apache.log4j.Logger;

// SMTBaseLibs
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

// WC Custom
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
	 * Captures USA Product Catalog data fields into the SolrDocument.
	 */
	@Override
	public void setData(Object o) {
		ProductVO vo = (ProductVO) o;
		super.setLanguage("en");
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		super.setDocumentId(vo.getProductId());
		super.setTitle(vo.getProductName());
		super.setSummary(vo.getDescText());
		
	    /* 2017-06-08: DBargerhuff:  In order to filter searches by product catalog ID, 
	     * we are adding product catalog ID to the document using a custom field. */
		this.setCustomFieldCatalog(StringUtil.checkVal(vo.getCatalogId()).toLowerCase());
	}

	@SolrField(name=ProductSolrIndex.CUSTOM_FIELD_CATALOG)
	public String getCustomFieldCatalog() {
		return customFieldCatalog;
	}

	public void setCustomFieldCatalog(String customFieldCatalog) {
		this.customFieldCatalog = customFieldCatalog;
	}
}