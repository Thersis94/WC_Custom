package com.fastsigns.action.franchise.vo;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.common.SiteVO;


/****************************************************************************
 * <b>Title</b>: FranchiseVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 19, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseVO extends DealerLocationVO {
	private static final long serialVersionUID = 2451557118459726954L;
	
	// Franchise specific values
	private String locationDescId = null;
	private String rightImageUrl = null;
	private String franchiseId = null;
	private String centerImage = null;
	private String imageAltText = null;
	private boolean pendingWbChange = false;
	private boolean pendingImgChange = false;
	private String facebookUrl = null;
	private String twitterUrl = null;
	private String linkedinUrl = null;
	private String foursquareUrl = null;
	private String pinterestUrl = null;
	private String googlePlusUrl = null;
	private String countryCode = null;
	private String whiteBoardText = null;
	private String webId = null; //a 3 or 4 digit number; In WC its the same as franchiseId, but in Keystone franchiseId is a GUID.
	private String resellerId = null;
	private String resellerLink = null;
	private String resellerImg = null;
	private int useRaqSaf;
	private int useGlobalMod;
	
	public FranchiseVO() {
		
	}
	
	public FranchiseVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	public FranchiseVO(SMTServletRequest req) {
		super(req);
		franchiseId = this.getDealerLocationId();
		StringUtil.checkVal(((SiteVO)req.getAttribute("siteData")).getLocale());
		String loc = StringUtil.checkVal(((SiteVO)req.getAttribute("siteData")).getLocale());
		countryCode = loc.substring(loc.indexOf("_")+1, loc.length());
	}
	
	public void assignData(ResultSet rs) {
		super.setData(rs);
		DBUtil db = new DBUtil();
		locationDescId = db.getStringVal("location_desc_option_id", rs);
		rightImageUrl = db.getStringVal("image_path_url", rs);
		franchiseId = db.getStringVal("franchise_id", rs);
		centerImage = db.getStringVal("center_image_url", rs);
		imageAltText = db.getStringVal("center_image_alt_txt", rs);
		facebookUrl = db.getStringVal("facebook_url", rs);
		twitterUrl = db.getStringVal("twitter_url", rs);
		linkedinUrl = db.getStringVal("linkedin_url", rs);
		foursquareUrl = db.getStringVal("foursquare_url", rs);
		pinterestUrl = db.getStringVal("pinterest_url", rs);
		googlePlusUrl = db.getStringVal("google_plus_url", rs);
		countryCode = db.getStringVal("country_cd", rs);
		whiteBoardText = db.getStringVal("white_board_text", rs);
		resellerId = db.getStringVal("reseller_button_id", rs);
		resellerLink = db.getStringVal("reseller_button_link", rs);
		resellerImg = db.getStringVal("reseller_button_img", rs);
		setUseRaqSaf(db.getIntVal("USE_RAQSAF", rs));
		setUseGlobalMod((db.getIntVal("USE_GLOBAL_MODULES_FLG", rs)));
		
		// Parse the [location] tag out of the description
		StringBuilder desc = new StringBuilder(StringUtil.checkVal(getLocationDesc()));
		int loc = desc.indexOf("[location]");
		if (loc > -1) desc.replace(loc, loc + 10, getLocationName());
		setLocationDesc(desc.toString());
	}

	/**
	 * @return the locationDesc
	 */
	public String getLocationDescId() {
		return locationDescId;
	}


	/**
	 * @param locationDesc the locationDesc to set
	 */
	public void setLocationDescId(String locationDescId) {
		this.locationDescId = locationDescId;
	}


	/**
	 * @return the rightImageUrl
	 */
	public String getRightImageUrl() {
		return rightImageUrl;
	}


	/**
	 * @param rightImageUrl the rightImageUrl to set
	 */
	public void setRightImageUrl(String rightImageUrl) {
		this.rightImageUrl = rightImageUrl;
	}


	/**
	 * @return the franchiseId
	 */
	public String getFranchiseId() {
		return franchiseId;
	}


	/**
	 * @param franchiseId the franchiseId to set
	 */
	public void setFranchiseId(String franchiseId) {
		this.franchiseId = franchiseId;
	}


	/**
	 * @return the centerImage
	 */
	public String getCenterImage() {
		return centerImage;
	}


	/**
	 * @param centerImage the centerImage to set
	 */
	public void setCenterImage(String centerImage) {
		this.centerImage = centerImage;
	}

	public String getImageAltText() {
		return imageAltText;
	}

	public void setImageAltText(String imageAltText) {
		this.imageAltText = imageAltText;
	}

	public void setPendingImgChange(boolean pendingImgChange) {
		this.pendingImgChange = pendingImgChange;
	}

	public boolean isPendingImgChange() {
		return pendingImgChange;
	}
	
	public void setPendingWbChange(boolean pendingWbChange) {
		this.pendingWbChange = pendingWbChange;
	}

	public boolean isPendingWbChange() {
		return pendingWbChange;
	}
	
	/**
	 * returns a list of tags/data that Freemarker will use to populate 
	 * dynamic text on the Center's page/modules.
	 * @return
	 */
	public Map<String, Object> getFreemarkerTags() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("franchiseWebId", this.getDealerLocationId());
		data.put("address", StringUtil.checkVal(this.getAddress()));
		data.put("address2", StringUtil.checkVal(this.getAddress2()));
		data.put("city", StringUtil.checkVal(this.getCity()));
		data.put("state", StringUtil.checkVal(this.getState()));
		data.put("zip", StringUtil.checkVal(this.getZip()));
		data.put("phone", StringUtil.checkVal(new PhoneNumberFormat(this.getPhone(), PhoneNumberFormat.PAREN_FORMATTING).getFormattedNumber()));
		data.put("fax", StringUtil.checkVal(new PhoneNumberFormat(this.getFax(), PhoneNumberFormat.PAREN_FORMATTING).getFormattedNumber()));
		data.put("email", StringUtil.checkVal(this.getEmailAddress()));
		data.put("websiteUrl", "http://" + StringUtil.checkVal(this.getWebsite()));
		data.put("designatorName", StringUtil.checkVal(this.getLocationName()));
		
		return data;
	}

	public void setFacebookUrl(String facebookUrl) {
		this.facebookUrl = facebookUrl;
	}

	public String getFacebookUrl() {
		return facebookUrl;
	}

	public void setTwitterUrl(String twitterUrl) {
		this.twitterUrl = twitterUrl;
	}

	public String getTwitterUrl() {
		return twitterUrl;
	}

	public void setLinkedinUrl(String linkedinUrl) {
		this.linkedinUrl = linkedinUrl;
	}

	public String getLinkedinUrl() {
		return linkedinUrl;
	}

	public void setFoursquareUrl(String foursquareUrl) {
		this.foursquareUrl = foursquareUrl;
	}

	public String getFoursquareUrl() {
		return foursquareUrl;
	}

	public String getPinterestUrl() {
		return pinterestUrl;
	}

	public void setPinterestUrl(String pinterestUrl) {
		this.pinterestUrl = pinterestUrl;
	}

	public String getGooglePlusUrl() {
		return googlePlusUrl;
	}

	public void setGooglePlusUrl(String googlePlusUrl) {
		this.googlePlusUrl = googlePlusUrl;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getCountryCode() {
		return countryCode;
	}
	
	public String getWhiteBoardText(){
		return whiteBoardText;
	}
	
	public void setWhiteBoardText(String whiteBoardText){
		this.whiteBoardText = whiteBoardText;
	}

	public String getWebId() {
		return webId;
	}

	public void setWebId(String webId) {
		this.webId = webId;
	}

	public String getResellerId() {
		return resellerId;
	}

	public void setResellerId(String resellerId) {
		this.resellerId = resellerId;
	}

	public String getResellerLink() {
		return resellerLink;
	}

	public void setResellerLink(String resellerLink) {
		this.resellerLink = resellerLink;
	}

	public String getResellerImg() {
		return resellerImg;
	}

	public void setResellerImg(String resellerImg) {
		this.resellerImg = resellerImg;
	}

	public int getUseRaqSaf() {
		return useRaqSaf;
	}

	public void setUseRaqSaf(int useRaqSaf) {
		this.useRaqSaf = useRaqSaf;
	}

	public int getUseGlobalMod() {
		return useGlobalMod;
	}

	public void setUseGlobalMod(int useGlobalMod) {
		this.useGlobalMod = useGlobalMod;
	}

}
