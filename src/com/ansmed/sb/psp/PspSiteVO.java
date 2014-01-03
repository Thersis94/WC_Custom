package com.ansmed.sb.psp;

// JDK 1.6
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.sql.ResultSet;

// SMT Base libs 2.0
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: PspSiteVO.java<p/>
 * <b>Description</b>: Value object for a PSP site. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Oct 5, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PspSiteVO {
	
	
	private Map<String,String> mainLinks;
	private int practiceId;
	private String siteStatus;
	private String sbOrgId = "SJM_PSP_";
	private String sbSiteId = "SJM_PSP_";
	private String sbSiteName;

	
	//index page
	private String homeWelcome;
	
	//overview page
	private String overviewWelcome;
	
	//location page
	private boolean map = false;
	private String direction;
	private String direction2;
	private String direction3;
	private String contactFormEmail;
	
	//general info page
	private String hours;
	private String insurance;
	private String payment;
	private String app;
	private String emergencies;
	private String addInfo;
	private String pdf;
	private String pdfFile;
	
	//basic info used sometimes in header or footer
	private String companyName;
	private String address1;
	private String address2;
	private String city;
	private String state;
	private String zip;
	private String phone;
	private String fax;
	private String email;
	
	//Maps, Lists for various page information
	private Map<String,String> pageImages = new LinkedHashMap<String,String>();
	private Map<String,String> links = new LinkedHashMap<String,String>();
	private List<String> documents = new ArrayList<String>();
	private Map<String,String> contacts = new LinkedHashMap<String,String>();
	private Map<String,String> services = new LinkedHashMap<String,String>();
	private List<PspPhysVO> profiles = new ArrayList<PspPhysVO>();
	private PspSiteTemplateVO template;
	private List<String> siteAliases = new ArrayList<String>();
	
	//flags
	private boolean logoIsContent = false;
	private boolean menuIsContent = false;
	private boolean imagesAreContent = false;
	
	/**
	 * default constructor
	 */
	public PspSiteVO() {
		mainLinks = new HashMap<String,String>();
		loadMainLinks();
	}
	
	/**
	 * Initializes a map with the standard links that a physician can choose 
	 * to display on the Links page in addition to the custom links.
	 */
	private void loadMainLinks() {
		if (mainLinks == null) mainLinks = new HashMap<String,String>();
		mainLinks.put("ans", "www.sjmneuro.com");
		mainLinks.put("npf", "www.nationalpainfoundation.org");
		mainLinks.put("aapm", "www.painmed.org");
		mainLinks.put("acpa", "www.theacpa.org");
		mainLinks.put("apf", "www.painfoundation.org");
		mainLinks.put("ncps", "www.ncps-cpr.org");
	}
	
	/**
	 * Sets page data for various PSP pages
	 * @param rs
	 */
	public void setData(ResultSet rs, String type) {
		
		switch(PspSiteAction.DataType.valueOf(type)) {
			case CONTACT:
				setContactData(rs);
				break;
			case DOCUMENTS:
				setDocumentsData(rs);
				break;
			case IMAGES:
				setImagesData(rs);
				break;
			case LINKS:
				setLinksData(rs);
				break;
			case PROFILES:
				setProfilesData(rs);
				break;
			case SERVICES:
				setServicesData(rs);
				break;
			case SITE:
				setSiteData(rs);
				break;
			case TEMPLATE:
				setTemplateData(rs);
				break;
			case ALIAS:
				setSiteAliasData(rs);
			default:
				break;
		}

	}
	
	/**
	 * Sets contact data
	 * @param rs
	 */
	public void setContactData(ResultSet rs) {
		DBUtil db = new DBUtil();
		contacts.put(db.getStringVal("name",rs), db.getStringVal("email",rs));
		db = null;
	}
	
	/**
	 * Sets documents data
	 * @param rs
	 */
	public void setDocumentsData(ResultSet rs) {
		DBUtil db = new DBUtil();
		documents.add(db.getStringVal("doc_path",rs));
		db = null;
	}
	
	/**
	 * Sets images data
	 * @param rs
	 */
	public void setImagesData(ResultSet rs) {
		DBUtil db = new DBUtil();
		pageImages.put("logocustom",db.getStringVal("logocustom",rs));
		pageImages.put("mapcustom",db.getStringVal("mapcustom",rs));
		pageImages.put("map2custom",db.getStringVal("map2custom",rs));
		pageImages.put("map3custom",db.getStringVal("map3custom",rs));
		pageImages.put("home1",db.getStringVal("home1",rs));
		pageImages.put("home1custom",db.getStringVal("home1custom",rs));
		pageImages.put("overview1",db.getStringVal("overview1",rs));
		pageImages.put("overview1custom",db.getStringVal("overview1custom",rs));
		pageImages.put("overview2",db.getStringVal("overview2",rs));
		pageImages.put("overview2custom",db.getStringVal("overview2custom",rs));
		pageImages.put("service1",db.getStringVal("service1",rs));
		pageImages.put("service1custom",db.getStringVal("service1custom",rs));
		pageImages.put("service2",db.getStringVal("service2",rs));
		pageImages.put("service2custom",db.getStringVal("service2custom",rs));
		pageImages.put("link1",db.getStringVal("link1",rs));
		pageImages.put("link1custom",db.getStringVal("link1custom",rs));
		db = null;
	}
	
	/**
	 * Sets links data
	 * @param rs
	 */
	public void setLinksData(ResultSet rs) {
		DBUtil db = new DBUtil();
		links.put(db.getStringVal("link", rs),db.getStringVal("linkname",rs));
		db = null;
	}
	
	/**
	 * Sets profiles data
	 * @param rs
	 */
	public void setProfilesData(ResultSet rs) {
		profiles.add(new PspPhysVO(rs));
	}
	
	/**
	 * Sets services data
	 * @param rs
	 */
	public void setServicesData(ResultSet rs) {
		DBUtil db = new DBUtil();
		services.put(db.getStringVal("type",rs),db.getStringVal("description",rs));
		db = null;
	}
	
	/**
	 * Sets template data
	 * @param rs
	 */
	public void setTemplateData(ResultSet rs) {
		template = new PspSiteTemplateVO(rs);
	}
	
	/**
	 * Sets site alias data
	 * @param rs
	 */
	public void setSiteAliasData(ResultSet rs) {
		DBUtil db = new DBUtil();
		siteAliases.add(db.getStringVal("site_alias_txt",rs));
		db = null;
	}
	
	/**
	 * Sets site data
	 * @param rs
	 */
	public void setSiteData(ResultSet rs) {
		DBUtil db = new DBUtil();
		practiceId = db.getIntegerVal("practice_id", rs);
		sbSiteId = sbSiteId + practiceId;
		sbOrgId = sbOrgId + practiceId;
		siteStatus = db.getStringVal("status", rs);
		//templateName = db.getStringVal("template", rs);
		homeWelcome = db.getStringVal("home_welcome", rs).replaceAll("\\n","<br/>");
		overviewWelcome =  db.getStringVal("overview_welcome", rs).replaceAll("\\n","<br/>");
		map = Convert.formatBoolean(db.getStringVal("map", rs));
		direction = StringUtil.checkVal(db.getStringVal("direction", rs)).replaceAll("\\n","<br/>");
		direction2 = StringUtil.checkVal(db.getStringVal("direction2", rs)).replaceAll("\\n","<br/>");
		direction3 = StringUtil.checkVal(db.getStringVal("direction3", rs)).replaceAll("\\n","<br/>");
		contactFormEmail = StringUtil.checkVal(db.getStringVal("contactform", rs)).replaceAll("\\n","<br/>");
		hours = StringUtil.checkVal(db.getStringVal("hours", rs)).replaceAll("\\n","<br/>");
		insurance = StringUtil.checkVal(db.getStringVal("insurance", rs)).replaceAll("\\n","<br/>");
		payment = StringUtil.checkVal(db.getStringVal("payment", rs)).replaceAll("\\n","<br/>");
		app = StringUtil.checkVal(db.getStringVal("app", rs)).replaceAll("\\n","<br/>");
		emergencies = StringUtil.checkVal(db.getStringVal("emergencies", rs));
		addInfo = StringUtil.checkVal(db.getStringVal("addinfo", rs)).replaceAll("\\n","<br/>");
		pdf = db.getStringVal("pdf",rs);
		pdfFile = db.getStringVal("pdf_file", rs);
		companyName = StringUtil.checkVal(db.getStringVal("company", rs));
		/*
		if (companyName.length() > 40) {
			sbSiteName = companyName.substring(0,39);
		} else {
			sbSiteName = companyName;
		}
		*/
		if(companyName.length() > 0) {
			sbSiteName = companyName;
		} else {
			sbSiteName = sbSiteId;
		}
		address1 = StringUtil.checkVal(db.getStringVal("address1", rs)); 
		address2 = StringUtil.checkVal(db.getStringVal("address2", rs));
		city = StringUtil.checkVal(db.getStringVal("city", rs));
		state = StringUtil.checkVal(db.getStringVal("state", rs));
		zip = StringUtil.checkVal(db.getStringVal("zip", rs));
		phone = StringUtil.checkVal(db.getStringVal("phone", rs));
		fax = StringUtil.checkVal(db.getStringVal("fax", rs));
		email = StringUtil.checkVal(db.getStringVal("email", rs));
		db = null;
	}
	
	/**
	 * Add a PSP physician profile VO to the List
	 * @param ppv
	 */
	public void addProfile(PspPhysVO ppv) {
		if (ppv != null) {
			profiles.add(ppv);
		}
	}
	
	/**
	 * @return the mainLinks
	 */
	public Map<String, String> getMainLinks() {
		return mainLinks;
	}

	/**
	 * @param mainLinks the mainLinks to set
	 */
	public void setMainLinks(Map<String, String> mainLinks) {
		this.mainLinks = mainLinks;
	}

	/**
	 * @return the practiceId
	 */
	public int getPracticeId() {
		return practiceId;
	}

	/**
	 * @param practiceId the practiceId to set
	 */
	public void setPracticeId(int practiceId) {
		this.practiceId = practiceId;
	}
	
	/**
	 * @return the homeWelcome
	 */
	public String getHomeWelcome() {
		return homeWelcome;
	}

	/**
	 * @param homeWelcome the homeWelcome to set
	 */
	public void setHomeWelcome(String homeWelcome) {
		this.homeWelcome = homeWelcome;
	}

	/**
	 * @return the overviewWelcome
	 */
	public String getOverviewWelcome() {
		return overviewWelcome;
	}

	/**
	 * @param overviewWelcome the overviewWelcome to set
	 */
	public void setOverviewWelcome(String overviewWelcome) {
		this.overviewWelcome = overviewWelcome;
	}

	/**
	 * @return the map
	 */
	public boolean isMap() {
		return map;
	}

	/**
	 * @param map the map to set
	 */
	public void setMap(boolean map) {
		this.map = map;
	}

	/**
	 * @return the direction
	 */
	public String getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction to set
	 */
	public void setDirection(String direction) {
		this.direction = direction;
	}

	/**
	 * @return the direction2
	 */
	public String getDirection2() {
		return direction2;
	}

	/**
	 * @param direction2 the direction2 to set
	 */
	public void setDirection2(String direction2) {
		this.direction2 = direction2;
	}

	/**
	 * @return the direction3
	 */
	public String getDirection3() {
		return direction3;
	}

	/**
	 * @param direction3 the direction3 to set
	 */
	public void setDirection3(String direction3) {
		this.direction3 = direction3;
	}

	/**
	 * @return the contactFormEmail
	 */
	public String getContactFormEmail() {
		return contactFormEmail;
	}

	/**
	 * @param contactFormEmail the contactFormEmail to set
	 */
	public void setContactFormEmail(String contactFormEmail) {
		this.contactFormEmail = contactFormEmail;
	}

	/**
	 * @return the hours
	 */
	public String getHours() {
		return hours;
	}

	/**
	 * @param hours the hours to set
	 */
	public void setHours(String hours) {
		this.hours = hours;
	}

	/**
	 * @return the insurance
	 */
	public String getInsurance() {
		return insurance;
	}

	/**
	 * @param insurance the insurance to set
	 */
	public void setInsurance(String insurance) {
		this.insurance = insurance;
	}

	/**
	 * @return the payment
	 */
	public String getPayment() {
		return payment;
	}

	/**
	 * @param payment the payment to set
	 */
	public void setPayment(String payment) {
		this.payment = payment;
	}

	/**
	 * @return the app
	 */
	public String getApp() {
		return app;
	}

	/**
	 * @param app the app to set
	 */
	public void setApp(String app) {
		this.app = app;
	}

	/**
	 * @return the emergencies
	 */
	public String getEmergencies() {
		return emergencies;
	}

	/**
	 * @param emergencies the emergencies to set
	 */
	public void setEmergencies(String emergencies) {
		this.emergencies = emergencies;
	}

	/**
	 * @return the addInfo
	 */
	public String getAddInfo() {
		return addInfo;
	}

	/**
	 * @param addInfo the addInfo to set
	 */
	public void setAddInfo(String addInfo) {
		this.addInfo = addInfo;
	}

	/**
	 * @return the pdf
	 */
	public String getPdf() {
		return pdf;
	}

	/**
	 * @param pdf the pdf to set
	 */
	public void setPdf(String pdf) {
		this.pdf = pdf;
	}

	/**
	 * @return the pdfFile
	 */
	public String getPdfFile() {
		return pdfFile;
	}

	/**
	 * @param pdfFile the pdfFile to set
	 */
	public void setPdfFile(String pdfFile) {
		this.pdfFile = pdfFile;
	}

	/**
	 * @return the name
	 */
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * @param companyName the companyName to set
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * @return the address1
	 */
	public String getAddress1() {
		return address1;
	}

	/**
	 * @param address1 the address1 to set
	 */
	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	/**
	 * @return the address2
	 */
	public String getAddress2() {
		return address2;
	}

	/**
	 * @param address2 the address2 to set
	 */
	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the zip
	 */
	public String getZip() {
		return zip;
	}

	/**
	 * @param zip the zip to set
	 */
	public void setZip(String zip) {
		this.zip = zip;
	}

	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/**
	 * @return the fax
	 */
	public String getFax() {
		return fax;
	}

	/**
	 * @param fax the fax to set
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return the pageImages
	 */
	public Map<String, String> getPageImages() {
		return pageImages;
	}

	/**
	 * @param pageImages the pageImages to set
	 */
	public void setPageImages(Map<String, String> pageImages) {
		this.pageImages = pageImages;
	}

	/**
	 * @return the links
	 */
	public Map<String, String> getLinks() {
		return links;
	}

	/**
	 * @param links the links to set
	 */
	public void setLinks(Map<String, String> links) {
		this.links = links;
	}

	/**
	 * @return the documents
	 */
	public List<String> getDocuments() {
		return documents;
	}

	/**
	 * @param documents the documents to set
	 */
	public void setDocuments(List<String> documents) {
		this.documents = documents;
	}

	/**
	 * @return the contacts
	 */
	public Map<String, String> getContacts() {
		return contacts;
	}

	/**
	 * @param contacts the contacts to set
	 */
	public void setContacts(Map<String, String> contacts) {
		this.contacts = contacts;
	}

	/**
	 * @return the services
	 */
	public Map<String, String> getServices() {
		return services;
	}

	/**
	 * @param services the services to set
	 */
	public void setServices(Map<String, String> services) {
		this.services = services;
	}

	/**
	 * @return the profiles
	 */
	public List<PspPhysVO> getProfiles() {
		return profiles;
	}

	/**
	 * @param profiles the profiles to set
	 */
	public void setProfiles(List<PspPhysVO> profiles) {
		this.profiles = profiles;
	}

	/**
	 * @return the logoIsContent
	 */
	public boolean logoIsContent() {
		return logoIsContent;
	}

	/**
	 * @param logoIsContent the logoIsContent to set
	 */
	public void setLogoIsContent(boolean logoIsContent) {
		this.logoIsContent = logoIsContent;
	}

	/**
	 * @return the menuIsContent
	 */
	public boolean menuIsContent() {
		return menuIsContent;
	}

	/**
	 * @param menuIsContent the menuIsContent to set
	 */
	public void setMenuIsContent(boolean menuIsContent) {
		this.menuIsContent = menuIsContent;
	}

	/**
	 * @return the imagesAreContent
	 */
	public boolean imagesAreContent() {
		return imagesAreContent;
	}

	/**
	 * @param imagesAreContent the imagesAreContent to set
	 */
	public void setImagesAreContent(boolean imagesAreContent) {
		this.imagesAreContent = imagesAreContent;
	}

	/**
	 * @return the template
	 */
	public PspSiteTemplateVO getTemplate() {
		return template;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(PspSiteTemplateVO template) {
		this.template = template;
	}

	/**
	 * @return the sbSiteId
	 */
	public String getSbSiteId() {
		return sbSiteId;
	}

	/**
	 * @param sbSiteId the sbSiteId to set
	 */
	public void setSbSiteId(String sbSiteId) {
		this.sbSiteId = sbSiteId.toUpperCase();
	}

	/**
	 * @return the sbOrgId
	 */
	public String getSbOrgId() {
		return sbOrgId;
	}

	/**
	 * @param sbOrgId the sbOrgId to set
	 */
	public void setSbOrgId(String sbOrgId) {
		this.sbOrgId = sbOrgId.toUpperCase();
	}

	/**
	 * @return the siteStatus
	 */
	public String getSiteStatus() {
		return siteStatus;
	}

	/**
	 * @param siteStatus the siteStatus to set
	 */
	public void setSiteStatus(String siteStatus) {
		this.siteStatus = siteStatus;
	}

	/**
	 * @return the siteName
	 */
	public String getSbSiteName() {
		return sbSiteName;
	}

	/**
	 * @param siteName the siteName to set
	 */
	public void setSbSiteName(String sbSiteName) {
		this.sbSiteName = sbSiteName;
	}

	/**
	 * @return the siteAliases
	 */
	public List<String> getSiteAliases() {
		return siteAliases;
	}

	/**
	 * @param siteAliases the siteAliases to set
	 */
	public void setSiteAliases(List<String> siteAliases) {
		this.siteAliases = siteAliases;
	}
		
}
