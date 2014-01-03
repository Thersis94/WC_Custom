package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CatalogVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 23, 2012
 ****************************************************************************/
public class CatalogVO implements Serializable {
	private static final long serialVersionUID = 4359598729416725935L;
	private String catalogNm = null;
	private Collection<CategoryVO> categories = null;
	
	public CatalogVO(Object catalogNm, Collection<CategoryVO> categories) {
		this.setCatalogNm(StringUtil.checkVal(catalogNm));
		this.setCategories(categories);
	}


	public String getCatalogNm() {
		return catalogNm;
	}


	public void setCatalogNm(String catalogNm) {
		this.catalogNm = catalogNm;
	}


	public Collection<CategoryVO> getCategories() {
		return categories;
	}


	public void setCategories(Collection<CategoryVO> categories) {
		this.categories = categories;
	}
	
	public String getEncodedNm() {
		try {
			return URLEncoder.encode(catalogNm, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return catalogNm;
		}
	}
}
