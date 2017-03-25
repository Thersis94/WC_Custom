package com.depuysynthes.scripts.showpad;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.report.ExcelStyleFactory.Styles;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.data.report.MultisheetExcelReport;


/****************************************************************************
 * <b>Title</b>: ReconcileExcelReport.java<p/>
 * <b>Description</b>: Renders the deltas between SMT and Showpad, for a series of Divisions, into Excel format.
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 25, 2017
 ****************************************************************************/
public class ReconcileExcelReport extends MultisheetExcelReport {

	private static final long serialVersionUID = 2628876805719396453L;

	private Map<String, GenericVO> rawData;
	private StringBuilder summary;

	public ReconcileExcelReport(Object data) {
		super(Styles.Standard);
		summary = new StringBuilder(500);
		String dt = Convert.formatDate(Calendar.getInstance().getTime(), Convert.DATE_DASH_PATTERN);
		setFileName("Showpad Reconcile Report " + dt + ".xls");
		setData(data);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.SMTReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		//iterate ours lists of data into Maps for the Excel report to understand
		String dt = Convert.formatDate(Calendar.getInstance().getTime(), Convert.DATE_DASH_PATTERN);
		summary.append("<h1>Showpad Reconcile Report &mdash; ").append(dt).append("</h1>");
		int total = 0;
		int mbTotal;

		GenericVO vo;
		Collection<Map<String, Object>> records;
		for (Map.Entry<String, GenericVO> entry : rawData.entrySet()) {
			vo = entry.getValue();
			records = makeAssetMapMB(vo.getKey());
			mbTotal = records.size();
			total += mbTotal;

			//add MB extras
			addSheet(entry.getKey() + " MB", entry.getKey() + " - Mediabin assets not in Showpad", 
					buildHeaderMapMB(), records);

			//add Showpad extras
			records = makeAssetMapSP(vo.getValue());
			total += records.size();
			addSheet(entry.getKey() + " SP", entry.getKey() + " - Showpad assets not in Mediabin", 
					buildHeaderMapSP(), records);

			//add to the summary HTML
			summary.append("<h3>").append(entry.getKey()).append("</h3>");
			summary.append("Mediabin assets not in Showpad: " ).append(mbTotal).append("<br/>");
			summary.append("Showpad assets not in Mediabin: " ).append(records.size()).append("<br/><br/>");
		}

		//return an empty array if there's nothing to report...so we aren't generating and attaching empty Excel files to every email
		if (total == 0) {
			return new byte[0];
		} else {
			return super.generateReport();
		}
	}


	/**
	 * the header map for Mediabin sheets in the Excel file
	 * @return
	 */
	private Map<String, String> buildHeaderMapMB() {
		Map<String, String> hdr = new HashMap<>();
		hdr.put("PRIMARY_KEY", "SMT Unique ID");
		hdr.put("TRACKING_NO", "Tracking No.");
		hdr.put("TITLE", "Title");
		hdr.put("FILE_NM", "File Name");
		hdr.put("SHOWPAD_ID", "Alleged Showpad ID");
		hdr.put("PRIVATE", "Private?");
		return hdr;
	}



	/**
	 * turns the List<MediaBinDeltaVO> into a List<Map> for rendering into Excel
	 * @param headerMap
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Collection<Map<String, Object>> makeAssetMapMB(Object obj) {
		List<MediaBinDeltaVO> assets = (List<MediaBinDeltaVO>) obj;
		List<Map<String, Object>> data = new ArrayList<>(assets.size());

		//loop the assets, build a map for each one that matches the headerMap columns, add it to the outgoing list
		Map<String, Object> row;
		for (MediaBinDeltaVO vo : assets) {
			row = new HashMap<>();
			row.put("PRIMARY_KEY", vo.getDpySynMediaBinId());
			row.put("TRACKING_NO", vo.getTrackingNoTxt());
			row.put("TITLE", vo.getTitleTxt());
			row.put("FILE_NM", vo.getFileName());
			row.put("SHOWPAD_ID",  StringUtil.checkVal(vo.getShowpadId()));
			row.put("PRIVATE",  3 == vo.getImportFileCd() ? "Y" : "");
			data.add(row);
		}

		return data;
	}


	/**
	 * The header map for Showpad sheets in the Excel file
	 * @return
	 */
	private Map<String, String> buildHeaderMapSP() {
		Map<String, String> hdr = new HashMap<>();
		hdr.put("PRIMARY_KEY", "Showpad ID");
		hdr.put("TITLE", "Asset Name");

		return hdr;
	}



	/**
	 * turns the List<MediaBinDeltaVO> into a List<Map> for rendering into Excel
	 * @param headerMap
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Collection<Map<String, Object>> makeAssetMapSP(Object obj) {
		List<MediaBinDeltaVO> assets = (List<MediaBinDeltaVO>) obj;
		List<Map<String, Object>> data = new ArrayList<>(assets.size());

		//loop the assets, build a map for each one that matches the headerMap columns, add it to the outgoing list
		Map<String, Object> row;
		for (MediaBinDeltaVO vo : assets) {
			row = new HashMap<>();
			row.put("PRIMARY_KEY", vo.getDpySynMediaBinId());
			row.put("TITLE", vo.getTitleTxt());
			data.add(row);
		}

		return data;
	}


	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.report.MultisheetExcelReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object data) {
		this.rawData = (Map<String, GenericVO>) data;
	}


	/**
	 * returns the compiled html summary (of what's in the Excel file)
	 * @return
	 */
	public String getEmailSummary() {
		return summary.toString();
	}
}
