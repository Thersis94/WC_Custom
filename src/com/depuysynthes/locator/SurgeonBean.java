package com.depuysynthes.locator;

// Java 7
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Google Gson lib
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/****************************************************************************
 * <b>Title: </b>SurgeonBean.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2016<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Feb 10, 2016<p/>
 *<b>Changes: </b>
 * Feb 10, 2016: David Bargerhuff: Created class.
 ****************************************************************************/
public class SurgeonBean implements Serializable {
	
	private static final long serialVersionUID = -6729600355066995890L;
    private int statusId;
    private int surgeonId;
    private String userName;
    private String lastName;
    private String firstName;
    private String suffixName;
    private String aamdUrl;
    private String customUrl;
    private String redirectUrl;
    private String redirectUrl2;
    private String emailAddress;
    private String degreeDesc;
    private List<Integer> specialties;
    private List<Integer> procedures;
    private List<Integer> products;
    private List<LocationBean> locations;
    private int locationSize;
    private List<String> affiliations;
    private double primaryDistance;
    private String uniqueId;
    
    public SurgeonBean(JsonElement jsonElement) {
    	specialties = new ArrayList<>();
    	procedures = new ArrayList<>();
    	products = new ArrayList<>();
    	locations = new ArrayList<>();
    	affiliations = new ArrayList<>();
    	this.parseData(jsonElement);
    }
    
    /**
     * Parses the surgeon JSON data.
     * @param rs
     */
    private void parseData(JsonElement jsonElement) {
    	if (jsonElement != null) {
    		JsonObject jS = jsonElement.getAsJsonObject();
    		if (jS.has("statusId")) statusId = jS.get("statusId").getAsInt();
    		if (jS.has("surgeonId")) surgeonId = jS.get("surgeonId").getAsInt();
    		if (jS.has("userName")) userName = jS.get("userName").getAsString();
    		if (jS.has("lastName")) lastName = jS.get("lastName").getAsString();
    		if (jS.has("firstName")) firstName = jS.get("firstName").getAsString();
    		if (jS.has("suffixName")) suffixName = jS.get("suffixName").getAsString();
    		if (jS.has("aamdUrl")) aamdUrl = jS.get("aamdUrl").getAsString();
    		if (jS.has("customUrl")) customUrl = jS.get("customUrl").getAsString();
    		if (jS.has("redirectUrl")) redirectUrl = jS.get("redirectUrl").getAsString();
    		if (jS.has("redirectUrl2")) redirectUrl2 = jS.get("redirectUrl2").getAsString();
    		if (jS.has("emailAddress")) emailAddress = jS.get("emailAddress").getAsString();
    		if (jS.has("degreeDesc")) degreeDesc = jS.get("degreeDesc").getAsString();
    		parseLocations(jS.getAsJsonArray("locations"));
    		specialties = parseIntegerList(jS.getAsJsonArray("specialties"));
    		procedures = parseIntegerList(jS.getAsJsonArray("procedures"));
    		products = parseIntegerList(jS.getAsJsonArray("products"));
    		parseAffiliations(jS.getAsJsonArray("affiliations")); 
    		
    	}
    }
    
    /**
     * Parses the JSON array and returns it as a List of Integers.
     * @param jsonArray
     * @return
     */
    private List<Integer> parseIntegerList(JsonArray jsonArray) {
    	List<Integer> iList = null;
    	if (jsonArray != null) {
    		iList = new ArrayList<>();
    		Iterator<JsonElement> jIter = jsonArray.iterator();
    		while (jIter.hasNext()) {
    			iList.add(jIter.next().getAsInt());
    		}
    	}
    	if (iList == null) iList = new ArrayList<>();
    	return iList;
    }
    
    /**
     * Parses the affiliations JSON
     * @param jsonArray
     */
    private void parseAffiliations(JsonArray jsonArray) {
    	if (jsonArray != null) {
    		Iterator<JsonElement> jIter = jsonArray.iterator();
    		while (jIter.hasNext()) {
    			affiliations.add(jIter.next().getAsString());
    		}
    	}
    }
    
    /**
     * Parses the surgeon's locations.
     * @param jLocations
     */
    private void parseLocations(JsonArray jLocations) {
    	if (jLocations != null) {
    		Iterator<JsonElement> locIter = jLocations.iterator();
    		while (locIter.hasNext()) {
    			locations.add(new LocationBean(locIter.next()));
    		}
    		locationSize = locations.size();
    	}
    }
    
	/**
	 * @return the statusId
	 */
	public int getStatusId() {
		return statusId;
	}

	/**
	 * @param statusId the statusId to set
	 */
	public void setStatusId(int statusId) {
		this.statusId = statusId;
	}

