package com.ansmed.datafeed.ambassador;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.smt.sitebuilder.action.survey.SurveyDataContainer;
import com.smt.sitebuilder.action.survey.SurveyDataModuleVO;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title</b>: AmbassadorReportBuilder.java<p/>
 * <p><b>Description: </b> Builds a simple MS Excel file using MS SpreadsheetML XML.
 * Receives an XML wrapper file, builds rows of data and inserts the rows
 * into the wrapper file to create the MS Excel file. 
 * <p/>
 * <p><b>Copyright:</b> (c) 2009<p/>
 * <p><b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since May 28, 2009
 ****************************************************************************/
public class AmbassadorReportBuilder {

	private int rowCount = 0;
	private StringBuffer rowData = new StringBuffer();
	private StringBuffer fileData = new StringBuffer();
	private String templatePath = "scripts//";
	
	/**
	 * 
	 * @param wrapperFile
	 */
	public AmbassadorReportBuilder(String wrapperFile) {
		// Load the XML template file from the filesystem
		loadTemplate(wrapperFile);
	}
	
	/**
	 * Creates the header row for the row data.
	 * @param fields
	 */
	protected void addHeaderRow(List<String> fields) {
		
		if (fields.isEmpty()) return;
		
		StringBuffer header = new StringBuffer("<ss:Row>");
		header.append("<ss:Cell><ss:Data ss:Type=\"String\">Date</ss:Data></ss:Cell>");
		header.append("<ss:Cell><ss:Data ss:Type=\"String\">Site Id</ss:Data></ss:Cell>");
		header.append("<ss:Cell><ss:Data ss:Type=\"String\">Transaction Id</ss:Data></ss:Cell>");
		header.append("<ss:Cell><ss:Data ss:Type=\"String\">Survey Name</ss:Data></ss:Cell>");
				
		for (Iterator<String> iter = fields.iterator(); iter.hasNext();) {
			header.append("<ss:Cell><ss:Data ss:Type=\"String\">");
			header.append(iter.next());
			header.append("</ss:Data></ss:Cell>");
		}
		
		header.append("</ss:Row>");
				
		rowData.append(header);
	}
	
	/**
	 * Creates rows of data
	 * @param cdc
	 */
	protected void addRows(SurveyDataContainer cdc) {
		
		// Builds an address row.  Uses specific styles for formatting the zipcode
		// so that Excel can handle it properly.  These styles are defined in the 
		// XML label wrapper file.
		List<SurveyDataModuleVO> vo = cdc.getCoreData();
		Map<String,String[]> extData = cdc.getExtData();
		
		for (Iterator<SurveyDataModuleVO> iter = vo.iterator(); iter.hasNext();) {
			SurveyDataModuleVO sdmvo = iter.next();
			rowData.append("<ss:Row>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(sdmvo.getSubmittalDate()).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(sdmvo.getSiteId()).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(sdmvo.getTransactionId()).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(sdmvo.getActionName()).append("</ss:Data></ss:Cell>");
			
			String[] data = extData.get(sdmvo.getTransactionId());
			String val = null;
			if (data.length > 0) {
				for (int i = 0; i < data.length; i++) {
					val = StringUtil.checkVal(data[i]);
					val = val.replace("<", "");
					val = val.replace(">", "");
					rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">");
					rowData.append(val);
					rowData.append("</ss:Data></ss:Cell>");
				}
			}
			rowData.append("</ss:Row>\n");
			rowCount++;			
		}
	}
	
	/**
	 * Loads template from file system based using template name passed as param.
	 * @param template
	 */
	protected void loadTemplate(String template) {
		
		try {
			FileReader fr = new FileReader(templatePath + template);
			BufferedReader br = new BufferedReader(fr);
			String strIn = "";
			
			while((strIn = br.readLine()) != null) {
				fileData.append(strIn).append("\n");
			}
			
			fr.close();
						
		} catch (FileNotFoundException fe) {
			System.exit(-1);
			
		} catch (IOException ioe) {
			System.exit(-1);
		}
	}
	
	/**
	 * Inserts the row data into the XML wrapper template and returns the
	 * completed XML file as a StringBuffer.
	 * @return
	 */
	protected StringBuffer getFileData() {
		
		int index = -1;
		
		index = fileData.indexOf("#rowData#");
		fileData.replace(index, index + 9, rowData.toString());
		
		return fileData;
	}
	
	/**
	 * Returns the number of rows created for this file.
	 * @return
	 */
	protected int getRowCount() {
		return rowCount;
	}
	
}
