package com.universal.util;

// Java 8
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.net.ssl.SSLSocketFactory;

// Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// Dom4j
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

// SMTBaseLibs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.http.HttpsSocketFactoryManager;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MessageVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.XMLUtil;
// WebCrescendo
import com.smt.sitebuilder.util.MessageSender;

/*****************************************************************************
 <b>Title: </b>WebServiceMonitor.java
 <b>Project: </b>
 <b>Description: </b>
 <b>Copyright: </b>(c) 2000 - 2017 SMT, All Rights Reserved
 <b>Company: Silicon Mountain Technologies</b>
 @author groot
 @version 1.0
 @since Oct 23, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class WebServiceMonitor extends CommandLineUtil {
    private Map<String, Object> mailAttributes;
    private final String SITE_PREFIX = "USA_";
    private final String SMT_USER = "SMTMonitor";
    private final String SMT_PWD = "123";
    public static final String BASE_XML_HEADER = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>";

    public WebServiceMonitor(String[] args) {
        super(args);
        log = Logger.getLogger(getClass());
        if (args.length < 1) {
            System.out.println("Usage: WebServiceMonitor propertiesFile");
            System.exit(-1);
        }
        if (args.length > 1) {
            PropertyConfigurator.configure((String)args[1]);
            log.info(("Using logger properties: " + args[1]));
        }
    }

    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        WebServiceMonitor wsm = new WebServiceMonitor(args);
        wsm.run();
        log.info("End of processing.");
    }

    /**
     * 
     */
    public void run() {
        loadProperties(args[0]);
        if (props == null) {
            log.error(("Error finding/loading properties file: " + args[0] + " exiting."));
            return;
        }
        log.info("Loaded properties file...");
        loadMailAttributes(props);
        monitor(props);
        if (dbConn != null) {
            closeDBConnection();
        }
    }

    /**
     * 
     * @param props
     */
    private void loadMailAttributes(Properties props) {
        log.info("Loading mail attributes map.");
        mailAttributes = new HashMap<>();
        mailAttributes.put("defaultMailHandler", props.get("defaultMailHandler"));
        mailAttributes.put("instanceName", props.get("instanceName"));
        mailAttributes.put("appName", props.get("appName"));
        mailAttributes.put("adminEmail", props.get("adminEmail"));
    }

    /**
     * 
     * @param props
     */
    private void monitor(Properties props) {
        log.info("Starting monitor...");
        String serviceUrl = null;
        for (Object key : props.keySet()) {
            if (!key.toString().startsWith(SITE_PREFIX)) continue;
            serviceUrl = props.getProperty((String)key);
            log.info(("Monitoring site with ID: " + key));
            try {
                monitorHttps(serviceUrl);
                Thread.sleep(500);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    /**
     * 
     * @param stubUrl
     */
    private void monitorHttps(String stubUrl) {
        boolean useSSL = true;
        String url = formatServiceURL(stubUrl, "login");
        log.info("Monitoring HTTPS");
        log.info("---> url: " + url);
        StringBuilder s = new StringBuilder(300);
        s.append(BASE_XML_HEADER).append("<MemberRequest>");
        s.append("<Email>").append(SMT_USER).append("</Email>");
        s.append("<Password>").append(SMT_PWD).append("</Password>");
        s.append("</MemberRequest>");
        Map<String,Object> params = new HashMap<>();
        params.put("xml", s.toString());
        callWebService(url, params, "root", useSSL, "https");
    }

    /**
     * 
     * @param url
     * @param suffix
     * @param useSSL
     * @return
     */
    private String formatServiceURL(String url, String suffix) {
        StringBuilder prefix = new StringBuilder(100);
        prefix.append("https://");
        prefix.append(url);
        prefix.append(suffix);
        return prefix.toString();
    }

    /**
     * 
     * @param url
     * @param xmlRequest
     * @param elem
     * @param useSSL
     * @param callType
     */
    private void callWebService(String url, Map<String,Object> params, 
    		String elem, boolean useSSL, String callType) {
        log.debug("xmlRequest: xml=" + params.get("xml"));
        SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
        if (useSSL) {
            SSLSocketFactory sfy = buildSSLSocketFactory();
            conn.setSslSocketFactory(sfy);
        }
        byte[] data = null;
        boolean isError = false;
        StringBuilder errMsg = new StringBuilder(30);
        try {
            data = conn.retrieveDataViaPost(url, params);
            if (conn.getResponseCode() != 200) {
                throw new InvalidDataException(Integer.toString(conn.getResponseCode()));
            }
            if (data == null) {
                throw new IllegalStateException("Data object is null.");
            }
        }
        catch (IOException ioe) {
            isError = true;
            errMsg.append("Unable to connect to web service: ");
            errMsg.append(ioe.getMessage());
            log.error((errMsg + ioe.getMessage()));
        }
        catch (InvalidDataException ide) {
            isError = true;
            errMsg.append("Invalid response code received from web service: ");
            errMsg.append(ide.getMessage());
            log.error((errMsg + ide.getMessage()));
        }
        catch (IllegalStateException ise) {
            isError = true;
            errMsg.append("No data received from web service: ");
            errMsg.append(ise.getMessage());
            log.error((errMsg + ise.getMessage()));
        }
        
        if (isError) {
            sendErrorNotification(url, params, data, errMsg, callType);
            log.error("Monitoring failed, sent notification.");
            return;
        }
        log.debug(("xml response data: " + new String(data)));
        
        try {
            retrieveElement(data, elem);
            log.info("---> Success");
        }
        catch (DocumentException de) {
            errMsg.append("Error parsing element from XML response data: ");
            errMsg.append(de.getMessage());
            sendErrorNotification(url, params, data, errMsg, callType);
            log.error("Monitoring failed, sent notification.");
        }
    }

    /**
     * 
     * @param data
     * @param elemName
     * @return
     * @throws DocumentException
     */
    private Element retrieveElement(byte[] data, String elemName) throws DocumentException {
        log.debug(("Retrieving XML data element: " + elemName));
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            Document doc = new SAXReader().read((InputStream)bais);
            Element root = doc.getRootElement();

            if (root.getName().equalsIgnoreCase("error")) {
            	StringBuilder errMsg = new StringBuilder(100);
            	errMsg.append(XMLUtil.checkVal(root.element("ErrorCode")));
            	errMsg.append("|");
            	errMsg.append(XMLUtil.checkVal(root.element("ErrorMessage")));
            	log.info("Webservice is available but returned an Error element: " + errMsg);
                return root;

            } else if (root.getName().equalsIgnoreCase("root")) {
            	return root;

            }

            return root.element(elemName);

        }

        catch (Exception e) {
            log.error(("Error reading XML data or error returned in webservice response: " + elemName), (Throwable)e);
            throw new DocumentException(e.getMessage());
        }
    }

    /**
     * 
     * @return
     */
    private SSLSocketFactory buildSSLSocketFactory() {
        String ksFilePath = props.getProperty("keyStorePath");
        String ksFileName = props.getProperty("keyStoreFileName");
        String ksPwd = props.getProperty("keyStorePassword");
        log.debug(("ks path|name|pwd: " + ksFilePath + "|" + ksFileName + "|" + ksPwd));
        HttpsSocketFactoryManager hsfm = new HttpsSocketFactoryManager(String.valueOf(ksFilePath) + ksFileName, decryptString(ksPwd));
        SSLSocketFactory ssf = null;
        try {
            ssf = hsfm.buildDefaultSslSocketFactory(HttpsSocketFactoryManager.SSLContextType.TLSv1_2.getContextName());
        }
        catch (Exception e) {
            log.error("Error building SSL socket factory for secure connection, ", (Throwable)e);
        }
        return ssf;
    }

    /**
     * 
     * @param encVal
     * @return
     */
    private String decryptString(String encVal) {
        if (encVal == null) {
            return encVal;
        }
        StringEncrypter se = null;
        try {
            se = new StringEncrypter(props.getProperty("encryptKey"));
            return se.decrypt(encVal);
        }
        catch (Exception e) {
            log.error(("Error decrypting value: " + e.getMessage()));
            return encVal;
        }
    }

    /**
     * 
     * @param apiUrl
     * @param xmlRequest
     * @param xmlResponse
     * @param errMsg
     * @param callType
     */
    private void sendErrorNotification(String apiUrl, Map<String,Object> params, 
    		byte[] xmlResponse, StringBuilder errMsg, String callType) {
    	log.debug("Sending error notification.");
        String toNotify = StringUtil.checkVal((String)props.getProperty("usaWebServiceNotify"), (String)null);
        if (toNotify == null) {
            return;
        }
        StringBuilder body = buildErrorNotificationBody(apiUrl, params, xmlResponse, errMsg, callType);
        EmailMessageVO msg = new EmailMessageVO();
        try {
            msg.setFrom(props.getProperty("emailFrom"));
            msg.addRecipient(toNotify);
            toNotify = StringUtil.checkVal(props.get("smtWebServiceNotify"), (String)null);
            if (toNotify != null) {
                msg.addBCC(toNotify);
            }
            msg.setSubject("Universal Screen Arts Mobile Site Web Service Error Notification");
            msg.setTextBody(body.toString());
        }
        catch (InvalidDataException ide) {
            log.error(("Error formatting error email notification, " + ide.getMessage()));
            return;
        }
        if (dbConn == null) {
            loadDBConnection(props);
        }
        MessageSender ms = new MessageSender(mailAttributes, dbConn);
        ms.sendMessage((MessageVO)msg);
        if (msg.getErrorString() != null) {
            log.error(("Error sending error email notification, " + msg.getErrorString()));
        }
    }

    /**
     * 
     * @param apiUrl
     * @param xmlRequest
     * @param xmlResponse
     * @param errMsg
     * @param callType
     * @return
     */
    private StringBuilder buildErrorNotificationBody(String apiUrl, Map<String,Object> params, 
    		byte[] xmlResponse, StringBuilder errMsg, String callType) {
        char lf = '\n';
        StringBuilder body = new StringBuilder(1000);
        body.append("Date/Time: ").append(Calendar.getInstance().getTime()).append(lf).append(lf);
        body.append("Webservice URL: ").append(apiUrl).append(lf).append(lf);
        body.append("Webservice Call Type: ").append(callType);
        body.append(lf).append(lf);
        body.append("Error Message: ").append(lf);
        body.append(errMsg).append(lf).append(lf);
        body.append("Original Request: ").append(lf);
        body.append("xml=").append(params.get("xml")).append(lf).append(lf);
        body.append("Webservice Response: ").append(lf);
        body.append(xmlResponse != null ? new String(xmlResponse) : "- n/a -");
        body.append(lf).append(lf);
        body.append("End of notification.");
        return body;
    }

}
