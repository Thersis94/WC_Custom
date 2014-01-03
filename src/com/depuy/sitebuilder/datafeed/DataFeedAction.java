package com.depuy.sitebuilder.datafeed;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.depuy.datafeed.tms.CustomerDataParser;
import com.depuy.datafeed.tms.TransactionManager;
import com.depuy.datafeed.tms.modules.CallReasonVO;
import com.depuy.datafeed.tms.modules.ChannelVO;
import com.depuy.datafeed.tms.modules.CustomerVO;
import com.depuy.datafeed.tms.modules.DataSourceVO;
import com.depuy.datafeed.tms.modules.ProfileVO;
import com.depuy.datafeed.tms.modules.ResponseVO;
import com.siliconmtn.action.AbstractActionController;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:DataFeedAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Aug 28, 2008
 * <b>Changes: </b>
 ****************************************************************************/
public class DataFeedAction extends AbstractActionController {
	
	/**
	 * 
	 */
	public DataFeedAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DataFeedAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		log.debug("Listing Customer Data");

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Updating Customer Data");
		CustomerVO customer = new CustomerVO();
		
		// Set the user information
		UserDataVO user = new UserDataVO();
		user.setData(req);
		ProfileVO profile = new ProfileVO();
		profile.setData(user.getDataMap());
		
		String name = user.getFirstName() + " " + user.getLastName();
		String msg = "You have successfully inserted a record for " + name;
		log.debug("User name: " + name);
		
		// Set the call reason VO
		CallReasonVO callReason = new CallReasonVO(req.getParameter("callReasonCode"), null);
		log.debug("call Reason: " + req.getParameter("callReasonCode") + "|" + callReason);
		
		// Set the customer base data
		customer.setProfile(profile);
		customer.setTmsCustomerId(req.getParameter("customerId"));
		customer.setCallHistoryId(req.getParameter("callHistoryId"));
		customer.setProductCode(req.getParameter("productCode"));
		customer.setSecondaryProductCode(req.getParameter("secondaryProductCode"));
		customer.setAttemptDate(Convert.formatDate(req.getParameter("attemptDate")));
		customer.setDataSource(req.getParameter("callSourceCode"));
		customer.setResultCode(req.getParameter("resultCode"));
		customer.setSelectionCode(req.getParameter("selectionCode"));
		customer.setMainHelp(req.getParameter("scriptTypeCode"));
		customer.setCallReason(callReason);
		customer.setCallTarget(req.getParameter("callTargetCode"));
		customer.setContactMethod(req.getParameter("contactTypeCode"));
		customer.setDnis(req.getParameter("dnisId"));
		customer.setTollFreeNumber(req.getParameter("tollFreeNumber"));
		customer.setOptStatus(req.getParameter("allowCommunication"));
		customer.setBirthYear(req.getParameter("birthYear"));
		customer.setReferringPath(req.getParameter("referringPath"));
		customer.setReferringSite(req.getParameter("referringSite"));
		
		log.debug("Allow Comm: " + req.getParameter("allowCommunication") + "|" + customer.getOptStatus());
		ChannelVO channel = new ChannelVO();
		channel.setBrcCode(req.getParameter("brcCode"));
		if (StringUtil.checkVal(req.getParameter("brcSignatureDate")).length() > 0) {
			channel.setBrcSignatureDate(Convert.formatDate(req.getParameter("brcSignatureDate")));
			channel.setBrcSignature(Boolean.TRUE);
		}
		customer.setChannel(channel);
		
		// Add the question responses
		List<ResponseVO> responses = new ArrayList<ResponseVO>();
		String[] codes = req.getParameterValues("questionCode");
		String[] values = req.getParameterValues("responseText");
		for (int i=0; i < codes.length; i++) {
			String key = codes[i];
			String value = values[i];
			if (StringUtil.checkVal(key).length() > 0) {
				ResponseVO response = new ResponseVO();
				response.setQuestionId(key);
				response.setResponseText(value);
				responses.add(response);
			}
		}
		
		customer.setResponses(responses);
		log.debug("Customer Data: " + customer);
		
		// Add the customer data to the db
		String encryptKey = (String)this.getAttribute(Constants.ENCRYPT_KEY);
		String geoUrl = (String)this.getAttribute("geocoderUrl");
		String dbSchema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		log.debug("CDP Call Data: " + encryptKey + "|" + geoUrl);
		CustomerDataParser cdp = new CustomerDataParser(dbConn, encryptKey, "DATA_FEED.dbo", geoUrl);
		try {
			DataSourceVO source = new DataSourceVO();
			source.setSourceId(1);
			source.setFileName("ManualFileFeed.txt");
			TransactionManager tm = new TransactionManager();
			String id = tm.createTransaction(dbConn, dbSchema, source, 1, new Date());
			cdp.storeCustomer(customer, req.getParameter("organizationId"), CustomerDataParser.MANUAL_CUSTOMER_INSERT, id);
			tm.updateTransaction(dbConn,dbSchema, id);
		} catch(DatabaseException de) {
			msg = "Error inserting record for " + name + ", " + de.getMessage();
			log.error("Unable to add customer", de);
		}
		
		// Redirect the user
		SiteBuilderUtil util = new SiteBuilderUtil();
		util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#getAttributes()
	 */
	@Override
	public Map<String, Object> getAttributes() {
		return null;
	}

}
