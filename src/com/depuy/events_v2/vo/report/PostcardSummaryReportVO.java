package com.depuy.events_v2.vo.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import com.depuy.events.vo.CoopAdVO;
import com.depuy.events_v2.CoopAdsActionV2;
import com.depuy.events_v2.vo.DePuyEventSeminarVO;
import com.depuy.events_v2.vo.DePuyEventSurgeonVO;
import com.depuy.events_v2.vo.PersonVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.security.UserDataVO;

/*****************************************************************************
 <p><b>Title</b>: PostcardSummaryReportVO.java</p>
 <p>A comprehensive summary for the entire Seminar and all data points.</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Jane 20, 2014
 ***************************************************************************/

public class PostcardSummaryReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private DePuyEventSeminarVO sem = new DePuyEventSeminarVO();
    private static final String TITLE_TEXT = "Seminar Information";
    public PostcardSummaryReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Seminar-Summary.xls");
    }
    
    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
    public void setData(Object o) {
	    this.sem = (DePuyEventSeminarVO) o;
    }
    
	public byte[] generateReport() {
		log.debug("starting PostcardSummaryReport");
		int rowCnt = 0;
		
		//Create Excel Object
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();
		Row r = s.createRow(rowCnt++);
		Cell c = null;
		
		// makes title row
		this.getHeader(r,s);
		r = s.createRow(rowCnt++);
		
		//each row is two strings
		List<GenericVO> row = getSeminarRows();
	
		rowCnt = buildReportRows(s,r,row, rowCnt);
		
		List <GenericVO> eventRow = getEventRows();
		
		rowCnt = buildReportRows(s,r,eventRow, rowCnt);
		
		List <GenericVO> postRow = getPostcardRows();
		
		rowCnt = buildReportRows(s,r,postRow, rowCnt);
		
		List <GenericVO> speakerRow = getSpeakerRows();
		
		rowCnt = buildReportRows(s,r,speakerRow, rowCnt);
			
		r = s.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue("Ad Information");
		
		r = s.createRow(rowCnt++);
		
		List<GenericVO> adRow = getAdRows();
		
		rowCnt = buildReportRows(s,r,adRow, rowCnt);
			
		
		//existing commented out code
		//add the Radio Ad data
//		if (sem.getRadioAd() != null && sem.getRadioAd().getCoopAdId() != null) {
//			CoopAdVO ad = sem.getRadioAd();
//			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
//			rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Radio Ad</td></tr>\r");
//			rpt.append("<tr><td>Radio Station:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(StringUtil.checkVal(ad.getNewspaper1Phone())).append(")</td></tr>\r");
//			rpt.append("<tr><td>Contact Name:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper2Text())).append(" (").append(StringUtil.checkVal(ad.getNewspaper2Phone())).append(")</td></tr>\r");
//			rpt.append("<tr><td>Ad Deadline:</td><td align='center'>").append(StringUtil.checkVal(ad.getAdDatesText())).append("</td></tr>\r");
//		}

		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException ioe) {
			log.error("could not write output stream", ioe);
		}finally {
			try { wb.close(); } catch (Exception e) {}
		}
		return null;
	}
	
	/**
	 * takes the data in generic VOs and builds the rows and cells on the sheet
	 * @param s
	 * @param r
	 * @param row
	 * @return
	 */
	private int buildReportRows(Sheet s, Row r, List<GenericVO> row, int rowCnt) {
		Cell c = null;
		for (GenericVO vo : row){
			
				c = r.createCell(0);
				c.setCellType(Cell.CELL_TYPE_STRING);
				c.setCellValue((String)vo.getKey());
				c = r.createCell(1);
				c.setCellType(Cell.CELL_TYPE_STRING);
				c.setCellValue((String)vo.getValue());	

				r = s.createRow(rowCnt++);
			}
		return rowCnt;
	}

	/**
	 * generates the ad section of the report
	 * @return
	 */
	private List<GenericVO> getAdRows() {
		List<GenericVO> row = new ArrayList<>();
		//add the Co-Op Ad data
		if (sem.getAllAds() != null && !sem.getAllAds().isEmpty() ) {
			StringBuilder sb = new StringBuilder(32);
			
			GenericVO vo = null;
			for (CoopAdVO ad : sem.getAllAds() ){
				int adSts = Convert.formatInteger(ad.getStatusFlg(), 0).intValue();
				
				vo = new GenericVO();
				vo.setKey("");
				vo.setValue("");
				row.add(vo);
				
				vo = new GenericVO();
				vo.setKey("Newspaper Ad");
				vo.setValue("");
				row.add(vo);
				
				vo = new GenericVO();
				vo.setKey("Ad Type:");
				vo.setValue(StringUtil.checkVal(ad.getAdType()));
				row.add(vo);
			
				vo = new GenericVO();
				vo.setKey("Sponsored Newspaper:");
				vo.setValue(sb.append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(ad.getNewspaper1Phone()).append(")").toString());
				row.add(vo);
				
				vo = new GenericVO();
				vo.setKey("Coordinator approved ad?:");
				vo.setValue((adSts == CoopAdsActionV2.CLIENT_APPROVED_AD) ? "Yes" : "No");
				row.add(vo);
				
				vo = new GenericVO();
				vo.setKey("Approved Paper:");
				vo.setValue(StringUtil.checkVal(ad.getApprovedPaperName()));
				row.add(vo);
				
				vo = new GenericVO();
				vo.setKey("Total Cost:");
				vo.setValue(String.valueOf(ad.getTotalCostNo()));
				row.add(vo);
				
					//calculate cost of ad to territory or surgeon
				if ("CFSEM".equalsIgnoreCase(sem.getEvents().get(0).getEventTypeCd())) {
					int surgSts = Convert.formatInteger(ad.getSurgeonStatusFlg(), 0).intValue();
					vo = new GenericVO();
					vo.setKey("Speaker approved ad?:");
					vo.setValue(surgSts == CoopAdsActionV2.SURG_APPROVED_AD ? "Yes" : "No");
					row.add(vo);
					
					vo = new GenericVO();
					vo.setKey("Speaker paid for ad?:");
					vo.setValue(surgSts == CoopAdsActionV2.SURG_PAID_AD ? "Yes" : "No");
					row.add(vo);
					
					vo = new GenericVO();
					vo.setKey("Ad Cost to Speaker:");
					vo.setValue(String.valueOf(ad.getCostToRepNo()));
					row.add(vo);
				}else{
					vo = new GenericVO();
					vo.setKey("Ad Cost to Territory:");
					vo.setValue(String.valueOf(ad.getCostToRepNo()));
					row.add(vo);
				}
				
				if (ad.getAdFileUrl() != null){
					vo = new GenericVO();
					vo.setKey("Ad File:");
					vo.setValue(sb.append("<a href=\"").append(sem.getBaseUrl()).append("/ads/").append(ad.getAdFileUrl()).append("\" target='_blank'>").append(ad.getAdFileUrl()).append("</a>").toString());
					row.add(vo);
				}
				
				
				
			}
		}
		
		return row;
	}

	/**
	 * generates the speaker section of the report
	 * @return
	 */
	private List <GenericVO> getSpeakerRows() {
		List <GenericVO> row = new ArrayList<>();
		
		GenericVO vo = new GenericVO();
		vo.setKey("");
		vo.setValue("");
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("Speaker Information");
		vo.setValue("");
		row.add(vo);
		
		for (DePuyEventSurgeonVO surg : sem.getSurgeonList()) {
			
			vo = new GenericVO();
			vo.setKey("Speaker Name:");
			vo.setValue(surg.getSurgeonName());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("The Field Marketing Director has reviewed the Speaker Guidelines with speaker?:");
			vo.setValue(surg.getSeenGuidelinesFlg() == 1 ? "yes" : "no");
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Years practicing: ");
			vo.setValue(String.valueOf(surg.getExperienceYrs()));
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Years at current practice:");
			vo.setValue(String.valueOf(surg.getPractYrs()));
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Employed by hospital?:");
			vo.setValue(surg.getHospEmployeeFlg() == 1 ? "yes" : "no");
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Hospital Address:");
			vo.setValue(StringUtil.checkVal(surg.getHospAddress()));
			row.add(vo);
			
			String location = (surg.getPractLocation() != null) ? surg.getPractLocation().getFormattedLocation() : "";
			
			vo = new GenericVO();
			vo.setKey("Practice Address:");
			vo.setValue(location);
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Practice Phone:");
			vo.setValue(surg.getPractPhone());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Speaker/Office Email(s):");
			vo.setValue(surg.getPractEmail());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Secondary Contact:");
			vo.setValue(surg.getSecPhone());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Secondary Contact Email:");
			vo.setValue(surg.getSecEmail());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Practice Website:");
			vo.setValue(StringUtil.checkVal(surg.getPractWebsite()));
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Speaker Photo:");
			vo.setValue(surg.getLogoFileUrl());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Speaker Bio:");
			vo.setValue(StringUtil.checkVal(surg.getSurgeonBio()));
			row.add(vo);
			
		}
		
		return row;
	}

	/**
	 * generates the post card section of the report
	 * @return
	 */
	private List <GenericVO> getPostcardRows() {
		StringBuilder sb = new StringBuilder(32);
		List <GenericVO> row = new ArrayList<>();
		
		GenericVO vo = new GenericVO();
		vo.setKey("");
		vo.setValue("");
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("Date of Seminar");
		vo.setValue(sb.append(Convert.formatDate(sem.getEarliestEventDate(), Convert.DATE_LONG)).toString());
		sb.setLength(0);
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("RSVP Deadline");
		vo.setValue(sb.append(Convert.formatDate(sem.getRSVPDate(), Convert.DATE_LONG)).toString());
		sb.setLength(0);
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("Initial Invitation Send Date");
		vo.setValue(Convert.formatDate(sem.getPostcardSendDate(), Convert.DATE_LONG));
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("Additional Postcards Send Date: ");
		vo.setValue(Convert.formatDate(sem.getAddtlPostcardSendDate(), Convert.DATE_LONG));
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("Additional cards should have the letter \"S\" at the end of the seminar#");
		vo.setValue("");
		row.add(vo);
		
		UserDataVO owner = sem.getOwner();
		
		vo = new GenericVO();
		vo.setKey("Additional Postcards");
		vo.setValue("Send additional postcards to:");
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("");
		vo.setValue(sb.append(StringUtil.checkVal(owner.getFirstName())).append(" ").append(StringUtil.checkVal(owner.getLastName())).toString());
		row.add(vo);
		sb.setLength(0);
		
		vo = new GenericVO();
		vo.setKey("");
		vo.setValue(StringUtil.checkVal(owner.getAddress()));
		row.add(vo);
		
		vo = new GenericVO();
		vo.setKey("");
		vo.setValue(sb.append(StringUtil.checkVal(owner.getCity())).append(", ").append(StringUtil.checkVal(owner.getState())).append(" ").append(StringUtil.checkVal(owner.getZipCode())).toString());
		row.add(vo);
		sb.setLength(0);
		
		return row;
	}

	/**
	 * generates the event section of the report
	 * @return
	 */
	private List<GenericVO> getEventRows() {
		List<GenericVO> row = new ArrayList<>();
		StringBuilder sb = new StringBuilder(32);
		GenericVO vo = null;
		for (EventEntryVO event : sem.getEvents()) {
			
			vo = new GenericVO();
			vo.setKey("");
			vo.setValue("");
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey(sb.append("Seminar #").append(event.getRSVPCode()).toString());
			vo.setValue(event.getEventName());
			row.add(vo);
			sb.setLength(0);
			vo = new GenericVO();
			vo.setKey("Seminar Date/Time");
			vo.setValue(sb.append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append(" ").append(event.getLocationDesc()).toString());
			sb.setLength(0);
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Seminar Location");
			vo.setValue(sb.append(event.getCityName()).append(", ").append(event.getStateCode()).append(" ").append(event.getZipCode()).toString());
			sb.setLength(0);
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Joint");
			vo.setValue(sem.getJointLabel());
			row.add(vo);
			
			if (sem.getProductCodes().length() > 0){
				vo = new GenericVO();
				vo.setKey("Product");
				vo.setValue(sem.getProductCodes());
				row.add(vo);
			}
			
			vo = new GenericVO();
			vo.setKey("Venue Location");
			vo.setValue(event.getEventDesc());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Venue Name");
			vo.setValue(event.getEventName());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Refreshment Choice");
			vo.setValue(StringUtil.checkVal(event.getServiceText()));
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("Venue Address");
			vo.setValue(event.getAddressText());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("");
			vo.setValue(event.getAddress2Text());
			row.add(vo);
			
			vo = new GenericVO();
			vo.setKey("");
			vo.setValue(sb.append(event.getCityName()).append(" " ).append(event.getStateCode()).append(", " ).append(event.getZipCode()).toString());
			row.add(vo);

		}
		return row;
	}

	/**
	 * used generic vos so i could deal with areas that didn't have unique keys
	 * generates the top part of the report as a map
	 * @return
	 */
	private List<GenericVO> getSeminarRows() {
		List<GenericVO> row = new ArrayList<>();
		
		GenericVO vo = new GenericVO();
		vo.setKey("Product");
		vo.setValue(sem.getJointLabel());
		row.add(vo);
		vo = new GenericVO();
		vo.setKey("Seminar Type");
		vo.setValue(sem.getEvents().get(0).getEventTypeDesc());
		row.add(vo);
		vo = new GenericVO();
		vo.setKey("Seminar Promotion #");
		vo.setValue(sem.getRSVPCodes());
		row.add(vo);
		vo = new GenericVO();
		
		StringBuilder sb = new StringBuilder();
		
		for (PersonVO p : sem.getPeople()) {
			vo.setKey(p.getRoleCode().toString());
			vo.setValue(sb.append(StringUtil.checkVal(p.getFirstName())).append(" ")
						.append(StringUtil.checkVal(p.getLastName()))
						.append(" (").append(p.getEmailAddress()).append(")").toString());
			row.add(vo );
			sb.setLength(0);
			vo = new GenericVO();
		}
		
		return row;
	}

	/*
	*		adds the title to this report
	*/
	private void getHeader(Row r, Sheet s) {
		
		Cell c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue(TITLE_TEXT);
		//merge it the length of the report.
		s.addMergedRegion(new CellRangeAddress(0,0,0,1));		
	}
	
}
