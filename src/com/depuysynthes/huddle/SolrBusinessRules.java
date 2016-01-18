package com.depuysynthes.huddle;

/****************************************************************************
 * <b>Title</b>: SolrBusinessRules.java<p/>
 * <b>Description: leverage the rules we have for DSI, customized for Huddle.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 25, 2015
 ****************************************************************************/
public class SolrBusinessRules extends com.depuysynthesinst.SolrBusinessRules {

	public SolrBusinessRules() {
		super();
	}
	
	
	public String getThumbnailPath() {
		String trackingNo = super.getThumbnailImg();
		if (trackingNo == null || trackingNo.length() < 3) return trackingNo;
		
		return "/binary/org/DPY_SYN_HUDDLE/mediabin/" + trackingNo.substring(0, 3) + "/" + trackingNo + ".jpg";
	}

}
