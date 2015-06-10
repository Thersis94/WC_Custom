package com.ansmed.datafeed;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/****************************************************************************
 * <b>Title</b>: LabelMaker.java<p/>
 * <b>Description: </b> Builds standard address labels in WordXML format. 
 * <p/>
 * <b>Copyright:</b> (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar. 21, 2008
 ****************************************************************************/
public class LabelMaker {

	private int slot = 0;
	private int rowCount = 0;
	private StringBuilder row = new StringBuilder();
	private StringBuilder blankRow = new StringBuilder();
	private StringBuilder currentRow = new StringBuilder();
	private StringBuilder rowBlock = new StringBuilder();
	private StringBuilder section = new StringBuilder();
	private StringBuilder currentSection = new StringBuilder();
	private StringBuilder sectionBody = new StringBuilder();
	private StringBuilder labelFile = new StringBuilder();
	private String templatePath = "scripts//";
	
	public LabelMaker(String rowFile, String sectionFile, String wrapperFile) {
		loadTemplate(rowFile);
		loadTemplate(sectionFile);
		loadTemplate(wrapperFile);
	}
	
	/**
	 * Adds address to label row.  Passes completed row to section rows.
	 * @param name
	 * @param address
	 * @param cityStateZip
	 */
	protected void addLabel(StringBuilder name, StringBuilder address, StringBuilder cityStateZip) {
		
		int index = -1;
		
		if (slot == 0) {
			currentRow = getNewRow();
		}
		
		slot++;
		
		if (slot % 3 !=0) {
			if (slot == 1) {

				index = currentRow.indexOf("#NAME1#");
				currentRow.replace(index, index + 7, name.toString());
				index = currentRow.indexOf("#ADDRESS1#");
				currentRow.replace(index, index + 10, address.toString());
				index = currentRow.indexOf("#CITYSTATEZIP1#");
				currentRow.replace(index, index + 15, cityStateZip.toString());

			} else {

				index = currentRow.indexOf("#NAME2#");
				currentRow.replace(index, index + 7, name.toString());
				index = currentRow.indexOf("#ADDRESS2#");
				currentRow.replace(index, index + 10, address.toString());
				index = currentRow.indexOf("#CITYSTATEZIP2#");
				currentRow.replace(index, index + 15, cityStateZip.toString());
			}
			
		} else {

			index = currentRow.indexOf("#NAME3#");
			currentRow.replace(index, index + 7, name.toString());
			index = currentRow.indexOf("#ADDRESS3#");
			currentRow.replace(index, index + 10, address.toString());
			index = currentRow.indexOf("#CITYSTATEZIP3#");
			currentRow.replace(index, index + 15, cityStateZip.toString());
			
			addRow(currentRow);
			
			slot = 0;
		}	
	}
	
	/**
	 * Adds label row to section rows.  Passes complete section to section body. 
	 * @param theRow
	 */
	protected void addRow(StringBuilder theRow) {
		
		if (rowCount == 0) {
			rowBlock = new StringBuilder();
		}
		
		rowCount++;
		
		if (rowCount % 10 !=0){
			
			rowBlock.append(theRow).append("\n");
			
		} else {
			
			rowBlock.append(theRow).append("\n");
			addSection(rowBlock);
			rowCount = 0;
			
		}
	}
	
	/**
	 * Adds section to section body.
	 * @param theSection
	 */
	protected void addSection(StringBuilder theRows) {
		
		currentSection = getNewSection();
		
		int index = -1;
		index = currentSection.indexOf("#ROWS#");
		currentSection.replace(index, index + 5, theRows.toString());
		
		sectionBody.append(currentSection);
	}
	
	/**
	 * Pads incomplete row with white space.
	 */
	protected void flushRow() {
		
		StringBuilder lastRow = getCurrentRow();
		int lastSlot = getCurrentSlot();
		int index = -1;
		
		switch(lastSlot) {
			case 0:
				lastRow = getBlankRow();
				addRow(lastRow);
				break;
				
			case 1:
				index = lastRow.indexOf("#NAME2#");
				lastRow.replace(index, index + 7, "");
				index = lastRow.indexOf("#ADDRESS2#");
				lastRow.replace(index, index + 10, "");
				index = lastRow.indexOf("#CITYSTATEZIP2#");
				lastRow.replace(index, index + 15, "");
				index = lastRow.indexOf("#NAME3#");
				lastRow.replace(index, index + 7, "");
				index = lastRow.indexOf("#ADDRESS3#");
				lastRow.replace(index, index + 10, "");
				index = lastRow.indexOf("#CITYSTATEZIP3#");
				lastRow.replace(index, index + 15, "");
				addRow(lastRow);
				break;
				
			case 2:
				index = lastRow.indexOf("#NAME3#");
				lastRow.replace(index, index + 7, "");
				index = lastRow.indexOf("#ADDRESS3#");
				lastRow.replace(index, index + 10, "");
				index = lastRow.indexOf("#CITYSTATEZIP3#");
				lastRow.replace(index, index + 15, "");
				addRow(lastRow);
				break;
				
			default:
				break;
		}

	}
	
