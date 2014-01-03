package com.fastsigns.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.fastsigns.action.RequestAQuoteSTF;
import com.fastsigns.action.RequestAQuoteSTF.TransactionStep;
import com.fastsigns.action.saf.SAFConfig;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

/****************************************************************************
 * <b>Title</b>: SendThisFileReconcile.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Sends a request to SendThisFile.com to reconcile file uploads
 * that were never completed under the normal (preferred) workflow.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 19, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SendThisFileReconcile {
	
	protected static Logger log = null;
	private SAFConfig safConfig = null;
	private String stfUrl = "http://www.sendthisfile.com/api/transfer/status/get-custom-transfer-status.jsp?sendthisfilecode=rHhjmUQGP8wcsiOiGc4tJdAq&custominput=custominput12243&uniqueid=";
	private Date startDate = null;
	private Date timeoutDate = null;
	private static final String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	private String dbUrl = "jdbc:sqlserver://10.0.20.63:1433;selectMethod=cursor;responseBuffering=adaptive";
	private String dbUser = "fs_sb_user";
	private String dbPass = "sqll0gin";
	private SMTDBConnection conn = null;
	private RequestAQuoteSTF raq = null;
	public static final Integer NO_CHANGE = Integer.valueOf(99999);
	private Integer timeout = Integer.valueOf(15); //minutes that we wait before timing-out a transaction  (15mins by default)
	
	public SendThisFileReconcile(String[] args) {
		PropertyConfigurator.configure("/data/log4j.properties");
		log = Logger.getLogger(SendThisFileReconcile.class);
		
		log.info("args=" + StringUtil.getDelimitedList(args, false, ","));
		if (args.length >=4) {
			dbUrl = args[0];
			dbUser = args[1];
			dbPass = args[2];
			safConfig = SAFConfig.getInstance(args[3]); //should be a 2-digit countryCode but defaults to US
			if (args.length >= 5) timeout = Convert.formatInteger(args[4], timeout);
			if (args.length >= 6) startDate = Convert.formatDate(args[5]);
			
		} else {
			System.out.println("need args dbUrl, dbUser, dbPass, postbackDomain[, timeout (in hours, default 2), startDate (default -2 days)]");
			System.exit(0);
		}
		
		openDBConnection();
		
		//startDate ensures we're not re-running months and months of bad data every time.
		//go back a max of 2 days and process from there forward.
		//This can be overwritten at runtime for intentional re-runs.
		if (startDate == null) {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DAY_OF_YEAR, -2);
			startDate = c.getTime();
			log.debug("set startDate=" + c.getTime());
		}
		
		//take the current timestamp and roll back # of minutes to our "timeout" point
		//transactions older than "timeout" will be finished, regardless of status.
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -timeout);
		timeoutDate = cal.getTime();
		
		//init helper classes
		raq = new RequestAQuoteSTF();
		raq.setDBConnection(conn);
		

		log.info("startDate=" + Convert.formatDate(startDate, Convert.DATE_TIME_SLASH_PATTERN));
		log.info("timeoutDate=" + Convert.formatDate(timeoutDate, Convert.DATE_TIME_SLASH_PATTERN));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SendThisFileReconcile obj = new SendThisFileReconcile(args);
		obj.process();
		obj.destroy();
	}
	
	/**
	 * this method is the controller
	 */
	public void process() {
		List<DataVO> data = getRecords();
		
		for (DataVO vo: data) {
			//records we need to query STF on
			if (TransactionStep.fileSent == vo.stage) {
				reconcile(vo);
			
			//everything else, force completion once timeout has been reached
			} else if (vo.createDt.before(timeoutDate)) {
				log.error("transaction timed out " + vo.csId);
				finish(vo);
			}
		}
	}
	
	/**
	 * call the database and get the records needing status reconciliation
	 * @return
	 */
	private List<DataVO> getRecords() {
		List<DataVO> data = new ArrayList<DataVO>();
		StringBuilder sql = new StringBuilder();
		sql.append("select a.contact_submittal_id, b.value_txt as 'stage', a.create_dt, 1 as use_stf_flg from contact_submittal a ");
		sql.append("inner join contact_data b on a.contact_submittal_id=b.contact_submittal_id ");
		sql.append("and b.contact_field_id=? ");
		sql.append("where cast(b.VALUE_TXT as varchar(50)) != ? ");
		sql.append("and a.create_dt >= ?");
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(1, safConfig.getTransactionStageFieldId());
			ps.setString(2, TransactionStep.complete.toString());
			ps.setDate(3, new java.sql.Date(startDate.getTime()));
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new DataVO(rs));
			
		} catch (SQLException sqle) {
			log.error("retrieving records to lookup", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		log.info("found " + data.size() + " records to evaluate");
		return data;
	}
	
	/**
	 * finishes processing a request for users who didn't furnish their own postback, 
	 * or who lied and didn't send files!
	 * @param csi
	 */
	@SuppressWarnings("deprecation")
	private void finish(DataVO vo) {
		//make http call to postback servlet.
		StringBuilder path = new StringBuilder();
		path.append("http://").append(safConfig.getPostbackDomain()).append("/smt_saf_postback?");
		path.append("uniqueFastSignsId=").append(vo.csId).append("&callback=filecomplete");
		
		int x=0;
		for (FilePartDataBean f : vo.files) {
			path.append("&filename").append(x).append("=").append(StringEncoder.urlEncode(f.getFileName()));
			path.append("&link").append(x).append("=").append(f.getPath());
			++x;
		}
		log.debug("sending postback to url: " + path);
		try {
			// Send data request
			URL url = new URL(path.toString());
			URLConnection conn = url.openConnection();
			
			//get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while (rd.readLine() != null) {
				//do nothing, this just ensures the HTTP call is completed (and not abandoned)
			}
		} catch (Exception e) {
			log.error("could not send Http Request to postback", e);
		}
	}
	
	/**
	 * perform the write transction back to the database.
	 * @param csId
	 * @param status
	 */
	private void updateStatus(String csId, String status) {
		raq.recordStatus(csId, status, safConfig);
	}
	/**
	 * proxy the Http call to retrieve status, then evaluate the response
	 * @param vo
	 */
	private void reconcile(DataVO vo) {
		byte[] data = sendHttpRequest(vo.csId);
		parseXml(vo, data);
		
		switch (vo.code) {
			case 100: 
				//vo.status = "Upload in process";
				//updateStatus(vo.csId, vo.status);
				log.info("files still being uploaded for " + vo.csId);
				break;
			case 200: 
				vo.status = "Internet connection error - possibly closed browser";
			case 300: 
				if (vo.code == 300) vo.status = "Upload canceled by user";
			case 0:
				if (vo.code == 0) vo.status = "Successful Upload";
				updateStatus(vo.csId, vo.status);
				
				//these levels (above) are all ok to update the DB with.
				log.info("retrieved status " + vo.code + ": " + vo.status + " for csId=" + vo.csId);
				finish(vo);
				break;
			
			case 400:
			case 500:
			case 600:
				vo.status = "Files were not uploaded";
			case 800:
			default:
				updateStatus(vo.csId, vo.status);
				log.error("error status " + vo.code + ": " + vo.status + " for csId=" + vo.csId);
				
				//if this error condition persists beyond the timeout period, finish it (best we can do!)
				if (vo.createDt.before(timeoutDate)) {
					log.error("transaction timed out " + vo.csId);
					finish(vo);
				}
		}
	}
	
	/**
	 * parses the XMl data into a status message (String)
	 * @param data
	 * @return
	 */
	private void parseXml(DataVO vo, byte[] data) {

		// Parse out the XML Data and create the root element
		ByteArrayInputStream bais = null;
		try {
			bais = new ByteArrayInputStream(data);
			SAXReader reader = new SAXReader();
			Document doc = reader.read(bais);
			List<?> result = doc.selectNodes("//*[name()='result']");
			for(int i=0; i < result.size(); i++) {
				Element ele = (Element) result.get(i);
				vo.code = Convert.formatInteger(ele.element("code").getStringValue(), NO_CHANGE); //zero is our success value, so don't confuse that with a default from Convert
				vo.status = "STF Reported: " + StringUtil.checkVal(ele.element("description").getStringValue());
				
				//success obtained: look for files
				if (vo.code != null && vo.code == 0) {
					List<?> files = ele.element("fileset").elements("file");
					for(int x=0; x < files.size(); x++) {
						Element e = (Element) files.get(x);
						vo.addFile(e.element("filename").getStringValue(), e.element("downloadlink").getStringValue());
						log.debug("added file " + e.element("filename").getStringValue() + "->" + e.element("downloadlink").getStringValue());
					}
				}
			}
			
		} catch (Exception e) {
			log.error("parsing XML: " + data.toString(), e);
			vo.status = "STF Parse Error";
		}
	}
	
	/**
	 * sends the http request to the 3rd party website
	 * @param csId
	 * @return
	 */
	private byte[] sendHttpRequest(String csId) {
		StringBuffer sb = new StringBuffer();
		try {
			// Send data request
			URL url = new URL(stfUrl + csId);
			//log.debug("url=" + url);
			URLConnection conn = url.openConnection();
			
			//get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				if (line.trim().length() == 0) continue;  //dump empty CR/LFs or Reader will barf! 
				sb.append(line);
			}
			rd.close();
			log.debug("url: " + url.toString());
			log.debug("resp: " + sb.toString());
		} catch (Exception e) {
			log.error("could not complete STF Http Request", e);
		}
		return sb.toString().getBytes();
	}
	
	private void openDBConnection() {
		log.debug("opening dbConn");
		DatabaseConnection dbc = new DatabaseConnection(dbDriver, dbUrl, dbUser, dbPass);
		try {
			conn = new SMTDBConnection(dbc.getConnection());
		} catch (Exception de) {
			de.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void destroy() {
		log.debug("closing dbConn");
		try {
			raq = null;
			conn.close();
		} catch (Exception e) {
			log.error(e);
		}
	}
	

	public class DataVO {
		String csId = null;
		String status = null;
		TransactionStep stage = null;
		Date createDt = null;
		Integer code = NO_CHANGE;
		boolean useSTF = false;
		List<FilePartDataBean> files = new ArrayList<FilePartDataBean>();
		
		public DataVO(ResultSet rs) {
			DBUtil db = new DBUtil();
			csId = db.getStringVal("contact_submittal_id", rs);
			try {
				stage = TransactionStep.valueOf(db.getStringVal("stage", rs));
			} catch (Exception e) {
				stage = TransactionStep.initiated;
			}
			createDt = db.getDateVal("create_dt", rs);
			useSTF = db.getBoolVal("use_stf_flg", rs);
		}
		
		public DataVO() {
		}
		
		@SuppressWarnings("deprecation")
		void addFile(String name, String url) {
			FilePartDataBean fpdb = new FilePartDataBean();
			fpdb.setFileName(name);
			fpdb.setPath(url);
			files.add(fpdb);
		}
	}
}
