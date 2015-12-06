package com.depuysynthes.action;

import java.sql.ResultSet;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.db.DBUtil;
import com.smt.sitebuilder.action.SBModuleVO;

/**
 * **************************************************************************
 * <b>Title</b>: PatentAction.PatentVO.java<p/>
 * <b>Description: Simple VO for Patent records coming from the database.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 4, 2015
 ***************************************************************************
 */
public class PatentVO extends SBModuleVO {
	private static final long serialVersionUID = -7637893290584665787L;
	public String company;
	public String code;
	public String item;
	public String desc;
	public String patents;

	public PatentVO() {
		super();
	}

	public PatentVO(ResultSet rs) {
		this();
		DBUtil dbUtil = new DBUtil();
		//this.company = dbUtil.getStringVal("company_nm", rs);
		this.code = dbUtil.getStringVal("code_txt", rs);
		this.item = dbUtil.getStringVal("item_txt", rs);
		this.desc = dbUtil.getStringVal("desc_txt", rs);
		this.patents = dbUtil.getStringVal("patents_txt", rs);
	}

	public String getCompany() {
		return company;
	}

	public String getCode() {
		return code;
	}

	public String getItem() {
		return item;
	}

	public String getDesc() {
		return desc;
	}

	public String getPatents() {
		return patents;
	}

	@Importable(name = "Operating Company", type = DataType.STRING)
	public void setOperatingCompany(String company) {
		this.company = company;
	}

	@Importable(name = "Bar Code", type = DataType.STRING)
	public void setBarCode(String code) {
		this.code = code;
	}

	@Importable(name = "Item Number", type = DataType.STRING)
	public void setItemNumber(String item) {
		this.item = item;
	}

	@Importable(name = "Product Description", type = DataType.STRING)
	public void setProductDescription(String desc) {
		this.desc = desc;
	}

	@Importable(name = "Patents", type = DataType.STRING)
	public void setPatents(String patents) {
		this.patents = patents;
	}
}