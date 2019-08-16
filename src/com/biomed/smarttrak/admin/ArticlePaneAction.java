package com.biomed.smarttrak.admin;

import java.io.IOException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: AdminControllerAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Controller for the SMARTTRAK Admin website (/manage).  
 * Loads and invokes all internal functions after permissions are validated.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Eric Damschroder
 * @version 1.0
 * @since Aug 16, 2019
 ****************************************************************************/

public class ArticlePaneAction extends SBActionAdapter {
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			SMTHttpConnectionManager conn = buildNewConnection();
			String url = StringEncoder.urlDecode(req.getParameter("articleUrl"));
			if (url.indexOf("http") == -1)
				url = "https://" + url;
			
			byte[] data = conn.retrieveData(url);
			String baseDomain = getBaseDomain(url);
			
			super.putModuleData(prepareHtml(data, baseDomain));
		} catch (IOException e) {
			throw new ActionException(e);
		}
		
	}
	
	
	/**
	 * Remove javascript and turn all relative links into absolute links.
	 * @param data
	 * @param baseDomain
	 * @return
	 */
	private String prepareHtml(byte[] data, String baseDomain) {
		String html = new String(data);
		 
		html = html.replaceAll("<script[\\s\\S]*?>[\\s\\S]*?<\\/script>", "");
		html = html.replaceAll("src=\"\\/", "src=\"" + baseDomain + "/");
		html = html.replaceAll("src='\\/", "src='" + baseDomain + "/");
		html = html.replaceAll("href=\"\\/", "href=\"" + baseDomain + "/");
		html = html.replaceAll("href='\\/", "href='" + baseDomain + "/");
		
		return html;
	}


	/**
	 * Get the base domain from the supplied url.
	 * @param url
	 * @return
	 */
	private String getBaseDomain(String url) {
		int lastDot = url.lastIndexOf('.');
		int endIndex = url.indexOf('/', lastDot);
		if (endIndex == -1)
			endIndex = url.indexOf('?', lastDot);
		if (endIndex == -1) {
			return url;
		} else {
			return url.substring(0,  endIndex);
		}
	}

	
	/**
	 * Based on passed cPage, instantiate the appropriate class and return.
	 * @return
	 */
	private SMTHttpConnectionManager buildNewConnection() {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager(true);
		conn.addRequestHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36");
		conn.addRequestHeader("Accept", "*/*");
		conn.addRequestHeader("Accept-Language", "en-US,en;q=0.8");
		conn.addRequestHeader("Cache-Control" , "no-cache");
		conn.addRequestHeader("Connection", "keep-alive");
		conn.addRequestHeader("Pragma" , "no-cache");
		conn.setConnectionTimeout(30000);
		conn.setFollowRedirects(true);
		
		return conn;
	}

}