	/**
	 * If current section is incomplete, pads current section with blank rows.
	 */
	protected void flushSection() {
		
		int lastRowCount = getRowCount();
		
		if (lastRowCount > 0) {
			
			while (lastRowCount < 10) {
				
				rowBlock.append(getBlankRow());
				lastRowCount++;
				
			}
			addSection(rowBlock);
		}

	}
	
	/**
	 * Loads template from file system based on template name passed as param.
	 * @param template
	 */
	protected void loadTemplate(String template) {
		
		try {
			FileReader fr = new FileReader(templatePath + template);
			BufferedReader br = new BufferedReader(fr);
			String strIn = "";
			
			if (template.toLowerCase().indexOf("row") > 0) {
				while((strIn = br.readLine()) != null) {
					row.append(strIn).append("\n");
				}
			} else if (template.toLowerCase().indexOf("section") > 0) {
					while((strIn = br.readLine()) != null) {
						section.append(strIn).append("\n");
					}
				} else if (template.toLowerCase().indexOf("wrapper") > 0) {
					while((strIn = br.readLine()) != null) {
						labelFile.append(strIn).append("\n");
					}
				}
			
			fr.close();
						
		} catch (FileNotFoundException fe) {
			System.exit(-1);
			
		} catch (IOException ioe) {
			System.exit(-1);
		}

	}

	/**
	 * Returns a label row with blanks in place of text placeholders.
	 * @return
	 */
	protected StringBuilder getBlankRow() {
		
		if (blankRow.length() > 0) {
			
			return blankRow;
			
		} else {

			int index = -1;
			blankRow = new StringBuilder(row);
			
			index = blankRow.indexOf("#NAME1#");
			blankRow.replace(index, index + 7, "");
			index = blankRow.indexOf("#ADDRESS1#");
			blankRow.replace(index, index + 10, "");
			index = blankRow.indexOf("#CITYSTATEZIP1#");
			blankRow.replace(index, index + 15, "");
			index = blankRow.indexOf("#NAME2#");
			blankRow.replace(index, index + 7, "");
			index = blankRow.indexOf("#ADDRESS2#");
			blankRow.replace(index, index + 10, "");
			index = blankRow.indexOf("#CITYSTATEZIP2#");
			blankRow.replace(index, index + 15, "");
			index = blankRow.indexOf("#NAME3#");
			blankRow.replace(index, index + 7, "");
			index = blankRow.indexOf("#ADDRESS3#");
			blankRow.replace(index, index + 10, "");
			index = blankRow.indexOf("#CITYSTATEZIP3#");
			blankRow.replace(index, index + 15, "");
			
			return blankRow;
		}
	}
	
	
	/**
	 * Returns a label row template with text placeholders.
	 * @return
	 */
	protected StringBuilder getNewRow() {
		return new StringBuilder(row);
	}
	
	
	/**
	 * Returns label section template.
	 * @return
	 */
	protected StringBuilder getNewSection() {
		return new StringBuilder(section);
	}
	
	
	/**
	 * Returns the current data position in the current row.
	 * @return
	 */
	protected int getCurrentSlot() {
		return slot;
	}
	
	
	/**
	 * Returns the current row being modified.
	 * @return
	 */
	protected StringBuilder getCurrentRow() {
		return currentRow;
	}
	
	
	/**
	 * Returns the current row count.
	 * @return
	 */
	protected int getRowCount() {
		return rowCount;
	}
	
	
	/**
	 * Returns the current section being modified.
	 * @return
	 */
	protected StringBuilder getCurrentSection() {
		return currentSection;
	}
	
	/**
	 * Returns the completed label file.
	 * @return
	 */
	protected StringBuilder getLabelFile() {
		
		int index = -1;
		flushRow();
		flushSection();
		
		index = labelFile.indexOf("SECTIONS");
		labelFile.replace(index, index + 10, sectionBody.toString());
		
		return labelFile;
	}
	
}
