package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: SalesConsultantAlignVO.java<p/>
 * <b>Description: Represents a record in the incoming "Alignment" Excel file, 
 * which is dynamically parsed and immediately fed into Solr.  
 * This data is not stored in the database.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 10, 2016
 ****************************************************************************/
public class SalesConsultantAlignVO extends SolrDocumentVO {
	
	//the sales rep's attributes
	private String OPCO_CD;
	private String ALGN_STRUC_CD;
	private String CNSMR_NO;
	private String CNSMR_NM;
	private String TERR_ID;
	private String REP_ID;
	private String REP_FIRST_NM;
	private String REP_LAST_NM;
	private String REP_TEL_NO;
	private String REP_EMAIL_ADDR;
	private String repTitle;
	
	//their manager's attributes
	private String CORE_DIV_MGR_ID;
	private String CORE_DIV_MGR_FIRST_NM;
	private String CORE_DIV_MGR_LAST_NM;
	private String CORE_DIV_MGR_TEL_NO;
	private String CORE_DIV_MGR_EMAIL_ADDR;
	
	private Set<String> hospitals;
	
	/**
	 * @param solrIndex
	 */
	public SalesConsultantAlignVO(String solrIndex) {
		super(solrIndex);
		hospitals = new HashSet<>();
	}
	
	public SalesConsultantAlignVO() {
		this(HuddleUtils.IndexType.HUDDLE_CONSULTANTS.toString());
	}

	@SolrField(name=HuddleUtils.SOLR_OPCO_FIELD)
	public String getOPCO_CD() {
		return HuddleUtils.getBusUnitNmFromAbbr(OPCO_CD);
	}

	@SolrField(name="hospital_no_i")
	public Integer getCNSMR_NO() {
		return Convert.formatInteger(CNSMR_NO);
	}

	@SolrField(name="hospital_nm_ss")
	public List<String> getHospitals() {
		return new ArrayList<>(hospitals);
	}
	
	public String getCNSMR_NM() {
		return CNSMR_NM;
	}

	@SolrField(name="territory_no_i")
	public Integer getTERR_ID() {
		return Convert.formatInteger(TERR_ID);
	}

	@SolrField(name="rep_id_i")
	public Integer getREP_ID() {
		return Convert.formatInteger(REP_ID);
	}

	@SolrField(name="rep_first_nm_s")
	public String getREP_FIRST_NM() {
		return REP_FIRST_NM;
	}

	@SolrField(name="rep_last_nm_s")
	public String getREP_LAST_NM() {
		return REP_LAST_NM;
	}
	
	
	/** 
	 * use Solr's title field for tokenized name to make searching more accurate
	 */
	@SolrField(name=SearchDocumentHandler.TITLE)
	public String getTitle() {
		return getREP_FIRST_NM() + " " + getREP_LAST_NM();
	}
	
	@SolrField(name="rep_title_s")
	public String getRepTitle() {
		return repTitle;
	}
	
	@SolrField(name="rep_phone_s")
	public String getREP_TEL_NO() {
		return StringUtil.removeNonNumeric(REP_TEL_NO);
	}

	@SolrField(name="rep_email_s")
	public String getREP_EMAIL_ADDR() {
		return REP_EMAIL_ADDR;
	}

	@SolrField(name="mgr_id_i")
	public Integer getCORE_DIV_MGR_ID() {
		return Convert.formatInteger(CORE_DIV_MGR_ID);
	}

	@SolrField(name="mgr_first_nm_s")
	public String getCORE_DIV_MGR_FIRST_NM() {
		return CORE_DIV_MGR_FIRST_NM;
	}

	@SolrField(name="mgr_last_nm_s")
	public String getCORE_DIV_MGR_LAST_NM() {
		return CORE_DIV_MGR_LAST_NM;
	}

	@SolrField(name="mgr_phone_s")
	public String getCORE_DIV_MGR_TEL_NO() {
		return StringUtil.removeNonNumeric(CORE_DIV_MGR_TEL_NO);
	}

	@SolrField(name="mgr_email_s")
	public String getCORE_DIV_MGR_EMAIL_ADDR() {
		return CORE_DIV_MGR_EMAIL_ADDR;
	}

