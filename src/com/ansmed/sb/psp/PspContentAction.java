package com.ansmed.sb.psp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ansmed.sb.psp.pages.AbstractPspContent;
import com.ansmed.sb.psp.pages.ContentWithImages;
import com.ansmed.sb.psp.pages.ContentWithOutImages;
import com.ansmed.sb.psp.pages.ImageContent;

/****************************************************************************
 * <b>Title</b>: PspContentAction.java<p/>
 * <b>Description</b>: Retrieves content pages for an existing 
 * PSP site so that the content can be migrated to SiteBuilder.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Dec 01, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PspContentAction {
	
	public static final String PSP_SITE_CONTENT = "pspSiteContent";
	public enum ContentType {
		HOME, OVERVIEW, LOCATION, CONTACT, SERVICE, GENERAL, PROFILE,
		LINK, DISCLAIMER, CSS, ADDRESS
	}
	
	public PspContentAction() {
	}
	
	/**
	 * Retrieves the site content pages encapsulated in a PspContentVO
	 * @param psv
	 * @return
	 */
	public Map<String,List<PSPContent>> retrieve(PspSiteVO psv) {
		Map<String,List<PSPContent>> cm;
				
		// retrieve standard site content
		cm = retrieveSitePages(psv);
		
		return cm;
	}
	
	/**
	 * Creates and retrieves site content pages encapsulated in a PspContentVO
	 * @param psv
	 * @return
	 */
	public Map<String,List<PSPContent>> retrieveSitePages(PspSiteVO psv) {
		Map<String,List<PSPContent>> acm = new HashMap<String,List<PSPContent>>();
				
		AbstractPspContent app = null;
		AbstractPspContent appImage = null;
		PSPContent image = null;
		boolean buildImageContent = false;
		PSPContent logo = null;
		boolean includeLogo = false;
		
		//if content column number is same as image column number...
		if (psv.getTemplate().getContColNo() == psv.getTemplate().getImgColNo()) {
			// images are included in content
			System.out.println("Building content w/images");
			app = new ContentWithImages(psv);
		} else {
			// content is text only and we will build image content separately
			System.out.println("Building content w/out images");
			app = new ContentWithOutImages(psv);
			int col = psv.getTemplate().getImgColNo();
			// image column number is between 1 and 3, then build image content.
			if (col >= 0 && col < 4) {
				System.out.println("Building images as separate content");
				appImage = new ImageContent(psv);
				buildImageContent = true;
			}
		}
		
		//set logo content values
		if(psv.getTemplate().getContColNo() == psv.getTemplate().getLogoColNo()) {
			logo = app.createLogoContent();
			includeLogo = true;
		}
		
		// create/retrieve site content pages
		System.out.println("Creating all pages...");
		
		for(ContentType ct : ContentType.values()) {
			PSPContent pc = null;
			List<PSPContent> content = new ArrayList<PSPContent>();
			
			if(ct.name().equalsIgnoreCase("css")) {

				try {
					pc = app.getCSS(app.getCSSPath());
				} catch (IOException ioe) {
					pc = new PSPContent();
					pc.setContentName(ct.name().toLowerCase());
					pc.setContentText("/* PSP styles go here /*");
				}
				//add the page content to the list
				content.add(pc);
				
			} else if(ct.name().equalsIgnoreCase("address")) {
				
				pc = app.createAddress(psv.getTemplate().getAddrTypeFlg());
				//add the page content to the list
				content.add(pc);
				
			} else {
				//get the page content
				if(ct.name().equalsIgnoreCase("profile")) {
					pc = app.createProfileContent(psv.getProfiles(), psv.getTemplate().getProfileTypeFlg());	
				} else {
					pc = app.createContent(ct);
				}
				
				//add the logo content to the list if necessary
				if (includeLogo) {
					content.add(logo);
				}
				
				//add the page content to the list
				content.add(pc);
				
				//add the image content to the list if necessary
				if (buildImageContent) {
					image = appImage.createContent(ct);
					if (image != null)
					content.add(appImage.createContent(ct));
				}
			}
			
			//add the content list to the map
			acm.put(pc.getContentName(), content);
			
		}
		return acm;
	}

}
