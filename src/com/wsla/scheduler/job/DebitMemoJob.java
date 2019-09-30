package com.wsla.scheduler.job;

// JDK 1.8.x
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Quartz 2.2.3
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// SMT Base Libs
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.data.report.PDFGenerator;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.http.filter.fileupload.FileTransferStructureImpl;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.resource.ResourceBundleLoader;
// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;
import com.wsla.common.WSLAConstants;
import com.wsla.data.provider.ProviderVO;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.DebitMemoVO;

/****************************************************************************
 * <b>Title</b>: DebitMemoJob.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Creates Debit Memos on a periodic basis via the job
 * scheduler in WC
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 8, 2019
 * @updates:
 ****************************************************************************/

public class DebitMemoJob extends AbstractSMTJob {

	/**
	 * Field for the database schema name
	 */
	private Map<String, Object> attributes;
	private Map<String, String> resourceBundle = new HashMap<>();

	/**
	 * 
	 */
	public DebitMemoJob() {
		super();
	}

	/**
	 * 
	 * @param conn
	 * @param attributes
	 */
	public DebitMemoJob(Connection conn, Map<String, Object> attributes) {
		this();
		this.conn = conn;
		this.attributes = attributes;
	}

	/**
	 * Helper to build and test the class outside of the scheduler app
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		DebitMemoJob job = new DebitMemoJob();

		// Assign the needed attributes
		job.attributes = new HashMap<>();
		job.attributes.put(Constants.PATH_TO_BINARY, "/home/ryan/git/WebCrescendo/binary");
		job.attributes.put(Constants.CUSTOM_DB_SCHEMA, "custom.");
		job.attributes.put(Constants.INCLUDE_DIRECTORY, "/WEB-INF/include/");
		job.attributes.put("fileManagerType", "2");

		// Get a db connection
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass("org.postgresql.Driver");
		dbc.setUrl("jdbc:postgresql://sonic:5432/webcrescnedo_dev08282019_sb?defaultRowFetchSize=25&amp;prepareThreshold=3");
		dbc.setUserName("ryan_user_sb");
		dbc.setPassword("sqll0gin");
		job.conn = dbc.getConnection();

		// Load the resource bundle
		job.getResourceBundleData("en", "US", "WSLA_BUNDLE");

		// Process the job
		job.log.debug("Starting ...");
		job.processDebitMemos(StringUtil.checkVal(job.attributes.get(Constants.CUSTOM_DB_SCHEMA)));
	}

	/**
	 * 
	 * @param language
	 * @param country
	 */
	public void getResourceBundleData(String language, String country, String bundleId) {
		// Get the resource bundle map
		ResourceBundleLoader loader = new ResourceBundleLoader(new SMTDBConnection(conn));
		List<GenericVO> data = loader.getBundle(bundleId, language, country);

		for (GenericVO val : data) { 
			resourceBundle.put(val.getKey() + "", val.getValue() + ""); 
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.scheduler.AbstractSMTJob#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		super.execute(ctx);
		attributes = ctx.getMergedJobDataMap().getWrappedMap();
		getResourceBundleData("es", "MX", "WSLA_BUNDLE");
		boolean success = true;

		String message = "Success " + processDebitMemos(StringUtil.checkVal(attributes.get(Constants.CUSTOM_DB_SCHEMA)));

		try {
			this.finalizeJob(success, message);
		} catch (Exception e) {
			/** nothing to do here **/ 
		}
	}

	/**
	 * Query to find unique retailer, oem pairs that have unassigned and 
	 * Approved credit memos
	 * @param schema
	 * @return
	 */
	public String processDebitMemos(String schema) {
		List<DebitMemoVO> memos = getGroupData(schema);
		log.debug("memo size "  + memos.size());

		StringBuilder messages = new StringBuilder(100);
		log.debug(memos);
		for (DebitMemoVO memo : memos) {
			
			try {
				// get the credit memos
				this.getCreditMemos(schema, memo);

				// get the oem data
				this.assignOEM(schema, memo);

				// get the retailer data
				this.assignRetailer(schema, memo);

				// Assign a human readable value and misc values
				String slug = RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS);
				memo.setDebitMemoId(new UUIDGenerator().getUUID());
				memo.setCustomerMemoCode(slug.toUpperCase());
				memo.setCreateDate(new Date());
				log.debug(" user data " + memo.getUser().getFirstName());
				log.debug(" retailer data " + memo.getRetailer().getProviderName());
				log.debug(" oem data " + memo.getOem().getProviderName());
				// process the memo
				processDebitMemo(memo, schema);

			} catch (Exception e) {
				log.error("Unable to create debit memo", e);
				messages.append(e.getLocalizedMessage()).append("|");
			}

		}
		return messages.toString();
	}
	/**
	 * When run, it creates a debit memo for all approved credit memos that 
	 * have not been included in a debit memo.  The debit memos are created by
	 * a unique pair of oem and retailer
	 * @param attributes
	 */
	protected void processDebitMemo(DebitMemoVO memo, String schema) throws Exception {

		// Create the actual debit memo and attach as an asset and asset
		memo.setFilePathUrl(this.buildMemoPDF(memo));
		log.debug("pdf generated ");
		// Create a debit memo for each oem/retailer pair
		DBProcessor db = new DBProcessor(conn, schema);
		db.insert(memo);

		// Add the debit memo id to each of the credit memos assigned to the debit memo
		this.updateCreditMemos(schema, memo.getDebitMemoId(), memo.getCreditMemos());
		log.debug("credit memos updated");
	}

