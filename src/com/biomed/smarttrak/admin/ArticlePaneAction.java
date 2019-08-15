package com.biomed.smarttrak.admin;

import java.io.IOException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.smt.sitebuilder.action.SBActionAdapter;

public class ArticlePaneAction extends SBActionAdapter {
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
			conn.setConnectionTimeout(30000);
			conn.setFollowRedirects(true);
			String url = StringEncoder.urlDecode(req.getParameter("articleUrl"));
			if (url.indexOf("http") == -1)
				url = "https://" + url;
			
			byte[] data = conn.retrieveData(url);
			
			String html = new String(data);
			String baseDomain;
			int lastDot = url.lastIndexOf('.');
			int endIndex = url.indexOf('/', lastDot);
			if (endIndex == -1)
				endIndex = url.indexOf('?', lastDot);
			if (endIndex == -1) {
				baseDomain = url;
			} else {
				baseDomain = url.substring(0,  endIndex);
			}
				
 
			html = html.replaceAll("<script[\\s\\S]*?>[\\s\\S]*?<\\/script>", "");
			html = html.replaceAll("src=\"\\/", "src=\"" + baseDomain + "/");
			html = html.replaceAll("src='\\/", "src='" + baseDomain + "/");
			html = html.replaceAll("href=\"\\/", "href=\"" + baseDomain + "/");
			html = html.replaceAll("href='\\/", "href='" + baseDomain + "/");
			
			
			super.putModuleData(html);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
