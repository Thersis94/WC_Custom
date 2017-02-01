package com.depuy.sitebuilder.locator;

import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>:LocatorSubmittalVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 6, 2008
 * <b>Changes: </b>
 ****************************************************************************/
public class LocatorSubmittalVO extends SBModuleVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String locatorSubmit = null;;
	String language = null;
	String country = null;
	String resultsPerPage = null;
	String specialty = null;
	String product = null;
	String address = null;
	String city = null;
	String state = null;
	String zip = null;
	String radius = null;
	String consent = null;
	String locatorType = null;
	String baseUrl = null;

	/**
	 * 
	 */
	public LocatorSubmittalVO() {
		super();
	}
	
	/**
	 * 
	 */
	public LocatorSubmittalVO(ActionRequest req) {
		super();
		this.setData(req);
	}
	
	/**
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		language = req.getParameter("language");
		country = req.getParameter("country");
		resultsPerPage = req.getParameter("number_of_results_per_page");
		specialty = req.getParameter("specialty");
		product = req.getParameter("product");
		address = req.getParameter("address");
		city = req.getParameter("city");
		state = req.getParameter("state");
		zip = req.getParameter("zip");
		radius = req.getParameter("radius");
		consent = req.getParameter("consent");
		locatorType = req.getParameter("locatorType");
		locatorSubmit = req.getParameter("locatorSubmit");
		baseUrl = req.getRequestURI();
	}
	
	/**
	 * builds the redirection URL to the locator
	 * @return
	 */
	public String getRedirectUrl() {
    	StringBuffer url = new StringBuffer();
    	url.append(baseUrl);
    	url.append("?language=").append(language);
    	url.append("&country=").append(country);
    	url.append("&number_of_results_per_page=").append(resultsPerPage);
    	url.append("&locatorSubmit=").append(locatorSubmit);
    	url.append("&specialty=").append(specialty);
    	url.append("&product=").append(product);
    	url.append("&address=").append(address);
    	url.append("&city=").append(city);
    	url.append("&state=").append(state);
    	url.append("&zip=").append(zip);
    	url.append("&radius=").append(radius);
    	url.append("&consent=").append(consent);
    	url.append("&displayResults=true");
    	url.append("&locatorType=").append(locatorType);
    	url.append("&page=1");
    	
		return url.toString();
	}

	/**
	 * @return the locatorSubmit
	 */
	public String getLocatorSubmit() {
		return locatorSubmit;
	}

	/**
	 * @param locatorSubmit the locatorSubmit to set
	 */
	public void setLocatorSubmit(String locatorSubmit) {
		this.locatorSubmit = locatorSubmit;
	}

	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the resultsPerPage
	 */
	public String getResultsPerPage() {
		return resultsPerPage;
	}

	/**
	 * @param resultsPerPage the resultsPerPage to set
	 */
	public void setResultsPerPage(String resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
	}

	/**
	 * @return the specialty
	 */
	public String getSpecialty() {
		return specialty;
	}

	/**
	 * @param specialty the specialty to set
	 */
	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}

	/**
	 * @return the product
	 */
	public String getProduct() {
		return product;
	}

	/**
	 * @param product the product to set
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
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
	 * @return the radius
	 */
	public String getRadius() {
		return radius;
	}

	/**
	 * @param radius the radius to set
	 */
	public void setRadius(String radius) {
		this.radius = radius;
	}

	/**
	 * @return the consent
	 */
	public String getConsent() {
		return consent;
	}

	/**
	 * @param consent the consent to set
	 */
	public void setConsent(String consent) {
		this.consent = consent;
	}

	/**
	 * @return the locatorType
	 */
	public String getLocatorType() {
		return locatorType;
	}

	/**
	 * @param locatorType the locatorType to set
	 */
	public void setLocatorType(String locatorType) {
		this.locatorType = locatorType;
	}

}
