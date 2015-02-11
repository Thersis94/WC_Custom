package com.depuy.events_v2.vo.report;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;

/****************************************************************************
 * <b>Title</b>: CustomReportVO.java<p/>
 * <b>Description: Report with custom fields </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Oct 20, 2014
 ****************************************************************************/
public class CustomReportVO extends AbstractSBReportVO {

	private static final Map<String, String> typeMap = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("ESEM", "Patient");
			put("CFSEM", "Co-Funded");
			put("CFSEM50", "Co-Funded 50/50");
			put("CFSEM25", "Co-Funded 50/25/25");
			put("CPSEM", "Physician");
			put("HSEM", "Hospital Sponsored");
		}
	};

	public static enum FieldList {
		JOINT_FLG("joint_flg", "java.lang.String","Joint","JOINT_FLG"),
		SEMINAR_TYPE_FLG("seminar_type_flg", "java.lang.String","Seminar Type","SEMINAR_TYPE_FLG"),
		SEMINAR_CODE_FLG("seminar_code_flg", "java.lang.String", "Seminar Code","SEMINAR_CODE_FLG"),
		COORDINATOR_FLG("coordinator_flg", "java.lang.String", "Seminar Coordinator","COORDINATOR_FLG"),
		STATUS_FLG("status_flg", "java.lang.Integer","Seminar Status","STATUS_FLG"),
		START_DATE_FLG("start_date_flg", "java,util.Date", "Seminar Date","START_DATE_FLG"),
		TIME_FLG("time_flg", "java.util.Date", "Seminar Time","TIME_FLG"),
		SPEAKER_FLG("speaker_flg", "java.lang.String","Seminar Speaker","SPEAKER_FLG"),
		RSVP_COUNT_FLG("rsvp_count_flg", "java.lang.Integer", "RSVP Count","RSVP_TOTAL_FLG"),
		ATTENDEE_COUNT_FLG("attendee_count_flg", "java.lang.Integer", "Attendee Count","ATTENDEE_TOTAL_FLG"),
		//OPT_IN_FLG("opt_in_flg", "java.lang.Integer","Opt-In","OPT_IN_FLG"),
		LOCATION_NM_FLG("location_nm_flg", "java.lang.String","Location Name","LOCATION_FLG"),
		CITY_NM_FLG("city_nm_flg", "java.lang.String","City","CITY_FLG"),
		STATE_CD_FLG("state_cd_flg", "java.lang.String","State","STATE_FLG"),
		VENUE_COST_FLG("venue_cost_flg", "java.lang.Double","Venue Cost","VENUE_COST_FLG"),
		REFRESHMENT_COST_FLG("refreshment_cost_flg", "java.lang.Double", "Refreshment Cost","REFRESHMENT_COST_FLG"),
		POSTCARD_DT_FLG("postcard_dt_flg", "java.util.Date", "Postcard Mail Date","POSTCARD_DATE_FLG"),
		NEWSPAPER_NM_FLG("newspaper_nm_flg", "java.lang.String","Newspaper Name","NEWSPAPER_FLG"),
		AD_DT_FLG("ad_dt_flg", "java.util.Date", "Ad Date","AD_DATE_FLG"),
		AD_COST_FLG("ad_cost_flg", "java,lang.Date", "Total Ad Cost","AD_COST_FLG"),
		TERRITORY_COST_FLG("territory_cost_flg", "java.lang.Double", "Cost to Territory","TERRITORY_COST_FLG"),
		SURGEON_COST_FLG("surgeon_cost_flg", "java.lang.Double","Cost to Surgeon","SURGEON_COST_FLG"),
		HOSPITAL_COST_FLG("hospital_cost_flg", "java.lang.Double", "Cost to Hospital","HOSPITAL_COST_FLG"),
		UPFRONT_FEE_FLG("upfront_fee_flg", "java.lang.Integer", "$200 Upfront Fee", "UPFRONT_FEE_FLG"),
		TERRITORY_NO("territory_no","java.lang.Integer","Territory#","TERRITORY_FLG"),
		POSTCARD_COUNT_NO("postcard_count_no","java.lang.Integer","Postcard/Invitation Qnty","POSTCARD_TOTAL_FLG"),
		//INVITATION_COUNT_NO("invitation_count_no","java.lang.Integer","Invitation No","INVITATION_TOTAL_FLG"),
		POSTCARD_COST_NO("postcard_cost_no","java.lang.Double","Postcard/Invitation Cost","POSTCARD_COST_FLG");
		//INVITATION_COST_NO("invitation_cost_no","java.lang.Double","Total Invitation Cost","INVITATION_COST_FLG");

		private final String name;
		private final String className;
		private final String reportLabel;
		private final String dbName;
		
		//enum's Constructor
		FieldList(String name, String className, String reportLabel, String dbName) {
			this.name=name;
			this.className=className;
			this.reportLabel=reportLabel;
			this.dbName = dbName;
		}
		
		/**
		 * @return The name of the input field.
		 */
		public String getFieldName() { return name; }
		/**
		 * @return The class name for the field's data type.
		 */
		public String getClassName() { return className; }
		/**
		 * @return The label used for this field in the report header.
		 */
		public String getReportLabel() { return reportLabel; }
		/**
		 * @return The name of the DEPUY_EVENT_REPORT column corresponding to this field.
		 */
		public String getDbName() { return dbName; }
	}
	
	
	
	private Map<String, Integer> paramList = new LinkedHashMap<>();
	private Map<FieldList,String> filterMap = new HashMap<>();
	private List<DePuyEventSeminarVO> semList = new ArrayList<>();
	private String reportId = null;
	private String reportName = null;

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public CustomReportVO() {
		super();
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Custom Report.xls");
	}

	/**
	 * Populate report params with resultset
	 * @param rs
	 */
	public CustomReportVO(ResultSet rs) {
		//Initialize the file information
		this();
		DBUtil db = new DBUtil();
		reportId = db.getStringVal("report_id", rs);
		setReportName(db.getStringVal("report_nm", rs));

		//get list of fields that should be included in the query
		for ( FieldList key : FieldList.values() ){
			//get list of parameters
			paramList.put(key.getFieldName(), db.getIntVal(key.getDbName(), rs));
		}
	}

	/**
	 * Populate report with request object
	 * @param req
	 */
	public CustomReportVO(SMTServletRequest req) {
		this(); //for setting up file info

		reportId = req.getParameter("reportId");
		setReportName(req.getParameter("reportName"));

		for (FieldList key : FieldList.values()) {
			paramList.put(key.getFieldName(), Convert.formatInteger(StringUtil.checkVal(req.getParameter(key.getFieldName()))));
			//log.debug(StringUtil.checkVal(req.getParameter(key.getFieldName()),"EMPTY"));
		}
		
		//apply runtime filters
		if (req.hasParameter("seminarTypeFilter"))
			filterMap.put(FieldList.SEMINAR_TYPE_FLG, req.getParameter("seminarTypeFilter"));
		if (req.hasParameter("statusFilter"))
			filterMap.put(FieldList.STATUS_FLG, req.getParameter("statusFilter"));
		if (req.hasParameter("rsvpCodeFilter"))
			filterMap.put(FieldList.SEMINAR_CODE_FLG, req.getParameter("rsvpCodeFilter"));
		if (req.hasParameter("startDateFilter"))
			filterMap.put(FieldList.START_DATE_FLG, req.getParameter("startDateFilter"));
	}
	

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {

		//Creates a quick list of all active fields
		List<FieldList> columns = new ArrayList<>(paramList.size());
		for (String key : paramList.keySet()) {
			if (paramList.get(key) ==1) 
				columns.add(FieldList.valueOf(key.toUpperCase()) );
		}

		StringBuilder report = new StringBuilder(10000);
		//create headers
		report.append(getHeader(columns));

		//Loop over each seminar
		for (DePuyEventSeminarVO vo : semList) {
			StringBuilder row = new StringBuilder();
			
			if (!semPassesFilters(vo)) continue;
			
			row.append("<tr>");
			EventEntryVO event = vo.getEvents().get(0);
			//loop over each field to be included in the results
			for (FieldList col : columns ) {
				row.append("<td>");
				//change information appended based on the selected field
				switch(col) {
					case JOINT_FLG:
						//Set of joints for this seminar
						row.append(vo.getJointLabel());
						break;
					case SEMINAR_TYPE_FLG:
						String typeCd = StringUtil.checkVal(event.getEventTypeCd());
						row.append(StringUtil.checkVal(typeMap.get(typeCd ) ));
						break;
					case SEMINAR_CODE_FLG:
						row.append(StringUtil.checkVal(event.getRSVPCode()));
						break;
					case COORDINATOR_FLG:
						row.append( StringUtil.checkVal(vo.getOwner().getFullName()));
						break;
					case STATUS_FLG:
						row.append( StringUtil.checkVal(vo.getStatusName()));
						break;
					case START_DATE_FLG:
						row.append(Convert.formatDate(event.getStartDate(), Convert.DATE_SLASH_PATTERN) );
						break;
					case TIME_FLG:
						row.append(StringUtil.checkVal(event.getLocationDesc()));
						break;
					case SPEAKER_FLG:
						row.append(StringUtil.checkVal(vo.getSurgeon().getSurgeonName()));
						break;
					case RSVP_COUNT_FLG:
						row.append( vo.getRsvpCount() ); //TODO
						break;
					case ATTENDEE_COUNT_FLG:
						row.append(event.getRsvpTotal() ); //TODO
						break;
//					case OPT_IN_FLG:
//						row.append( (vo.getOptInFlag() == 1 ? "Yes" : "No") );
//						break;
					case LOCATION_NM_FLG:
						if (StringUtil.checkVal(event.getShortDesc()).length() > 0) {
							row.append(event.getShortDesc());
						} else {
							row.append(StringUtil.checkVal(event.getEventDesc()));
						}
						break;
					case CITY_NM_FLG:
						row.append(StringUtil.checkVal(event.getCityName()) );
						break;
					case STATE_CD_FLG:
						row.append(StringUtil.checkVal(event.getStateCode()));
						break;
					case VENUE_COST_FLG:
						//TODO
						break;
					case REFRESHMENT_COST_FLG:
						//TODO
						break;
					case POSTCARD_DT_FLG:
						row.append( StringUtil.checkVal(vo.getPostcardMailDate()) );
						break;
					case NEWSPAPER_NM_FLG:
						printMultiVal(row, "getNewspaper1Text",vo.getAllAds());
						break;
					case AD_DT_FLG:
						vo.getAllAds().get(0).getCostToRepNo();
						printMultiVal(row, "getAdDatesText",vo.getAllAds());
						break;
					case AD_COST_FLG:
						row.append(vo.getAdCost("total"));
						break;
					case TERRITORY_COST_FLG:
						row.append(vo.getAdCost("rep"));
						break;
					case SURGEON_COST_FLG:
						row.append(vo.getAdCost("surgeon"));
						break;
					case HOSPITAL_COST_FLG:
						row.append(vo.getAdCost("hospital"));
						break;
//					case INVITATION_COST_NO:
//						row.append(StringUtil.checkVal(vo.getPostcardCostNo()));
//						break;
//					case INVITATION_COUNT_NO:
//						row.append( StringUtil.checkVal(vo.getQuantityNo()));
//						break;
					case POSTCARD_COST_NO:
						row.append(StringUtil.checkVal(vo.getPostcardCostNo()));
						break;
					case POSTCARD_COUNT_NO:
						row.append( StringUtil.checkVal(vo.getQuantityNo()));
						break;
					case TERRITORY_NO:
						row.append(StringUtil.checkVal( vo.getTerritoryNumber() ));
						break;
					case UPFRONT_FEE_FLG:
						row.append( ( vo.getUpfrontFeeFlg() == 1 ? "Yes" : "No") );
						break;
					default:
						break;
				}
				row.append("</td>");
			}
			row.append("</tr>\r");
			report.append(row);
			row = null;
		}

		//append footer
		report.append( getFooter() );

		//Returns report as byte[]
		return report.toString().getBytes();
	}

	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		if ( o instanceof DePuyEventSeminarVO ){
			semList.add( (DePuyEventSeminarVO) o);
		} else {
			semList = (List<DePuyEventSeminarVO>) o;
		}

	}

	
	@SuppressWarnings("incomplete-switch")
	private boolean semPassesFilters(DePuyEventSeminarVO sem) {
		if (filterMap == null || filterMap.size() == 0) return true;
		String value;
		EventEntryVO event = sem.getEvents().get(0);
		
		if (filterMap.containsKey(FieldList.SEMINAR_TYPE_FLG)) {
			value = filterMap.get(FieldList.SEMINAR_TYPE_FLG);
			if (!event.getEventTypeCd().startsWith(value)) return false;
		}
		if (filterMap.containsKey(FieldList.STATUS_FLG)) {
			value = filterMap.get(FieldList.STATUS_FLG);
			if (sem.getStatusFlg().intValue() != Convert.formatInteger(value).intValue()) return false;
		}
		if (filterMap.containsKey(FieldList.SEMINAR_CODE_FLG)) {
			value = filterMap.get(FieldList.SEMINAR_CODE_FLG);
			String[] codes = sem.getRSVPCodes().split(",");
			if (StringUtil.stringContainsItem(value, codes)) return false;
		}
		if (filterMap.containsKey(FieldList.START_DATE_FLG)) {
			value = filterMap.get(FieldList.START_DATE_FLG);
			Date d = Convert.formatDate(Convert.DATE_SLASH_PATTERN, value);
			if (d.after(sem.getEarliestEventDate())) return false;
		}
		return true;
	}

	/**
	 * Helper to print multiple ad values for into a single column. 
	 * @param row Destination for output
	 * @param meth Getter method that returns value to be appended
	 * @param list List of objects with the m method.
	 */
	private void printMultiVal(StringBuilder row, String meth, List<CoopAdVO> list) {
		for (int i = 0; i < list.size(); i++) {
			Object obj = list.get(i);
			try {
				Method m = CoopAdVO.class.getDeclaredMethod(StringUtil.checkVal(meth));
				row.append(StringUtil.checkVal( m.invoke(obj) ));
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				log.error("could not append multi-val", e);
			}

			if ( i < list.size() - 1)
				row.append(",");
		}
	}

	/**
	 * Generates the header row for the report
	 * @return
	 */
	private StringBuilder getHeader(List<FieldList> columns) {
		StringBuilder hdr = new StringBuilder();
		hdr.append("<table border='1'>\r<tr style='background-color:#ccc;'>");

		//Loop through all of the parameters in the list to make a list of headers
		for (FieldList key : columns) {
			hdr.append("<th>").append(key.getReportLabel()).append("</th>");
		}
		hdr.append("</tr>\r");
		return hdr;
	}

	/**
	 * Generates the footer for the report
	 * @return
	 */
	private StringBuilder getFooter() {
		return new StringBuilder("</table>");
	}

	public Map<String,Integer> getParamList(){
		return paramList;
	}

	/**
	 * @return the semList
	 */
	public List<DePuyEventSeminarVO> getSemList() {
		return semList;
	}

	/**
	 * @param semList the semList to set
	 */
	public void setSemList(List<DePuyEventSeminarVO> semList) {
		this.semList = semList;
	}

	/**
	 * @return the reportId
	 */
	public String getReportId() {
		return reportId;
	}

	/**
	 * @param reportId the reportId to set
	 */
	public void setReportId(String reportId) {
		this.reportId = reportId;
	}

	/**
	 * @return the reportName
	 */
	public String getReportName() {
		return reportName;
	}

	/**
	 * @param reportName the reportName to set
	 */
	public void setReportName(String reportName) {
		if (reportName == null || reportName.length() == 0) return; 
		this.reportName = reportName;
		setFileName(reportName + ".xls");
	}

	/**
	 * @param paramList the paramList to set
	 */
	public void setParamList(Map<String, Integer> paramList) {
		this.paramList = paramList;
	}

	public List<String> getUsedFields(){
		List<String> lst = new ArrayList<>();
		for (String nm:paramList.keySet())
			lst.add(nm);
		return lst;
	}

}
