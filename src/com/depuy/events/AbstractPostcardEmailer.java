package com.depuy.events;

// SMT BaseLibs
import java.sql.Connection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.depuy.events.vo.DePuyEventPostcardVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;

// SB Libs
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: EventPostcardEmailer.java<p/>
 * <b>Description: </b> pulls the leads data from the patient data 
 * (profile tables) for this postcard/mailing
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Mar 16, 2006
 ****************************************************************************/
public abstract class AbstractPostcardEmailer  {
	
	protected static Logger log = null;
	protected Map<String, Object> attributes = null;
	protected Connection dbConn = null;
	
	public AbstractPostcardEmailer(Map<String, Object> attrs, Connection conn) {
		this.attributes = attrs;
		log = Logger.getLogger(getClass());
		this.dbConn = conn;
	}
	
	/**
	 * sends the site admin an email whenever a user downloads patient data/reports
	 * @param postcard
	 * @param user
	 * @param type
	 * @param site
	 */
	public abstract void notifyAdminOfListPull(DePuyEventPostcardVO postcard, UserDataVO user, int type, SiteVO site);
	
	
	/**
	 * sends event approval request to the site administrator
	 * @param req
	 */
	public abstract void sendApprovalRequest(ActionRequest req);

	/**
	 * sends event owner notification their event/postcard was approved
	 * @param req
	 */
	public abstract void sendApprovedResponse(ActionRequest req);
	
	/**
	 * send vendor and DePuy PMs an email with postcard info/layout and critical dates
	 * @param req
	 * @param eventPostcardId
	 */
	public abstract void sendVendorSummary(ActionRequest req);
	
	
	/**
	 * send DePuy TGMs and sales rep an email with postcard info and authorization approvals
	 * @param req
	 */
	public abstract void sendPreAuthPaperwork(ActionRequest req);
	
	
	/**
	 * send certain admins a notice that a postcard was canceled
	 * @param req
	 * @param eventPostcardId
	 */
	public abstract void sendPostcardCancellation(ActionRequest req);
	
	
	protected Object getAttribute(String nm) {
		return attributes.get(nm);
	}
	
	public static AbstractPostcardEmailer newInstance(String productId, Map<String, Object> attribs, Connection dbConn) {
//		if (DePuyEventPostcardVO.PROD_ORTHOVISC.equals(productId)) {
//			return new MitekPostcardEmailer(attribs, dbConn);
//		} else {			
			return new DePuyPostcardEmailer(attribs, dbConn);
//		}
	}
}
