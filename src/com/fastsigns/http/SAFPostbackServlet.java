package com.fastsigns.http;

// JDK .1.6x
import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// J2EE 1.5.x
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

// Dom4J 
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

// FASTSIGNS libs
import com.fastsigns.action.RequestAQuoteSTF;
import com.fastsigns.action.RequestAQuoteSTF.TransactionStep;
import com.fastsigns.action.saf.SAFConfig;

// SMT Base Libs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTBaseServlet;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.QueryStringParser;
import com.siliconmtn.http.parser.SMTParser;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;

// WC Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SAFPostbackServlet.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 5, 2011
 ****************************************************************************/
public class SAFPostbackServlet extends SMTBaseServlet {
	private static final long serialVersionUID = -2696722044921482053L;
	public static final int MAX_FILE_UPLOAD = 5;
	
	public SAFPostbackServlet() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.http.SMTBaseServlet#processRequest(com.siliconmtn.http.SMTServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void processRequest(SMTServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		log.debug("starting...");
		log.debug("callback QueryString: " + req.getQueryString());

		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SAFConfig safConfig = SAFConfig.getInstance(site.getCountryCode());
		
		String status = req.getParameter("callback");
		if ("submit".equalsIgnoreCase(status)) {
			// Process the transfer started code
			this.transferStarted(req, safConfig);
			res.setStatus(HttpServletResponse.SC_OK);
		} else if ("status".equalsIgnoreCase(status)) {
			// provides a mechanism for the web page to update transaction status
			// (what's going on in the browser!)
			this.updateStatus(req, safConfig);
			res.setStatus(HttpServletResponse.SC_OK);
			
		} else if ("filecomplete".equalsIgnoreCase(status)) {
			// Process the complete code
			this.transferComplete(req, res);
			res.setStatus(HttpServletResponse.SC_OK);
			
		} else {
			this.processYSICallBack(req, res);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void transferComplete(SMTServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		log.debug("Starting transfer complete");
		
		// Set the status code
		req.setParameter("status", "File Transfer Complete");
		req.setParameter("csi", req.getParameter("uniqueFastSignsId"));
		req.setValidateInput(false);
		
		// Create a collection of uploaded files
		List<FilePartDataBean> files = new ArrayList<FilePartDataBean>();
		for (int i=0; i < MAX_FILE_UPLOAD; i++) {
			String filename = StringUtil.checkVal(req.getParameter("filename" + i));
			if (filename.length() == 0) continue;
			
			//fix unicode in file name
			try {
				filename = URLDecoder.decode(filename, "UTF-8");
			} catch (Exception e) {
				log.error("could not decode file name for SAF csi=" + 
						req.getParameter("uniqueFastSignsId") + ", " + filename);
			}
			
			// Build a file object and add it to the collection
			FilePartDataBean fpdb = new FilePartDataBean();
			fpdb.setFileName(filename);
			fpdb.setPath(req.getParameter("link" + i));
			//fpdb.setFileId(req.getParameter("uniquenotificationid" + i));
			//fpdb.setKey(ele.element("date_uploaded").getStringValue());
			files.add(fpdb);
			
			log.debug("Added File: " + filename);
		}

		req.setValidateInput(true);
		
		// Call the action
		RequestAQuoteSTF raq = new RequestAQuoteSTF();
		SMTDBConnection conn = this.getDBConnection();
		ModuleVO mod = new ModuleVO();
		mod.setActionData(files);
		try {
			
			// Call the action
			raq.setDBConnection(conn);
			raq.setAttributes((Map<String, Object>) getProperty(GlobalConfig.KEY_ALL_CONFIG));
			raq.setAttribute(Constants.MODULE_DATA, mod);
			raq.processPostback(req);
		} catch (Exception e) {
			log.error("Unable to update status: " + req.getQueryString(), e);
		} finally {
			try {
				conn.close();
			} catch(Exception e) {}
		}

	}
	
	/**
	 * 
	 * @param req
	 */
	public void transferStarted(SMTServletRequest req, SAFConfig safConfig) {
		log.debug("Processing status change");
		RequestAQuoteSTF raq = new RequestAQuoteSTF();
		SMTDBConnection conn = this.getDBConnection();
		try {
			raq.setDBConnection(conn);
			raq.recordStep(req.getParameter("uniqueFastSignsId"), TransactionStep.fileSent, safConfig);
		} finally {
			try {
				conn.close();
			} catch(Exception e) {}
		}
	}
	
	
	/**
	 * 
	 * @param req
	 */
	public void updateStatus(SMTServletRequest req, SAFConfig safConfig) {
		log.debug("Processing status change");
		RequestAQuoteSTF raq = new RequestAQuoteSTF();
		SMTDBConnection conn = this.getDBConnection();
		try {
			raq.setDBConnection(conn);
			if (req.hasParameter("status"))
				raq.recordStatus(req.getParameter("csi"), req.getParameter("status"), safConfig);
			
			if (req.hasParameter("tStep"))
				raq.recordStep(req.getParameter("csi"), TransactionStep.valueOf(req.getParameter("tStep")), safConfig);
			
		} finally {
			try {
				conn.close();
			} catch(Exception e) {}
		}
	}
	
	
	/**
	 * 
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void processYSICallBack(SMTServletRequest req, HttpServletResponse res) 
	throws ServletException, IOException {
		String msg = "OK, XML received"; //the word "OK" must be in the response according to the YSI APIs.
		String status = "YSI Postback Received";  //gets written to the DB by the action
		
		//turn this off or the XML will be mangled
		req.setValidateInput(Boolean.FALSE);
		
		//for debugging purposes
		log.info("received SAF postback: " + req.getParameter("xml_response"));
		
		Document doc = null;
		try {
			// Parse out the XML Data and create the Document
			String xmlData = req.getParameter("xml_response");
			ByteArrayInputStream bais = new ByteArrayInputStream(xmlData.getBytes());
			SAXReader reader = new SAXReader();
			doc = reader.read(bais);
		} catch (Exception e) {
			msg = "could not open XML";
			status = msg;
			log.error(msg, e);
		}
		
		if (doc == null) return;
		
		//get back our request params
		Node n = null;
		try {
			n = doc.selectSingleNode("//*[name()='request_param']");
			SMTParser p = new QueryStringParser();
			p.processData(n.getStringValue());
			
			//loop what was given and put them on our SMTServletRequest
			for (String s : p.getParameterMap().keySet()) {
				req.setParameter(s, p.getParameter(s));
				log.debug("setting " + s + "=" + p.getParameter(s));
			}
		} catch (Exception e) {
			log.error("parsing request_param values", e);
			status = "error processing XML";
		}
		
		//retrieve the file data from the XML
		List<FilePartDataBean> files = new ArrayList<FilePartDataBean>();
		try {
			List<?> result = doc.selectNodes("//*[name()='file_info']");
			for(int i=0; i < result.size(); i++) {
				Element ele = (Element) result.get(i);
				FilePartDataBean fpdb = new FilePartDataBean();
				fpdb.setFileName(ele.element("file_name").getStringValue());
				fpdb.setPath(ele.element("file_download_link").getStringValue());
				fpdb.setFileId(ele.element("file_size").getStringValue());
				fpdb.setKey(ele.element("date_uploaded").getStringValue());
				files.add(fpdb);
				fpdb = null;
			}
		} catch (Exception e) {
			log.error("loading files", e);
			status = "error parsing file data";
		}
		
		req.setParameter("status", status);

		ModuleVO mod = new ModuleVO();
		mod.setPageModuleId(req.getParameter("pmid"));
		mod.setActionData(files);
		
		SMTDBConnection conn = getDBConnection();
		// trigger the postback, which will log files, record status, and send emails
		try {
			RequestAQuoteSTF raq = new RequestAQuoteSTF();
			raq.setDBConnection(conn);
			raq.setAttribute(Constants.MODULE_DATA, mod);
			raq.setAttributes((Map<String, Object>) getProperty(GlobalConfig.KEY_ALL_CONFIG));
			raq.processPostback(req);
		} catch (Exception e) {
			log.error(e);
		} finally {
			try {
				conn.close();
			} catch (Exception e) {}
		}
		
		log.debug("request complete");
		res.setStatus(HttpServletResponse.SC_OK);
		res.getWriter().write(msg);
		return;
	}
}