	/**
	 * @return the surgeonId
	 */
	public int getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(int surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}
	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}
	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	/**
	 * @return the suffixName
	 */
	public String getSuffixName() {
		return suffixName;
	}
	/**
	 * @param suffixName the suffixName to set
	 */
	public void setSuffixName(String suffixName) {
		this.suffixName = suffixName;
	}

	/**
	 * @return the aamdUrl
	 */
	public String getUrl() {
		return aamdUrl;
	}
	/**
	 * @param aamdUrl the aamdUrl to set
	 */
	public void setUrl(String aamdUrl) {
		this.aamdUrl = aamdUrl;
	}
	/**
	 * @return the customUrl
	 */
	public String getCustomUrl() {
		return customUrl;
	}
	/**
	 * @param customUrl the customUrl to set
	 */
	public void setCustomUrl(String customUrl) {
		this.customUrl = customUrl;
	}
	/**
	 * @return the redirectUrl
	 */
	public String getRedirectUrl() {
		return redirectUrl;
	}
	/**
	 * @param redirectUrl the redirectUrl to set
	 */
	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}
	/**
	 * @return the redirectUrl2
	 */
	public String getRedirectUrl2() {
		return redirectUrl2;
	}
	/**
	 * @param redirectUrl2 the redirectUrl2 to set
	 */
	public void setRedirectUrl2(String redirectUrl2) {
		this.redirectUrl2 = redirectUrl2;
	}
	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}
	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	/**
	 * @return the degreeDesc
	 */
	public String getDegreeDesc() {
		return degreeDesc;
	}
	/**
	 * @param degreeDesc the degreeDesc to set
	 */
	public void setDegreeDesc(String degreeDesc) {
		this.degreeDesc = degreeDesc;
	}
	/**
	 * @return the specialties
	 */
	public List<Integer> getSpecialties() {
		return specialties;
	}
	/**
	 * @param specialties the specialties to set
	 */
	public void setSpecialties(List<Integer> specialties) {
		this.specialties = specialties;
	}
	
	public void addSpecialty(Integer spec) {
		this.specialties.add(spec);
	}
	
	/**
	 * @return the procedures
	 */
	public List<Integer> getProcedures() {
		return procedures;
	}
	/**
	 * @param procedures the procedures to set
	 */
	public void setProcedures(List<Integer> procedures) {
		this.procedures = procedures;
	}
	
	public void addProcedure(Integer proc) {
		this.procedures.add(proc);
	}
	
	/**
	 * @return the products
	 */
	public List<Integer> getProducts() {
		return products;
	}
	/**
	 * @param products the products to set
	 */
	public void setProducts(List<Integer> products) {
		this.products = products;
	}
	
	public void addProduct(Integer prod) {
		this.products.add(prod);
	}
	
	/**
	 * @return the locations
	 */
	public List<LocationBean> getLocations() {
		return locations;
	}
	/**
	 * @param locations the locations to set
	 */
	public void setLocations(List<LocationBean> locations) {
		this.locations = locations;
	}
	
	public void addLocation(LocationBean location) {
		if (locations == null) locations = new ArrayList<>();
		this.locations.add(location);
	}

	/**
	 * @return the affiliations
	 */
	public List<String> getAffiliations() {
		return affiliations;
	}

	/**
	 * @param affiliations the affiliations to set
	 */
	public void setAffiliations(List<String> affiliations) {
		this.affiliations = affiliations;
	}
	
	/**
	 * Sets affiliation values using either a String or a comma-delimited String
	 * @param affiliations
	 */
	public void setAffiliations(String affiliations) {
		if (affiliations == null || affiliations.length() == 0) return;
		if (affiliations.indexOf(",") > -1) {
			String[] sa = affiliations.split(",");
			for (int i = 0; i < sa.length; i++) {
				this.addAffiliation(sa[i]);
			}
		} else {
			this.addAffiliation(affiliations);
		}
	}
	
	public void addAffiliation(String affiliation) {
		if (affiliations == null) affiliations = new ArrayList<>();
		if (affiliation == null || affiliation.length() == 0) return;
		this.affiliations.add(affiliation);
	}

	/**
	 * @return the aamdUrl
	 */
	public String getAamdUrl() {
		return aamdUrl;
	}

	/**
	 * @param aamdUrl the aamdUrl to set
	 */
	public void setAamdUrl(String aamdUrl) {
		this.aamdUrl = aamdUrl;
	}

	/**
	 * @return the primaryDistance
	 */
	public double getPrimaryDistance() {
		if (primaryDistance == 0.0) {
			if (locations != null && locations.size() > 0) {
				primaryDistance = locations.get(0).getDistance();
			}
		}
		return primaryDistance;
	}

	/**
	 * @param primaryDistance the primaryDistance to set
	 */
	public void setPrimaryDistance(double primaryDistance) {
		this.primaryDistance = primaryDistance;
	}

	/**
	 * @return the uniqueId
	 */
	public String getUniqueId() {
		if (locations != null && locations.size() > 0) {
			uniqueId = locations.get(0).getUniqueId();
			return uniqueId;
		} else {
			return "0";
		}
	}

	/**
	 * @return the locationSize
	 */
	public int getLocationSize() {
		return locationSize;
	}

}
