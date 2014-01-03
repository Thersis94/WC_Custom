package com.sjm.corp.mobile.collection;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: MarketingUsingVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jun 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MarketingUsingVO extends SBModuleVO {
	private static final long serialVersionUID = 1L;
	private boolean usingAppointmentCards;
	private boolean usingFolders;
	private boolean usingNewspaperAds;
	private boolean usingBrochures;
	private boolean usingLetterhead;
	private boolean usingRolodex;
	private boolean usingBusinessCards;
	private boolean usingLogos;
	private boolean usingSocialMedia;
	private boolean usingDirectMail;
	private boolean usingMagazineAds;
	private boolean usingWebsites;
	private boolean usingFaxReferrals;
	private boolean usingNewsletters;
	
	public MarketingUsingVO(){
		super();
	}

	public boolean isUsingAppointmentCards() {
		return usingAppointmentCards;
	}

	public void setUsingAppointmentCards(boolean usingAppointmentCards) {
		this.usingAppointmentCards = usingAppointmentCards;
	}

	public boolean isUsingFolders() {
		return usingFolders;
	}

	public void setUsingFolders(boolean usingFolders) {
		this.usingFolders = usingFolders;
	}

	public boolean isUsingNewspaperAds() {
		return usingNewspaperAds;
	}

	public void setUsingNewspaperAds(boolean usingNewspaperAds) {
		this.usingNewspaperAds = usingNewspaperAds;
	}

	public boolean isUsingBrochures() {
		return usingBrochures;
	}

	public void setUsingBrochures(boolean usingBrochures) {
		this.usingBrochures = usingBrochures;
	}

	public boolean isUsingLetterhead() {
		return usingLetterhead;
	}

	public void setUsingLetterhead(boolean usingLetterhead) {
		this.usingLetterhead = usingLetterhead;
	}

	public boolean isUsingRolodex() {
		return usingRolodex;
	}

	public void setUsingRolodex(boolean usingRolodex) {
		this.usingRolodex = usingRolodex;
	}

	public boolean isUsingBusinessCards() {
		return usingBusinessCards;
	}

	public void setUsingBusinessCards(boolean usingBusinessCards) {
		this.usingBusinessCards = usingBusinessCards;
	}

	public boolean isUsingLogos() {
		return usingLogos;
	}

	public void setUsingLogos(boolean usingLogos) {
		this.usingLogos = usingLogos;
	}

	public boolean isUsingSocialMedia() {
		return usingSocialMedia;
	}

	public void setUsingSocialMedia(boolean usingSocialMedia) {
		this.usingSocialMedia = usingSocialMedia;
	}

	public boolean isUsingDirectMail() {
		return usingDirectMail;
	}

	public void setUsingDirectMail(boolean usingDirectMail) {
		this.usingDirectMail = usingDirectMail;
	}

	public boolean isUsingWebsites() {
		return usingWebsites;
	}

	public void setUsingWebsites(boolean usingWebsites) {
		this.usingWebsites = usingWebsites;
	}

	public boolean isUsingMagazineAds() {
		return usingMagazineAds;
	}

	public void setUsingMagazineAds(boolean usingMagazineAds) {
		this.usingMagazineAds = usingMagazineAds;
	}

	public boolean isUsingFaxReferrals() {
		return usingFaxReferrals;
	}

	public void setUsingFaxReferrals(boolean usingFaxReferrals) {
		this.usingFaxReferrals = usingFaxReferrals;
	}

	public boolean isUsingNewsletters() {
		return usingNewsletters;
	}

	public void setUsingNewsletters(boolean usingNewsletters) {
		this.usingNewsletters = usingNewsletters;
	}
}
