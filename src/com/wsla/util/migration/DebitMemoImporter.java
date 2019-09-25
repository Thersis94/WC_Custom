package com.wsla.util.migration;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//SMT Base Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WSLA Libs
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.DebitMemoVO;
import com.wsla.scheduler.job.DebitMemoJob;
import com.wsla.util.migration.vo.DebitMemoFileVO;

/****************************************************************************
 * <b>Title</b>: DebitMemoImporter.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Imports a spreadsheet of the current debit memos
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 23, 2019
 * @updates:
 ****************************************************************************/
public class DebitMemoImporter extends AbsImporter {
	
	// Members
	private List<DebitMemoFileVO> data;
	private Map<String, List<DebitMemoFileVO>> entries = new HashMap<>();
	private Map<String, Map<String, List<DebitMemoFileVO>>> groups = new HashMap<>();
	
	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#save()
	 */
	@Override
	void save() throws Exception {
		log.info("Saving");

	}

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("debitMemoFile"), DebitMemoFileVO.class, SHEET_1);
		
		// Asign the credit memos to the month grouping
		monthlyGroup();
		
		// Groups the elements within a month into a retailer/oem pair
		processDebitMemoGroup();
		
		// Loop each element and store the data
		storeData();
	}
	
	/**
	 * Groups the credit memos into month/year 
	 */
	private void monthlyGroup() {
		// Group the credit memos by month for processing 
		for (DebitMemoFileVO vo : data) {
			String key = Convert.formatDate(vo.getOemAuthDate(), Convert.DATE_SHORT_PATTERN);
			if (entries.containsKey(key)) {
				entries.get(key).add(vo);
			} else {
				List<DebitMemoFileVO> item = new ArrayList<>();
				item.add(vo);
				entries.put(key, item); 
			}
		}
		
		log.info("Num Months: " + entries.size());
	}
	
	/**
	 * For each month, it puts the CMs in oem/retailer group
	 */
	private void processDebitMemoGroup() {
		
		for (Map.Entry<String, List<DebitMemoFileVO>> items : entries.entrySet()) {
			String monthYear = items.getKey();
			List<DebitMemoFileVO> cms = items.getValue();
			Map<String, List<DebitMemoFileVO>> cmGroup = new HashMap<>();
			
			for (DebitMemoFileVO cm : cms) {
				String key = cm.getOemId() + "|" + cm.getRetailerId();
				
				if (cmGroup.containsKey(key)) {
					cmGroup.get(key).add(cm);
				} else {
					List<DebitMemoFileVO> item = new ArrayList<>();
					item.add(cm);
					cmGroup.put(key, item); 
				}
			}

			groups.put(monthYear, cmGroup);
		}
	}
	
	/**
	 * Processes the credit and debit memos and creates them in the db
	 */
	private void storeData() {
		
		for (Map.Entry<String, Map<String, List<DebitMemoFileVO>>> monthYear : groups.entrySet()) {
			String key = monthYear.getKey();
			
			for (Map.Entry<String, List<DebitMemoFileVO>> oemRet : monthYear.getValue().entrySet()) {
				String groupId = oemRet.getKey();
				createDebitMemo(key, groupId, oemRet.getValue());
			}
		}
	}
	
	/**
	 * 
	 * @param monthYear
	 * @param key
	 * @param cms
	 * @return
	 */
	public void createDebitMemo(String monthYear, String key, List<DebitMemoFileVO> cms) {
		int index = key.indexOf('|');
		String oemId = key.substring(0, index);
		String retailId = key.substring(index + 1);
		List<Object> vals = getCreditMemoIds(cms);
		
		DebitMemoVO memo = new DebitMemoVO();
		String slug = RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS);
		memo.setDebitMemoId(new UUIDGenerator().getUUID());
		memo.setCustomerMemoCode(slug.toUpperCase());
		memo.setCreateDate(Convert.formatDate(Convert.DATE_SHORT_PATTERN, monthYear));
		memo.setRetailId(retailId);
		memo.setOemId(oemId);
		memo.setTotalCreditMemos(cms.size());
		memo.setCreditMemos(getCreditMemos(vals));
		
		// Merge the db credit memo with the excel data
		mergeCreditMemos(memo, cms);
		
		// Create the PDF
		//createPDF(memo);
		
		// Store the debit memo
		DBProcessor db = new DBProcessor(dbConn, schema);
		try {
			db.insert(memo);
		} catch (Exception e) {
			log.error("Unable to save debit memos", e);
		}
		
		// Update the credit memos
		try {
			//this.writeToDB(memo.getCreditMemos(), false);
		} catch (Exception e) {
			log.error("Unable to update credit memos", e);
		}
	}
	
	/**
	 * Uses the debit memo job to generate the pdf file
	 * @param memo
	 */
	public void createPDF(DebitMemoVO memo) {
		// Assign the needed attributes
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(Constants.PATH_TO_BINARY, props.get(Constants.PATH_TO_BINARY));
		attributes.put(Constants.CUSTOM_DB_SCHEMA, schema);
		attributes.put(Constants.INCLUDE_DIRECTORY, "/WEB-INF/include/");
		attributes.put("fileManagerType", "2");
		
		// Create the Debit Memo job that already creates the PDF
		DebitMemoJob job = new DebitMemoJob(dbConn, attributes);
		
		// Load the resource bundle
		job.getResourceBundleData("es", "MX", "WSLA_BUNDLE");
	
		try {
			memo.setFilePathUrl(job.buildMemoPDF(memo));
		} catch (FileWriterException e) {
			log.error("Unable to create PDF", e);
		}
		
	}
	
	/**
	 * Converts the list into a map using the 
	 * @param cms
	 * @return
	 */
	public Map<String, CreditMemoVO> convertCMListToMap(List<CreditMemoVO> cms) {
		Map<String, CreditMemoVO> map = new HashMap<>();
		for (CreditMemoVO vo : cms) {
			map.put(vo.getCreditMemoId(), vo);
		}
		
		return map;
	}
	
	/**
	 * Merges the database information with the Excel data 
	 * @param memo
	 * @param cms
	 */
	public void mergeCreditMemos(DebitMemoVO memo, List<DebitMemoFileVO> cms) {
		Map<String, CreditMemoVO> dbCms = convertCMListToMap(memo.getCreditMemos());
		
		for (DebitMemoFileVO vo : cms) {
		
			CreditMemoVO cm = dbCms.get("credit_" + vo.getTicketId());
			cm.setDebitMemoId(memo.getDebitMemoId());
			cm.setAuthorizationDate(vo.getOemAuthDate());
			cm.setCreateDate(vo.getInitialContactDate());
			if (! StringUtil.isEmpty(vo.getRetailerCreditMemoId())) 
				cm.setCustomerMemoCode(vo.getRetailerCreditMemoId());
		}
	}
	
	/**
	 * Loads all of the credit memos that will be in the debit memo
	 * @param ids
	 * @return
	 */
	public List<CreditMemoVO> getCreditMemos(List<Object> ids) {
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from ").append(schema).append("wsla_credit_memo ");
		sql.append("where credit_memo_id in (").append(DBUtil.preparedStatmentQuestion(ids.size()));
		sql.append(")");
		
		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql.toString(), ids, new CreditMemoVO());
	}

	/**
	 * Gets a list of credit memos for a given debit memo
	 * @param dmfv
	 * @return
	 */
	private List<Object> getCreditMemoIds(List<DebitMemoFileVO> dmfv) {
		List<Object> ids = new ArrayList<>(dmfv.size());
		
		for (DebitMemoFileVO vo : dmfv) {
			ids.add("credit_" + vo.getTicketId());
		}
		
		return ids;
		
	}
}
