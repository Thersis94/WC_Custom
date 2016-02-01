package com.depuysynthes.huddle;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.SMTSerializer;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.cms.DocumentAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EmailFriendAction.java<p/>
 * <b>Description: Decorates WC-core EmailAFriend with support for the 'cart' the
 * user is able to build in the UI (w/multiple URLs).</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 18, 2016
 ****************************************************************************/
public class EmailFriendAction extends SimpleActionAdapter {

	public EmailFriendAction() {
		super();
	}

	public EmailFriendAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		//if the request is for a product's assets, call the HuddleProductAction to load
		//everything we need to know about this particular Product
		if (req.hasParameter("productId")) {
			req.setParameter("runDetail", "true");
			req.setParameter("skipContacts", "true");
			 req.setParameter("reqParam_1", req.getParameter("productId"));
			HuddleProductAction hpa = new HuddleProductAction(actionInit);
			hpa.setDBConnection(dbConn);
			hpa.setAttributes(getAttributes());
			hpa.retrieve(req);
			//we can go straight to View with what's already been put into mod.getActionData()
		}
		
		// parse the cookie data
		loadSharePOJOs(req);
	}
	
	
	/**
	 * turns the javascript Array[JSON] stored in a cookie into POJOs so we can 
	 * use them in the View
	 * @param req
	 */
	private void loadSharePOJOs(SMTServletRequest req) {
		Cookie prods = req.getCookie(HuddleUtils.PROD_SHARE_COOKIE);
		if  (prods == null || prods.getValue() == null) return;
		
		try {
			String shareStr = new StringEncoder().decode(prods.getValue());
			ShareVO[] shares = (ShareVO[]) SMTSerializer.fromJson(shareStr, ShareVO[].class);
			Map<String, ShareVO> shareMap = new HashMap<>(shares.length);
			for (ShareVO sh : shares)
				shareMap.put(sh.getId(), sh);
			
			req.setAttribute("shareMap", shareMap);
		} catch (Exception e) {
			log.warn("could not parse ShareVOs array", e);
		}
	}
	
	
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("**********************");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		
		String[] addys = req.getParameter("rcptEml").split(",");
		req.setParameter("rcptEml", addys, true);
		
		if (req.hasParameter("isProducts")) {
			// parse the cookie data
			loadSharePOJOs(req);
			
			// build the alternate message body w/multiple URLs
			req.setAttribute(com.smt.sitebuilder.action.tools.EmailFriendAction.EMAIL_MSG_BODY, buildProductEmailBody(req));
		}
		
		actionInit.setActionId("" + mod.getAttribute(SBModuleVO.ATTRIBUTE_2));
		com.smt.sitebuilder.action.tools.EmailFriendAction efa = new com.smt.sitebuilder.action.tools.EmailFriendAction(actionInit);
		efa.setDBConnection(dbConn);
		efa.setAttributes(getAttributes());
		efa.build(req);
	}

	
	/**
	 * builds the body of the email message for product Assets, which needs to 
	 * loop around the 'cart' the user has built.
	 * @param req
	 * @return
	 */
	private String buildProductEmailBody(SMTServletRequest req) {
		StringBuilder sb = new StringBuilder(500);
		sb.append("<p>I thought you may be interested in the attached information from DePuy Synthes.</p>\n");
		sb.append("<ul>");
		String url;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		//String assetBase = site.getFullSiteAlias() + HuddleUtils.ASSET_PG_ALIAS + StringUtil.checkVal(getAttribute(Constants.QS_PATH));
		String mbBase = site.getFullSiteAlias() + HuddleUtils.MEDIABIN_REDIR_URL;
		@SuppressWarnings("unchecked")
		Map<String, ShareVO> shareMap = (Map<String, ShareVO>) req.getAttribute("shareMap");
		for (ShareVO vo : shareMap.values()) {
			if (vo.getId().startsWith(DocumentAction.SOLR_PREFIX)) {
				url = site.getFullSiteAlias() + vo.getUrl();
			//} else if ("video".equals(vo.getType()) || "podcast".equals(vo.getType())) {
			//	url = assetBase + vo.getId();
			} else {
				url = mbBase + vo.getId();
			}
			sb.append("<li><a href=\"").append(url).append("\">").append(vo.getTitle()).append("</a></li>\n");
		}
		sb.append("</ul>");
		sb.append("<br><br>Please do not reply to this auto-generated email.\n");
		log.debug("msg=" + sb);
		return sb.toString();
	}
}