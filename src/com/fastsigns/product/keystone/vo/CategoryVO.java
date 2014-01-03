package com.fastsigns.product.keystone.vo;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CategoryVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 23, 2012
 ****************************************************************************/
public class CategoryVO implements Serializable {
	private static final long serialVersionUID = 4359598729416725935L;
	public static final String DEFAULT_CATEGORY_HOOK = ":smt_general_catalog:"; //comes from Keystone
	
	private String categoryNm = null;
	private Collection<KeystoneProductVO> products = null;
	
	
	public CategoryVO(Object categoryNm, Collection<KeystoneProductVO> products) {
		this.setCategoryNm(StringUtil.checkVal(categoryNm));
		this.setProducts(products);
	}


	public String getCategoryNm() {
		return categoryNm;
	}


	public void setCategoryNm(String categoryNm) {
		//double spaces cause issues when comparing URL parmeters in JSTL.  Used in catalog/category View.
		this.categoryNm = StringUtil.replace(categoryNm, "  "," ");
	}


	public Collection<KeystoneProductVO> getProducts() {
		return products;
	}


	public void setProducts(Collection<KeystoneProductVO> products) {
		this.products = products;
	}
	
	public String getEncodedNm() {
		if (DEFAULT_CATEGORY_HOOK.equals(categoryNm)) return "";
		try {
			return URLEncoder.encode(categoryNm, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return categoryNm;
		}
	}
}
