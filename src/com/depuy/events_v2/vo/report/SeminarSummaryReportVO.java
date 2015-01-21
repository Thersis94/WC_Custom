/**
 * 
 */
package com.depuy.events_v2.vo.report;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
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
 * <b>Title</b>: SeminarSummaryReportVO.java<p/>
 * <b>Description: Report with custom fields </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Oct 20, 2014
 ****************************************************************************/
public class SeminarSummaryReportVO extends AbstractSBReportVO {
	
	private static Map<String, String> typeMap = new HashMap<String, String>(){
		private static final long serialVersionUID = 1L;{
		put("ESEM", "Patient");
		put("CFSEM", "Co-Funded");
		put("CFSEM50", "Co-Funded 50/50");
		put("CFSEM25", "Co-Funded 50/25/25");
		put("CPSEM", "Physician");
		put("HSEM", "Hospital Sponsored");
	}};

	public static enum FieldList { 
		JOINT_FLG("joint_flg", "java.lang.String","Joint","JOINT_FLG"),
		SEMINAR_TYPE_FLG("seminar_type_flg", "java.lang.String","Seminar Type","SEMINAR_TYPE_FLG"),
		SEMINAR_CODE_FLG("seminar_code_flg", "java.lang.String", "Seminar Code","SEMINAR_CODE_FLG"),
		COORDINATOR_FLG("coordinator_flg", "java.lang.String", "Seminar Coordinator","COORDINATOR_FLG"),
		STATUS_FLG("status_flg", "java.lang.Integer","Seminar Status","STATUS_FLG"),
		START_DATE_FLG("start_date_flg", "java,util.Date", "Start Date","START_DATE_FLG"),
		TIME_FLG("time_flg", "java.util.Date", "Start Time","TIME_FLG"),
		SPEAKER_FLG("speaker_flg", "java.lang.String","Seminar Speaker","SPEAKER_FLG"),
		RSVP_COUNT_FLG("rsvp_count_flg", "java.lang.Integer", "RSVP Count","RSVP_TOTAL_FLG"),
		ATTENDEE_COUNT_FLG("attendee_count_flg", "java.lang.Integer", "Attendee Count","ATTENDEE_TOTAL_FLG"),
		OPT_IN_FLG("opt_in_flg", "java.lang.Integer","Opt-In","OPT_IN_FLG"),
		LOCATION_NM_FLG("location_nm_flg", "java.lang.String","Location Name","LOCATION_FLG"),
		CITY_NM_FLG("city_nm_flg", "java.lang.String","City","CITY_FLG"),
		STATE_CD_FLG("state_cd_flg", "java.lang.String","State","STATE_FLG"),
		VENUE_COST_FLG("venue_cost_flg", "java.lang.Double","Venue Cost","VENUE_COST_FLG"),
		REFRESHMENT_COST_FLG("refreshment_cost_flg", "java.lang.Double", "Refreshment Cost","REFRESHMENT_COST_FLG"),
		POSTCARD_DT_FLG("postcard_dt_flg", "java.util.Date", "Postcard Send Date","POSTCARD_DATE_FLG"),
		NEWSPAPER_NM_FLG("newspaper_nm_flg", "java.lang.String","Newspaper Name","NEWSPAPER_FLG"),
		AD_DT_FLG("ad_dt_flg", "java.util.Date", "Ad Date","AD_DATE_FLG"),
		AD_COST_FLG("ad_cost_flg", "java,lang.Date", "Total Ad Cost","AD_COST_FLG"),
		TERRITORY_COST_FLG("territory_cost_flg", "java.lang.Double", "Cost to Territory","TERRITORY_COST_FLG"),
		SURGEON_COST_FLG("surgeon_cost_flg", "java.lang.Double","Cost to Surgeon","SURGEON_COST_FLG"),
		HOSPITAL_COST_FLG("hospital_cost_flg", "java.lang.Double", "Cost to Hospital","HOSPITAL_COST_FLG"),
		UPFRONT_FEE_FLG("upfront_fee_flg", "java.lang.Integer", "$200 Upfront Fee", "UPFRONT_FEE_FLG"),
		TERRITORY_NO("territory_no","java.lang.Integer","Territory No","TERRITORY_FLG"),
		POSTCARD_COUNT_NO("postcard_count_no","java.lang.Integer","Postcard No","POSTCARD_TOTAL_FLG"),
		INVITATION_COUNT_NO("invitation_count_no","java.lang.Integer","Invitation No","INVITATION_TOTAL_FLG"),
		POSTCARD_COST_NO("postcard_cost_no","java.lang.Double","Total Postcard Cost","POSTCARD_COST_FLG"),
		INVITATION_COST_NO("invitation_cost_no","java.lang.Double","Total Invitation Cost","INVITATION_COST_FLG")
		;
		
