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
import com.siliconmtn.data.report.ExcelReport;
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
 * @updates
 * 		JM 02.11.15 - completely refactored, was not extensible.
 ****************************************************************************/
public class CustomReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
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

	/**
	 * the ordering of the enum controls the ordering of the column in the geneated report
	 */
	public static enum FieldList {
		
		JOINT_FLG("joint_flg", "Joint"),
		SEMINAR_TYPE_FLG("seminar_type_flg","Seminar Type"),
		SEMINAR_CODE_FLG("seminar_code_flg", "Seminar Code"),
		COORDINATOR_FLG("coordinator_flg", "Seminar Coordinator"),
		STATUS_FLG("status_flg", "Seminar Status"),
		START_DATE_FLG("start_date_flg", "Seminar Date"),
		END_DATE_FLG("start_date_flg", "Seminar Date"),
		TIME_FLG("time_flg", "Seminar Time"),
		SPEAKER_FLG("speaker_flg", "Seminar Speaker"),
		RSVP_COUNT_FLG("rsvp_count_flg",  "RSVP Count"),
		ATTENDEE_COUNT_FLG("attendee_count_flg",  "Attendee Count"),
		//OPT_IN_FLG("opt_in_flg", "Opt-In"),
		LOCATION_NM_FLG("location_nm_flg", "Location Name"),
		CITY_NM_FLG("city_nm_flg", "City"),
		STATE_CD_FLG("state_cd_flg", "State"),
		VENUE_COST_FLG("venue_cost_flg", "Venue Cost"),
		REFRESHMENT_COST_FLG("refreshment_cost_flg", "Refreshment Cost"),
		POSTCARD_DT_FLG("postcard_dt_flg","Postcard Mail Date"),
		NEWSPAPER_NM_FLG("newspaper_nm_flg", "Newspaper Name"),
		AD_DT_FLG("ad_dt_flg","Ad Date"),
		AD_COST_FLG("ad_cost_flg", "Total Ad Cost"),
		TERRITORY_COST_FLG("territory_cost_flg", "Cost to Territory"),
		SURGEON_COST_FLG("surgeon_cost_flg", "Cost to Surgeon/Hospital"),
		//HOSPITAL_COST_FLG("hospital_cost_flg", "Cost to Hospital"),  //50/25/25 - n o longer supported
		UPFRONT_FEE_FLG("upfront_fee_flg",  "$200 Upfront Fee"),
		TERRITORY_NO("territory_no","Territory#"),
		POSTCARD_COUNT_NO("postcard_count_no","Postcard/Invitation Qnty"),
		POSTCARD_COST_NO("postcard_cost_no","Postcard/Invitation Cost");

		private final String name;
		private final String reportLabel;
		
		//enum's Constructor
		FieldList(String name, String reportLabel) {
			this.name=name;
			this.reportLabel=reportLabel;
		}
		
		/**
		 * @return The name of the input field.
		 */
		public String getFieldName() { return name; }
		/**
		 * @return The label used for this field in the report header.
		 */
		public String getReportLabel() { return reportLabel; }
	}
	
	
	
	private Map<String, Integer> paramList = new LinkedHashMap<>();
	private Map<FieldList,String> filterMap = new HashMap<>();
	private List<DePuyEventSeminarVO> semList = new ArrayList<>();
	private String reportId = null;
	private String reportName = null;
	private DBUtil db = null;

	private static final long serialVersionUID = 1L;

	public CustomReportVO() {
		super();
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Custom Report.xls");
		db = new DBUtil();
	}

	/**
	 * Populate report params with resultset
	 * @param rs
	 */
	public CustomReportVO(ResultSet rs) {
		//Initialize the file information
		this();
		reportId = db.getStringVal("report_id", rs);
		setReportName(db.getStringVal("report_nm", rs));
	}
	
	public void addField(ResultSet rs) {
		paramList.put(db.getStringVal("COLUMN_NM",rs), 1);
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
			//these are checkboxes.  They won't appear on the request unless they were checked.
			if (! req.hasParameter(key.getFieldName())) continue;
			paramList.put(key.getFieldName(), 1);
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
		if (req.hasParameter("endDateFilter"))
			filterMap.put(FieldList.END_DATE_FLG, req.getParameter("endDateFilter"));
	}
	

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {

		//Creates a quick list of all active fields
		List<FieldList> columns = new ArrayList<>(paramList.size());
		
		columns = fillColumns(columns);
		
		ExcelReport rpt = new ExcelReport(this.getHeader(columns));
		
		List<Map<String, Object>> rows = new ArrayList<>(semList.size());

		rows = generateDataRows(rows, columns);

		rpt.setData(rows);
		
		return rpt.generateReport();
	}

	
	/**
	 * loops the param list and fills the columns
	 * @param columns
	 * @return
	 */
	private List<FieldList> fillColumns(List<FieldList> columns) {
		
		for (String key : paramList.keySet()) {
			if (paramList.get(key) ==1) 
				columns.add(FieldList.valueOf(key.toUpperCase()) );
		}
		return columns;
	}

	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @param columns 
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows, List<FieldList> columns) {
		//Loop over each seminar
		for (DePuyEventSeminarVO vo : semList) {
			Map<String, Object> row = new HashMap<String, Object>();
			
			if (!semPassesFilters(vo)) continue;
			
			EventEntryVO event = vo.getEvents().get(0);
			//loop over each field to be included in the results
			for (FieldList col : columns ) {
				//change information appended based on the selected field
				switch(col) {
					case JOINT_FLG:
						//Set of joints for this seminar
						row.put(col.name,vo.getJointLabel());
						break;
					case SEMINAR_TYPE_FLG:
						String typeCd = StringUtil.checkVal(event.getEventTypeCd());
						row.put(col.name,StringUtil.checkVal(typeMap.get(typeCd ) ));
						break;
					case SEMINAR_CODE_FLG:
						row.put(col.name,StringUtil.checkVal(event.getRSVPCode()));
						break;
					case COORDINATOR_FLG:
						row.put(col.name, StringUtil.checkVal(vo.getOwner().getFullName()));
						break;
					case STATUS_FLG:
						row.put(col.name, StringUtil.checkVal(vo.getStatusName()));
						break;
					case START_DATE_FLG:
						row.put(col.name,Convert.formatDate(event.getStartDate(), Convert.DATE_SLASH_PATTERN));
						break;
					case TIME_FLG:
						row.put(col.name,StringUtil.checkVal(event.getLocationDesc()));
						break;
					case SPEAKER_FLG:
						row.put(col.name,StringUtil.checkVal(vo.getAllSurgeonNames()));
						break;
					case RSVP_COUNT_FLG:
						row.put(col.name, vo.getRsvpCount() );
						break;
					case ATTENDEE_COUNT_FLG:
						if ("HSEM".equals(event.getEventTypeCd())) {
							row.put(col.name,Convert.formatInteger(vo.getPcAttribute1()));
						} else {
							row.put(col.name,Convert.formatInteger(vo.getSurveyResponse("attendee_cnt")));
						}
						break;
//					case OPT_IN_FLG:
//						row.put(col.name, (vo.getOptInFlag() == 1 ? "Yes" : "No") );
//						break;
					case LOCATION_NM_FLG:
						if (StringUtil.checkVal(event.getShortDesc()).length() > 0) {
							row.put(col.name,event.getShortDesc());
						} else {
							row.put(col.name,StringUtil.checkVal(event.getEventDesc()));
						}
						break;
					case CITY_NM_FLG:
						row.put(col.name,StringUtil.checkVal(event.getCityName()) );
						break;
					case STATE_CD_FLG:
						row.put(col.name,StringUtil.checkVal(event.getStateCode()));
						break;
					case VENUE_COST_FLG:
						row.put(col.name,StringUtil.checkVal(vo.getSurveyResponse("venue_cost")));
						break;
					case REFRESHMENT_COST_FLG:
						row.put(col.name,StringUtil.checkVal(vo.getSurveyResponse("refresh_cost")));
						break;
					case POSTCARD_DT_FLG:
						row.put(col.name,Convert.formatDate(vo.getPostcardMailDate(), Convert.DATE_SLASH_PATTERN));
						break;
					case NEWSPAPER_NM_FLG:
						row.put(col.name,printMultiVal("getNewspaper1Text",vo.getAllAds()));
						break;
					case AD_DT_FLG:
						row.put(col.name,printMultiVal("getAdDatesText",vo.getAllAds()));
						break;
					case AD_COST_FLG:
						row.put(col.name,vo.getAdCost("total"));
						break;
					case TERRITORY_COST_FLG:
						row.put(col.name,vo.getAdCost("rep"));
						break;
					case SURGEON_COST_FLG:
						row.put(col.name,vo.getAdCost("surgeon"));
						break;
//					case HOSPITAL_COST_FLG:
//						row.put(col.name,vo.getAdCost("hospital"));
//						break;
//					case INVITATION_COST_NO:
//						row.put(col.name,StringUtil.checkVal(vo.getPostcardCostNo()));
//						break;
//					case INVITATION_COUNT_NO:
//						row.put(col.name, StringUtil.checkVal(vo.getQuantityNo()));
//						break;
					case POSTCARD_COST_NO:
						row.put(col.name,StringUtil.checkVal(vo.getPostcardCostNo()));
						break;
					case POSTCARD_COUNT_NO:
						row.put(col.name, StringUtil.checkVal(vo.getQuantityNo()));
						break;
					case TERRITORY_NO:
						row.put(col.name,StringUtil.checkVal( vo.getTerritoryNumber() ));
						break;
					case UPFRONT_FEE_FLG:
						row.put(col.name, ( vo.getUpfrontFeeFlg() == 1 ? "Yes" : "No") );
						break;
					default:
						break;
				}
				
			}
			
			rows.add(row);
		}
		return rows;
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

	/**
	 * 
	 * @param sem
	 * @return
	 */
	public boolean semPassesFilters(DePuyEventSeminarVO sem) {
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
			log.debug("filtering for seminar " + value);
			String[] codes = sem.getRSVPCodes().split(",");
			if (!StringUtil.stringContainsItem(value, codes)) return false;
		}
		if (filterMap.containsKey(FieldList.START_DATE_FLG)) {
			value = filterMap.get(FieldList.START_DATE_FLG);
			Date d = Convert.formatDate(Convert.DATE_SLASH_PATTERN, value);
			if (d.after(sem.getEarliestEventDate())) return false;
		}
		if (filterMap.containsKey(FieldList.END_DATE_FLG)) {
			value = filterMap.get(FieldList.END_DATE_FLG);
			Date d = Convert.formatDate(Convert.DATE_SLASH_PATTERN, value);
			if (d.before(sem.getEarliestEventDate())) return false;
		}
		return true;
	}

	/**
	 * Helper to print multiple ad values for into a single column. 
	 * @param row Destination for output
	 * @param meth Getter method that returns value to be appended
	 * @param list List of objects with the m method.
	 */
	private String printMultiVal(String meth, List<CoopAdVO> list) {
		StringBuilder sb = new StringBuilder(50);
		for (int i = 0; i < list.size(); i++) {
			Object obj = list.get(i);
			try {
				Method m = CoopAdVO.class.getDeclaredMethod(StringUtil.checkVal(meth));
				sb.append(StringUtil.checkVal( m.invoke(obj) ));
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				log.error("could not append multi-val", e);
			}

			if ( i < list.size() - 1)
				sb.append(",");
		}
		return sb.toString();
	}

	/**
	 * builds the header map for the excel report
	 * @param columns 
	 * @return
	 */
	private LinkedHashMap<String, String> getHeader(List<FieldList> columns) {
		
		LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
		//Loop through all of the parameters in the list to make a list of headers
				for (FieldList key : columns) {
					//log.debug("field name: " +key.getFieldName()+ " report label: " +key.getReportLabel());
					headerMap.put(key.getFieldName() , key.getReportLabel());
				}
		
		return headerMap;
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
