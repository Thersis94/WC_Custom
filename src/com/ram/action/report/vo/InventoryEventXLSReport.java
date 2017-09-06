package com.ram.action.report.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ram.datafeed.data.InventoryEventVO;
import com.ram.datafeed.data.InventoryItemVO;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.data.report.StandardExcelReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: InventoryEventXLSReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Generates an excel file related to an 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Sep 1, 2017
 * @updates:
 ****************************************************************************/
public class InventoryEventXLSReport extends AbstractSBReportVO  {
	
	private static final long serialVersionUID = 9106005186153267311L;
	InventoryEventVO data = null;
	private enum column {
		COL_0("Product Name"), COL_1("Manufactureer"), COL_2("SKU"), 
		COL_3("Qty"), COL_4("Kit");

		private String header;

		private column(String header) {
			this.header = header;
		}
	}
	
	public InventoryEventXLSReport() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("InventoryEventReport.xls");
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("starting generateReport()");

		Map<String, String> headerMap = this.getHeader();

		ExcelReport rpt = new StandardExcelReport(headerMap);

		StringBuilder sb = new StringBuilder(100);
		sb.append("Inventory Event ID: ").append(StringUtil.checkVal(data.getInventoryEventId()));
		sb.append(" (Report Created Date: ").append(StringUtil.checkVal(Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN )));
		sb.append(")");
		
		rpt.setTitleCell(sb.toString());
		
		List<Map<String, Object>> rows = new ArrayList<>();

		rows = generateCaseInfoRows(rows);
		rows = generateItemHeader(rows);
		rows = generateItemRows(rows);

		rpt.setData(rows);

		
		return rpt.generateReport();
	}


	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateItemRows(List<Map<String, Object>> rows) {
		
		for(InventoryItemVO i : data.getInventoryItems()){
			Map<String, Object> row = new HashMap<>();
			row.put(column.COL_0.name(),i.getProductNm());
			row.put(column.COL_1.name(),i.getManufacturer());
			row.put(column.COL_2.name(),i.getCustomerProductId());
			row.put(column.COL_3.name(),i.getQuantity());
			row.put(column.COL_4.name(),(i.getKitFlag() == 1)? "Yes":"No");
			
			rows.add(row);
		}
		return rows;
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateItemHeader(List<Map<String, Object>> rows) {
		Map<String, Object> row = new HashMap<>();
		row.put(column.COL_0.name(),column.COL_0.header);
		row.put(column.COL_1.name(),column.COL_1.header);
		row.put(column.COL_2.name(),column.COL_2.header);
		row.put(column.COL_3.name(),column.COL_3.header);
		row.put(column.COL_4.name(),column.COL_4.header);
		
		rows.add(row);
		return rows;
	}

	/**
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateCaseInfoRows(List<Map<String, Object>> rows) {
		Map<String, Object> row1 = new HashMap<>();
		row1.put(column.COL_0.name(), "Event Location: ");
		row1.put(column.COL_1.name(), StringUtil.checkVal(data.getLocationName()));
		row1.put(column.COL_2.name(), "Location Address: ");
		row1.put(column.COL_3.name(), StringUtil.checkVal(data.getLocation()));
		
		rows.add(row1);
		Map<String, Object> row2 = new HashMap<>();
		row2.put(column.COL_0.name(), "Scheduled Date: ");
		row2.put(column.COL_1.name(), formatDate(data.getScheduleDate()));
		row2.put(column.COL_2.name(), "Completed Date: ");
		row2.put(column.COL_3.name(), formatDate(data.getInventoryCompleteDate()));

		rows.add(row2);
		Map<String, Object> row3 = new HashMap<>();
		row3.put(column.COL_0.name(), "Date Loaded Date: ");
		row3.put(column.COL_1.name(), formatDate(data.getDataLoadCompleteDate()));
		
		rows.add(row3);
		
		Map<String, Object> row4 = new HashMap<>();
		row4.put(column.COL_0.name(), "Total Item Count: ");
		row4.put(column.COL_1.name(), StringUtil.checkVal((data.getNumberTotalProducts())));
		row4.put(column.COL_2.name(), "Unique SKU Count: ");
		row4.put(column.COL_3.name(), StringUtil.checkVal((data.getInventoryItems().size())));

		rows.add(row4);
		Map<String, Object> row5 = new HashMap<>();
		row5.put(column.COL_0.name(), "Received Item Count: ");
		row5.put(column.COL_1.name(), StringUtil.checkVal((data.getNumberReceivedProducts())));
		row5.put(column.COL_2.name(), "Returned Item Count: ");
		row5.put(column.COL_3.name(), StringUtil.checkVal((data.getNumberReturnedProducts())));

		rows.add(row5);

		Map<String, Object> spacerRow = new HashMap<>();
		spacerRow.put(column.COL_0.name(), " ");
		rows.add(spacerRow);
		return rows;
	}

	/**
	 * 
	 * @param d
	 * @return
	 */
	private String formatDate(Date d){
		return Convert.formatDate(d, Convert.DATE_TIME_SLASH_PATTERN_12HR);
	}
	
	/**
	 * @return
	 */
	private Map<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(column.COL_0.name(),"");
		headerMap.put(column.COL_1.name(),"");
		headerMap.put(column.COL_2.name(),"");
		headerMap.put(column.COL_3.name(),"");
		headerMap.put(column.COL_4.name(),"");
		return headerMap;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		if (o instanceof InventoryEventVO) {
			data = (InventoryEventVO) o;
		}
	}

}
