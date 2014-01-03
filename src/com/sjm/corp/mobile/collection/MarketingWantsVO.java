package com.sjm.corp.mobile.collection;

import java.util.HashMap;
import java.util.Map;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: MarketingWantsVO.java<p/>
 * <b>Description: Object for all of the Marketing Wants for SJM's Mobile Data Collection App</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 26, 2012
 ****************************************************************************/


public class MarketingWantsVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private boolean appointmentCards;
	private boolean faxReferrals;
	private boolean newsletters;
	private boolean brochures;
	private boolean folders;
	private boolean newspaperAds;
	private boolean businessCards;
	private boolean letterhead;
	private boolean rolodex;
	private boolean postcards;
	private boolean logo;
	private boolean socialMedia;
	private boolean envelopes;
	private boolean magazineAds;
	private boolean website;
	private Map<String, String> names;
	
	public MarketingWantsVO(){
		super();
		names = new HashMap<String, String>();
		names.put("appointmentCards", "Appointment Cards");
		names.put("faxReferrals", "Fax referrals");
		names.put("newsletters", "Newsletters");
		names.put("brochures", "Brochures");
		names.put("folders", "Folders");
		names.put("newspaperAds", "Newspaper ads");
		names.put("businessCards", "Business Cards");
		names.put("letterhead", "Letterhead");
		names.put("rolodex", "Rolodex");
		names.put("postcards", "Postcards");
		names.put("logo","Logo");
		names.put("socialMedia", "Social Media");
		names.put("envelopes", "Envelopes");
		names.put("magazineAds", "Magazine ads");
		names.put("website", "Website");
	}
	
	public boolean isWantAppointmentCards() {
		return appointmentCards;
	}

	public void setWantAppointmentCards(boolean wantAppointmentCards) {
		this.appointmentCards = wantAppointmentCards;
	}

	public boolean isWantFaxReferrals() {
		return faxReferrals;
	}

	public void setWantFaxReferrals(boolean wantFaxReferrals) {
		this.faxReferrals = wantFaxReferrals;
	}

	public boolean isWantNewsletters() {
		return newsletters;
	}

	public void setWantNewsletters(boolean wantNewsletters) {
		this.newsletters = wantNewsletters;
	}

	public boolean isWantBrochures() {
		return brochures;
	}

	public void setWantBrochures(boolean wantBrochures) {
		this.brochures = wantBrochures;
	}

	public boolean isWantNewspaperAds() {
		return newspaperAds;
	}

	public void setWantNewspaperAds(boolean wantNewspaperAds) {
		this.newspaperAds = wantNewspaperAds;
	}

	public boolean isWantFolders() {
		return folders;
	}

	public void setWantFolders(boolean wantFolders) {
		this.folders = wantFolders;
	}

	public boolean isWantBusinessCards() {
		return businessCards;
	}

	public void setWantBusinessCards(boolean wantBusinessCards) {
		this.businessCards = wantBusinessCards;
	}

	public boolean isWantLetterhead() {
		return letterhead;
	}

	public void setWantLetterhead(boolean wantLetterhead) {
		this.letterhead = wantLetterhead;
	}

	public boolean isWantRolodex() {
		return rolodex;
	}

	public void setWantRolodex(boolean wantRolodex) {
		this.rolodex = wantRolodex;
	}

	public boolean isWantPostcards() {
		return postcards;
	}

	public void setWantPostcards(boolean wantPostcards) {
		this.postcards = wantPostcards;
	}

	public boolean isWantLogo() {
		return logo;
	}

	public void setWantLogo(boolean wantLogo) {
		this.logo = wantLogo;
	}

	public boolean isWantSocialMedia() {
		return socialMedia;
	}

	public void setWantSocialMedia(boolean wantSocialMedia) {
		this.socialMedia = wantSocialMedia;
	}

	public boolean isWantEnvelopes() {
		return envelopes;
	}

	public void setWantEnvelopes(boolean wantEnvelopes) {
		this.envelopes = wantEnvelopes;
	}

	public boolean isWantMagazineAds() {
		return magazineAds;
	}

	public void setWantMagazineAds(boolean wantMagazineAds) {
		this.magazineAds = wantMagazineAds;
	}

	public boolean isWantWebsite() {
		return website;
	}

	public void setWantWebsite(boolean wantWebsite) {
		this.website = wantWebsite;
	}

	public Map<String, String> getNames() {
		return names;
	}

	public void setNames(Map<String, String> names) {
		this.names = names;
	}
}