package com.depuysynthes.huddle;

import com.siliconmtn.annotations.DataType;
import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: SalesConsultantRepVO.java<p/>
 * <b>Description: Represents data from the suplimental upload file (repData).  
 * This data is not stored in the database, rather, married to the AlignmentVO and pushed to Solr.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 25, 2016
 ****************************************************************************/
public class SalesConsultantRepVO {
	
	//the sales rep's attributes
	private String WWID;
	private String SC_CLASS;
	private String OFF_TITLE;
	
	
	public String getWWID() {
		return WWID;
	}
	public String getTitle() {
		return StringUtil.checkVal(SC_CLASS, OFF_TITLE);
	}
	public String getOFF_TITLE() {
		return OFF_TITLE;
	}
	
	@Importable(name = "WWID", type = DataType.STRING)
	public void setWWID(String wWID) {
		WWID = wWID;
	}
	
	@Importable(name = "SC_CLASS", type = DataType.STRING)
	public void setSC_CLASS(String sC_CLASS) {
		SC_CLASS = sC_CLASS;
	}
	
	@Importable(name = "OFF_TITLE", type = DataType.STRING)
	public void setOFF_TITLE(String oFF_TITLE) {
		OFF_TITLE = oFF_TITLE;
	}
}