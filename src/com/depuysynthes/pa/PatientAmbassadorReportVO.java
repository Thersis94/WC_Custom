package com.depuysynthes.pa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.depuysynthes.pa.PatientAmbassadorStoriesTool.PAFConst;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadorReportVO.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Report VO Used to export Patient Story Records from the
 * PatientAmbassadorStory Data Tool.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 12, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class PatientAmbassadorReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DataContainer dc = null;
	private String siteUrl = null;
	/**
	 * 
	 */
	public PatientAmbassadorReportVO() {
		super();
	}
	
	public PatientAmbassadorReportVO(String fileName) {
		super();
		setFileName(fileName);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		//Build Excel File
		Workbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet("importValues");

		/*
		 * decide if we are writing a landscape (multi-result) report,
		 * a portrait (single-result) report or print No results found.
		 */
		if (dc.getTransactions().size() > 1) {
			landscapeReport(sheet);
		} else if (dc.getTransactions().size() == 1) {
			portraitReport(sheet);
		} else {
			Row row = sheet.createRow(0);
			row.createCell(0).setCellValue("No Results Found");
		}

		//Write xls to ByteStream and return.
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			log.error(e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		dc = (DataContainer) o;
	}

	/**
	 * Helper method used to set the siteUrl so that the profileImages have
	 * a full path that can be used in a browser.
	 * @param siteUrl
	 */
	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	/**
	 * Helper method that provides the list of Headers to be written.
	 * @return
	 */
	private List<String> getHeaders() {
		List<String> headers = new ArrayList<String>();
		headers.add("Patient Name");
		headers.add("Email");
		headers.add("City");
		headers.add("State");
		headers.add("Zipcode");
		headers.add("Photo URL");
		headers.add("Joints");
		headers.add("Hobbies");
		headers.add("Surgeon Name");
		headers.add("Had Surgery");
		headers.add("Life Before");
		headers.add("Turning Point");
		headers.add("Life After");
		headers.add("Advice for others");
		headers.add("Story Title");
		headers.add("Story Text");
		headers.add("Status");
		headers.add("User opened consent statement");
		headers.add("User was emailed consent statement");
		headers.add("User agreed to consent statement");

		return headers;
	}

	/**
	 * Helper methods that adds a Cell to the given row.
	 * @param cellPos
	 * @param value
	 * @param r
	 */
	private void addCell(int cellPos, FormFieldVO vo, Row r) {
		String value = (vo != null) ? vo.getResponseText() : "";
		addCell(cellPos,value,r);
	}
	private void addCell(int cellPos, String value, Row r) {
		Cell cell = r.createCell(cellPos);
		cell.setCellValue(StringUtil.checkVal(value));
	}

	/**
	 * Method that generates a Landscape report.  This is used when we have
	 * multiple results to print in the worksheet.
	 * @param sheet
	 */
	private void landscapeReport(Sheet sheet) {
		int c = 0, r = 0;
		
		Row row = sheet.createRow(r++);
		Cell cell = null;
		//Loop Headers and set cell values.
		for(String n : getHeaders()) {
			cell = row.createCell(c++);
			cell.setCellValue(n);
		}
		c = 0;
		row = sheet.createRow(r++);
		
		//Loop over transactions and print data appropriately.
		for(FormTransactionVO vo : dc.getTransactions().values()) {
			//reset cell counter
			c = 0;

			//Add Name
			addCell(c++, vo.getFirstName() + " " + vo.getLastName(), row);

			//Add Email
			addCell(c++, vo.getEmailAddress(), row);

			//Add City
			addCell(c++, vo.getCity(), row);

			//Add State
			addCell(c++, vo.getState(), row);

			//Add Zip
			addCell(c++, vo.getZipCode(), row);

			//Set Image Url
			String imgPath = "";
			if (vo.getFieldById(PAFConst.PROFILE_IMAGE_ID.getId()) != null)
					imgPath = vo.getFieldById(PAFConst.PROFILE_IMAGE_ID.getId()).getResponseText();
			if (imgPath.length() > 0) imgPath = "http://" + siteUrl + imgPath;
			addCell(c++, imgPath, row);

			//Set Joints
			StringBuilder sb = new StringBuilder();
			int i = 0;
			if (vo.getFieldById(PAFConst.JOINT_ID.getId()) != null) {
				for(String s : vo.getFieldById(PAFConst.JOINT_ID.getId()).getResponses()) {
					if(i > 0) sb.append(", ");
					sb.append(s);
					i++;
				}
			}
			addCell(c++, sb.toString(), row);
			i = 0;

			//Set Hobbies
			sb = new StringBuilder();
			if (vo.getFieldById(PAFConst.HOBBIES_ID.getId()) != null) {
				for (String s : vo.getFieldById(PAFConst.HOBBIES_ID.getId()).getResponses()) {
					if (i > 0) sb.append(", ");
					//Skip other, we'll add it later.
					if (!s.equals("OTHER")) {
						sb.append(s);
						i++;
					}
				}
			}

			//Add Other Hobby if present.
			String other = (vo.getFieldById(PAFConst.OTHER_HOBBY_ID.getId()) != null) ? vo.getFieldById(PAFConst.OTHER_HOBBY_ID.getId()).getResponseText() : "";
			if (other != null && other.length() > 0) {
				if (i > 0) sb.append(", ");
				sb.append(other);
			}
			addCell(c++, sb.toString(), row);
			
			//Add Surgeon Name
			addCell(c++, vo.getFieldById(PAFConst.SURGEON_NM.getId()), row);

			//Add Has had Replacement
			String response = (vo.getFieldById(PAFConst.HAS_REPLACED_ID.getId()) != null) ? vo.getFieldById(PAFConst.HAS_REPLACED_ID.getId()).getResponseText() : null;
			addCell(c++, StringUtil.checkVal(response).replace("_", " "), row);

			//Add Life Before
			addCell(c++, vo.getFieldById(PAFConst.LIFE_BEFORE_ID.getId()), row);

			//Add Turning Point
			addCell(c++, vo.getFieldById(PAFConst.TURNING_POINT_ID.getId()), row);

			//Add Life After
			addCell(c++, vo.getFieldById(PAFConst.LIFE_AFTER_ID.getId()), row);

			//Add Advice
			addCell(c++, vo.getFieldById(PAFConst.ADVICE_ID.getId()), row);

			//Add Story Title
			addCell(c++, vo.getFieldById(PAFConst.STORY_TITLE_ID.getId()), row);

			//Add Story Text
			addCell(c++, vo.getFieldById(PAFConst.STORY_TEXT_ID.getId()), row);
			
			//Add Status Text
			addCell(c++, vo.getFieldById(PAFConst.STATUS_ID.getId()), row);

			//Add Modal Opened Text
			if(vo.getFieldById(PAFConst.MODAL_OPENED_ID.getId()) != null)
				addCell(c++, vo.getFieldById(PAFConst.MODAL_OPENED_ID.getId()).getResponseText(), row);
			else
				addCell(c++, "No", row);

			//Add Email Consent Text
			if(vo.getFieldById(PAFConst.EMAIL_CONSENT_ID.getId()) != null)
				addCell(c++, vo.getFieldById(PAFConst.EMAIL_CONSENT_ID.getId()).getResponseText(), row);
			else
				addCell(c++, "No", row);

			//Add agreed Consent Flag
			addCell(c++, ((vo.getAcceptPrivacyFlg() == 1) ? "Yes" : "No"), row);
			
			//Close out the Transaction Row.
			row = sheet.createRow(r++);
		}

	}

	/**
	 * Method prints out a 2 column report of (header, value) pairs for use
	 * when working with a single item report.
	 * @param sheet
	 */
	private void portraitReport(Sheet sheet) {
		int r = 0;
		FormTransactionVO vo = dc.getTransactions().values().iterator().next();

		//Write Name in Portrait
		addRow(r++, vo.getFirstName() + " " + vo.getLastName(), sheet);

		//Add Email
		addRow(r++, vo.getEmailAddress(), sheet);

		//Add City
		addRow(r++, vo.getCity(), sheet);

		//Add State
		addRow(r++, vo.getState(), sheet);

		//Add Zip
		addRow(r++, vo.getZipCode(), sheet);


		//Set Image Url
		String imgPath = "";
		if (vo.getFieldById(PAFConst.PROFILE_IMAGE_ID.getId()) != null)
				imgPath = vo.getFieldById(PAFConst.PROFILE_IMAGE_ID.getId()).getResponseText();
		if (imgPath.length() > 0) imgPath = "http://" + siteUrl + imgPath;
		addRow(r++, imgPath, sheet);

		//Set Joints
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(String s : vo.getFieldById(PAFConst.JOINT_ID.getId()).getResponses()) {
			if(i > 0) sb.append(", ");
			sb.append(s);
			i++;
		}
		addRow(r++, sb.toString(),sheet);
		i = 0;

		//Set Hobbies
		sb = new StringBuilder();
		for(String s : vo.getFieldById(PAFConst.HOBBIES_ID.getId()).getResponses()) {
			if(i > 0) sb.append(", ");
			//Skip other, we'll add it later.
			if(!s.equals("OTHER")) {
				sb.append(s);
				i++;
			}
		}

		//Add Other Hobby if present.
		String other = (vo.getFieldById(PAFConst.OTHER_HOBBY_ID.getId()) != null) ? vo.getFieldById(PAFConst.OTHER_HOBBY_ID.getId()).getResponseText() : "";
		if (other != null && other.length() > 0) {
			if (i > 0) sb.append(", ");
			sb.append(other);
		}
		addRow(r++, sb.toString(), sheet);
		
		//Add Surgeon Name
		addRow(r++, vo.getFieldById(PAFConst.SURGEON_NM.getId()), sheet);

		//Add Has had Replacement
		addRow(r++, vo.getFieldById(PAFConst.HAS_REPLACED_ID.getId()), sheet);

		//Add Life Before
		addRow(r++, vo.getFieldById(PAFConst.LIFE_BEFORE_ID.getId()), sheet);

		//Add Turning Point
		addRow(r++, vo.getFieldById(PAFConst.TURNING_POINT_ID.getId()), sheet);

		//Add Life After
		addRow(r++, vo.getFieldById(PAFConst.LIFE_AFTER_ID.getId()), sheet);

		//Add Advice
		addRow(r++, vo.getFieldById(PAFConst.ADVICE_ID.getId()).getResponseText(), sheet);

		//Add Story Title
		if(vo.getFieldById(PAFConst.STORY_TITLE_ID.getId()) != null)
			addRow(r++, vo.getFieldById(PAFConst.STORY_TITLE_ID.getId()).getResponseText(), sheet);
		else
			addRow(r++, "", sheet);

		//Add Story Text
		if(vo.getFieldById(PAFConst.STORY_TEXT_ID.getId()) != null)
			addRow(r++, vo.getFieldById(PAFConst.STORY_TEXT_ID.getId()).getResponseText(), sheet);
		else
			addRow(r++, "", sheet);

		//Add Status Text
		if(vo.getFieldById(PAFConst.STATUS_ID.getId()) != null)
			addRow(r++, vo.getFieldById(PAFConst.STATUS_ID.getId()).getResponseText(), sheet);
		else
			addRow(r++, "", sheet);

		//Add Modal Opened Flag
		if(vo.getFieldById(PAFConst.MODAL_OPENED_ID.getId()) != null)
			addRow(r++, vo.getFieldById(PAFConst.MODAL_OPENED_ID.getId()).getResponseText(), sheet);
		else
			addRow(r++, "No", sheet);

		//Add Email Consent Flag
		if(vo.getFieldById(PAFConst.EMAIL_CONSENT_ID.getId()) != null)
			addRow(r++, vo.getFieldById(PAFConst.EMAIL_CONSENT_ID.getId()).getResponseText(), sheet);
		else
			addRow(r++, "No", sheet);
		
		//Add agreed Consent Flag
		addRow(r++, ((vo.getAcceptPrivacyFlg() == 1) ? "Yes" : "No"), sheet);
	}
	
	

	/**
	 * Helper methods for writing a pair of cells into a row.
	 * @param r
	 * @param value
	 * @param s
	 */
	private void addRow(int r, FormFieldVO vo, Sheet s) {
		String value = (vo != null) ? vo.getResponseText() : "";
		addRow(r,value,s);
	}
	private void addRow(int r, String value, Sheet s) {
		Row row = s.createRow(r);
		row.createCell(0).setCellValue(getHeaders().get(r));
		row.createCell(1).setCellValue(StringUtil.checkVal(value));
	}
}
