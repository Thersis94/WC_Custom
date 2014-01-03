package com.ansmed.sb.psp.pages;

import com.ansmed.sb.psp.PSPContent;
import com.ansmed.sb.psp.PspSiteVO;
import com.ansmed.sb.psp.PspContentAction.ContentType;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ImageContent.java<p/>
 * <b>Description</b>: Creates content consisting solely of images for PSP
 * sites that have text content and page images in separate columns. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Dec 03, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class ImageContent extends AbstractPspContent {
	
	public final String HOME_IMAGE_START = "<div id=\"pspHomeImages\">";
	public final String OVIEW_IMAGE_START = "<div id=\"pspOverviewImages\">";
	public final String SERVICES_IMAGE_START = "<div id=\"pspServicesImages\">";
	public final String LINKS_IMAGE_START = "<div id=\"pspLinksImages\">";
		
	public ImageContent() {
		super();
	}
	
    public ImageContent(PspSiteVO site) {
    	super();
    	siteData = site;
    	orgImagePath = ORG_PATH + siteData.getSbSiteId() + "/images/";
    	orgCommonImagePath = ORG_PATH + siteData.getSbOrgId() + "/images/common/";
    	orgBinaryDocPath = ORG_PATH + siteData.getSbSiteId() + "/";
    }
    
    public PSPContent createContent(ContentType c) {
    	PSPContent pc = null; 
    	
    	StringBuffer sb;
    	String image1;
    	String image2;
    	switch(c) {
	    	case HOME:
	        	sb = new StringBuffer(HOME_IMAGE_START);
	        	
	        	//add the image
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("home1"));
	        	
	        	if (image1.equalsIgnoreCase("img00")) {
		        	String image1Custom = siteData.getPageImages().get("home1custom");
		        	if (image1Custom.length() > 0) {
		        		sb.append("<img src=\"").append(orgImagePath);
		        		sb.append(image1Custom).append("\" class=\"pspHomeImage\">");
		        	}
	        	} else {
	        		sb.append("<img src=\"").append(orgCommonImagePath);
	        		sb.append(image1).append("\" class=\"pspHomeImage\">");
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("home_image");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getImgColNo());
	    		break;
	    		
	    	case OVERVIEW:
	        	sb = new StringBuffer(OVIEW_IMAGE_START);
	        	
	        	//add page images
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("overview1"));
	        	image2 = StringUtil.checkVal(siteData.getPageImages().get("overview2"));
	        	    	
	        	if (image1.length() > 0) {
	        		sb.append("<p>");
	         		if (image1.equalsIgnoreCase("img00")) {
	         			String o1 = StringUtil.checkVal(siteData.getPageImages().get("overview1custom"));
	         			if(o1.length() > 0) {
	         				sb.append("<img src=\"").append(orgImagePath);
	         				sb.append(o1).append("\">");
	         			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image1).append("\">");
	        		}
	         		sb.append("</p>");
	        	}
	        	
	        	if (image2.length() > 0) {
	        		sb.append("<p>");
	         		if (image2.equalsIgnoreCase("img00")) {
	         			String o2 = StringUtil.checkVal(siteData.getPageImages().get("overview2custom"));
	        			sb.append("<img src=\"").append(orgImagePath);
	        			sb.append(o2).append("\">");
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image2).append("\">");
	        		}
	         		sb.append("</p>");
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("overview_image");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getImgColNo());
	    		break;
	    		
	    	case SERVICE:
	        	sb = new StringBuffer(SERVICES_IMAGE_START);
	        	//add the images
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("service1"));
	        	image2 = StringUtil.checkVal(siteData.getPageImages().get("service2"));
	        	
	        	if (image1.length() > 0) {
	        		sb.append("<p>");
	         		if (image1.equalsIgnoreCase("img00")) {
	         			String s1 = StringUtil.checkVal(siteData.getPageImages().get("service1custom"));
	         			if(s1.length() > 0) {
	         				sb.append("<img src=\"").append(orgImagePath);
	         				sb.append(s1).append("\">");
	         			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image1).append("\">");
	        		}
	         		sb.append("</p>");
	        	}
	        	
	        	if (image2.length() > 0) {
	        		sb.append("<p>");
	         		if (image2.equalsIgnoreCase("img00")) {
	         			String s2 = StringUtil.checkVal(siteData.getPageImages().get("service2custom"));
	         			if(s2.length() > 0) {
	         				sb.append("<img src=\"").append(orgImagePath);
	         				sb.append(s2).append("\">");
	         			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image2).append("\">");
	        		}
	         		sb.append("</p>");
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("service_image");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getImgColNo());
	    		break;
	    		
	    	case LINK:
	    		sb = new StringBuffer(LINKS_CONTENT_START);

	        	//Add images
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("link1"));
	        	
	        	if (image1.length() > 0) {
	        		if (image1.equalsIgnoreCase("img00")) {
	        			String imageCustom = StringUtil.checkVal(siteData.getPageImages().get("link1custom"));
	        			if(imageCustom.length() > 0) {
	        				sb.append("<img src=\"").append(orgImagePath).append(imageCustom);
	        				sb.append("\">");
	        			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath).append(image1);
	        			sb.append("\">");
	        		}
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("link_image");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getImgColNo());

	    		break;
	    	default:
	    		break;
    	}
    	return pc;
    }
    
    /**
     * Unused
     */
    public PSPContent createLocation() {return null;}
    
    /**
     * Unused
     */
    public PSPContent createContactUs() {return null;}
    
    /**
     * Unused
     */
    public PSPContent createGeneralInfo() {return null;}
    
    /**
     * Unused
     */
    public PSPContent createPhysicianProfile() {return null;}
    
    /**
     * Unused
     */
    public PSPContent createDisclaimer() {return null;}
    
    /**
     * Unused
     */
	public PSPContent createCSS() {return null;}
	
	/**
	 * Unused
	 */
	public String getCSSPath() {return null;}
        
}