	@SolrField(name="rep_region_s")
	public String getALGN_STRUC_CD() {
		return ALGN_STRUC_CD;
	}

	
	
	@Importable(name = "OPCO_CD", type = DataType.STRING)
	public void setOPCO_CD(String oPCO_CD) {
		OPCO_CD = oPCO_CD;
	}

	@Importable(name = "CNSMR_NO", type = DataType.STRING)
	public void setCNSMR_NO(String cNSMR_NO) {
		CNSMR_NO = cNSMR_NO;
	}

	@Importable(name = "CNSMR_NM", type = DataType.STRING)
	public void setCNSMR_NM(String cNSMR_NM) {
		CNSMR_NM = cNSMR_NM;
	}
	public void addHospital(String hosp) {
		hospitals.add(hosp);
	}

	@Importable(name = "CITY_NM", type = DataType.STRING)
	public void setCNSMR_CITY(String cNSMR_CITY) {
		super.setCity(cNSMR_CITY);
	}

	@Importable(name = "STATE_CD", type = DataType.STRING)
	public void setCNSMR_STATE(String cNSMR_STATE) {
		super.setState(cNSMR_STATE);
	}

	@Importable(name = "TERR_ID", type = DataType.STRING)
	public void setTERR_ID(String tERR_ID) {
		TERR_ID = tERR_ID;
	}

	@Importable(name = "REP_ID", type = DataType.STRING)
	public void setREP_ID(String rEP_ID) {
		REP_ID = rEP_ID;
	}

	@Importable(name = "REP_FIRST_NM", type = DataType.STRING)
	public void setREP_FIRST_NM(String rEP_FIRST_NM) {
		REP_FIRST_NM = rEP_FIRST_NM;
	}

	@Importable(name = "REP_LAST_NM", type = DataType.STRING)
	public void setREP_LAST_NM(String rEP_LAST_NM) {
		REP_LAST_NM = rEP_LAST_NM;
	}

	@Importable(name = "REP_TEL_NO", type = DataType.STRING)
	public void setREP_TEL_NO(String rEP_TEL_NO) {
		REP_TEL_NO = rEP_TEL_NO;
	}

	@Importable(name = "REP_EMAIL_ADDR", type = DataType.STRING)
	public void setREP_EMAIL_ADDR(String rEP_EMAIL_ADDR) {
		REP_EMAIL_ADDR = rEP_EMAIL_ADDR;
	}

	@Importable(name = "CORE_DIV_MGR_ID", type = DataType.STRING)
	public void setCORE_DIV_MGR_ID(String id) {
		CORE_DIV_MGR_ID = id;
	}

	@Importable(name = "CORE_DIV_MGR_FIRST_NM", type = DataType.STRING)
	public void setCORE_DIV_MGR_FIRST_NM(String firstNm) {
		CORE_DIV_MGR_FIRST_NM = firstNm;
	}

	@Importable(name = "CORE_DIV_MGR_LAST_NM", type = DataType.STRING)
	public void setCORE_DIV_MGR_LAST_NM(String lastNm) {
		CORE_DIV_MGR_LAST_NM = lastNm;
	}

	@Importable(name = "CORE_DIV_MGR_TEL_NO", type = DataType.STRING)
	public void setCORE_DIV_MGR_TEL_NO(String tel) {
		CORE_DIV_MGR_TEL_NO = tel;
	}

	@Importable(name = "CORE_DIV_MGR_EMAIL_ADDR", type = DataType.STRING)
	public void setCORE_DIV_MGR_EMAIL_ADDR(String email) {
		CORE_DIV_MGR_EMAIL_ADDR = email;
	}

	@Importable(name = "ALGN_STRUC_CD", type = DataType.STRING)
	public void setALGN_STRUC_CD(String str) {
		ALGN_STRUC_CD = str;
	}
	
	public String getHierarchy() {
		String tok = SearchDocumentHandler.HIERARCHY_DELIMITER;
		return this.getState() + tok + this.getCity() + tok + this.getCNSMR_NM();
	}

	public void setRepTitle(String repTitle) {
		this.repTitle = repTitle;
	}
}