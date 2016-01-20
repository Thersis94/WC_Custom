package com.depuysynthes.huddle;

import java.io.Serializable;

import com.siliconmtn.util.SMTSerializer;

/****************************************************************************
 * <b>Title</b>: ShareVO.java<p/>
 * <b>Description: represents an email-a-friend product asset.  The user will build a collection of these,
 * then email the entire list.  Like a shopping cart.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 19, 2016
 ****************************************************************************/
public class ShareVO implements Serializable {
	private static final long serialVersionUID = 4219252389256722482L;
	private String id;
	private String title;
	private String type;
	
	public String getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public String getType() {
		return type;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String toString() { return SMTSerializer.toJson(this); }
}