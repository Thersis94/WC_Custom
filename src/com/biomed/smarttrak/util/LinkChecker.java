package com.biomed.smarttrak.util;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.biomed.smarttrak.action.AdminControllerAction.Status;
import com.biomed.smarttrak.vo.LinkVO;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: LinkChecker.java<p/>
 * <b>Description: Reads data from DB then runs regex looking for embedded URLs.
 * URLs compiled are then sent an HTTP HEAD request to check if they are valid.
 * The outcomes are logged to a separate database table for reporting and maintenance.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 01, 2017
 ****************************************************************************/
public class LinkChecker extends CommandLineUtil {

	static final String HREF_EXP = "<a([^<>]*)href=(\"|')(\\S*)(\"|')([^<>]*)>"; 
	static final int HTTP_CONN_TIMEOUT = 10000;
	static final int HTTP_READ_TIMEOUT = 10000;

	//section & url constants
	private static final String PRODUCTS = "products";
	private static final String COMPANIES = "companies";
	private static final String MARKETS = "markets";
	private static final String ANALYSIS = "analysis";  //formerly called INSIGHTS
	private static final String UPDATES = "updates";

	private Map<String, Long> accessTimes;
	private static final long LAG_TIME_MS = 1000;

	/**
	 * This enum is what we iterate when the script runs.
	 */
	enum Table {
		COMPANY_ATTR_XR(COMPANIES, true,"select i.company_id, value_txt, company_attribute_id from custom.BIOMEDGPS_COMPANY_ATTRIBUTE_XR i left join custom.BIOMEDGPS_COMPANY c on c.company_id = i.company_id left join custom.biomedgps_product p on p.company_id = i.company_id where i.status_no = ? and c.status_no = ? group by i.company_id, value_txt, company_attribute_id having count(p.product_id)>0"),
		PROD_ATTR_XR(PRODUCTS, true,"select i.product_id, value_txt, product_attribute_id from custom.BIOMEDGPS_PRODUCT_ATTRIBUTE_XR i left join custom.BIOMEDGPS_PRODUCT p on p.product_id = i.product_id where i.status_no = ? and p.status_no = ?"),
		MKRT_ATTR_XR(MARKETS, true,"select i.market_id, value_txt, market_attribute_id from custom.BIOMEDGPS_MARKET_ATTRIBUTE_XR i left join custom.BIOMEDGPS_MARKET m on m.market_id = i.market_id where i.status_no = ? and m.status_no = ?"),
		ANALYSIS_ABS(ANALYSIS, false,"select insight_id, abstract_txt, 'Abstract' from custom.BIOMEDGPS_INSIGHT where status_cd = ?"),
		ANALYSIS_MAIN(ANALYSIS, false,"select insight_id, content_txt, 'Article' from custom.BIOMEDGPS_INSIGHT where status_cd = ?");

		String selectSql;
		String section;
		boolean extraCheck;

		Table(String sec, boolean extraCheck, String sel) {
			this.selectSql = sel;
			this.section = sec;
			this.extraCheck = extraCheck;
		}
		String getSelectSql() { return selectSql; }
		String getSection() { return section; }
		boolean needsExtraCheck() { return extraCheck; }
	}

	List<String> validCompanyIds;
	List<String> validProductIds;
	List<String> validMarketIds;
	List<String> validInsightIds;  //AKA analysis
	List<String> recentlyChecked;
	StringEncoder encoder;
	List<String> getDomains; //known domains that don't support HTTP HEAD requests - set in config file
	List<String> getDomainsReport; //temp object for domains we'll report in the log, at the end.
	boolean onlyBroken;
	String mockUserAgent;

	//email reporting metrics
	long startTime;
	int found;
	int extTested;
	int intTested;
	int skipped;
	List<LinkVO> linksFailed;