	/**
	 * Builds the PDF file and stores it to the file system
	 * @param memo
	 * @return relative path and file name to the generated file
	 * @throws InvalidDataException 
	 * @throws FileWriterException 
	 */
	public String buildMemoPDF(DebitMemoVO memo) 
			throws FileWriterException {
		// Get the file name and path
		FileTransferStructureImpl fs = new FileTransferStructureImpl(null, "12345678.pdf", attributes);
		log.debug("FS: " + fs.getFullPath());
		
		// Create the file loader and write to the file system
		FileLoader fl = new FileLoader(attributes);
		fl.setPath(fs.getFullPath());
		fl.setFileName(fs.getStorageFileName());
		log.debug("File Name: " + fs.getStorageFileName());
		try {
			fl.setData(createPDF(memo));
			fl.writeFiles();
		} catch(Exception e) {
			throw new FileWriterException(e);
		}

		// Return the full path
		return "/binary/file_transfer" + fs.getCanonicalPath() + fs.getStorageFileName();
	}

	/**
	 * Creates the PDF file
	 * @param memo
	 * @return
	 * @throws IOException 
	 * @throws InvalidDataException 
	 */
	protected byte[] createPDF(DebitMemoVO memo) throws IOException {
		String oldRetailname ="";
		if (memo.getUser() != null && ! StringUtil.isEmpty(memo.getUser().getUserId())) {
			oldRetailname = memo.getRetailer().getProviderName();
			memo.getRetailer().setProviderName(memo.getUser().getFirstName() + " " + memo.getUser().getLastName());   
			log.debug("retail name on pdf " + memo.getRetailer().getProviderName());
			
		}
		
		try {
			Reader reader = new InputStreamReader(getClass().getResourceAsStream("debit_memo.ftl"));
			PDFGenerator pdf = new PDFGenerator(reader, memo, resourceBundle);
			
			byte[] doc = pdf.generate();
			
			if (memo.getUser() != null && ! StringUtil.isEmpty(memo.getUser().getUserId())) {
				memo.getRetailer().setProviderName(oldRetailname);  
				log.debug("end user refund detected adjusting pdf name back to " + memo.getRetailer().getProviderName());
			}
			
			return doc;
		} catch (Exception e) {
			throw new IOException(e);
		}
		
	}