		private final String name;
		private final String className;
		private final String reportLabel;
		private final String dbName;
		FieldList(String name, String className, String reportLabel, String dbName){
			this.name=name;
			this.className=className;
			this.reportLabel=reportLabel;
			this.dbName = dbName;
		}
		/**
		 * @return The name of the input field.
		 */
		public String getFieldName(){ return name; }
		/**
		 * @return The class name for the field's data type.
		 */
		public String getClassName(){ return className; }
		/**
		 * @return The label used for this field in the report header.
		 */
		public String getReportLabel(){ return reportLabel; }
		/**
		 * @return The name of the DEPUY_EVENT_REPORT column corresponding to this field.
		 */
		public String getDbName() { return dbName; }
	}
	private Map<String, Integer> paramList = new LinkedHashMap<>();
	private Map<String,String> filterMap = new HashMap<>();
	private List<DePuyEventSeminarVO> semList = new ArrayList<>();
	private String reportId = null;
	private String reportName = null;
	
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SeminarSummaryReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Seminar-Summary.xls");
	}
	
	/**
	 * Populate report params with resultset
	 * @param rs
	 */
	public SeminarSummaryReportVO(ResultSet rs){
		//Initialize the file information
		this();
		
		DBUtil db = new DBUtil();
		
		reportId = db.getStringVal("report_id", rs);
		reportName = db.getStringVal("report_nm", rs);
		
		//get list of fields that should be included in the query
		for ( FieldList key : FieldList.values() ){
			//get list of parameters
			paramList.put(key.getFieldName(), Convert.formatInteger( 
					db.getIntVal(key.getDbName(), rs) ) );
		}
		
		//get filters
		String[] pairs = StringUtil.checkVal( db.getStringVal("filter_txt", rs))
				.split(",");
		for ( int i=0; i<pairs.length; i++ ){
			String [] keyval = pairs[i].split(":");
			if (keyval.length > 1)
			filterMap.put(keyval[0],keyval[1]);
		}
	}
	
	/**
	 * Populate report with request object
	 * @param req
	 */
	public SeminarSummaryReportVO( SMTServletRequest req ){
		this(); //for setting up file info
		
		reportId = req.getParameter("reportId");
		reportName = req.getParameter("reportName");
		
		for( FieldList key : FieldList.values() ){
			paramList.put(key.getFieldName(), Convert.formatInteger(
					StringUtil.checkVal(req.getParameter(key.getFieldName()))));
			//log.debug(StringUtil.checkVal(req.getParameter(key.getFieldName()),"EMPTY"));
			if ( StringUtil.checkVal(req.getParameter(key.getFieldName()) ).equals("-1") ){
				filterMap.put(key.getFieldName(), StringUtil.checkVal(
						req.getParameter("by_"+key.getFieldName())));
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		
		//Creates a quick list of all active fields
		List<FieldList> lst = new ArrayList<>();
		for ( String key : paramList.keySet() ){
			if (paramList.get(key) ==1 || paramList.get(key) ==-1) { 
				lst.add(FieldList.valueOf(key.toUpperCase()) ); 
			}
		}
		
		StringBuilder report = new StringBuilder();
		//create headers
		report.append( getHeader() );
		
		//Loop over each seminar
		for ( DePuyEventSeminarVO vo : semList ){
			StringBuilder rpt = new StringBuilder();
			boolean appendIt = true;
			rpt.append("<tr>");
			EventEntryVO event = vo.getEvents().get(0);
			//loop over each field to be included in the results
			for ( FieldList fl : lst ){
				rpt.append("<td>");
				//change information appended based on the selected field
				String filterKey = fl.getFieldName();
				switch( fl ){
				
				case JOINT_FLG:
					//Set of joints for this seminar
					String jointCode = vo.getJointCodes();
					String jointCodeRev = new StringBuilder(jointCode).reverse().toString(); //In case code is 5,4 instead of 4,5
					//if this is not a filter parameter, or if it is, but matches the filter value, include it
					if( !filterMap.containsKey(filterKey) || 
						filterMap.get(filterKey).equalsIgnoreCase(jointCode) ||
						filterMap.get(filterKey).equalsIgnoreCase(jointCodeRev) ){
						//if this joint is to be included, flag it as such
						rpt.append( vo.getJointLabel() );
					} else {
						appendIt = false;
					}
					break;
				case SEMINAR_TYPE_FLG:
					String typeCd = StringUtil.checkVal(event.getEventTypeCd());
					if ( (!filterMap.containsKey(filterKey) ) || 
							filterMap.get(filterKey).equalsIgnoreCase(typeCd)){
						rpt.append(StringUtil.checkVal(typeMap.get(typeCd ) ));
					} else {
						appendIt = false;
					}
					break;
				case SEMINAR_CODE_FLG:
					String rsvpCd = StringUtil.checkVal(event.getRSVPCode());
					if ( (!filterMap.containsKey(filterKey)) || 
							filterMap.get(filterKey).equalsIgnoreCase(rsvpCd)){
						rpt.append(StringUtil.checkVal(event.getRSVPCode()));
					} else {
						appendIt = false;
					}
					break;
				case COORDINATOR_FLG:
					rpt.append( StringUtil.checkVal(event.getContactName() ));
					break;
				case STATUS_FLG:
					String status = StringUtil.checkVal(vo.getStatusName());
					if ( (!filterMap.containsKey(filterKey)) || 
							filterMap.get(filterKey).equalsIgnoreCase(status))
						rpt.append( StringUtil.checkVal(vo.getStatusName()));
					break;
				case START_DATE_FLG:
					rpt.append( Convert.formatDate(event.getStartDate(), Convert.DATE_LONG) );
					break;
				case TIME_FLG:
					rpt.append( Convert.formatDate(event.getStartDate(), Convert.TIME_LONG_PATTERN));
					break;
				case SPEAKER_FLG:
					rpt.append( StringUtil.checkVal(vo.getSurgeon().getSurgeonName()) );
					break;
				case RSVP_COUNT_FLG:
					rpt.append( vo.getRsvpCount() );
					break;
				case ATTENDEE_COUNT_FLG:
					rpt.append(event.getRsvpTotal() );
					break;
				case OPT_IN_FLG:
					rpt.append( (vo.getOptInFlag() == 1 ? "Yes" : "No") );
					break;
				case LOCATION_NM_FLG:
					rpt.append( StringUtil.checkVal(vo.getVenueText()) );
					break;
				case CITY_NM_FLG:
					rpt.append(StringUtil.checkVal(event.getCityName()) );
					break;
				case STATE_CD_FLG:
					rpt.append(StringUtil.checkVal(event.getStateCode()));
					break;
				case VENUE_COST_FLG:
					break;
				case REFRESHMENT_COST_FLG:
					break;
				case POSTCARD_DT_FLG:
					rpt.append( StringUtil.checkVal(vo.getPostcardSendDate()) );
					break;
				case NEWSPAPER_NM_FLG:
					printMultiVal(rpt, "getNewspaper1Text",vo.getAllAds());
					break;
				case AD_DT_FLG:
					vo.getAllAds().get(0).getCostToRepNo();
					printMultiVal(rpt, "getAdDatesText",vo.getAllAds());
					break;
				case AD_COST_FLG:
					printMultiVal(rpt,"getTotalCostNo",vo.getAllAds());
					break;
				case TERRITORY_COST_FLG:
					printMultiVal(rpt,"getCostToRepNo", vo.getAllAds());
					break;
				case SURGEON_COST_FLG:
					printMultiVal(rpt,"getCostToSurgeonNo", vo.getAllAds());
					break;
				case HOSPITAL_COST_FLG:
					printMultiVal(rpt,"getCostToHospitalNo",vo.getAllAds());
					break;
				case INVITATION_COST_NO:
					rpt.append(StringUtil.checkVal(vo.getCostNo()));
					break;
				case INVITATION_COUNT_NO:
					rpt.append( StringUtil.checkVal(vo.getQuantityNo()));
					break;
				case POSTCARD_COST_NO:
					rpt.append(StringUtil.checkVal(vo.getCostNo()));
					break;
				case POSTCARD_COUNT_NO:
					rpt.append( StringUtil.checkVal(vo.getQuantityNo()));
					break;
				case TERRITORY_NO:
					rpt.append(StringUtil.checkVal( vo.getTerritoryNumber() ));
					break;
				case UPFRONT_FEE_FLG:
					rpt.append( ( vo.getUpfrontFeeFlg() == 1 ? "Yes" : "No") );
					break;
				default:
					break;
				}
				rpt.append("</td>");
			}
			rpt.append("</tr>\r");
			if ( appendIt ){
				report.append(rpt);
			}
			rpt = null;
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
	
	/**
	 * Get the set of fields and filters from the request object.
	 * @param req
	 * @param fields
	 * @param filters
	 */
	public void parseParameters( SMTServletRequest req, Map<String,Integer> fields, Map<String,String>filters){
		final String FILTER_PREFIX = "by_";
		final int INCLUDE = 1, FILTER=-1;
		
		//For each valid field, check for values to be collected
		for( FieldList fl : FieldList.values() ){
			switch( Convert.formatInteger( req.getParameter(fl.getFieldName().toLowerCase()))){
			case FILTER:
				filters.put(fl.getFieldName(), StringUtil.checkVal(req.getParameter(
						FILTER_PREFIX+fl.getFieldName())));
				//No break, so filter params are included in the report
			case INCLUDE:
				fields.put(fl.name(), 1);
				break;
			default:
				fields.put(fl.name(), 0);
				break;
			}
		}
		filterMap = filters;
		paramList = fields;
	}
	
	/**
	 * Helper to print multiple ad values for into a single column. 
	 * @param rpt Destination for output
	 * @param meth Getter method that returns value to be appended
	 * @param list List of objects with the m method.
	 */
	private void printMultiVal( StringBuilder rpt, String meth, List<CoopAdVO> list ){
		for ( int i = 0; i < list.size(); i++ ){
			Object obj = list.get(i);
			
			try {
				Method m = CoopAdVO.class.getDeclaredMethod(StringUtil.checkVal(meth));
				rpt.append(StringUtil.checkVal( m.invoke(obj) ));
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				log.error(e);
			}
			
			if ( i < list.size() - 1)
				rpt.append(",");
		}
	}
	
	/**
	 * Generates the header row for the report
	 * @return
	 */
	private StringBuilder getHeader() {
		StringBuilder hdr = new StringBuilder();
		hdr.append("<table border='1'>\r<tr style='background-color:#ccc;'>");
		
		//Loop through all of the parameters in the list to make a list of headers
		for ( String key : paramList.keySet() ){
			//if the field was marked for inclusion, append to the header string
			if ( paramList.get(key)==1 || paramList.get(key)==-1){
				FieldList fl = FieldList.valueOf(key.toUpperCase());
				hdr.append("<th>").append(fl.getReportLabel());
				hdr.append("</th>");
			}
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
		this.reportName = reportName;
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

	/**
	 * @return the filterMap
	 */
	public Map<String, String> getFilterMap() {
		return filterMap;
	}

	/**
	 * @param filterMap the filterMap to set
	 */
	public void setFilterMap(Map<String, String> filterMap) {
		this.filterMap = filterMap;
	}
}
