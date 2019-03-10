package com.depuysynthes.action;

// Java 8
import java.sql.ResultSet;

//SMTBaseLibs
import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBModuleVO;

/**
 * **************************************************************************
 * <b>Title</b>: PatentVO.java<p/>
 * <b>Description: Simple VO for Patent records coming from the database.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 4, 2015
 * Change Log:
 * 2018-04-02: DBargerhuff, DS-392, implementing patent mgmt data tool
 * 2018-07-02: DBargerhuff, DS-464, restored support for bulk import.
 ***************************************************************************
 */
public class PatentVO extends SBModuleVO {
	private static final long serialVersionUID = -7637893290584665787L;
	private int patentId;
	private String company;
	/**
	 * Primary Bar Code of the Patent
	 */
	private String code;
	/**
	 * Reference
	 */
	private String item;
	private String desc;
	/**
	 * Patent numbers specified for this patent record
	 */
	private String patents;
	private String redirectAddress;
	private String redirectName;
	
	private int statusFlag;

	public PatentVO() {
		super();
	}
	
	public PatentVO(ResultSet rs) {
		super(rs);
		DBUtil dbUtil = new DBUtil();
		this.patentId = dbUtil.getIntVal("patent_id", rs);
		this.company = dbUtil.getStringVal("company_nm", rs);
		this.code = dbUtil.getStringVal("code_txt", rs);
		this.item = dbUtil.getStringVal("item_txt", rs);
		this.desc = dbUtil.getStringVal("desc_txt", rs);
		this.patents = dbUtil.getStringVal("patents_txt", rs);
		this.redirectAddress = dbUtil.getStringVal("redirect_address_txt", rs);
		this.redirectName = dbUtil.getStringVal("redirect_nm", rs);
		this.statusFlag = dbUtil.getIntVal("status_flg", rs);
		this.createDate = dbUtil.getDateVal("create_dt", rs);
		this.updateDate = dbUtil.getDateVal("update_dt", rs);
	}

	/**
	 * @return the patentId
	 */
	public int getPatentId() {
		return patentId;
	}

	/**
	 * @param patentId the patentId to set
	 */
	public void setPatentId(int patentId) {
		this.patentId = patentId;
	}

	/**
	 * @return the company
	 */
	public String getCompany() {
		return company;
	}

	/**
	 * @param company the company to set
	 */
	@Importable(name = "Operating Company", type = DataType.STRING)
	public void setCompany(String company) {
		this.company = company;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	@Importable(name = "Bar Code", type = DataType.STRING)
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the item
	 */
	public String getItem() {
		return item;
	}

	/**
	 * @param item the item to set
	 */
	@Importable(name = "Item Number", type = DataType.STRING)
	public void setItem(String item) {
		this.item = item;
	}
	
	/**
	 * @return the desc
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @param desc the desc to set
	 */
	@Importable(name = "Product Description", type = DataType.STRING)
	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * @return the patents
	 */
	public String getPatents() {
		return patents;
	}

	/**
	 * @param patents the patents to set
	 */
	@Importable(name = "Patents", type = DataType.STRING)
	public void setPatents(String patents) {
		this.patents = patents;
	}

	/**
	 * @return the redirectAddress
	 */
	public String getRedirectAddress() {
		if (StringUtil.isEmpty(redirectAddress)) return null;
		if (! redirectAddress.toLowerCase().startsWith("http")) {
				return "http://" + redirectAddress;
		}
		return redirectAddress;
	}

	/**
	 * @param redirectAddress the redirectAddress to set
	 */
	@Importable(name = "Redirect Address", type = DataType.STRING)
	public void setRedirectAddress(String redirectAddress) {
		this.redirectAddress = redirectAddress;
	}

	/**
	 * @return the redirectName
	 */
	public String getRedirectName() {
		return redirectName;
	}

	/**
	 * @param redirectName the redirectName to set
	 */
	@Importable(name = "Redirect Name", type = DataType.STRING)
	public void setRedirectName(String redirectName) {
		this.redirectName = redirectName;
	}

	/**
	 * @return the statusFlag
	 */
	public int getStatusFlag() {
		return statusFlag;
	}

	/**
	 * @param statusFlag the statusFlag to set
	 */
	public void setStatusFlag(int statusFlag) {
		this.statusFlag = statusFlag;
	}

}
