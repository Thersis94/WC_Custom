/**
 * 
 */
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
		
		//Retrieve Column Names
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		//Build Excel File
		Workbook wb = new HSSFWorkbook();
		Sheet sheet = wb.createSheet("importValues");
		
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
		//If there are no results then Say so.
		if(dc.getTransactions().size() == 0) {
			cell = row.createCell(c++);
			cell.setCellValue("No Results Found");
		}
		
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
			addCell(c++, siteUrl + vo.getFieldById(PAFConst.PROFILE_IMAGE_ID.getId()).getResponses().get(0), row);

			//Set Joints
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for(String s : vo.getFieldById(PAFConst.JOINT_ID.getId()).getResponses()) {
				if(i > 0) sb.append(", ");
				sb.append(s);
				i++;
			}
			addCell(c++, sb.toString(), row);
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
			if(vo.getFieldById(PAFConst.OTHER_HOBBY_ID.getId()).getResponses().size() > 0 && StringUtil.checkVal(vo.getFieldById(PAFConst.OTHER_HOBBY_ID.getId()).getResponses().get(0)).length() > 0) {
				if(i > 0) sb.append(", ");
				sb.append(vo.getFieldById(PAFConst.OTHER_HOBBY_ID.getId()).getResponses().get(0));
			}
			addCell(c++, sb.toString(), row);

			//Add Has had Replacement
			addCell(c++, vo.getFieldById(PAFConst.HAS_REPLACED_ID.getId()).getResponses().get(0), row);

			//Add Life Before
			addCell(c++, vo.getFieldById(PAFConst.LIFE_BEFORE_ID.getId()).getResponses().get(0), row);

			//Add Turning Point
			addCell(c++, vo.getFieldById(PAFConst.TURNING_POINT_ID.getId()).getResponses().get(0), row);

			//Add Life After
			addCell(c++, vo.getFieldById(PAFConst.LIFE_AFTER_ID.getId()).getResponses().get(0), row);

			//Add Advice
			addCell(c++, vo.getFieldById(PAFConst.ADVICE_ID.getId()).getResponses().get(0), row);

			//Close out the Transaction Row.
			row = sheet.createRow(r++);
		}

		//Write xls to ByteStream and return.
		try {
			wb.write(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			log.error(e);
		} finally {
			try {baos.close();} catch (IOException e) {log.error(e);}
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
	
	//Set the SiteUrl for image pathing.
	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}
	
	private List<String> getHeaders() {
		List<String> headers = new ArrayList<String>();
		headers.add("Author Name");
		headers.add("Email");
		headers.add("City");
		headers.add("State");
		headers.add("ZipCode");
		headers.add("ImageUrl");
		headers.add("Joints");
		headers.add("Hobbies");
		headers.add("Have a replacement?");
		headers.add("Life Before?");
		headers.add("Turning Point");
		headers.add("Life After");
		headers.add("Advice for others");
		return headers;
	}
	
	private void addCell(int cellPos, String value, Row r) {
		Cell cell = r.createCell(cellPos);
		cell.setCellValue(value);
	}

}
