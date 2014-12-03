package com.depuy.events_v2.vo.report;

import java.util.ArrayList;
import java.util.List;

import com.depuy.events_v2.vo.AttendeeSurveyVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: AttendeeSurveyReportVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Nov 11, 2014
 ****************************************************************************/
public class AttendeeSurveyReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = 1L;
	private List<AttendeeSurveyVO> surveyList = null;
	
	/**
	 * Fields that will be included in the report.
	 * The parameter string is the text to be displayed in the column heading.
	 */
	private enum ReportField {
		RSVP_CODE("Seminar Code"), 
		FIRST_NAME("First Name"), 
		LAST_NAME("Last Name"), 
		QUESTION_TEXT("Question Text"), 
		RESPONSE_TEXT("Response Text");
		
		private final String label;
		ReportField(String label){
			this.label = label;
		}
		public String getLabel(){ return label; }
	}
	
	/**
	 * Default Constructor
	 */
	public AttendeeSurveyReportVO() {
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Attendee-Survey-Report.xls");
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	public byte[] generateReport() {
		
		StringBuilder rpt = new StringBuilder();
		//set header row
		setHeader(rpt);
		
		//Iterate over all survey entries
		for ( AttendeeSurveyVO vo: surveyList ){
			
			//Since multiple questions are included in a single survey, loop through them here
			for ( int i = 0; i < vo.getQuestionList().size() ; i++){
				rpt.append("<tr>");
				
				//grab each required field in order from the vo and add it to the row
				for ( ReportField field : ReportField.values() ){
					rpt.append("<td>");
					
					switch (field){
					case RSVP_CODE:
						rpt.append(vo.getRsvpCode());
						break;
					case FIRST_NAME:
						rpt.append(vo.getFirstName());
						break;
					case LAST_NAME:
						rpt.append(vo.getLastName());
						break;
					case QUESTION_TEXT:
						rpt.append( vo.getQuestionList().get(i) );
						break;
					case RESPONSE_TEXT:
						rpt.append( vo.getAnswerList().get(i) );
						break;
					default:
						log.error("Invalid report field specified: "+field.name());
						break;
					}
					
					rpt.append("</td>");
				}
				rpt.append("</tr>\r");
			}
		}
		
		//add footer
		setFooter(rpt);
		//Return as byte array
		return rpt.toString().getBytes();
	}

	@SuppressWarnings("unchecked")
	@Override
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	public void setData(Object o) {
		if ( o instanceof AttendeeSurveyVO ){
			surveyList = new ArrayList<>();
			surveyList.add( (AttendeeSurveyVO) o);
		} else {
			surveyList = (List<AttendeeSurveyVO>) o;
		}
	}
	
	/**
	 * Appends the header to the report
	 * @param rpt
	 */
	private void setHeader( StringBuilder rpt ){
		rpt.append("<table border='1'>\r<tr style='background-color:#ccc;'>");
		//Get header names from the ReportField enum
		for ( ReportField f : ReportField.values() ){
			rpt.append("<th>").append(f.getLabel()).append("</th>");
		}
		rpt.append("</tr>\r");
	}
	
	/**
	 * Appends the footer to the report
	 * @param rpt
	 */
	private void setFooter( StringBuilder rpt ){
		rpt.append("</table>");
	}
}
