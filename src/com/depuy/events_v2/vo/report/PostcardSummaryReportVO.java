package com.depuy.events_v2.vo.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	
		for (GenericVO vo : row){
			
			log.debug(vo.getKey());
			log.debug(vo.getValue());
			c = r.createCell(0);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue((String)vo.getKey());
			c = r.createCell(1);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue((String)vo.getValue());	

			r = s.createRow(rowCnt++);
		}
		
		Map <String, String> eventRow = getEventRows();
		
		for (String key : eventRow.keySet()){
			
			log.debug(key);
			log.debug(eventRow.get(key));
			c = r.createCell(0);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(key);
			c = r.createCell(1);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(eventRow.get(key));	

			r = s.createRow(rowCnt++);
		}
		
		s.addMergedRegion(new CellRangeAddress(rowCnt-4,rowCnt-2,0,0));
		
		Map <String, String> postRow = getPostcardRows();
		
		for (String key : postRow.keySet()){
			
			log.debug(key);
			log.debug(postRow.get(key));
			c = r.createCell(0);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(key);
			c = r.createCell(1);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(postRow.get(key));	

			r = s.createRow(rowCnt++);
		}
		
		s.addMergedRegion(new CellRangeAddress(rowCnt-5,rowCnt-2,0,0));
		s.addMergedRegion(new CellRangeAddress(rowCnt-7,rowCnt-6,1,1));
		
		Map <String, String> speakerRow = getSpeakerRows();
		
		for (String key : speakerRow.keySet()){
			
			log.debug(key);
			log.debug(speakerRow.get(key));
			c = r.createCell(0);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(key);
			c = r.createCell(1);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(speakerRow.get(key));	

			r = s.createRow(rowCnt++);
		}
		r = s.createRow(rowCnt++);
		c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue("Ad Information");
		
		r = s.createRow(rowCnt++);
		
		List<GenericVO> adRow = getAdRows();
		
