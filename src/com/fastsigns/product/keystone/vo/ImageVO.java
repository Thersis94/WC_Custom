/**
 * 
 */
package com.fastsigns.product.keystone.vo;

import java.io.Serializable;

/****************************************************************************
 * <b>Title</b>: ImageVO.java<p/>
 * <b>Description: Images tied to a product.  Used at the ProductDetail Level.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 21, 2012
 ****************************************************************************/
public class ImageVO implements Serializable, Cloneable {
	private static final long serialVersionUID = -6697112278546959621L;
	
	private String imageThumbUrl = null;
	private String imageUrl = null;
	
	
	public String getImageThumbUrl() {
		return imageThumbUrl;
	}
	public void setImageThumbUrl(String imageThumbUrl) {
		this.imageThumbUrl = imageThumbUrl;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	public ImageVO clone() throws CloneNotSupportedException {
		return (ImageVO) super.clone();
	}

}