	/**
	 * default constructor
	 * @param args
	 */
	public LinkChecker(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/link-checker.properties");
		loadDBConnection(props);
		accessTimes = new HashMap<>(5000);
		validCompanyIds = new ArrayList<>(5000);
		validProductIds = new ArrayList<>(5000);
		validMarketIds = new ArrayList<>(5000);
		validInsightIds = new ArrayList<>(5000);
		getDomainsReport = new ArrayList<>(50);
		recentlyChecked = new ArrayList<>(15000);
		encoder = new StringEncoder();
		getDomains = Arrays.asList(props.getProperty("getDomains").split(","));
		linksFailed = new ArrayList<>(10000);
		startTime = System.currentTimeMillis();
		onlyBroken = Convert.formatBoolean(props.getProperty("checkAllBroken"));
		mockUserAgent = StringUtil.checkVal(props.getProperty("mockUserAgent"));
		System.setProperty("http.agent", "");
	}


	/**
	 * main method
	 * @param args
	 */
	public static void main(String[] args) {
		LinkChecker eui = new LinkChecker(args);
		eui.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 * call this method from main() to iterate the enum and execute all tables
	 */
	@Override
	public void run() {
		String days = props.getProperty("runInterval");
		//populate our lookup tables for companies, markets, insights, and products. ..so we're not http-spamming our own site!
		populateLookup("select market_id from custom.biomedgps_market where status_no in ('P')", validMarketIds, false);
		populateLookup("select product_id from custom.biomedgps_product where status_no in ('P')", validProductIds, false);
		populateLookup("select company_id from custom.biomedgps_company where status_no in ('P')", validCompanyIds, false);
		populateLookup("select insight_id from custom.biomedgps_insight where status_cd in ('P')", validInsightIds, false);

		if (onlyBroken) {
			//consider everything NOT a 404 as valid, and we won't check them
			populateLookup("select url_txt from custom.biomedgps_link where status_no != 404", recentlyChecked, true);
		} else {
			populateLookup("select url_txt from custom.biomedgps_link where check_dt > (CURRENT_DATE - interval '"+days+" days')", recentlyChecked, true);
		}

		deleteArchives(days);

		//iterate the enum, read, convert, and write for each
		for (Table t : Table.values())
			run(t);

		sendEmail();
	}


	/**
	 * build lists of recordIds in our database for cross-checking
	 */
	protected void populateLookup(String sql, List<String> records, boolean removeProtocol) {
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (removeProtocol) {
					records.add(StringUtil.checkVal(rs.getString(1)).replaceAll("http(s)?://", ""));
				} else {
					records.add(rs.getString(1));
				}
			}
		} catch (SQLException sqle) {
			log.error("could not load lookups from: " + sql, sqle);
		}
		log.debug("loaded " + records.size() + " lookup values for " + sql.split(" ")[1]);
	}


	/**
	 * call this from main() to run a single enum/table.
	 * @param t
	 */
	protected void run(Table t) {
		List<LinkVO> links = parseLinks(readRecords(t));
		checkLinks(links);
		saveOutcomes(links);
	}


	/**
	 * reads the key/value pairings from the database using the getSelectSql() query defined in the enum
	 * @param t
	 * @return
	 */
	protected List<LinkVO> readRecords(Table t) {
		List<LinkVO> records = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(t.getSelectSql())) {
			ps.setString(1, Status.P.toString());
			if (t.needsExtraCheck())
				ps.setString(2, Status.P.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				records.add(new LinkVO(t.getSection(), rs.getString(1), rs.getString(2), rs.getString(3)));
		} catch (SQLException sqle) {
			log.error("could not read records from table " + t.toString(), sqle);
		}
		return records;
	}


	/**
	 * converts the markup to html for each of the records retrieved
	 * @param records
	 */
	protected List<LinkVO> parseLinks(List<LinkVO> records) {
		List<LinkVO> urls = new ArrayList<>();
		int priorSkips = skipped;

		for (LinkVO vo : records) {
			if (StringUtil.isEmpty(vo.getHtml())) 
				continue;
			
			// Check to see if the content is html or a single link. If neither it is skipped
			if (vo.getHtml().contains("<")) {
				checkLinksInHtml(urls, vo);
			} else if (vo.getHtml().contains("/") || vo.getHtml().contains(".")) {
				urls.add(LinkVO.makeForUrl(vo.getSection(), vo.getObjectId(), vo.getHtml(), vo.getContentId()));
			}
		}
		log.debug("need to check " + urls.size() + " embedded links.  Skipping " + (skipped-priorSkips));
		return urls;
	}


	protected void checkLinksInHtml(List<LinkVO> urls, LinkVO vo) {
		//parse the html into a list of urls.  Each of the found URLs will become it's own LinkVO
		Pattern pat = Pattern.compile(HREF_EXP, Pattern.CASE_INSENSITIVE);
		Matcher m = pat.matcher(vo.getHtml());
		String  u;
		while (m.find()) {
			++found;
			u = m.group(3);
			//remove html encoding
			if (!StringUtil.isEmpty(u) && u.length() > 4)
				u = encoder.decodeValue(u);

			//omit empty, page anchors ("#"), and recently tested (regardless of outcome)
			boolean isRecent = recentlyChecked.contains(StringUtil.checkVal(u).replaceAll("http(s)?://", ""));
			if (StringUtil.isEmpty(u) || u.length() < 2 || isRecent) {
				if (isRecent) 
					++skipped;
				continue;
			}
			urls.add(LinkVO.makeForUrl(vo.getSection(), vo.getObjectId(), u, vo.getContentId()));
		}
	}


	/**
	 * run an http test against each URL - if they're external.
	 * internal links will get cross-referenced against our populated list of valid (accessible) IDs.
	 * @param links
	 */
	protected void checkLinks(List<LinkVO> links) {
		for (LinkVO vo : links) {
			if (vo.getUrl().matches("^https?://(www|app\\.)?smarttrak\\.(com|net)/(.*)") || vo.getUrl().matches("(?i)^/([A-Z]{1,})(.*)")) {
				testInternalLink(vo);
				++intTested;
			} else {
				httpTest(vo);
				++extTested;
			}
			//if it failed, add it to the report email - 200=success, 3xx=redirects (okay)
			//429 is not a failure necessarily, it's a request rate limit.
			if (vo.getOutcome() != 429 && vo.getOutcome() > 403)
				linksFailed.add(vo);
		}
	}


	/**
	 * @param vo
	 * @param id
	 */
	protected void testInternalLink(LinkVO vo) {
		String relaUrl = vo.getUrl().replaceAll("(?i)^https?://(www|app\\.)?smarttrak\\.(com|net)", ""); //remove FQDN
		log.debug(relaUrl + " from " + vo.getUrl());
		if (relaUrl.indexOf('#') > -1) //strip anchor tags
			relaUrl = relaUrl.substring(0, relaUrl.indexOf('#'));

		String targetObjectId = relaUrl.replaceAll("(?i)//?([A-Z]+)/(qs/)?([A-Z0-9]+)/?(.*)?$", "$3"); //get assetId
		String urlSection = relaUrl.replaceAll("(?i)//?([A-Z]+)/(.*)?$", "$1").toLowerCase(); //URI
		log.debug("cross check - section=" + urlSection + ", id=" + targetObjectId);

		if (MARKETS.equals(urlSection)) {
			vo.setOutcome(validMarketIds.contains(targetObjectId) ? 200 : 404);
		} else if (COMPANIES.equals(urlSection)) {
			vo.setOutcome(validCompanyIds.contains(targetObjectId) ? 200 : 404);
		} else if (PRODUCTS.equals(urlSection)) {
			vo.setOutcome(validProductIds.contains(targetObjectId) ? 200 : 404);
		} else if (ANALYSIS.equals(urlSection) || "archives".equals(urlSection)) {
			vo.setOutcome(validInsightIds.contains(targetObjectId) ? 200 : 404);
		} else if (urlSection.matches("/manage\\?(.*)")) {
			vo.setOutcome(404); //links should never go to the /manage tool
		} else {
			inspectLocalUrl(vo, relaUrl);
		}
		log.debug(vo.getOutcome() == 200 ? "success!" : targetObjectId + " in '" + urlSection + "' not found");
	}


	/**
	 * check local urls that could be /tools/* or /secBinary/*
	 * @param vo
	 * @param urlSection
	 */
	protected void inspectLocalUrl(LinkVO vo, String relaUrl) {
		if (relaUrl.startsWith("/tools") || relaUrl.startsWith("/analysis") || 
				relaUrl.startsWith("/explorer") || relaUrl.startsWith("/financial")) {
			vo.setOutcome(200); //known site pages - legacy covered by Apache rewrites
			log.debug("identified as tools url: " + relaUrl);
			return;
		}

		String thecusPath = props.getProperty("sBinaryPath");
		String relaPath = relaUrl.replaceAll("(?i)/(secBinary|media)/","/"); //remove protocol & domain
		if (!relaPath.startsWith("/org/"))
			relaPath = "/org/BMG_SMARTTRAK" + relaPath;

		File f = new File(thecusPath + relaPath);
		if (!f.exists()) log.warn("missing file " + thecusPath + relaPath);
		vo.setOutcome(f.exists() ? 200 : 404);
	}


	/**
	 * @param vo
	 */
	protected void httpTest(LinkVO vo) {
		try {
			URL url = new URL(vo.getUrl());

			// If necessary pause to avoid 429 responses - "Too Fast"
			throttleRequests(vo.getUrl());

			//some domains don't support HEAD - don't bother.
			if (getDomains.contains(url.getHost())) { 
				httpGetTest(vo,url);
			} else {
				httpHeadTest(vo,url);
			}
			//check for redirects
			checkRedirect(vo);

		} catch (Exception e) {
			log.warn("URL Failed: " + e.getMessage() + " sts=" + vo.getOutcome());
			if ("Connection reset".equals(e.getMessage()) || 429 == vo.getOutcome()) {
				//maybe we're too aggressive, let's sleep for 30 secs
				try {
					log.info("sleeping 10 seconds");
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					log.error(e);
					Thread.currentThread().interrupt();
				}
			}

			//try to salvage a quality http response code - fallback to 404
			String s = StringUtil.checkVal(e.getMessage()).replaceAll("(.*)Server returned HTTP response code: ([0-9]{3,3})?(.*)", "$2");
			vo.setOutcome(Convert.formatInteger(s, 404));
		}
	}


	/**
	 * if the initial http call returned a redirect, transpose it into the main URL field and call a second time.
	 * @param vo
	 */
	private void checkRedirect(LinkVO vo) {
		// Check to see if we have been redirected multiple times
		// Any link with this many redirects is a failure and should stop being tested.
		if (vo.getNumChecks() >= 10) return;
			
		if (isRedirect(vo.getOutcome()) && !StringUtil.isEmpty(vo.getRedirectUrl())) {
			log.debug("got redirected to: " + vo.getRedirectUrl());
			if (StringUtil.isEmpty(vo.getOriginalUrl()))
				vo.setOriginalUrl(vo.getUrl());
			vo.setUrl(vo.getRedirectUrl());
			vo.setRedirectUrl(null); //flush this or we're in a continuous loop
			vo.setOutcome(0);
			vo.setNumChecks(vo.getNumChecks()+1);
			httpTest(vo);
		}
	}


	/**
	 * runs a HEAD request against the domains.  Falls-back to GET if a SocketException occurs.
	 * @param vo
	 * @param url
	 */
	protected void httpHeadTest(LinkVO vo, URL url) throws IOException {
		log.debug("HEAD " + vo.getUrl());
		try {
			HttpURLConnection conn = setupConnection("HEAD", url);
			conn.connect();
			vo.setOutcome(conn.getResponseCode());

			if (isRedirect(vo.getOutcome())) {
				vo.setRedirectUrl(conn.getHeaderField("Location"));
			} else if (200 != vo.getOutcome()) {
				log.debug("failed HEAD " + vo.getOutcome() +" reason: " + conn.getResponseMessage());
			}

			//cleanup at the TCP level so Keep-Alives can be leveraged at the IP level
			conn.getInputStream().close();
			conn.disconnect();


		} catch (Exception se) {
			httpGetTest(vo,url);
			//if the above line succeeds, we know this domain does not support HEAD requests
			getDomains.add(url.getHost());
			//add it to the report/email so it can be added to the config file for next time.
			getDomainsReport.add(url.getHost());
		}
	}


	/**
	 * try a GET instead of HEAD
	 * @param vo
	 */
	protected void httpGetTest(LinkVO vo, URL url) throws IOException {
		log.debug("GET " + vo.getUrl());
		HttpURLConnection conn = setupConnection("GET", url);
		conn.connect();
		vo.setOutcome(conn.getResponseCode());

		if (isRedirect(vo.getOutcome())) {
			vo.setRedirectUrl(conn.getHeaderField("Location"));

		} else if (200 != vo.getOutcome()) {
			log.debug("failed GET " + vo.getOutcome() +" reason: " + conn.getResponseMessage());
		}

		//cleanup at the TCP level so Keep-Alives can be leveraged at the IP level
		conn.getInputStream().close();
		conn.disconnect();
	}


	/**
	 * is this http response code a redirect
	 * @param outcome
	 * @return
	 */
	private boolean isRedirect(int outcome) {
		return 301 == outcome || 302 == outcome;
	}


	/**
	 * HTTP request builder - used by HEAD and GET.  
	 * Sets headers like User-Agent and Accept-Encoding to fool remote systems into thinking we're a user.
	 * @param string
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	private HttpURLConnection setupConnection(String verb, URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(verb);
		conn.setConnectTimeout(HTTP_CONN_TIMEOUT);
		conn.setReadTimeout(HTTP_READ_TIMEOUT);
		conn.addRequestProperty("User-Agent", mockUserAgent);
		conn.addRequestProperty("Accept-Encoding", "*");
		conn.addRequestProperty("Accept", "*/*");
		conn.addRequestProperty("Connection", "keep-alive");
		conn.addRequestProperty("Referer", "https://app.smarttrak.com/");
		conn.setUseCaches(true); //true because anything cached won't live beyond the JVM - so encourage reuse within this 'run' (of the script).
		conn.setAllowUserInteraction(false);
		HttpURLConnection.setFollowRedirects(true);
		conn.setInstanceFollowRedirects(true);
		return conn;
	}


	/**
	 * deletes all the DB records we've retested in this run.
	 * if checkDt > interval means preservable links, then checkDt < interval is going to be retested, and therefore is deletable
	 * @param records
	 * @param t
	 */
	protected void deleteArchives(String days) {
		String sql;
		if (onlyBroken) {
			sql = "delete from custom.biomedgps_link where status_no=404";
		} else {
			sql = StringUtil.join("delete from custom.biomedgps_link where check_dt < (CURRENT_DATE - interval '", days," days')");
		}

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int cnt = ps.executeUpdate();
			log.info("deleted " + cnt + " old records");

		} catch (Exception e) {
			log.error("could not delete old records", e);
		}
	}


	/**
	 * writes the converted html back to the database using the getUpdateSql() query in the enum
	 * @param records
	 * @param t
	 */
	protected void saveOutcomes(List<LinkVO> records) {
		UUIDGenerator uuid = new UUIDGenerator();
		String sql = "insert into custom.biomedgps_link (link_id, company_id, market_id, product_id, insight_id, update_id, " +
				"url_txt, check_dt, status_no, content_id, create_dt) values (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";

		for (LinkVO vo : records) {
			try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
				ps.setString(1, uuid.getUUID());
				ps.setString(2, COMPANIES.equals(vo.getSection()) ? vo.getObjectId() : null);
				ps.setString(3, MARKETS.equals(vo.getSection()) ? vo.getObjectId() : null);
				ps.setString(4, PRODUCTS.equals(vo.getSection()) ? vo.getObjectId() : null);
				ps.setString(5, ANALYSIS.equals(vo.getSection()) ? vo.getObjectId() : null);
				ps.setString(6, UPDATES.equals(vo.getSection()) ? vo.getObjectId() : null);
				ps.setString(7,  StringUtil.isEmpty(vo.getOriginalUrl())? vo.getUrl() : vo.getOriginalUrl());
				ps.setTimestamp(8, Convert.formatTimestamp(vo.getLastChecked()));
				ps.setInt(9, vo.getOutcome());
				ps.setString(10, vo.getContentId());
				ps.executeUpdate();

			} catch (Exception e) {
				log.error("could not save outcome for " + vo.getUrl(), e);
			}
		}
	}


	/**
	 * email the administrators a summary of what happened
	 */
	protected void sendEmail() {
		final String br = "<br/>";
		StringBuilder msg = new StringBuilder(1000);
		msg.append("<h3>SmartTRAK Link Checker &mdash; ").append(Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN)).append("</h3>\n");
		msg.append("<h4>Embedded URLs Found: ").append(fmtNo(found)).append(br);
		msg.append("Skipped (Tested Recently): ").append(fmtNo(skipped)).append(br);
		msg.append("Internal URLs Tested: ").append(fmtNo(intTested)).append(br);
		msg.append("External URLs Tested: ").append(fmtNo(extTested)).append(br);
		msg.append("Broken Links Found: ").append(linksFailed.size()).append(br);
		//exec time
		long secs = System.currentTimeMillis()-startTime;
		if (secs > 60000) {
			msg.append("Execution Time: ").append(fmtNo(secs/60000)).append(" minutes");
		} else {
			msg.append("Execution Time: ").append(fmtNo(secs/1000)).append(" seconds");
		}
		msg.append("</h4>");

		if (!linksFailed.isEmpty())
			appendBrokenLinkTable(msg);

		if (!getDomainsReport.isEmpty())
			appendGetDomainsList(msg);

		try {
			super.sendEmail(msg,null);
		} catch (Exception e) {
			log.error("could not send admin email", e);
		}
	}


	protected String fmtNo(int no) {
		return NumberFormat.getNumberInstance(Locale.US).format(no);
	}
	protected String fmtNo(long no) {
		return NumberFormat.getNumberInstance(Locale.US).format(no);
	}


	/**
	 * @param msg
	 */
	protected void appendBrokenLinkTable(StringBuilder msg) {
		String baseDomain = props.getProperty("baseDomain");
		//add a table of failed links
		msg.append("<h4>Broken Links (").append(linksFailed.size()).append(") - Recurrences removed for brevity</h4>\n");
		msg.append("<table border='1' width='95%' align='center'><thead><tr>");
		msg.append("<th>Page</th>");
		msg.append("<th>Outcome</th>");
		msg.append("<th>URL</th>");
		msg.append("</tr></thead><tbody>");

		String qsPath = props.getProperty("qsPath");
		List<String> printed = new ArrayList<>(linksFailed.size()); //de-duplicate the results
		for (LinkVO vo : linksFailed) {
			String unqKey = vo.getUrl() + vo.getSection() + vo.getObjectId();
			if (printed.contains(unqKey)) 
				continue; //only print the ones that failed
			msg.append("<tr>");
			msg.append("<td nowrap><a href=\"").append(baseDomain).append("/").append(vo.getSection()).append(qsPath).append(vo.getObjectId()).append("\">").append(vo.getSection()).append("</a></td>");
			msg.append("<td nowrap>").append(vo.getOutcome()).append("</td>");
			msg.append("<td><a href=\"").append(vo.getUrl().startsWith("/") ? baseDomain + vo.getUrl() : vo.getUrl()).append("\">").append(vo.getUrl()).append("</a></td>");
			msg.append("</tr>");
			printed.add(unqKey);
		}
		msg.append("</tbody></table> <br/>\n");
	}

	/**
	 * @param msg
	 */
	private void appendGetDomainsList(StringBuilder msg) {
		//add a table of failed links
		msg.append("<h4>Domains not supporting HEAD (Sys-Admin: add these to the config file)</h4>\n");
		for (String domain: getDomainsReport)
			msg.append(domain).append("<br/>\n");		
	}


	/**
	 * Puts the thread to sleep if we haven't waited at least a minimum amount of time between USPTO queries
	 * Calculate a wait time based on the last time we queried them.  If less than the threshold, put our thread to sleep.
	 */
	private void throttleRequests(String url) {
		String domain = StringUtil.stripProtocol(url);
		
		if (domain.contains("/"))
			domain = domain.substring(0, domain.indexOf('/'));

		Long lastAccessTime = accessTimes.get(domain);
		if (lastAccessTime == null) lastAccessTime = Long.valueOf(0);
		log.debug("domain from URL= " + domain + " last access=" + lastAccessTime);

		long mustWaitTime = System.currentTimeMillis() - lastAccessTime;
		if (mustWaitTime > 0 && mustWaitTime < LAG_TIME_MS) {
			try {
				//sleep the remaining time to get us to the threshold
				log.debug("sleeping for " + (LAG_TIME_MS - mustWaitTime));
				Thread.sleep(LAG_TIME_MS - mustWaitTime);
			} catch (Exception e) {
				//don't care - this would bubble up as runtime issues anyways
			}
		}
		accessTimes.put(domain, System.currentTimeMillis());
	}
}