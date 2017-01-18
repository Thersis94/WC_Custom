package com.orthopediatrics.action;

// JDK 1.6.x
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTClassLoader;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB II Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: ERPReportAction.java <p/>
 * <b>Project</b>: SB_Ortho <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 1, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ERPReportAction extends SBActionAdapter {
	
	public static final String DEFAULT_RS_FORMAT_CODE = "XML";
	public static final String DEFAULT_REPORT_TYPE = "html";
	public static final Integer ROLE_ORDER_AVP = 60;
	private boolean success = false;

	/**
	 * 
	 */
	public ERPReportAction() {
	}

	/**
	 * @param actionInit
	 */
	public ERPReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	@Override
	public void copy(ActionRequest req) throws ActionException {
		super.copy(req);	
		RecordDuplicatorUtility rdu = new RecordDuplicatorUtility(attributes, dbConn, "Web_Crescendo_SB_Custom.dbo.op_srs_field", "ACTION_ID", true);
		rdu.addWhereClause(DB_ACTION_ID, req.getParameter(SB_ACTION_ID));
		rdu.setPrimaryKeyNm("FIELD_ID");
		rdu.copy();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	public void delete(ActionRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		if (! Convert.formatBoolean(req.getParameter("facadeType"))) {
			super.delete(req);
		} else {
			try {
				deleteField(req.getParameter("fieldId"));
			} catch (SQLException e) {
				log.error("Unable to delete SRS Field", e);
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
		}
		
		// Perform the redirect
		new SiteBuilderUtil().adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(ActionRequest req) throws ActionException {
		try {
			// Get the field data
			SRSReportVO data = getFieldMap(actionInit.getActionId());

			// Build the url
			String url = this.buildUrl(data, req);
			log.debug("url: " + url);
			
			// Retrieve the data from the proxy
			String reportType = StringUtil.checkVal(req.getParameter("reportType"), DEFAULT_REPORT_TYPE);
			
			byte[] report = this.retrieveReportData(req, url, reportType);
			log.debug("reportData size: " + report.length);
			
			// if the report retrieval failed, reset the report type to HTML
			if (! success) reportType = DEFAULT_REPORT_TYPE;
			
			// Create the report
			SRSReport srs = new SRSReport();
			srs.setData(report);
			srs.setFileName(data.getActionName() + "." + reportType);
			
			// Set the report tpo be displayed outside the web site
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, srs);
			
			log.debug("Content Type: " + srs.getContentType());
		} catch (Exception e) {
			log.error("Unable to generate OP SRS Report", e);
		}
	}
	
	/**
	 * Calls the proxy server and returns the data
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	/*public byte[] retrieveReportData(String url, String reportType) throws IOException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] b = conn.retrieveData(url);
		return (parseData(b, reportType));
	} */
	public byte[] retrieveReportData(ActionRequest req, String url, String reportType) throws IOException {
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		byte[] b = null;
		success = true;
		try {
			b = conn.retrieveData(url);
		} catch(Exception e) {
			Map<String, String> hm = conn.getHeaderMap();
			for (Iterator<String> iter = hm.keySet().iterator(); iter.hasNext(); ) {
				String key = iter.next();
				log.debug("Header: " + key + "|" + hm.get(key));
			}
			b = "<h2>Unable to retrieve report.</h2>".getBytes();
			log.error("Error retrieving report data, ", e);
			success = false;
			//throw new IOException(e.getMessage(), e);
		}
		/*
		if (success) {
			log.debug("report size before parsing: " + b.length);
			return (parseData(req, b, reportType));
		} else {
			return b;
		} */
		return b;
	}
	
	/**
	 * Calls the parser and parses the report data based on reportType.
	 * @param req
	 * @param b
	 * @param reportType
	 * @return
	 */
	@SuppressWarnings("unused")
	private byte[] parseData(ActionRequest req, byte[] b, String reportType) {
		log.debug("parsing data...for report type: " + reportType);
		ERPReportParser parser = new ERPReportParser(req);
		parser.setRequest(req);
   		parser.setReportData(b);
   		parser.setReportFormat(reportType);
		try {
			b = parser.parseReportData();
		} catch (Exception e) {
			log.error("Error parsing report with format type of " + reportType, e);
			b = "<h2>Unable to format the report data.</h2>".getBytes();
			success = false;
		}
    	return b;    	
	}
	
	/**
	 * 
	 * @param data
	 * @param req
	 * @return
	 */
	public String buildUrl(SRSReportVO data, ActionRequest req) {
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		SalesRepVO rep = (SalesRepVO)user.getUserExtendedInfo();
		log.debug("Rep: " + rep);
		
		StringBuilder rp = new StringBuilder();
		List<SRSFieldVO> fields = data.getFields();
		for (int i=0; i < fields.size(); i++) {
			SRSFieldVO f = fields.get(i);
			if (i > 0) rp.append("&");
			rp.append(f.getFieldName()).append("=");
			if (f.getFieldTypeId() == 1) { // field type 1 is the 'user' type
				if (f.getFieldName().toLowerCase().contains("date")) {
					// if a 'date' field, make sure the date format is dashes
					if (StringUtil.checkVal(req.getParameter(f.getFieldName())).length() > 0) {
						rp.append((req.getParameter(f.getFieldName())).replace('/', '-'));
					}
				} else {
					// otherwise just append the value
					rp.append(req.getParameter(f.getFieldName()));
				}
			}
			else { // field type 2 or 3
				if ("${userId}".equalsIgnoreCase(f.getFieldValue())) {
					rp.append(rep.getLoginId());
				} else if ("${classId}".equalsIgnoreCase(f.getFieldValue())) {
					rp.append(rep.getClassId());
				} else if ("${territoryId}".equalsIgnoreCase(f.getFieldValue())) {
					if (role.getRoleLevel() < ROLE_ORDER_AVP) {
						// append rep's territory ID
						rp.append(rep.getTerritoryId());
					} else {
						// for AVP role and above, append territory ID from the request param
						String tid = StringUtil.checkVal(req.getParameter("territoryId"));
						if (tid.equalsIgnoreCase("all")) {
							// append nothing
						} else {
							rp.append(tid);
						}
					}
				} else if ("${regionId}".equalsIgnoreCase(f.getFieldValue())) {
					if (role.getRoleLevel() < ROLE_ORDER_AVP) {
						// append region ID
						rp.append(rep.getRegionId());
					} else {
						// for AVP role and above, append region ID from the request param
						rp.append(StringUtil.checkVal(req.getParameter("regionId")));
					}
				} else if ("${productFamily}".equalsIgnoreCase(f.getFieldValue())) {
					rp.append(StringUtil.checkVal(req.getParameter("productFamily")));
				} else {
					// append the field value
					rp.append(f.getFieldValue());
				}
			}
		}
		
		// append the RS format
		log.debug("report parameters: " + rp.toString());

		// Encode the parameters
		String baseUrl = attributes.get("opSrsReportProxy") + "?"; 
		try {
			String repPath = StringUtil.checkVal(data.getAttribute(SBModuleVO.ATTRIBUTE_1));
			String path = "path=" + URLEncoder.encode(repPath, "UTF-8");
			baseUrl += path + "&params=" + URLEncoder.encode(rp.toString(), "UTF-8");
			//baseUrl += path + "&params=" + URLEncoder.encode(testrp.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		log.debug("RP URL: " + baseUrl);
		return baseUrl;
	}
	
	/**
	 * Deletes a field from the OP_ SRS Field Action
	 * @param fieldId
	 * @throws SQLException
	 */
	public void deleteField(String fieldId) throws SQLException {
		String cds = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String s = "delete from " + cds + "op_srs_field where field_id = ? ";
		log.debug("OP Field Del SQL: " + s + "|" + fieldId);
		PreparedStatement ps = dbConn.prepareStatement(s);
		ps.setString(1, fieldId);
		ps.executeUpdate();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		try {
			SRSReportVO data = getFieldMap(actionInit.getActionId());
			this.putModuleData(data, data.getFields().size(), false);
		} catch (Exception e) {
			log.error("Unable to retrieve OP SRS Field Data", e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(ActionRequest req) throws ActionException {
		log.debug("Updating info");
		if (! Convert.formatBoolean(req.getParameter("facadeType"))) {
			super.update(req);
			return;
		} 
		
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		try {
			updateField(req);
		} catch(Exception e) {
			log.error("Unable to add OP SRS Field", e);
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		new SiteBuilderUtil().adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}
	
	/**
	 * 
	 * @param req
	 * @throws SQLException
	 */
	public void updateField(ActionRequest req) throws SQLException {
		String cds = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder s = new StringBuilder();
		s.append("insert into ").append(cds).append("op_srs_field (field_id, ");
		s.append("data_type_id, field_type_id, action_id, field_nm, field_label_txt, ");
		s.append("field_value_txt, create_dt) values(?,?,?,?,?,?,?,?)");
		log.debug("SRS Field Update SQL: " + s);
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, new UUIDGenerator().getUUID());
		ps.setString(2, req.getParameter("dataTypeId"));
		ps.setInt(3, Convert.formatInteger(req.getParameter("fieldTypeId")));
		ps.setString(4, req.getParameter("sbActionId"));
		ps.setString(5, req.getParameter("fieldName"));
		ps.setString(6, req.getParameter("fieldLabel"));
		ps.setString(7, req.getParameter("fieldValue"));
		ps.setTimestamp(8, Convert.getCurrentTimestamp());
		ps.executeUpdate();
		ps.close();
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(ActionRequest req) throws ActionException {
		log.debug("Listing SRS Fields");
		super.retrieve(req);
		
		if (Convert.formatBoolean(req.getParameter("facadeType"))) {
			try {
				SRSReportVO data = getFieldMap(req.getParameter("sbActionId"));
				this.putModuleData(data, data.getFields().size(), true);
			} catch (Exception e) {
				log.error("Unable to retrieve OP SRS Field Data", e);
			}
		}
	}
	
	/**
	 * Retrieves a list of fields that are used to build the URL
	 * @param actionId
	 * @return
	 * @throws SQLException
	 */
	public SRSReportVO getFieldMap(String actionId) throws SQLException {
		String cdb = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("select * from sb_action a left outer join ").append(cdb);
		s.append("op_srs_field b on a.action_id = b.action_id ");
		s.append("left outer join ").append(cdb).append("op_srs_field_type c ");
		s.append("on b.field_type_id = c.field_type_id ");
		s.append("left outer join ").append(cdb).append("op_srs_data_type d ");
		s.append("on b.data_type_id = d.data_type_id ");
		s.append("where a.action_id = ? ");
		s.append("order by field_type_nm, field_nm");
		log.debug("OP SRS Field SQL: " + s.toString() + "|" + actionId);
		
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		ps.setString(1, actionId);
		ResultSet rs = ps.executeQuery();
		SRSReportVO  data = new SRSReportVO();
		
		for (int i = 0; rs.next(); i++) {
			if (i == 0) data.assignData(rs);
			else data.addField(new SRSFieldVO(rs));
		}
		
		ps.close();
		
		return data;
	}
	
	/**
	 * Loads the custom parser.
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unused")
	private Object loadParser() throws ClassNotFoundException {	
    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
    	String parserClass = StringUtil.checkVal(mod.getAttribute(ModuleVO.ATTRIBUTE_2));
    	log.debug("parserClass name is: " + parserClass);
    	if (parserClass.length() == 0) return null;
		
		SMTClassLoader smt = new SMTClassLoader();
		Object parser = smt.getClassInstance(parserClass);
		log.debug("loaded parser class: " + parserClass);
		return parser;
	}
	
}