/*				
		//add the Co-Op Ad data
		if (sem.getAllAds() != null && !sem.getAllAds().isEmpty() ) {
			for (CoopAdVO ad : sem.getAllAds() ){
				int adSts = Convert.formatInteger(ad.getStatusFlg(), 0).intValue();
				
				rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
				rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Newspaper Ad</td></tr>\r");
				rpt.append("<tr><td>Ad Type:</td><td align='center'>").append(StringUtil.checkVal(ad.getAdType())).append("</td></tr>\r");
				rpt.append("<tr><td>Sponsored Newspaper:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(ad.getNewspaper1Phone()).append(")</td></tr>\r");
				rpt.append("<tr><td>Coordinator approved ad?:</td><td align='center'>").append((adSts == CoopAdsActionV2.CLIENT_APPROVED_AD) ? "Yes" : "No").append("</td></tr>\r");
				rpt.append("<tr><td>Approved Paper:</td><td align='center'>").append(StringUtil.checkVal(ad.getApprovedPaperName())).append("</td></tr>\r");
				rpt.append("<tr><td>Total Cost:</td><td align='center'>").append(ad.getTotalCostNo()).append("</td></tr>\r");
				//calculate cost of ad to territory or surgeon
				if ("CFSEM".equalsIgnoreCase(sem.getEvents().get(0).getEventTypeCd())) {
					int surgSts = Convert.formatInteger(ad.getSurgeonStatusFlg(), 0).intValue();
					rpt.append("<tr><td>Speaker approved ad?:</td><td align='center'>").append((surgSts == CoopAdsActionV2.SURG_APPROVED_AD) ? "Yes" : "No").append("</td></tr>\r");
					rpt.append("<tr><td>Speaker paid for ad?:</td><td align='center'>").append((surgSts == CoopAdsActionV2.SURG_PAID_AD) ? "Yes" : "No").append("</td></tr>\r");
					rpt.append("<tr><td>Ad Cost to Speaker:</td><td align='center'>").append(ad.getCostToRepNo()).append("</td></tr>\r");
				} else {
					rpt.append("<tr><td>Ad Cost to Territory:</td><td align='center'>").append(ad.getCostToRepNo()).append("</td></tr>\r");
				}
				if (ad.getAdFileUrl() != null)
					rpt.append("<tr><td>Ad File:</td><td align='center'><a href=\"").append(sem.getBaseUrl()).append("/ads/").append(ad.getAdFileUrl()).append("\" target='_blank'>").append(ad.getAdFileUrl()).append("</a></td></tr>\r");
			}
		}
		//add the Radio Ad data
//		if (sem.getRadioAd() != null && sem.getRadioAd().getCoopAdId() != null) {
//			CoopAdVO ad = sem.getRadioAd();
//			rpt.append("<tr><td colspan='2'>&nbsp;</td></tr>\r");
//			rpt.append("<tr><td colspan='2' style='background-color: #ccc;'><b>Radio Ad</td></tr>\r");
//			rpt.append("<tr><td>Radio Station:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(StringUtil.checkVal(ad.getNewspaper1Phone())).append(")</td></tr>\r");
//			rpt.append("<tr><td>Contact Name:</td><td align='center'>").append(StringUtil.checkVal(ad.getNewspaper2Text())).append(" (").append(StringUtil.checkVal(ad.getNewspaper2Phone())).append(")</td></tr>\r");
//			rpt.append("<tr><td>Ad Deadline:</td><td align='center'>").append(StringUtil.checkVal(ad.getAdDatesText())).append("</td></tr>\r");
//		}
		
		rpt.append(this.getFooter());*/
		
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
	 * generates the ad section of the report
	 * @return
	 */
	private List<GenericVO> getAdRows() {
		
		//add the Co-Op Ad data
		if (sem.getAllAds() != null && !sem.getAllAds().isEmpty() ) {
			StringBuilder sb = new StringBuilder(32);
			List<GenericVO> row = new ArrayList<GenericVO>();
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
				vo.setValue(ad.getTotalCostNo());
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
					vo.setValue(ad.getCostToRepNo());
					row.add(vo);
				}else{
					vo = new GenericVO();
					vo.setKey("Ad Cost to Territory:");
					vo.setValue(ad.getCostToRepNo());
					row.add(vo);
				}
				
				if (ad.getAdFileUrl() != null){
					vo = new GenericVO();
					vo.setKey("Ad File:");
					vo.setValue("");
					row.add(vo);
				}
				
				
				
			}
		}
		
		
		
		/*
		rpt.append("<tr><td></td><td align='center'><a href=\"").append(sem.getBaseUrl()).append("/ads/").append(ad.getAdFileUrl()).append("\" target='_blank'>").append(ad.getAdFileUrl()).append("</a></td></tr>\r");
		}*/
		return null;
	}

	/**
	 * generates the speaker section of the report
	 * @return
	 */
	private Map<String, String> getSpeakerRows() {
		StringBuilder sb = new StringBuilder(32);
		Map<String, String> row = new LinkedHashMap<String, String>();
		
		row.put("","");
		row.put("Speaker Information","");
		for (DePuyEventSurgeonVO surg : sem.getSurgeonList()) {
			row.put("Speaker Name:",surg.getSurgeonName());
			row.put("The Field Marketing Director has reviewed the Speaker Guidelines with speaker?:",surg.getSeenGuidelinesFlg() == 1 ? "yes" : "no");
			row.put("Years practicing: ", String.valueOf(surg.getExperienceYrs()));
			row.put("Years at current practice:", String.valueOf(surg.getPractYrs()));
			row.put("Employed by hospital?:",surg.getHospEmployeeFlg() == 1 ? "yes" : "no");
			row.put("Hospital Address:",StringUtil.checkVal(surg.getHospAddress()));
			String location = (surg.getPractLocation() != null) ? surg.getPractLocation().getFormattedLocation() : "";
			row.put("Practice Address:",location);
			row.put("Practice Phone:", surg.getPractPhone());
			row.put("Speaker/Office Email(s):",surg.getPractEmail());
			row.put("Secondary Contact:",surg.getSecPhone());
			row.put("Secondary Contact Email:",surg.getSecEmail());
			row.put("Practice Website:",StringUtil.checkVal(surg.getPractWebsite()));
			row.put("Speaker Photo:",surg.getLogoFileUrl());
			row.put("Speaker Bio:",StringUtil.checkVal(surg.getSurgeonBio()));

		}
		
		return row;
	}

	/**
	 * generates the post card section of the report
	 * @return
	 */
	private Map<String, String> getPostcardRows() {
		StringBuilder sb = new StringBuilder(32);
		Map<String, String> row = new LinkedHashMap<String, String>();
		
		row.put("","");
		row.put("Date of Seminar",sb.append(Convert.formatDate(sem.getEarliestEventDate(), Convert.DATE_LONG)).toString());
		sb.setLength(0);
		row.put("RSVP Deadline",sb.append(Convert.formatDate(sem.getRSVPDate(), Convert.DATE_LONG)).toString());
		sb.setLength(0);
		row.put("Initial Invitation Send Date", Convert.formatDate(sem.getPostcardSendDate(), Convert.DATE_LONG));
		row.put("Additional Postcards Send Date: ",Convert.formatDate(sem.getAddtlPostcardSendDate(), Convert.DATE_LONG));
		row.put("Additional cards should have the letter \"S\" at the end of the seminar#","");
		UserDataVO owner = sem.getOwner();
		row.put("Additional Postcards","Send additional postcards to:");
		
		row.put("add1",sb.append(StringUtil.checkVal(owner.getFirstName())).append(" ").append(StringUtil.checkVal(owner.getLastName())).toString());
		sb.setLength(0);
		row.put("add2",StringUtil.checkVal(owner.getAddress()));
		row.put("add3",sb.append(StringUtil.checkVal(owner.getCity())).append(", ").append(StringUtil.checkVal(owner.getState())).append(" ").append(StringUtil.checkVal(owner.getZipCode())).toString());
		sb.setLength(0);
		
		
		return row;
	}

	/**
	 * generates the event section of the report
	 * @return
	 */
	private Map<String, String> getEventRows() {
		Map<String, String> row = new LinkedHashMap<String, String>();
		StringBuilder sb = new StringBuilder(32);
		
		for (EventEntryVO event : sem.getEvents()) {
			row.put("","");
			row.put(sb.append("Seminar #").append(event.getRSVPCode()).toString(),event.getEventName());
			sb.setLength(0);
			row.put("Seminar Date/Time",sb.append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append(" ").append(event.getLocationDesc()).toString());
			sb.setLength(0);
			row.put("Seminar Location",sb.append(event.getCityName()).append(", ").append(event.getStateCode()).append(" ").append(event.getZipCode()).toString());
			sb.setLength(0);
			row.put("Joint",sem.getJointLabel());
			if (sem.getProductCodes().length() > 0) row.put("Product",sem.getProductCodes());
			row.put("Venue Location",event.getEventDesc());
			row.put("Venue Name",event.getEventName());
			row.put("Refreshment Choice",StringUtil.checkVal(event.getServiceText()));
			row.put("Venue Address", event.getAddressText());
			row.put("ven1", event.getAddress2Text());
			row.put("ven2", sb.append(event.getCityName()).append(" " ).append(event.getStateCode()).append(", " ).append(event.getZipCode()).toString());

		}
		return row;
	}

	/**
	 * used generic vos so i could deal with areas that didn't have unique keys
	 * generates the top part of the report as a map
	 * @return
	 */
	private List<GenericVO> getSeminarRows() {
		List<GenericVO> row = new ArrayList<GenericVO>();
		
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
