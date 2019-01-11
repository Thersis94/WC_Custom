package com.wsla.scheduler.job;

import java.io.IOException;
// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletResponse;

// Quartz 2.2.3
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// SMT Base Libs
import com.siliconmtn.data.report.PDFGenerator;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.HttpBeanContext;
import com.siliconmtn.http.filter.fileupload.Constants;
import com.siliconmtn.http.filter.fileupload.FileTransferStructureImpl;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.FileLoader;

// WC Libs
import com.smt.sitebuilder.scheduler.AbstractSMTJob;
import com.wsla.common.WSLAConstants;
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
	public static final String DB_SCHEMA = "DB_SCHEMA";
	private Map<String, Object> attributes;
	
	/**
	 * 
	 */
	public DebitMemoJob() {
		super();
	}

	/**
	 * Helper to build and test the class outside of the scheduler app
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception {
		DebitMemoJob job = new DebitMemoJob();
		job.attributes = new HashMap<>();
		job.attributes.put(Constants.PATH_TO_BINARY, "/Users/james/Code/git/java/WebCrescendo/binary");
		job.attributes.put(DB_SCHEMA, "custom.");
		job.attributes.put(Constants.INCLUDE_DIRECTORY, "/WEB-INF/include/");
		job.attributes.put("fileManagerType", "2");
		
		String path = job.buildMemoPDF(new DebitMemoVO());
		job.log.info("File Path: " + path);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.scheduler.AbstractSMTJob#execute(org.quartz.JobExecutionContext)
	 */
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		super.execute(ctx);
		attributes = ctx.getMergedJobDataMap().getWrappedMap();
		processDebitMemos();
	}
	
	/**
	 * When run, it creates a debit memo for all approved credit memos that 
	 * have not been included in a debit memo.  The debit memos are created by
	 * a unique pair of oem and retailer
	 * @param attributes
	 */
	protected void processDebitMemos() {
		log.info("Processing: " + attributes);
		String schema = StringUtil.checkVal(attributes.get(DB_SCHEMA));
		
		// Query to find unique retailer, oem pairs that have unassigned and 
		// Approved credit memos
		List<DebitMemoVO> memos = getDebitMemos(schema);
		
		// Update the DB Tables
		DBProcessor db = new DBProcessor(conn, schema);
		for (DebitMemoVO memo : memos) {
			String slug = RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS);
			memo.setDebitMemoId(slug);
			try {
				// Create the actual debit memo and attach as an asset and asset
				this.buildMemoPDF(memo);
				
				// Create a debit memo for each oem/retailer pair
				db.save(memo);
				
				// Add the debit memo id to each of the credit memos assigned to the debit memo
				this.updateCreditMemos(schema, slug, memo.getCreditMemos());
				
			} catch(Exception e) {
				log.error("Unable to create debit memo", e);
			}
		}		
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
		
		// Create the file loader and write to the file system
		FileLoader fl = new FileLoader(attributes);
		fl.setPath(fs.getFullPath());
		fl.setFileName(fs.getStorageFileName());

		try {
			fl.setData(createPDF(memo));
			fl.writeFiles();
		} catch(Exception e) {
			throw new FileWriterException(e);
		}
		
		// Return the full path
		return fs.getFullPath() + fs.getStorageFileName();
	}
	
	/**
	 * Creates the PDF file
	 * @param memo
	 * @return
	 * @throws IOException 
	 * @throws InvalidDataException 
	 */
	protected byte[] createPDF(DebitMemoVO memo) throws IOException {
		
		// Generate the pdf
		String path = getClass().getResource("debit_memo.ftl").getPath();
		Locale locale = new Locale("es", "MX");
		HttpBeanContext.create(null, (HttpServletResponse)null);
		ResourceBundle rb = ResourceBundle.getBundle(WSLAConstants.RESOURCE_BUNDLE, locale);
		
		try{
			PDFGenerator pdf = new PDFGenerator(path, memo, rb);
			return pdf.generate();
		} catch (Exception e) {
			throw new IOException(e);
		}
		
		
	}
	
	/**
	 * Updates the credit memos with the debit memo id to assign these values
	 * @param schema
	 * @param slug
	 * @param creditMemos
	 * @throws SQLException
	 */
	public void updateCreditMemos(String schema, String slug, List<CreditMemoVO> creditMemos) 
	throws SQLException {
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder();
		sql.append("update ").append(schema).append("wsla_credit_memo ");
		sql.append("set debit_memo_id = ? where credit_memo_id in (");
		DBUtil.preparedStatmentQuestion(creditMemos.size(), sql);
		sql.append(")");
		
		try(PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			int ctr = 1;
			ps.setString(ctr++, slug);
			
			// Add all of the credit memos as params
			for (CreditMemoVO memo : creditMemos) {
				ps.setString(ctr++, memo.getCreditMemoId());
			}
			
			// Update the credit memos
			ps.executeUpdate();
			
		}
	}
	
	/**
	 * Query to find unique retailer, oem pairs that have unassigned and 
	 * Approved credit memos
	 * @param schema
	 * @return
	 */
	public List<DebitMemoVO> getDebitMemos(String schema) {
		log.info(conn + "|" + schema);
		
		return new ArrayList<>();
	}
}

