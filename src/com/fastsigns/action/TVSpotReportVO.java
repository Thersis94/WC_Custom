package com.fastsigns.action;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Row;

/*****************************************************************************
 <p><b>Title</b>: TVSpotReportVO.java</p>
 <p>Description: <b/>Excel representation of the TV Spot contact us portlet/data.</p>
 <p>Copyright: Copyright (c) 2000 - 2014 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 27, 2014
 ***************************************************************************/

public class TVSpotReportVO extends AbstractSBReportVO {
	private static final long serialVersionUID = 1l;
	private ContactDataContainer cdc = null;

	public TVSpotReportVO() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("Commercial Consultation Report.xls");
	}

	/**
	 * set the data object
	 */
	@Override
	public void setData(Object o) {
		cdc = (ContactDataContainer) o;
	}

	@Override
	public byte[] generateReport() {
		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet sheet = workbook.createSheet("Report");
		log.debug("starting generateReport()");
		getHeader(sheet);

		int rowNum = 2;
		for (ContactDataModuleVO vo  : cdc.getData()) 
			appendRow(vo, sheet.createRow(rowNum++), workbook.createCellStyle());
		
		setColWidths(sheet);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			workbook.write(baos);
		} catch (IOException e) {
			log.error("Could not serialize workbook properly ", e);
			return null;
		}
		return baos.toByteArray();
	}
	
	/**
	 * Widen the columns for easier readability
	 * @param sheet
	 */
	private void setColWidths(HSSFSheet sheet) {
		int colNum = 0;
		sheet.setColumnWidth(colNum++, 8000);
		sheet.setColumnWidth(colNum++, 4000);
		sheet.setColumnWidth(colNum++, 4000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 5000);
		sheet.setColumnWidth(colNum++, 8000);
		sheet.setColumnWidth(colNum++, 4000);
		sheet.setColumnWidth(colNum++, 10000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 2000);
		sheet.setColumnWidth(colNum++, 15000);
		sheet.setColumnWidth(colNum++, 4000);
		sheet.setColumnWidth(colNum++, 3000);
		sheet.setColumnWidth(colNum++, 4000);
		sheet.setColumnWidth(colNum++, 15000);
		sheet.setColumnWidth(colNum++, 6000);
		sheet.setColumnWidth(colNum++, 7000);
		sheet.setColumnWidth(colNum++, 7000);
		sheet.setColumnWidth(colNum++, 15000);
	}
	

	/**
	 * Generate a map of reports for each center
	 * @return
	 */
	public Map<String, HSSFWorkbook> generateCenterReport() {
		log.debug("starting generateReport()");
		Calendar now = Calendar.getInstance();
		Map<String, HSSFWorkbook> byCenter = new HashMap<String, HSSFWorkbook>();
		HSSFWorkbook book;
		HSSFSheet sheet;
		HSSFRow row;
		
		// We cycle through this list in reverse order because we only want to create reports
		// for centers that have had a request in the last week.
        ListIterator<ContactDataModuleVO> li = cdc.getData().listIterator(cdc.getData().size());
        ContactDataModuleVO vo;
		while (li.hasPrevious()) {
			vo = li.previous();
			book = byCenter.get(vo.getDealerLocation().getOwnerEmail());
			
			// If we don't have this center in the map already start a new one..
			if (book == null) {
				// If the center's most recent request was more than a week ago we skip them.
				if (((vo.getSubmittalDate().getTime() - now.getTimeInMillis()) / (1000 * 60 * 60 * 24)) < -7) 
					continue;
				book = new HSSFWorkbook();
				sheet = book.createSheet("Report");
				getHeader(sheet);
				setColWidths(sheet);
				row = sheet.createRow(2);
			} else {
				row = book.getSheet("Report").createRow(book.getSheet("Report").getLastRowNum()+1);
			}
			
			appendRow(vo, row, book.createCellStyle());
			
			byCenter.put(vo.getDealerLocation().getOwnerEmail(), book);
		}
		
		return byCenter;
	}
	
	private void appendRow(ContactDataModuleVO vo, Row row, HSSFCellStyle style) {
		int cellNum = 0;
		Date d = vo.getSubmittalDate();
		PhoneNumberFormat pnf = new PhoneNumberFormat(vo.getMainPhone(), PhoneNumberFormat.DASH_FORMATTING);
		row.createCell(cellNum++).setCellValue(Convert.formatDate(d, Convert.DATE_SLASH_PATTERN));
		row.createCell(cellNum++).setCellValue(Convert.formatDate(d, Convert.TIME_LONG_PATTERN));
		row.createCell(cellNum++).setCellValue(vo.getDealerLocationId());
		row.createCell(cellNum++).setCellValue(vo.getDealerLocation().getOwnerName());
		row.createCell(cellNum++).setCellValue(vo.getFullName());
		row.createCell(cellNum++).setCellValue(vo.getEmailAddress());
		row.createCell(cellNum++).setCellValue(pnf.getFormattedNumber());
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.visitMethod.id())));
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.zipcode.id())));
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.state.id())));
//		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.industry.id())));
//		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.department.id())));
//		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.title.id())));
//		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.companyNm.id())));
//		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.businessChallenge.id())));
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.inquiry.id())));
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.saleAmount.id())));
		Calendar surveySentDt = Calendar.getInstance();
		surveySentDt.setTime(vo.getSubmittalDate());
		surveySentDt.add(Calendar.DAY_OF_YEAR, 7);
		surveySentDt.set(Calendar.HOUR, 6); //6am is when FS email campaigns kick-off
		surveySentDt.set(Calendar.MINUTE, 0);
		surveySentDt.set(Calendar.SECOND, 0);
		row.createCell(cellNum++).setCellValue((Calendar.getInstance().getTime().before(surveySentDt.getTime())) ? "No" : "Yes");
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.rating.id())));
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.feedback.id())));
		row.createCell(cellNum++).setCellValue(StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.consultation.id())));
		TVSpotUtil.Status status = TVSpotUtil.Status.valueOf(vo.getExtData().get(TVSpotUtil.ContactField.status.id()));
		if (status == TVSpotUtil.Status.initiated) {
			row.createCell(cellNum).setCellValue(status.getLabel());
			style.setFillForegroundColor(HSSFColor.RED.index);
			row.getCell(cellNum++).setCellStyle(style);
		} else {
			row.createCell(cellNum).setCellValue(status.getLabel());
			row.getCell(cellNum++).setCellValue(status.getLabel());
		}
		row.createCell(cellNum++).setCellValue(status.getLabel());
		String notes = StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.transactionNotes.id()));
		row.createCell(cellNum++).setCellValue(notes);
	}
	
	private void getHeader(HSSFSheet sheet) {
		int cellNum = 0;
		sheet.createRow(0).createCell(cellNum++).setCellValue("Commercial Consultation Report - ");
		sheet.getRow(0).createCell(cellNum++).setCellValue(Convert.formatDate(new Date(),  Convert.DATE_SLASH_PATTERN));
		
		cellNum = 0;
		Row row = sheet.createRow(1);
		
		row.createCell(cellNum++).setCellValue("Date");
		row.createCell(cellNum++).setCellValue("Time");
		row.createCell(cellNum++).setCellValue("Web Number");
		row.createCell(cellNum++).setCellValue("Franchise Owner");
		row.createCell(cellNum++).setCellValue("Prospect Name");
		row.createCell(cellNum++).setCellValue("Prospect Email");
		row.createCell(cellNum++).setCellValue("Phone Number");
		row.createCell(cellNum++).setCellValue("Way they Reached the site");
		row.createCell(cellNum++).setCellValue("Zip Code");
		row.createCell(cellNum++).setCellValue("State");
//		row.createCell(cellNum++).setCellValue("Industry");
//		row.createCell(cellNum++).setCellValue("Department");
//		row.createCell(cellNum++).setCellValue("Title");
//		row.createCell(cellNum++).setCellValue("Company Name");
//		row.createCell(cellNum++).setCellValue("Business Challenge");
		row.createCell(cellNum++).setCellValue("Customer Request");
		row.createCell(cellNum++).setCellValue("Sale Amount");
		row.createCell(cellNum++).setCellValue("Survey Sent");
		row.createCell(cellNum++).setCellValue("Survey Rating");
		row.createCell(cellNum++).setCellValue("Survey Feedback");
		row.createCell(cellNum++).setCellValue("Consultation Complete");
		row.createCell(cellNum++).setCellValue("Status");
		row.createCell(cellNum++).setCellValue("Notes (internal)");
	}
	
}
