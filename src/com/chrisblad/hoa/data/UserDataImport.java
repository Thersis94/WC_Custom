package com.chrisblad.hoa.data;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.db.ProfileImport;

/****************************************************************************
 * <b>Title</b>: UserDataImport.java<p/>
 * <b>Description: This object was specifically created to batch-load data for Chris Blad's HOA site.
 * It pushes the records from an Excel file through the frount door of the website's
 * registration page, rather than doing direct database insertion.  This was the
 * easiest way to cleanly process the records without have to deal with Profile and
 * registration data differently.  (some fields are encrypted, some aren't)</b>
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 07, 2012
 ****************************************************************************/
public class UserDataImport extends ProfileImport {

	private static String FILE_PATH="/scratch/hoa.csv";
	
	public UserDataImport() {
		super();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {        
        UserDataImport db = new UserDataImport();
		try {
			System.out.println("importFile=" + FILE_PATH);
			List<Map<String,String>> data = db.parseFile(FILE_PATH);
			db.insertRecords(data);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error Processing ... " + e.getMessage());
		}
		db = null;
	}
	
	/**
	 * @param records
	 * @throws Exception
	 */
	protected void insertRecords(List<Map<String, String>> records) throws Exception {
		int count=0;
		int failCnt = 0;
		String PAGE_URL = "http://c.sb.mckain.siliconmtn.com/register";
		
		SMTHttpConnectionManager conn = null;
		
		try {
			for (Map<String, String> data : records) {
				conn = new SMTHttpConnectionManager();
				conn.retrieveDataViaPost(PAGE_URL, buildParams(data));
				log.info("retStatus= " + conn.getResponseCode());
				if (conn.getResponseCode() == 200) { ++count; } else { ++failCnt; };
				//if (count == 10) return;
				conn = null;
			}
		} catch (IOException ioe) {
			log.error("IOException " + ioe.getMessage(), ioe);
		} catch (Exception e) {
			log.error("unexpected exception: " + e.getMessage(), e);
		}
		
		log.info("submitted " + count + " records with " + failCnt + " failures");
	}
	
	
	private String buildParams(Map<String, String> data) {
		StringBuilder params = new StringBuilder("1=1");		
		//append any runtime requests of the calling class.  (login would pass username & password here)
		for (String p : data.keySet()) {
			String key = StringUtil.replace(p, "%", "|");
			params.append("&").append(key).append("=").append(StringUtil.replace(data.get(p), "\\n","\n").trim());
		}
		
		//append some form constants that WC passed in hidden fields
		params.append("&formFields=7f000001407b18842a834a598cdeafa");
		params.append("&formFields=7f000001427b18842a834a598cdeafa");
		params.append("&formFields=7f000001447b18842a834a598cdeafa");
		params.append("&formFields=7f000001397b18842a834a598cdeafa");
		params.append("&formFields=c0a802417896130fa57c20779fd550c5");
		params.append("&formFields=7f000001467b18842a834a598cdeafa");
		params.append("&formFields=7f000001487b18842a834a598cdeafa");
		params.append("&formFields=7f000001497b18842a834a598cdeafa");
		params.append("&formFields=7f000001507b18842a834a598cdeafa");
		params.append("&formFields=7f000001577b18842a834a598cdeafa");
		params.append("&formFields=7f000001527b18842a834a598cdeafa");
		params.append("&formFields=7f000001517b18842a834a598cdeafa");
		params.append("&formFields=7f000001667b18842a834a598cdeafa");
		params.append("&formFields=7f000001547b18842a834a598cdeafa");
		params.append("&formFields=c0a80241789658e1ffabcbb596a522a0");
		params.append("&formFields=c0a8024178968fa78a8357eb2016bcd2");
		params.append("&formFields=c0a802417896bf8751cb6c0a3028e805");
		params.append("&formFields=c0a802417896f37192476a6bd3e29477");
		params.append("&formFields=c0a8024178971c7110996c6fd8e2c604");
		params.append("&formFields=c0a802417897532a83de36376a151a05");
		params.append("&formFields=c0a8024178978602369aaa9ad9c05f18");
		params.append("&formFields=c0a802417898521669da6245babdc7c8");
		params.append("&formFields=c0a80241789820686e1e3fc5a43c59e8");
		params.append("&pmid=c0a8023796445134a64efaf8af89a204");
		params.append("&requestType=reqBuild");
		params.append("&actionName=");
		params.append("&sbActionId=c0a8024178947fb7305072ba813c8dc");
		params.append("&page=2");
		params.append("&registerSubmittalId=");
		params.append("&postProcess=");
		params.append("&notifyAdminFlg=0");
		params.append("&finalPage=1");
		params.append("&apprReg=0");
		log.debug("post data=" + params);
		return params.toString();
	}
}
