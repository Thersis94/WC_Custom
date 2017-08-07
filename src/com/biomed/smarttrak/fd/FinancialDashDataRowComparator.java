package com.biomed.smarttrak.fd;

import java.util.Comparator;

import com.biomed.smarttrak.fd.FinancialDashVO.TableType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.SectionVO;

/**
 * **************************************************************************
 * <b>Title</b>: FinancialDashDataRowComparator
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Comparator for Sorting FD Data Rows
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since July 27, 2017
 ***************************************************************************
 */

public class FinancialDashDataRowComparator implements Comparator<FinancialDashDataRowVO> {
	private String column;
	private int sortDir;
	SmarttrakTree sections;
	TableType tableType;
	
	/**
	 * @param column
	 * @param sortDir
	 */
	public FinancialDashDataRowComparator(String column, int sortDir, SmarttrakTree sections, TableType tableType) {
		this.column = column;
		this.sortDir = sortDir;
		this.sections = sections;
		this.tableType = tableType;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(FinancialDashDataRowVO row1, FinancialDashDataRowVO row2) {
		int comparison = 0;
		
		if ("name".equalsIgnoreCase(column)) {
			comparison = sortByNameColumn(row1, row2);
		} else {
			comparison = sortByDataColumn(row1, row2);
		}
		
		return comparison * sortDir;
	}
	
	/**
	 * Return proper comparison for a data column
	 * 
	 * @param row1
	 * @param row2
	 * @return
	 */
	private int sortByDataColumn(FinancialDashDataRowVO row1, FinancialDashDataRowVO row2) {
		FinancialDashDataColumnVO col1 = row1.getColumns().get(column);
		FinancialDashDataColumnVO col2 = row2.getColumns().get(column);
		Integer dollarValue1 = col1.getDollarValue();
		Integer dollarValue2 = col2.getDollarValue();
		
		return dollarValue1.compareTo(dollarValue2);
	}
	
	/**
	 * Returns a comparison on the name column
	 * Markets - sort by the designated sort order
	 * Companies - sort by name, with "Other" last
	 * 
	 * @param row1
	 * @param row2
	 * @return
	 */
	private int sortByNameColumn(FinancialDashDataRowVO row1, FinancialDashDataRowVO row2) {
		if (TableType.MARKET == tableType) {
			SectionVO section1 = (SectionVO) sections.findNode(row1.getPrimaryKey()).getUserObject();
			SectionVO section2 = (SectionVO) sections.findNode(row2.getPrimaryKey()).getUserObject();
			Integer orderNo1 = section1.getOrderNo();
			Integer orderNo2 = section2.getOrderNo();
			
			return orderNo1.compareTo(orderNo2);
		} else {
			String name1 = row1.getName();
			String name2 = row2.getName();
			
			// The "Other" company should always be sorted last
			if ("other".equalsIgnoreCase(name1))
				name1 = "~~~~~~";
			if ("other".equalsIgnoreCase(name2))
				name2 = "~~~~~~";
			
			return name1.compareToIgnoreCase(name2);			
		}
	}
}