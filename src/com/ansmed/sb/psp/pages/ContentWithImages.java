package com.ansmed.sb.psp.pages;

import java.util.List;
import java.util.Map;

import com.ansmed.sb.psp.PSPContent;
//import com.ansmed.sb.psp.PspPhysVO;
import com.ansmed.sb.psp.PspSiteVO;
import com.ansmed.sb.psp.PspContentAction.ContentType;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ContentWithImages.java<p/>
 * <b>Description</b>: Content pages with images. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Dec 01, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class ContentWithImages extends AbstractPspContent {
	
	public final String STYLES_TEMPLATE_PATH = "scripts/pspStylesImages.css";
		
	public ContentWithImages() {
		super();
	}
	
    public ContentWithImages(PspSiteVO site) {
    	super();
    	siteData = site;
    	orgImagePath = ORG_PATH + siteData.getSbSiteId() + "/images/";
    	orgCommonImagePath = ORG_PATH + siteData.getSbOrgId() + "/images/common/";
    	orgBinaryDocPath = ORG_PATH + siteData.getSbSiteId() + "/doc/";
    }
    
    @SuppressWarnings("incomplete-switch")
	public PSPContent createContent(ContentType c) {
    	PSPContent pc = null; 
    	
    	boolean useTitles = false;
    	useTitles = Convert.formatBoolean(siteData.getTemplate().getTitlesFlg());
    	
    	StringBuffer sb;
    	String image1;
    	String image2;
    	switch(c) {
	    	case HOME:
	    	   	sb = new StringBuffer(HOME_CONTENT_START);
	    	   	if(useTitles) {
	    	   		sb.append("<h2>Welcome</h2>");
	    	   	}
	        	
	        	//add the image
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("home1"));
	        	
	        	if (image1.equalsIgnoreCase("img00")) {
		        	String image1Custom = StringUtil.checkVal(siteData.getPageImages().get("home1custom"));
	        		if(image1Custom.length() > 0) {
	        			sb.append("<img src=\"").append(orgImagePath);
	        			sb.append(image1Custom).append("\" class=\"pspHomeImage\">");
	        		}
	        	} else {
	        		sb.append("<img src=\"").append(orgCommonImagePath);
	        		sb.append(image1).append("\" class=\"pspHomeImage\">");
	        	}
	        	
	        	sb.append("<p>" + rewriteDocsPath(siteData.getHomeWelcome()) + "</p>");
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("home");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		
	    	case OVERVIEW:
	        	sb = new StringBuffer(OVIEW_CONTENT_START);
	        	if(useTitles) {
	        		sb.append("<h2>Overview</h2>");
	        	}
	        	
	        	//add page images
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("overview1"));
	        	image2 = StringUtil.checkVal(siteData.getPageImages().get("overview2"));
	        	    	
	        	if (image1.length() > 0) {
	         		if (image1.equalsIgnoreCase("img00")) {
	         			String o1 = StringUtil.checkVal(siteData.getPageImages().get("overview1custom"));
	         			if(o1.length() > 0) {
		        			sb.append("<img src=\"").append(orgImagePath);
		        			sb.append(o1);
		        			sb.append("\">");
	         			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image1).append("\">");
	        		}
	        	}
	        	
	        	if (image2.length() > 0) {
	        		sb.append("&nbsp;&nbsp;&nbsp;");
	         		if (image2.equalsIgnoreCase("img00")) {
	         			String o2 = StringUtil.checkVal(siteData.getPageImages().get("overview2custom"));
	         			if(o2.length() > 0) {
	         				sb.append("<img src=\"").append(orgImagePath);
	         				sb.append(o2);
	         				sb.append("\">");
	         			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image2).append("\">");
	        		}
	        	}
	        	sb.append("<p>" + rewriteDocsPath(siteData.getOverviewWelcome()) + "</p>");
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("overview");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		
	    	case LOCATION:
	        	sb = new StringBuffer(LOC_CONTENT_START);
	        	if(useTitles) {
	        		sb.append("<h2>Location</h2>");
	        	}
	        	
	        	//custom map image
	        	String mapImage = siteData.getPageImages().get("mapcustom");
	        	String mapImage2 = siteData.getPageImages().get("mapcustom2");
	        	String mapImage3 = siteData.getPageImages().get("mapcustom3");
	        	
	    		sb.append("<p>");
	        	if (mapImage != null && mapImage.length() > 0) {
	        		sb.append("<img src=\"").append(orgImagePath);
	        		sb.append(mapImage).append("\" class=\"pspLocationMapImage\" alt=\"Our Map\">");
	        	}
	        	
	        	if (mapImage2 != null && mapImage2.length() > 0) {
	        		sb.append("<br/>");
	        		sb.append("<img src=\"").append(orgImagePath);
	        		sb.append(mapImage2).append("\" class=\"pspLocationMapImage\" alt=\"Our Map\">");
	        	}
	        	
	        	if (mapImage3 != null && mapImage3.length() > 0) {
	        		sb.append("<br/>");
	        		sb.append("<img src=\"").append(orgImagePath);
	        		sb.append(mapImage3).append("\" class=\"pspLocationMapImage\" alt=\"Our Map\">");
	        	}
	    		sb.append("</p>");
	        	
	        	//add driving directions
	        	String dir = siteData.getDirection();
	        	if (dir != null && dir.length() > 0) {
	        		sb.append("<p><b>Directions:</b><br/>");
	        		sb.append(dir).append("</p>");
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("location");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		
	    	case CONTACT:
	        	sb = new StringBuffer(CONTACTUS_CONTENT_START);
	        	if(useTitles) {
	        		sb.append("<h2>Contact Us</h2>");
	        	}
	        	
	        	for(String mKey : siteData.getContacts().keySet()) {
	        		sb.append("<p><b>").append(mKey.replaceAll("\\n", "<br/>")).append("</b>");
	        		String mVal = siteData.getContacts().get(mKey);
	        		if(mVal != null && mVal.length() > 0) {
	        			sb.append("<br/><a href=\"mailto:").append(mVal).append("\">");
	        			sb.append(mVal).append("</a>");
	        		}
	        		sb.append("</p>");
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("contact");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		
	    	case SERVICE:
	    	   	sb = new StringBuffer(SERVICES_CONTENT_START);
	        	if(useTitles) {
	        		sb.append("<h2>Services</h2>");
	        	}
	        	
	        	//add the images
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("service1"));
	        	image2 = StringUtil.checkVal(siteData.getPageImages().get("service2"));
	        	
	        	sb.append("<p>");
	        	if (image1.length() > 0) {
	         		if (image1.equalsIgnoreCase("img00")) {
	         			String s1 = siteData.getPageImages().get("service1custom");
	         			if(s1.length() > 0) {
	         				sb.append("<img src=\"").append(orgImagePath);
	         				sb.append(s1).append("\">");
	         			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image1).append("\">");
	        		}
	        	}
	        	
	        	if (image2.length() > 0) {
	        		sb.append("&nbsp;&nbsp;&nbsp;");
	         		if (image2.equalsIgnoreCase("img00")) {
	         			String s2 = siteData.getPageImages().get("service2custom");
	         			if(s2.length() > 0) {
	         				sb.append("<img src=\"").append(orgImagePath);
	         				sb.append(s2).append("\">");
	         			}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath);
	        			sb.append(image2).append("\">");
	        		}
	        	}
	        	sb.append("</p>");
	        	
	        	//get the service types and descriptions
	        	Map<String,String> services = siteData.getServices();
	        	if (services.size() > 0) {
	        		for(String type : services.keySet()) {
	        			sb.append("<p><b>").append(type.replaceAll("\\n","<br/>")).append("</b><br/>");
	        			sb.append(services.get(type).replaceAll("\\n","<br/>")).append("</p>");
	        		}
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("service");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		
	    	case GENERAL:
	    	   	sb = new StringBuffer(GENINFO_CONTENT_START);
	        	if(useTitles) {
	        		sb.append("<h2>General Information</h2>");
	        	}
	        	sb.append("<p>");
	        	
	        	if (siteData.getHours() != null && siteData.getHours().length() > 0) {
	        		sb.append("<b>Office hours:</b><br/>");
	        		sb.append(siteData.getHours()).append("<br/><br/>");
	        	}
	        	if (siteData.getInsurance() != null && siteData.getInsurance().length() > 0) {
	        		sb.append("<b>Insurance:</b><br/>");
	        		sb.append(siteData.getInsurance()).append("<br/><br/>");
	        	}
	        	if (siteData.getPayment() != null && siteData.getPayment().length() > 0) {
	        		sb.append("<b>Payment Options:</b><br/>");
	        		sb.append(siteData.getPayment()).append("<br/><br/>");
	        	}
	        	if (siteData.getApp() != null && siteData.getApp().length() > 0) {
	        		sb.append("<b>Appointments:</b><br/>");
	        		sb.append(siteData.getApp()).append("<br/><br/>");
	        	}
	        	if (siteData.getEmergencies() != null && siteData.getEmergencies().length() > 0) {
	        		sb.append("<b>Emergencies:</b><br/>");
	        		sb.append(siteData.getEmergencies()).append("<br/><br/>");
	        	}
	        	if (siteData.getAddInfo() != null && siteData.getAddInfo().length() > 0) {
	        		sb.append("<b>Additional Information:</b><br/>");
	        		sb.append(rewriteDocsPath(siteData.getAddInfo()));
	        		sb.append("<br/><br/>");
	        	}
	        	
	        	//add the included PDF file link
	        	if (siteData.getPdf() != null && siteData.getPdf().equalsIgnoreCase("yes")) {
	        		String fileName = siteData.getPdfFile();
	        		if (fileName != null && fileName.length() > 0) {
	        			sb.append("<a href=\"").append(orgBinaryDocPath).append(fileName).append("\">");
	        			sb.append(fileName).append("</a><br/><br/>");
	        		}
	        	}
	        	
	        	//add the uploaded docs links
	        	List<String> docs = siteData.getDocuments();
	        	if (docs.size() > 0) {
	        		for (String name : docs) {
	        			sb.append("<a href=\"").append(orgBinaryDocPath).append(name);
	        			sb.append("\" target=\"_blank\">").append(name).append("</a><br/>");
	        		}
	        	}
	        	
	        	sb.append("</p>");
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("general");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    	
	    	/*
	    	case PROFILE:
	    	   	sb = new StringBuffer(PROF_CONTENT_START);
	    	   	if(useTitles) {
	    	   		sb.append("<h2>Physician Profiles</h2>");
	    	   	}
	        	
	        	List<PspPhysVO> phys = siteData.getProfiles();
	        	if (phys != null && phys.size() > 0) {
	        		
	        		for (PspPhysVO p : phys) {
	        			sb.append("<hr size=\"1\">");
	        			sb.append("<p>");
	            		sb.append("<b>").append(p.getName()).append("</b><br/>");
	            		
	            		//add the physician's photo if available
	            		if (p.getPhotoCustom() != null && p.getPhotoCustom().length() > 0) {
	            			sb.append("<img src=\"").append(orgImagePath);
	            			sb.append(p.getPhotoCustom()).append("\"");
	            			sb.append(" class=\"pspProfileImage\"><br/>");
	            		}
	            		
	            		//add link to the physician's CV if available
	            		if (p.getCvPath() != null && p.getCvPath().length() > 0) {
	            			sb.append("<a href\"").append(orgBinaryDocPath).append(p.getCvPath());
	            			sb.append("\" target=\"_blank\">");
	            			sb.append(p.getCvPath().substring(0, p.getCvPath().length() - 4));
	            			sb.append("</a><br/><br/>");
	            		}
	            		
	            		//add physician's profile if available
	            		sb.append(p.getProfile());
	            		sb.append("</p>");
	            		
	        		}
	        		
	        	}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("profile");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		*/
	    		
	    	case LINK:
	    	   	sb = new StringBuffer(LINKS_CONTENT_START);
	        	if(useTitles) {
	        		sb.append("<h2>Links</h2>");
	        	}
	        	
	        	sb.append("<p>");
	        	//Add images
	        	image1 = StringUtil.checkVal(siteData.getPageImages().get("link1"));
	        	if (image1.length() > 0) {
	        		if (image1.equalsIgnoreCase("img00")) {
	    	        	String imageCustom = StringUtil.checkVal(siteData.getPageImages().get("link1custom"));
	    	        	if(imageCustom.length() > 0) {
	    	        		sb.append("<img src=\"").append(orgImagePath).append(imageCustom);
	    	        		sb.append("\" vspace=\"5\" hspace=\"5\" align=\"right\">");
	    	        	}
	        		} else {
	        			sb.append("<img src=\"").append(orgCommonImagePath).append(image1);
	        			sb.append("\" vspace=\"5\" ");
	        			sb.append("hspace=\"5\" align=\"right\">");
	        		}
	        		sb.append("<br/>");
	        	}
	        	
	        	//For 'main' links the value is the link URL.
	        	//For 'custom' links the key is typically the URL.
	        	
	        	//loop the links map keys
	        	for(String linkKey : siteData.getLinks().keySet()) {
	        		//get the link value from the custom links map
	        		//String linkValue = siteData.getLinks().get(linkKey);
	        		String linkValue = "";
	    			
	        		//if link key is in main map, use main map value instead
	        		if (siteData.getMainLinks().containsKey(linkKey)) {
	        			linkValue = siteData.getMainLinks().get(linkKey);
	        		} else {
	        			//if custom map value is not empty, use custom map value
	        			if (siteData.getLinks() != null && siteData.getLinks().size() > 0) {
	            			linkValue = StringUtil.checkVal(siteData.getLinks().get(linkKey));
	        				//we have to check the key to handle variations...
	        				if (!linkKey.contains(".")) {
	        					//use the key as text...don't build a link
	        					sb.append(linkKey).append("<br/>");
	        				} else {
	        					//If the value is empty, the value is the key?
	        					if (linkValue.length() == 0) linkValue = linkKey;
	        					
	        					//Build link.
	    						if (linkKey.contains("http")) {
	    							sb.append("<a href=\"").append(linkKey);
	    							sb.append("\" target=\"_blank\">");
	    							sb.append(linkValue).append("</a><br/>");
	    						} else {
	    							sb.append("<a href=\"").append("http://").append(linkKey);
	    							sb.append("\" target=\"_blank\">").append(linkValue);
	    							sb.append("</a><br/>");
	        					}
	        				}
	        			} else {
	        				//use the key as text...don't build a link
	    					sb.append("<br/>").append(linkKey);
	        			}
	        		}
	        		//System.out.println("linkValue : " + linkValue);
	        	}
	        	sb.append("</p>");
	        	
	        	sb.append(CONTENT_END);

	        	pc = new PSPContent();
	        	pc.setContentName("link");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		
	    	case DISCLAIMER:
	    	   	sb = new StringBuffer(DISCLAIMER_CONTENT_START);
	    	   	if (useTitles) {
	    	   		sb.append("<h2>Disclaimer</h2>");
	    	   	}
    	    	
	       		try {
	       			sb.append(getDisclaimer(DISCLAIMER_TEMPLATE_PATH));
	       		} catch (Exception e) {
	       			//log.error("Error retrieving disclaimer template...", e);
	       			sb.append("Disclaimer text goes here...");
	       			sb.append(CONTENT_END);
	       			
	       			pc = new PSPContent();
	       	    	pc.setContentName("disclaimer");
	       	    	pc.setContentText(sb.toString());
	       	    	pc.setColumn(siteData.getTemplate().getContColNo());
	       	    	
	       	    	return pc;
	       		}
	        	
	       		//loop through disclaimer and add company name
	       		String companyName = siteData.getCompanyName().toUpperCase();
	       		int index = sb.indexOf("#company#");
	       		while (sb.indexOf("#company#") > -1) {
	       			sb.replace(index, (index + 9), companyName);
	       			index = sb.indexOf("#company#");
	       		}
	        	
	        	sb.append(CONTENT_END);
	        	
	        	pc = new PSPContent();
	        	pc.setContentName("disclaimer");
	        	pc.setContentText(sb.toString());
	        	pc.setColumn(siteData.getTemplate().getContColNo());
	    		break;
	    		
	    }
    	
    	return pc;
    }
    
    public String getCSSPath() {
    	return this.STYLES_TEMPLATE_PATH;
    }
    
}
