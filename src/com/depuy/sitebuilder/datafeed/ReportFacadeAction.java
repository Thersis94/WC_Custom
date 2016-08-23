package com.depuy.sitebuilder.datafeed;

// JDK 1.6.0
import java.util.HashMap;
import java.util.Map;






// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.SMTClassLoader;
import com.siliconmtn.util.StringUtil;

// SiteBuilder Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ReportFacadeAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 9, 2007
 ****************************************************************************/
public class ReportFacadeAction extends SBActionAdapter {
	public static final String DATABASE_SCHEMA = "dataFeedSchema";
	public static final String TRANSACTION_REPORT = "TransactionReport";
	public static final String LOCATION_REPORT = "LocationReport";
	public static final String STATE_LOCATION_REPORT = "StateLocationReport";
	public static final String QUALIFIED_CITY_REPORT = "QualifiedByCityReport";
	public static final String DAILY_SOURCE_REPORT = "DailySourceReport";
	public static final String LEAD_SOURCE_REPORT = "LeadSourceReport";
	public static final String TOLLFREE_SOURCE_REPORT = "TollFreeSourceReport";
	public static final String QUALIFIED_LEAD_REPORT = "QualifiedLeadReport";
	public static final String CHANNEL_REPORT = "ChannelReport";
	public static final String REGISTRATION_REPORT = "RegistrationReport";
	public static final String REGISTRATION_DATA_REPORT = "RegistrationDataReport";
	public static final String FULFILLMENT_REPORT = "FulfillmentReport";
	public static final String SHARE_STORY_REPORT = "ShareMyStoryReport";
	public static final String PEDO_KIT_REPORT = "PedoKitReport";

	protected static final String DF_SCHEMA = "DATA_FEED.dbo.";
	private Map<String, String> reportType = null;
	private Map<String, String> binaryReport = null;

	/**
	 * 
	 */
	public ReportFacadeAction() {
		super();
		setVals();
	}

	/**
	 * @param arg0
	 */
	public ReportFacadeAction(ActionInitVO arg0) {
		super(arg0);
		setVals();
	}

