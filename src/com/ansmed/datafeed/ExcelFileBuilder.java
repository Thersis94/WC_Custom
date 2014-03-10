package com.ansmed.datafeed;

/****************************************************************************
 * <b>Title</b>: ExcelFileBuilder.java<p/>
 * <b>Description: </b> Builds a simple MS Excel file using MS SpreadsheetML XML.
 * Receives an XML wrapper file, builds rows of data and inserts the rows
 * into the wrapper file to create the MS Excel file. 
 * <p/>
 * <b>Copyright:</b> (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Dec. 11, 2008
 * * Updates:
 * October 2010 - Moved the responsibility of loading the label file XML
 * template to the KitRequestProcessor class so that all template files are
 * loaded upon instantiation.  Thus a failure to load one template results in 
 * a clean system exit.
 ****************************************************************************/
public class ExcelFileBuilder {
	
	private int rowCount = 0;
	private StringBuffer rowData = new StringBuffer();
	private StringBuffer fileData = new StringBuffer();
	private String type = "csv";
	private String delimiter = ",";
	
	public ExcelFileBuilder(StringBuffer wrapperFile) {
		fileData = wrapperFile;
	}

	/**
	 * Creates the header row for the row data.
	 * @param header
	 */
	protected void addRowHeader(StringBuffer header) {
		rowData.append(header);
	}
	
	/**
	 * Creates a row containing an address.
	 * @param name
	 * @param address
	 * @param cityStateZip
	 * @param phone
	 */
	protected void addRow(String name, String address, String city, String state, 
			String zip, String phone) {
		
		if (type.equalsIgnoreCase("csv")) {
			// Builds a text, comma-delimited row.
			rowData.append(name).append(delimiter);
			rowData.append(address).append(delimiter);
			rowData.append(city).append(delimiter);
			rowData.append(state).append(delimiter);
			rowData.append(zip).append(delimiter);	
			rowData.append(phone).append(delimiter);
			rowData.append("R").append("\n");
			rowCount++;
		} else {
			// Builds an XLS address row.  Uses specific styles for formatting the zipcode
			// so that Excel can handle it properly.  These styles are defined in the 
			// XML label wrapper file.
			rowData.append("<ss:Row>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(name).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(address).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(city).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(state).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell ss:StyleID=\"NumberAsZipCode\"><ss:Data ss:Type=\"Number\">").append(zip).append("</ss:Data></ss:Cell>");	
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append(phone).append("</ss:Data></ss:Cell>");
			rowData.append("<ss:Cell><ss:Data ss:Type=\"String\">").append("R").append("</ss:Data></ss:Cell>");
			rowData.append("</ss:Row>\n");
			rowCount++;
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
		if (index > -1) fileData.replace(index, index + 9, rowData.toString());
		
		return fileData;
	}
	
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the delimiter
	 */
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * @param delimiter the delimiter to set
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Returns the number of rows created for this file.
	 * @return
	 */
	protected int getRowCount() {
		return rowCount;
	}
	
}
