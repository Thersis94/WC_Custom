package com.depuysynthes.huddle;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: ProductContactVO.java<p/>
 * <b>Description: Represents a row in the Excel file for annotation parser, and a SolrDocument for the indexer</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 15, 2016
 ****************************************************************************/
public class ProductContactVO extends SolrDocumentVO implements Comparable<ProductContactVO> {

	private String opcoNm;
	private String name;
	private String title;
	private String email;
	private String office;
	private String mobile;
	private String product;

	public ProductContactVO() {
		super(HuddleUtils.SOLR_PROD_CONTACT_IDX_TYPE);
	}

	
	/**
	 * Builder VO, used when retrieving from Solr to populate a list of beans
	 * we can sort correctly.
	 * @param sd
	 */
	public ProductContactVO(SolrDocument sd) {
		this();
		setOpcoNm(StringUtil.checkVal(HuddleUtils.SOLR_OPCO_FIELD));
		setName(StringUtil.checkVal(sd.get("rep_first_nm_s")));
		setTitle(StringUtil.checkVal(sd.get("rep_title_s")));
		setEmail(StringUtil.checkVal(sd.get("rep_email_s")));
		setOffice(StringUtil.checkVal(sd.get("rep_phone_s")));
		setMobile(StringUtil.checkVal(sd.get("mgr_phone_s")));
		setProduct(StringUtil.checkVal(sd.get(SearchDocumentHandler.TITLE)));
	}
	

	/**********************************************************
	 * 
	 * We're intentionally re-using the dynamic fields also used by SalesConsultantVO
	 * 
	 **********************************************************/

	@SolrField(name=HuddleUtils.SOLR_OPCO_FIELD)
	public String getOpcoNm() {
		return opcoNm;
	}

	@SolrField(name="rep_first_nm_s")
	public String getName() {
		return name;
	}

	@SolrField(name="rep_title_s")
	public String getTitle() {
		return title;
	}

	@SolrField(name="rep_email_s")
	public String getEmail() {
		return email;
	}

	@SolrField(name="rep_phone_s")
	public String getOffice() {
		return StringUtil.removeNonNumeric(office);
	}

	@SolrField(name="mgr_phone_s")
	public String getMobile() {
		return StringUtil.removeNonNumeric(mobile);
	}

	@SolrField(name=SearchDocumentHandler.TITLE)
	public String getProduct() {
		return product;
	}



	/**********************************************************
	 * 
	 * @Importable annotations match the column heading in the incoming Excel file
	 * 
	 **********************************************************/

	@Importable(name = "Op-Co", type = DataType.STRING)
	public void setOpcoNm(String opcoNm) {
		this.opcoNm = opcoNm;
	}

	@Importable(name = "Name", type = DataType.STRING)
	public void setName(String name) {
		this.name = name;
	}

	@Importable(name = "Title", type = DataType.STRING)
	public void setTitle(String title) {
		this.title = title;
	}

	@Importable(name = "Email", type = DataType.STRING)
	public void setEmail(String email) {
		this.email = email;
	}

	@Importable(name = "Office", type = DataType.STRING)
	public void setOffice(String office) {
		this.office = office;
	}

	@Importable(name = "Mobile", type = DataType.STRING)
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	@Importable(name = "Product", type = DataType.STRING)
	public void setProduct(String product) {
		this.product = product;
	}


	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ProductContactVO vo) {
		return StringUtil.checkVal(getName()).toLowerCase().compareTo(StringUtil.checkVal(vo.getName()).toLowerCase());
	}
}