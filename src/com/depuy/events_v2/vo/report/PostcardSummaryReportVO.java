package com.depuy.events_v2.vo.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
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
import com.siliconmtn.security.UserDataVO;

/*****************************************************************************
 <p><b>Title</b>: PostcardSummaryReportVO.java</p>
 <p>A comprehensive summary for the entire Seminar and all data points.</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Jane 20, 2014
 @updates
 		refactored from HTML stuffed in Excel file to a true POI Excel document.  - 08.24.2016 - JM
 ***************************************************************************/

public class PostcardSummaryReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 11233634423123l;
	private DePuyEventSeminarVO sem;

	private CreationHelper createHelper; //used by Excel for making hyperlinks

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
	@Override
	public void setData(Object o) {
		this.sem = (DePuyEventSeminarVO) o;
	}
	

	/**
	 * main method - called by servlet when its time to stream the report back to the browser
	 */
	@Override
	public byte[] generateReport() {
		if (sem == null) return new byte[0];
		log.debug("starting PostcardSummaryReport");

		//Create Excel Object
		Workbook wb = new HSSFWorkbook();
		Sheet s = wb.createSheet();
		createHelper = wb.getCreationHelper();
		
		//make a heading font we can use to separate the sections
		CellStyle headingStyle = wb.createCellStyle();
		Font font = wb.createFont();
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headingStyle.setFont(font);
		
		// make title row, its the first row in the sheet (0)
		addHeader(s, headingStyle);

		addSeminarRows(s, headingStyle);

		addEventRows(s, headingStyle);

		addPostcardRows(s, headingStyle);

		addSpeakerRows(s, headingStyle);

		addAdRows(s, headingStyle);

		//lastly, stream the WorkBook back to the browser
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException ioe) {
			log.error("could not write output stream", ioe);
		} finally {
			try { 
				wb.close(); 
			} catch (Exception e) {
				log.error("could not close ", e );
			}
		}

		return new byte[0];
	}
	
	
	/**
	 * creates a row and populates the 2 columns using the data provided
	 * supports heading rows (colspan=2) and hyperlinks (clickable links)
	 * @param s
	 * @param label
	 * @param value
	 * @param headingStyle
	 * @param link
	 */
	private void addRow(Sheet s, String label, String value, CellStyle headingStyle, String link) {
		int rowNo = s.getPhysicalNumberOfRows();
		Row r = s.createRow(rowNo);
		
		// the label
		Cell c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue(label);
		
		if (headingStyle != null) { //make it span both columns and use the heading font
			c.setCellStyle(headingStyle);
			s.addMergedRegion(new CellRangeAddress(rowNo, rowNo, 0, 1));
			
		} else { //print the value in column 2
			c = r.createCell(1);
			c.setCellType(Cell.CELL_TYPE_STRING);
			c.setCellValue(StringUtil.checkVal(value));
			if (link != null) {
				Hyperlink hLink = createHelper.createHyperlink(Hyperlink.LINK_URL);
			        hLink.setAddress(link);
			        c.setHyperlink(hLink);
			}
		}
	}
	
	
	/**
	 * overloaded for simplicity when added key/value pairs of plain text to the report.
	 * @param s
	 * @param label
	 * @param value
	 */
	private void addRow(Sheet s, String label, String value) {
		this.addRow(s, label, value, null, null);
	}

	
	/**
	 * used generic vos so i could deal with areas that didn't have unique keys
	 * generates the top part of the report as a map
	 * @return
	 */
	private void addSeminarRows(Sheet s, CellStyle headingStyle) {
		addRow(s, "Product", sem.getJointLabel());
		addRow(s, "Seminar Type", sem.getEvents().get(0).getEventTypeDesc());
		addRow(s, "Seminar Promotion #", sem.getRSVPCodes());
		
		StringBuilder sb;
		for (PersonVO p : sem.getPeople()) {
			sb = new StringBuilder(100);
			sb.append(StringUtil.checkVal(p.getFirstName())).append(" ");
			sb.append(StringUtil.checkVal(p.getLastName()));
			sb.append(" (").append(p.getEmailAddress()).append(")");
			
			addRow(s, p.getRoleCode().toString(), sb.toString());
		}
	}


	/**
	 * generates the event section of the report
	 * @return
	 */
	private void addEventRows(Sheet s, CellStyle headingStyle) {
		StringBuilder sb;
		
		for (EventEntryVO event : sem.getEvents()) {
			addRow(s, "", ""); //empty spacer
			addRow(s, "Seminar #" + event.getRSVPCode(), event.getEventName());

			sb = new StringBuilder(50);
			sb.append(Convert.formatDate(event.getStartDate(),Convert.DATE_LONG)).append(" ").append(event.getLocationDesc());
			addRow(s, "Seminar Date/Time", sb.toString());

			sb = new StringBuilder(50);
			sb.append(event.getCityName()).append(", ").append(event.getStateCode()).append(" ").append(event.getZipCode());
			addRow(s, "Seminar Location", sb.toString());
			
			addRow(s, "Joint", sem.getJointLabel());
			
			if (!sem.getProductCodes().isEmpty())
				addRow(s, "Product", sem.getProductCodes());


			sb = new StringBuilder(100);
			addRow(s, "Venue Location", event.getEventDesc());
			addRow(s, "Venue Name", event.getEventName());
			addRow(s, "Refreshment Choice", event.getServiceText());
			addRow(s, "Venue Address", event.getAddressText());
			if (!StringUtil.checkVal(event.getAddress2Text()).isEmpty())
					addRow(s, "", event.getAddress2Text());
			sb.append(event.getCityName()).append(" " ).append(event.getStateCode()).append(", " ).append(event.getZipCode());
			addRow(s, "", sb.toString());

		}
	}
	
	
	/**
	 * generates the post card section of the report
	 * @return
	 */
	private void addPostcardRows(Sheet s, CellStyle headingStyle) {
		//empty spacer row
		addRow(s, "","");

		addRow(s, "Date of Seminar", Convert.formatDate(sem.getEarliestEventDate(), Convert.DATE_LONG));
		addRow(s, "RSVP Deadline", Convert.formatDate(sem.getRSVPDate(), Convert.DATE_LONG));
		addRow(s, "Initial Invitation Send Date", Convert.formatDate(sem.getPostcardSendDate(), Convert.DATE_LONG));
		addRow(s, "Additional Postcards Send Date: ", Convert.formatDate(sem.getAddtlPostcardSendDate(), Convert.DATE_LONG));
		addRow(s, "Additional cards should have the letter \"S\" at the end of the seminar#", "");
		
		UserDataVO owner = sem.getOwner();
		addRow(s, "Additional Postcards", "Send additional postcards to:");
		//owner name
		StringBuilder sb = new StringBuilder(50);
		sb.append(StringUtil.checkVal(owner.getFirstName())).append(" ").append(StringUtil.checkVal(owner.getLastName()));
		addRow(s, "", sb.toString());
		//owner address
		sb = new StringBuilder(100);
		sb.append(StringUtil.checkVal(owner.getCity())).append(", ");
		sb.append(StringUtil.checkVal(owner.getState())).append(" ");
		sb.append(StringUtil.checkVal(owner.getZipCode()));
		addRow(s, "", owner.getAddress());
		addRow(s, "", sb.toString());
	}


	/**
	 * generates the speaker section of the report
	 * @return
	 */
	private void addSpeakerRows(Sheet s, CellStyle headingStyle) {
		addRow(s, "", ""); //spacer row
		addRow(s, "Speaker Information", "", headingStyle, null);

		for (DePuyEventSurgeonVO surg : sem.getSurgeonList()) {
			addRow(s, "Speaker Name:", surg.getSurgeonName());
			addRow(s, "The Field Marketing Director has reviewed the Speaker Guidelines with speaker?:",
					surg.getSeenGuidelinesFlg() == 1 ? "yes" : "no");

			addRow(s, "Years practicing: ", String.valueOf(surg.getExperienceYrs()));
			addRow(s, "Years at current practice:", String.valueOf(surg.getPractYrs()));
			addRow(s, "Employed by hospital?:", (surg.getHospEmployeeFlg() == 1 ? "yes" : "no"));
			addRow(s, "Hospital Address:", surg.getHospAddress());
			
			String location = (surg.getPractLocation() != null) ? surg.getPractLocation().getFormattedLocation() : "";
			addRow(s, "Practice Address:", location);
			addRow(s, "Practice Phone:", surg.getPractPhone());
			addRow(s, "Speaker/Office Email(s):", surg.getPractEmail());
			addRow(s, "Secondary Contact:", surg.getSecPhone());
			addRow(s, "Secondary Contact Email:", surg.getSecEmail());
			addRow(s, "Practice Website:", surg.getPractWebsite());
			if (surg.getLogoFileUrl() != null && !surg.getLogoFileUrl().isEmpty()) 
				addRow(s, "Speaker Photo:", "View file", null, surg.getLogoFileUrl());
			addRow(s, "Speaker Bio:", surg.getSurgeonBio());
		}
	}
	

	/**
	 * generates the ad section of the report
	 * @return
	 */
	private void addAdRows(Sheet s, CellStyle headingStyle) {
		if (sem.getAllAds() == null || sem.getAllAds().isEmpty())
			return;

		addRow(s, "", ""); //spacer row
		addRow(s, "Ad Information", null, headingStyle, null);
		
		int cnt = 1;
		StringBuilder sb;
		for (CoopAdVO ad : sem.getAllAds() ){
			int adSts = Convert.formatInteger(ad.getStatusFlg(), 0).intValue();

			if (cnt > 1) addRow(s, "", ""); //spacer row
			addRow(s, "Newspaper Ad #" + (cnt++), "");
			addRow(s, "Ad Type:", ad.getAdType());
			
			sb = new StringBuilder(100);
			sb.append(StringUtil.checkVal(ad.getNewspaper1Text())).append(" (").append(ad.getNewspaper1Phone()).append(")");
			addRow(s, "Sponsored Newspaper:", sb.toString());
			
			addRow(s, "Coordinator approved ad?:", adSts == CoopAdsActionV2.CLIENT_APPROVED_AD ? "Yes" : "No");
			addRow(s, "Approved Paper:", ad.getApprovedPaperName());
			addRow(s, "Total Cost:", String.valueOf(ad.getTotalCostNo()));
			
			//calculate cost of ad to territory or surgeon
			if ("CFSEM".equalsIgnoreCase(sem.getEvents().get(0).getEventTypeCd())) {
				int surgSts = Convert.formatInteger(ad.getSurgeonStatusFlg(), 0).intValue();
				addRow(s, "Speaker approved ad?:", surgSts == CoopAdsActionV2.SURG_APPROVED_AD ? "Yes" : "No");
				addRow(s, "Speaker paid for ad?:", surgSts == CoopAdsActionV2.SURG_PAID_AD ? "Yes" : "No");
				addRow(s, "Ad Cost to Speaker:", String.valueOf(ad.getCostToRepNo()));
			} else {
				addRow(s, "Ad Cost to Territory:", String.valueOf(ad.getCostToRepNo()));
			}

			if (ad.getAdFileUrl() != null) {
				sb = new StringBuilder(250);
				sb.append(sem.getBaseUrl()).append("/ads/").append(ad.getAdFileUrl());
				addRow(s, "Ad File:", "View File", null, sb.toString());
			}
		}
	}
	
	
	/**
	 * adds the title to this report
	 * @param s
	 * @param headingStyle
	 */
	private void addHeader(Sheet s, CellStyle headingStyle) {
		Row r = s.createRow(0);
		Cell c = r.createCell(0);
		c.setCellType(Cell.CELL_TYPE_STRING);
		c.setCellValue("Seminar Information");
		c.setCellStyle(headingStyle);
		//merge it the length of the report (2 columns).
		s.addMergedRegion(new CellRangeAddress(0,0,0,1));		
	}
}