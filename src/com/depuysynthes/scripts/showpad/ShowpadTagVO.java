package com.depuysynthes.scripts.showpad;

import java.io.Serializable;

import com.siliconmtn.util.StringUtil;

import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title</b>: ShowpadTagVO.java<p/>
 * <b>Description: POJO representation of a Showpad Tag.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 31, 2016
 ****************************************************************************/
public class ShowpadTagVO implements Serializable {

	private static final long serialVersionUID = -1963742619927011372L;
	private String name;
	private String id;
	private String externalId;
	private String divisionId;

	public ShowpadTagVO() {
	}
	
	public ShowpadTagVO(String id, String name, String division, String externalId) {
		this();
		this.id = id;
		this.name = name;
		this.divisionId = division;
		this.externalId = externalId;
	}
	
	public ShowpadTagVO(JSONObject tag, String division) {
		this(tag.getString("id"), tag.getString("name"), division, tag.getString("externalId"));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDivisionId() {
		return divisionId;
	}

	public void setDivisionId(String divisionId) {
		this.divisionId = divisionId;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	
	public String toString() {
		return StringUtil.getToString(this);
	}
}