package com.depuysynthes.huddle;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: HuddleProductVO.java<p/>
 * <b>Description: Extends the core ProductVO with additional variables for Huddle</b> 
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
	
	private Collection<String> images;
	
	private Map<Integer, String> resourceMap = new HashMap<>();

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

	public Collection<String> getImages() {
		return images;
	}

	public void setImages(Collection<String> images) {
		this.images = images;
	}
	
	public Map<String, Object> getResources() {
		Map<String, Object> vals = new LinkedHashMap<>();
		for (Integer i : resourceMap.keySet()) {
			String key = resourceMap.get(i);
			vals.put(key, super.getProdAttributes().get(key));
		}
		return vals;
	}
	
	public void addResource(int order, String key, Object value) {
		super.addProdAttribute(key, value);
		resourceMap.put(order, key);
	}

}