	/**
	 * Updates the credit memos with the debit memo id to assign these values
	 * @param schema
	 * @param debitMemoId
	 * @param creditMemos
	 * @throws SQLException
	 */
	public void updateCreditMemos(String schema, String debitMemoId, List<CreditMemoVO> creditMemos) 			throws SQLException {
		log.debug("number of credit memos " + creditMemos.size());
		if (creditMemos == null || creditMemos.isEmpty()) return;

		String sql =StringUtil.join(DBUtil.UPDATE_CLAUSE, schema, "wsla_credit_memo ", 
				"set debit_memo_id=? where credit_memo_id=?");

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for (CreditMemoVO memo : creditMemos) {
				ps.setString(1, debitMemoId);
				ps.setString(2, memo.getCreditMemoId());
				ps.addBatch();
			}

			// Update the credit memos
			int[] cnt = ps.executeBatch();
			log.debug(String.format("updated %d credit memos to set debitMemoId=%s", cnt.length, debitMemoId));
		}
	}

	/**
	 * Gets the debit memos by rolling up the provider / oem grouping
	 * @return
	 */
	private List<DebitMemoVO> getGroupData(String schema) {
		StringBuilder sql = new StringBuilder(2644);
		/*
		 * The top query of this union selects the standard credit memo to retailer relations ships thing hitachi owes walmart
		 * the second query of this union selects the credit memos where the warranty assigns a provider to cover the refund.  for example RCA says WSLA owns walmart
		 * the third query of this union will select credit memos where the oem owes the end user think hitachi ows john doe
		 * 
		 */
		sql.append("select replace(newid(), '-', '') as debit_memo_id, c.oem_id, ");
		sql.append("d.provider_id as retail_id, count(*) as credit_memo_no, null as user_id, null as first_nm, null as last_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_credit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location d on c.retailer_id = d.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_warranty e on c.product_warranty_id = e.product_warranty_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_warranty w on e.warranty_id = w.warranty_id ");
		sql.append("where debit_memo_id is null and approval_dt is not null ");
		sql.append("and (w.refund_provider_id is null or w.refund_provider_id = '') and (a.customer_assisted_cd is null or a.customer_assisted_cd = '') ");
		sql.append("and (a.end_user_refund_flg is null or a.end_user_refund_flg = 0) ");
		sql.append("group by oem_id, retail_id, user_id, first_nm, last_nm ");
		
		sql.append(DBUtil.UNION);
		sql.append("select replace(newid(), '-', '') as debit_memo_id, c.oem_id, ");
		sql.append("d.provider_id as retail_id, count(*) as credit_memo_no, null as user_id, null as first_nm, null as last_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_credit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_warranty e on c.product_warranty_id = e.product_warranty_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_warranty w on e.warranty_id = w.warranty_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider d on w.refund_provider_id = d.provider_id ");
		sql.append("where debit_memo_id is null and approval_dt is not null and (a.customer_assisted_cd is null or a.customer_assisted_cd = '') ");
		sql.append("and (a.end_user_refund_flg is null or a.end_user_refund_flg = 0) ");
		sql.append("group by oem_id, retail_id, user_id, first_nm, last_nm ");
		
		sql.append(DBUtil.UNION);
		sql.append("select replace(newid(), '-', '') as debit_memo_id, c.oem_id, ");
		sql.append("d.provider_id as retail_id, count(*) as credit_memo_no, u.user_id, u.first_nm, u.last_nm  ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_credit_memo a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_assignment ta on c.ticket_id = ta.ticket_id and ta.assg_type_cd = 'CALLER' ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_user u on ta.user_id  = u.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location d on c.retailer_id = d.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_warranty e on c.product_warranty_id = e.product_warranty_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_warranty w on e.warranty_id = w.warranty_id ");
		sql.append("where debit_memo_id is null and approval_dt is not null and (a.customer_assisted_cd is null or a.customer_assisted_cd = '') ");
		sql.append("and (a.end_user_refund_flg = 1) ");
		sql.append("group by oem_id, retail_id, u.user_id, u.first_nm, u.last_nm ");
		
		sql.append("order by oem_id, retail_id ");
		log.debug(sql.length() + "|" + sql);
		DBProcessor db = new DBProcessor(conn, schema);
		return db.executeSelect(sql.toString(), null, new DebitMemoVO());
		
	}

	/**
	 * Gets the credit memos for the given debit memo
	 * @param schema
	 * @param memo
	 */
	protected void getCreditMemos(String schema, DebitMemoVO memo) {
		//  Load the params
		List<Object> vals = new ArrayList<>();
		vals.add(memo.getOemId());
		vals.add(memo.getRetailId());
		vals.add(memo.getRetailId());

		StringBuilder sql = new StringBuilder(400);
		sql.append("select c.oem_id, ticket_no, coalesce(nullif(w.refund_provider_id,''),d.provider_id) as provider_id, product_nm, a.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_credit_memo a ");
		
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket_ref_rep b on a.ticket_ref_rep_id = b.ticket_ref_rep_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_ticket c on b.ticket_id = c.ticket_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_ticket_assignment ta on c.ticket_id = ta.ticket_id and ta.assg_type_cd = 'CALLER' ");
		
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_serial e on c.product_serial_id = e.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master f on e.product_id = f.product_id ");
		
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider_location d on c.retailer_id = d.location_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_warranty pw on e.product_serial_id = pw.product_serial_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_warranty w on pw.warranty_id = w.warranty_id ");
		
		sql.append("where debit_memo_id is null and approval_dt is not null ");
		sql.append("and oem_id = ? and (d.provider_id = ? or w.refund_provider_id =  ? ) and (a.customer_assisted_cd is null or a.customer_assisted_cd = '') ");
		
		if ( ! StringUtil.isEmpty(memo.getUserId())) {
			sql.append("and end_user_refund_flg = 1 and ta.user_id = ? ");
			vals.add(memo.getUserId());
		} else {
			sql.append("and (end_user_refund_flg = 0 or end_user_refund_flg = null ) ");
		}
		
		log.debug(sql.length() + "|" + sql + "|" + vals);

		// Get the memos
		DBProcessor db = new DBProcessor(conn, schema);
		List<CreditMemoVO> data = db.executeSelect(sql.toString(), vals, new CreditMemoVO());
		log.debug("number of credit memos found " + data.size());
		
		// Update the sum of the credit memos on the debit memo
		double total = 0;
		for (CreditMemoVO cm : data) total += cm.getRefundAmount();
		memo.setTotalCreditMemoAmount(total);
		memo.setCreditMemos(data);
	}

	/**
	 * Gets the OEM information from the database and assigns it to the memo
	 * @param schema
	 * @param memo
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void assignOEM(String schema, DebitMemoVO memo) 
			throws InvalidDataException, DatabaseException {
		ProviderVO prov = new ProviderVO();
		prov.setProviderId(memo.getOemId());

		DBProcessor db = new DBProcessor(conn, schema);
		db.getByPrimaryKey(prov);
		memo.setOem(prov);
	}

	/**
	 * Gets the OEM information from the database and assigns it to the memo
	 * @param schema
	 * @param memo
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void assignRetailer(String schema, DebitMemoVO memo) 
			throws InvalidDataException, DatabaseException {
		ProviderVO prov = new ProviderVO();
		prov.setProviderId(memo.getRetailId());

		DBProcessor db = new DBProcessor(conn, schema);
		db.getByPrimaryKey(prov);
		memo.setRetailer(prov);
	}
}
