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
import java.util.Set;

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

	public static enum FieldList { 
		JOINT_FLG("joint_flg", "java.lang.String","Joint"),
		SEMINAR_TYPE_FLG("seminar_type_flg", "java.lang.String","Seminar Type"),
		SEMINAR_CODE_FLG("seminar_code_flg", "java.lang.String", "Seminar Code"),
		COORDINATOR_FLG("coordinator_flg", "java.lang.String", "Seminar Coordinator"),
		STATUS_FLG("status_flg", "java.lang.Integer","Seminar Status"),
		START_DATE_FLG("start_dt_flg", "java,util.Date", "Start Date"),
		TIME_FLG("time_flg", "java.util.Date", "Start Time"),
		SPEAKER_FLG("speaker_flg", "java.lang.String","Seminar Speaker"),
		RSVP_COUNT_FLG("rsvp_count_flg", "java.lang.Integer", "RSVP Count"),
		ATTENDEE_COUNT_FLG("attendee_count_flg", "java.lang.Integer", "Attendee Count"),
		OPT_IN_FLG("opt_in_flg", "java.lang.Integer","Opt-In Count"),
		LOCATION_NM_FLG("location_nm_flg", "java.lang.String","Location Name"),
		CITY_NM_FLG("city_nm_flg", "java.lang.String","City"),
		STATE_CD_FLG("state_cd_flg", "java.lang.String","State"),
		VENUE_COST_FLG("venue_cost_flg", "java.lang.Double","Venue Cost"),
		REFRESHMENT_COST_FLG("refreshment_cost_flg", "java.lang.Double", "Refreshment Cost"),
		POSTCARD_DT_FLG("postcard_dt_flg", "java.util.Date", "Postcard Send Date"),
		NEWSPAPER_NM_FLG("newspaper_nm_flg", "java.lang.String","Newspaper Name"),
		AD_DT_FLG("ad_dt_flg", "java.util.Date", "Ad Date"),
		AD_COST_FLG("ad_cost_flg", "java,lang.Date", "Total Ad Cost"),
		TERRITORY_COST_FLG("territory_cost_flg", "java.lang.Double", "Cost to Territory"),
		SURGEON_COST_FLG("surgeon_cost_flg", "java.lang.Double","Cost to Surgeon"),
		HOSPITAL_COST_FLG("hospital_cost_flg", "java.lang.Double", "Cost to Hospital");
		
		private final String name;
		private final String className;
		private final String reportLabel;
		FieldList(String name, String className, String reportLabel){
			this.name=name;
			this.className=className;
			this.reportLabel=reportLabel;
		}
		public String getFieldName(){ return name; }
		public String getClassName(){ return className; }
		public String getReportLabel(){ return reportLabel; }
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
					db.getIntVal(key.getFieldName(), rs) ) );
		}
		
		//get filters
		String[] pairs = StringUtil.checkVal( db.getStringVal("filter_txt", rs))
				.split(":");
		for ( String s : pairs ){
			String [] keyval = s.split("=");
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
			log.debug(StringUtil.checkVal(req.getParameter(key.getFieldName()),"EMPTY"));
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
					Set<String> joints = vo.getJoints();
					joints.add("4,5");
					boolean hasJoint = false;
					for (String joint : joints){
						if( !filterMap.containsKey(filterKey) || 
								filterMap.get(filterKey).equalsIgnoreCase(joint)){
							hasJoint = true;
							break;
						} 
					}
					if ( hasJoint )
						rpt.append( vo.getJointLabel().substring(0, vo.getJointLabel().length()-1) );
					else
						appendIt = false;
					break;
				case SEMINAR_TYPE_FLG:
					Map<String,String> typeMap = new HashMap<String,String>(){
						private static final long serialVersionUID = 1L;{
						put("ESEM", "Patient");
						put("CFSEM", "Co-Funded");
						put("CFSEM50", "Co-Funded 50/50");
						put("CFSEM25", "Co-Funded 50/25/25");
						put("CPSEM", "Physician");
					}};
					
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
					printMultiVal(rpt, "getNewspaper1Text",vo.getPrintAndOnlineAds());
					break;
				case AD_DT_FLG:
					vo.getPrintAndOnlineAds().get(0).getCostToRepNo();
					printMultiVal(rpt, "getAdDatesText",vo.getPrintAndOnlineAds());
					break;
				case AD_COST_FLG:
					printMultiVal(rpt,"getTotalCostNo",vo.getPrintAndOnlineAds());
					break;
				case TERRITORY_COST_FLG:
					printMultiVal(rpt,"getCostToRepNo", vo.getPrintAndOnlineAds());
					break;
				case SURGEON_COST_FLG:
					printMultiVal(rpt,"getCostToSurgeonNo", vo.getPrintAndOnlineAds());
					break;
				case HOSPITAL_COST_FLG:
					printMultiVal(rpt,"getCostToHospitalNo",vo.getPrintAndOnlineAds());
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
}
