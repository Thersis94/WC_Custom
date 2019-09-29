package com.wsla.util.migration;

// JDK 1.8.x
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
	private Map<String, ProviderVO> providers = new HashMap<>(50);

	private Map<String, Object> jobAttributes = new HashMap<>();
	private DebitMemoJob job;


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("debitMemoUserFile"), DebitMemoFileVO.class, SHEET_1);
		
		//create a reusable job and load the resource bundle (once)
		job = new DebitMemoJob(dbConn, jobAttributes);
		job.getResourceBundleData("es", "MX", "WSLA_BUNDLE");
		
		// Asign the credit memos to the month grouping
		monthlyGroup();

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
			if (StringUtil.isEmpty(key)) continue;
			
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
	 * Processes the credit and debit memos and creates them in the db
	 * @throws Exception 
	 */
	private void storeData() throws Exception {
		for (Map.Entry<String, List<DebitMemoFileVO>> monthYear : entries.entrySet()) {
			String key = monthYear.getKey();
			
			for (DebitMemoFileVO vo : monthYear.getValue()) {
				if ("RCA1000".equalsIgnoreCase(vo.getOemId()))
					vo.setOemId("1ecc07d03101fe41ac107866a7995adf");
				
				createDebitMemo(key, vo);
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
	private void createDebitMemo(String monthYear, DebitMemoFileVO cms) throws Exception {
		DebitMemoVO memo = new DebitMemoVO();
		String slug = RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS);
		memo.setDebitMemoId(uuid.getUUID());
		memo.setUpdateDate(new Date());
		memo.setCustomerMemoCode(slug.toUpperCase());
		memo.setCreateDate(Convert.formatDate(Convert.DATE_SHORT_PATTERN, monthYear));
		memo.setOemId(cms.getOemId());
		memo.setTotalCreditMemos(1);
		memo.setTransferAmount(cms.getRefundCost());
		memo.setOem(getProviderById(memo.getOemId()));
		memo.setTotalCreditMemoAmount(cms.getRefundCost());
		
		List<CreditMemoVO> credMemo = getCreditMemos("credit_" + cms.getTicketId());
		if (credMemo.isEmpty()) return;
		CreditMemoVO cm = credMemo.get(0);
		
		// Merge the db credit memo with the excel data
		cm.setDebitMemoId(memo.getDebitMemoId());
		cm.setAuthorizationDate(cms.getOemAuthDate());
		cm.setUpdateDate(cms.getInitialContactDate());
		cm.setApprovedBy("Cypher");
		cm.setApprovalDate(cms.getOemAuthDate());
		cm.setCustomerMemoCode(RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS).toUpperCase());
		cm.setRefundAmount(cms.getRefundCost());
		cm.setEndUserRefundFlag(1);
		cm.setCreateDate(new Date());
		
		memo.addCreditMemo(cm);
		ProviderVO ret = new ProviderVO();
		ret.setProviderName(cm.getUserName());
		memo.setRetailer(ret);
		memo.setRetailerName(cm.getUserName());
		
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
	 * Loads all of the credit memos that will be in the debit memo
	 * @param ids
	 * @return
	 */
	private List<CreditMemoVO> getCreditMemos(String id) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select first_nm || ' ' || last_nm as user_nm, u.user_id, c.oem_id, ticket_no, ");
		sql.append("d.provider_id, product_nm, a.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_credit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_assignment ta ");
		sql.append("on c.ticket_id = ta.ticket_id and ta.assg_type_cd = 'CALLER' ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_user u on ta.user_id = u.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location d on c.retailer_id = d.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial e on c.product_serial_id = e.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master f on e.product_id = f.product_id ");
		sql.append("where a.credit_memo_id = ?");
		log.debug(sql.length() + "|" + sql + "|" + id);

		return db.executeSelect(sql.toString(), Arrays.asList(id), new CreditMemoVO());
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