	/**
	 * 
	 */
	private void setVals() {
		reportType = new HashMap<>();
		reportType.put(TRANSACTION_REPORT, "com.depuy.sitebuilder.datafeed.TransactionReport");
		reportType.put(LOCATION_REPORT, "com.depuy.sitebuilder.datafeed.LocationReport");
		reportType.put(STATE_LOCATION_REPORT, "com.depuy.sitebuilder.datafeed.StateLocationReport");
		reportType.put(QUALIFIED_CITY_REPORT, "com.depuy.sitebuilder.datafeed.QualifiedByCityReport");
		reportType.put(DAILY_SOURCE_REPORT, "com.depuy.sitebuilder.datafeed.DailySourceReport");
		reportType.put(TOLLFREE_SOURCE_REPORT, "com.depuy.sitebuilder.datafeed.TollFreeSourceReport");
		reportType.put(QUALIFIED_LEAD_REPORT, "com.depuy.sitebuilder.datafeed.QualifiedReport");
		reportType.put(CHANNEL_REPORT, "com.depuy.sitebuilder.datafeed.ChannelReport");
		reportType.put(LEAD_SOURCE_REPORT, "com.depuy.sitebuilder.datafeed.LeadSourceReport");
		reportType.put(REGISTRATION_REPORT, "com.depuy.sitebuilder.datafeed.RegistrationReport");
		reportType.put(REGISTRATION_DATA_REPORT, "com.depuy.sitebuilder.datafeed.RegistrationDataReport");
		reportType.put(FULFILLMENT_REPORT, "com.depuy.sitebuilder.datafeed.FulfillmentReport");
		reportType.put(SHARE_STORY_REPORT, "com.depuy.sitebuilder.datafeed.ShareMyStoryReport");
		reportType.put(PEDO_KIT_REPORT, "com.depuy.sitebuilder.datafeed.PedoKitReport");

		binaryReport = new HashMap<>();
		binaryReport.put(TRANSACTION_REPORT, "com.depuy.sitebuilder.datafeed.TransactionReportVO");
		binaryReport.put(LOCATION_REPORT, "com.depuy.sitebuilder.datafeed.LocationReportVO");
		binaryReport.put(STATE_LOCATION_REPORT, "com.depuy.sitebuilder.datafeed.StateLocationReportVO");
		binaryReport.put(QUALIFIED_CITY_REPORT, "com.depuy.sitebuilder.datafeed.QualifiedByCityReportVO");
		binaryReport.put(DAILY_SOURCE_REPORT, "com.depuy.sitebuilder.datafeed.DailySourceReportVO");
		binaryReport.put(TOLLFREE_SOURCE_REPORT, "com.depuy.sitebuilder.datafeed.TollFreeSourceReportVO");
		binaryReport.put(QUALIFIED_LEAD_REPORT, "com.depuy.sitebuilder.datafeed.QualifiedReportVO");
		binaryReport.put(CHANNEL_REPORT, "com.depuy.sitebuilder.datafeed.ChannelReportVO");
		binaryReport.put(LEAD_SOURCE_REPORT, "com.depuy.sitebuilder.datafeed.LeadSourceReportVO");
		binaryReport.put(REGISTRATION_REPORT, "com.depuy.sitebuilder.datafeed.RegistrationReportVO");
		binaryReport.put(REGISTRATION_DATA_REPORT, "com.depuy.sitebuilder.datafeed.RegistrationDataReportVO");
		binaryReport.put(FULFILLMENT_REPORT, "com.depuy.sitebuilder.datafeed.FulfillmentReportVO");
		binaryReport.put(SHARE_STORY_REPORT, "com.depuy.sitebuilder.datafeed.ShareMyStoryReportVO");
		binaryReport.put(PEDO_KIT_REPORT, "com.depuy.sitebuilder.datafeed.PedoKitReportVO");


	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		// Get Params
		String type = StringUtil.checkVal(req.getParameter("dfReportType"));
		log.info("Getting Data Feed Report for: " + type);
		Object o = attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		log.info("Class Type: " + o);
		ModuleVO mod = (ModuleVO) o;
		if (type.length() == 0) return;
		Report rep = null;

		try {

			// Call the Appropriate action for the Contact Data
			String path = reportType.get(type);
			if (path == null) throw new ActionException("Invalid Report Parameter");
			log.info("path " + path);

			// Add the report data to the collection
			SMTClassLoader scl = new SMTClassLoader();
			rep = (Report)scl.getClassInstance(path);


			rep.setDatabaseConnection(dbConn);
			rep.setAttibutes(attributes);
			Object data = rep.retrieveReport(req);

			if ("excel".equals(req.getParameter("reportType"))){

				req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
				req.setAttribute(Constants.BINARY_DOCUMENT, (AbstractDataFeedReportVO)getBinaryReport(req,type,scl,data));

			}else {
				mod.setActionData(data);
				log.info("Finished Retrieving report for: " + type);
				req.setAttribute(Constants.REDIRECT_DATATOOL, Boolean.TRUE);
				this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
			}

		} catch(Exception e) {
			log.error("Unable to retrieve " + type, e);
			mod.setError("Unable to retrieve " + type, e);
		}

	}


	/**
	 * @param scl 
	 * @param type 
	 * @param req 
	 * @param data 
	 * @return
	 * @throws ActionException 
	 * @throws ClassNotFoundException 
	 * @throws InvalidDataException 
	 * @throws DatabaseException 
	 */
	protected AbstractDataFeedReportVO getBinaryReport(SMTServletRequest req, String type, SMTClassLoader scl, Object data) throws ActionException, ClassNotFoundException {
		AbstractDataFeedReportVO rpt = null;

		String binaryPath = binaryReport.get(type);
		if (binaryPath == null) throw new ActionException("Invalid Report Parameter");
		log.info("binary report path " + binaryPath);

		rpt = (AbstractDataFeedReportVO)scl.getClassInstance(binaryPath);

		rpt.setRequestData(req);

		rpt.setData(data);
		return rpt;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest arg0) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest arg0) throws ActionException {
	}

}
