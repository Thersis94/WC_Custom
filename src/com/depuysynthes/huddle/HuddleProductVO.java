package com.depuysynthes.huddle;

import java.sql.ResultSet;
import java.util.Collection;

import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: HuddleProductVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 15, 2016
 ****************************************************************************/
public class HuddleProductVO extends ProductVO {

	private static final long serialVersionUID = 40058887748405283L;
	
	private Collection<ProductContactVO> contacts;

	public HuddleProductVO() {
		super();
	}

	public HuddleProductVO(SMTServletRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public HuddleProductVO(ResultSet rs) {
		super(rs);
	}
	
	public void setContacts(Collection<ProductContactVO> contacts) {
		this.contacts = contacts;
	}
	
	public Collection<ProductContactVO> getContacts() {
		return contacts;
	}
	
	public int getContactCnt() {
		return (contacts != null) ? contacts.size() : 0;
	}

}
