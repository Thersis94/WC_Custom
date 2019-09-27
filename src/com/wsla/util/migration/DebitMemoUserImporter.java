package com.wsla.util.migration;

// JDK 1.8.x
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//SMT Base Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.io.FileManagerFactoryImpl;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

// WSLA Libs
import com.wsla.common.WSLAConstants;
import com.wsla.data.provider.ProviderVO;
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
public class DebitMemoUserImporter extends AbsImporter {

	// Members
	private List<DebitMemoFileVO> data;
	private Map<String, List<DebitMemoFileVO>> entries = new HashMap<>();
	private Map<String, Map<String, List<DebitMemoFileVO>>> groups = new HashMap<>();
	private Map<String, ProviderVO> providers = new HashMap<>(50);

	private Map<String, Object> jobAttributes = new HashMap<>();
	private DebitMemoJob job;


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("debitMemoFile"), DebitMemoFileVO.class, SHEET_1);

		//create a reusable job and load the resource bundle (once)
		job = new DebitMemoJob(dbConn, jobAttributes);
		job.getResourceBundleData("es", "MX", "WSLA_BUNDLE");

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
	 * @throws Exception 
	 */
	private void storeData() throws Exception {
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
	 * @throws Exception 
	 */
	private void createDebitMemo(String monthYear, String key, List<DebitMemoFileVO> cms) throws Exception {
		int index = key.indexOf('|');
		String oemId = key.substring(0, index);
		String retailId = key.substring(index + 1);
		List<Object> vals = getCreditMemoIds(cms);

		DebitMemoVO memo = new DebitMemoVO();
		String slug = RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS);
		memo.setDebitMemoId(uuid.getUUID());
		memo.setCustomerMemoCode(slug.toUpperCase());
		memo.setCreateDate(Convert.formatDate(Convert.DATE_SHORT_PATTERN, monthYear));
		memo.setRetailId(retailId);
		memo.setOemId(oemId);
		memo.setTotalCreditMemos(cms.size());
		memo.setCreditMemos(getCreditMemos(vals));

		// Merge the db credit memo with the excel data
		mergeCreditMemos(memo, cms);

		memo.setOem(getProviderById(memo.getOemId()));
		memo.setRetailer(getProviderById(memo.getRetailId()));

		// Create the PDF
		createPDF(memo);

		// Store the debit memo
		db.insert(memo);

		// Update the credit memos
		writeToDB(memo.getCreditMemos(), false);
	}


	/**
	 * Uses the debit memo job to generate the pdf file
	 * @param memo
	 */
	private void createPDF(DebitMemoVO memo) {
		try {
			memo.setFilePathUrl(job.buildMemoPDF(memo));
		} catch (FileWriterException e) {
			log.error("Unable to create PDF", e);
		}
	}


	/**
	 * Merges the database information with the Excel data 
	 * @param memo
	 * @param cms
	 */
	private void mergeCreditMemos(DebitMemoVO memo, List<DebitMemoFileVO> cms) {
		Map<String, CreditMemoVO> dbCms = convertCMListToMap(memo.getCreditMemos());

		for (DebitMemoFileVO vo : cms) {
			CreditMemoVO cm = dbCms.get("credit_" + vo.getTicketId());
			if (cm != null) {
				cm.setDebitMemoId(memo.getDebitMemoId());
				cm.setAuthorizationDate(vo.getOemAuthDate());
				cm.setCreateDate(vo.getInitialContactDate());
				if (! StringUtil.isEmpty(vo.getRetailerCreditMemoId())) 
					cm.setCustomerMemoCode(vo.getRetailerCreditMemoId());
			}
		}
		log.info("merged memos");
	}


	/**
	 * Converts the list into a map using the 
	 * @param cms
	 * @return
	 */
	private Map<String, CreditMemoVO> convertCMListToMap(List<CreditMemoVO> cms) {
		Map<String, CreditMemoVO> map = new HashMap<>(cms.size());
		for (CreditMemoVO vo : cms)
			map.put(vo.getCreditMemoId(), vo);

		log.info(String.format("returning map of size=%d", map.size()));
		return map;
	}


	/**
	 * Loads all of the credit memos that will be in the debit memo
	 * @param ids
	 * @return
	 */
	private List<CreditMemoVO> getCreditMemos(List<Object> ids) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select c.oem_id, ticket_no, d.provider_id, product_nm, a.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_credit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location d on c.retailer_id = d.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial e on c.product_serial_id = e.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master f on e.product_id = f.product_id ");
		sql.append("where a.credit_memo_id in (").append(DBUtil.preparedStatmentQuestion(ids.size())).append(")");
		log.debug(sql.length() + "|" + sql + "|" + ids);

		List<CreditMemoVO> memos = db.executeSelect(sql.toString(), ids, new CreditMemoVO());
		log.info(String.format("loaded %d memos from %d IDs", memos.size(), ids.size()));
		return memos;
	}


	/**
	 * Gets a list of credit memos for a given debit memo
	 * @param dmfv
	 * @return
	 */
	private List<Object> getCreditMemoIds(List<DebitMemoFileVO> dmfv) {
		List<Object> ids = new ArrayList<>(dmfv.size());
		for (DebitMemoFileVO vo : dmfv)
			ids.add("credit_" + vo.getTicketId());

		log.info(String.format("returning %d IDs", ids.size()));
		return ids;
	}


	/**
	 * Gets the OEM information from the database and assigns it to the memo.
	 * Used for both Retailer and OEM lookups
	 * @param schema
	 * @param memo
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	private ProviderVO getProviderById(String providerId) throws Exception {
		//return a cached record if we have one
		if (providers.containsKey(providerId))
			return providers.get(providerId);

		ProviderVO prov = new ProviderVO();
		prov.setProviderId(providerId);
		db.getByPrimaryKey(prov);

		//cache for later
		providers.put(providerId, prov);

		return prov;
	}


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#save()
	 */
	@Override
	void save() throws Exception {
		//unused - everything invoked from run()
	}


	@Override
	protected void setAttributes(Connection conn, Properties props, String[] args) {
		super.setAttributes(conn, props, args);

		jobAttributes.put(Constants.PATH_TO_BINARY, props.get(Constants.PATH_TO_BINARY));
		jobAttributes.put(Constants.CUSTOM_DB_SCHEMA, schema);
		jobAttributes.put(FileManagerFactoryImpl.CONFIG_FILE_MANAGER_TYPE, "2");
	}
}
