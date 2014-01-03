package com.ansmed.sb.psp.pages;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.ansmed.sb.psp.PSPContent;
import com.ansmed.sb.psp.PspPhysVO;
import com.ansmed.sb.psp.PspSiteVO;
import com.ansmed.sb.psp.PspContentAction.ContentType;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: AbstractPspContent.java<p/>
 * <b>Description</b>: Abstract PSP site page content class.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Oct 2, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public abstract class AbstractPspContent {
	
	public final String HOME_CONTENT_START = "<div id=\"pspHomeContent\">";
	public final String OVIEW_CONTENT_START = "<div id=\"pspOverviewContent\">";
	public final String LOC_CONTENT_START = "<div id=\"pspLocationContent\">";
	public final String CONTACTUS_CONTENT_START = "<div id=\"pspContactUsContent\">";
	public final String SERVICES_CONTENT_START = "<div id=\"pspServicesContent\">";
	public final String GENINFO_CONTENT_START = "<div id=\"pspGenInfoContent\">";
	public final String PROF_CONTENT_START = "<div id=\"pspPhysProfileContent\">";
	public final String LINKS_CONTENT_START = "<div id=\"pspLinksContent\">";
	public final String DISCLAIMER_CONTENT_START = "<div id=\"pspDisclaimerContent\">";
	public final String CONTENT_END = "</div>";
	public final String ORG_PATH = "/binary/org/";
	public final String DISCLAIMER_TEMPLATE_PATH = "scripts/pspDisclaimer.html";
	public String orgImagePath;
	public String orgCommonImagePath;
	public String orgBinaryDocPath;
	public PspSiteVO siteData;
		
	/**
	 * 
	 */
	public AbstractPspContent() {
		siteData = new PspSiteVO();
	}
	
	// Content
	public abstract PSPContent createContent(ContentType ct);
	
	/* Address content:  This is not technically an existing page.  We are
	building this piece of content so that it can be used in the new template
	structure in SB. */
    public PSPContent createAddress(int addrType) {
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append("<div id=\"pspAddress\">");
    	
    	switch(addrType) {
    	case 1:
    		sb.append("<address>");
			sb.append(siteData.getCompanyName()).append("<br/>");
			sb.append(siteData.getAddress1()).append("<br/>");
			sb.append(siteData.getAddress2()).append("<br/>");
        	sb.append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip()).append("<br/>");
        	sb.append(siteData.getPhone()).append("<br/>");
        	sb.append(siteData.getFax()).append("<br/>");
    		sb.append("</address>");
    		break;
    	case 2:
        	sb.append(siteData.getCompanyName());
        	sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        	sb.append(siteData.getAddress1());
        	sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        	sb.append(siteData.getAddress2());
        	sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        	sb.append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip());
        	sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        	sb.append("<br/>");
        	sb.append(siteData.getPhone());
        	sb.append("&nbsp;&nbsp;&nbsp;");
        	sb.append(siteData.getFax());
        	sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        	sb.append("<a href=\"mailto:");
        	sb.append(siteData.getEmail());
        	sb.append("\">").append(siteData.getEmail());
        	sb.append("</a>");
    		break;
    	case 3:
			sb.append(siteData.getAddress1()).append("<br/>");
			sb.append(siteData.getAddress2()).append("<br/>");
        	sb.append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip()).append("<br/>");
        	sb.append(siteData.getPhone()).append("&nbsp;&nbsp;&nbsp;<br/>");
        	sb.append(siteData.getFax()).append("<br/>");
        	sb.append("<a href=\"mailto:");
        	sb.append(siteData.getEmail());
        	sb.append("\">").append(siteData.getEmail());
        	sb.append("</a>");
    		break;
    	case 4:
        	sb.append(siteData.getCompanyName());
        	sb.append("&nbsp;");
        	sb.append(siteData.getAddress1());
        	sb.append("&nbsp;");
        	sb.append(siteData.getAddress2());
        	sb.append("&nbsp;&nbsp;");
        	sb.append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip());
        	sb.append("&nbsp;");
        	sb.append("<br/>");
        	sb.append(siteData.getPhone());
        	sb.append("&nbsp;&nbsp;&nbsp;");
        	sb.append(siteData.getFax());
        	sb.append("&nbsp;&nbsp;&nbsp;");
        	sb.append("<a href=\"mailto:");
        	sb.append(siteData.getEmail());
        	sb.append("\">").append(siteData.getEmail());
        	sb.append("</a>");
    		break;
    	case 5:
			sb.append(siteData.getCompanyName()).append("<br/>");
			if(siteData.getAddress1().length() > 0) {
				sb.append(siteData.getAddress1()).append("<br/>");
				if(siteData.getAddress2().length() > 0) {
					sb.append(", ").append(siteData.getAddress2());
				}
			} else if(siteData.getAddress2().length() > 0) {
				sb.append(siteData.getAddress2());
			}
			sb.append("<br/>");
        	sb.append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip()).append("<br/>");
        	if(siteData.getPhone().length() > 0) {
        		sb.append(siteData.getPhone()).append("<br/>");
        	}
        	if(siteData.getFax().length() > 0) {
        		sb.append(siteData.getFax()).append("<br/>");
        	}
        	if(siteData.getEmail().length() > 0) {
        		sb.append("<a href=\"mailto:");
            	sb.append(siteData.getEmail());
            	sb.append("\">").append(siteData.getEmail());
            	sb.append("</a>");
        	}
    		break;
    	case 6:
			sb.append(siteData.getCompanyName()).append("&nbsp;&nbsp;");
			if(siteData.getAddress1().length() > 0) {
				sb.append(siteData.getAddress1()).append("<br/>");
				if(siteData.getAddress2().length() > 0) {
					sb.append(",&nbsp;").append(siteData.getAddress2());
				}
			} else if(siteData.getAddress2().length() > 0) {
				sb.append("&nbsp;").append(siteData.getAddress2());
			}
        	sb.append("&nbsp;&nbsp;").append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip()).append("<br/>");
        	if(siteData.getPhone().length() > 0) {
        		sb.append(siteData.getPhone());
        	}
        	if(siteData.getFax().length() > 0) {
        		sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(siteData.getFax()).append("<br/>");
        	}
        	if(siteData.getEmail().length() > 0) {
        		sb.append(siteData.getEmail());
        	}
    		break;
    	case 7:
			sb.append(siteData.getCompanyName()).append("<br/>");
			if(siteData.getAddress1().length() > 0) {
				sb.append(siteData.getAddress1()).append("<br/>");
				if(siteData.getAddress2().length() > 0) {
					sb.append("<br/>").append(siteData.getAddress2());
				}
			} else if(siteData.getAddress2().length() > 0) {
				sb.append(siteData.getAddress2());
			}
			sb.append("<br/>");
        	sb.append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip()).append("<br/>");
        	if(siteData.getPhone().length() > 0) {
        		sb.append(siteData.getPhone()).append("<br/>");
        	}
        	if(siteData.getFax().length() > 0) {
        		sb.append(siteData.getFax()).append("<br/>");
        	}
        	if(siteData.getEmail().length() > 0) {
            	sb.append(siteData.getEmail());
        	}
    		break;
    	case 8:
			sb.append(siteData.getCompanyName()).append("<br/>");
			if(siteData.getAddress1().length() > 0) {
				sb.append(siteData.getAddress1()).append("<br/>");
				if(siteData.getAddress2().length() > 0) {
					sb.append(", ").append(siteData.getAddress2());
				}
			} else if(siteData.getAddress2().length() > 0) {
				sb.append(siteData.getAddress2());
			}
			sb.append("<br/>");
        	sb.append(siteData.getCity()).append(", ");
        	sb.append(siteData.getState()).append(" ");
        	sb.append(siteData.getZip()).append("<br/>");
        	if(siteData.getPhone().length() > 0) {
        		sb.append(siteData.getPhone()).append("<br/>");
        	}
        	if(siteData.getFax().length() > 0) {
        		sb.append(siteData.getFax()).append("<br/>");
        	}
        	if(siteData.getEmail().length() > 0) {
            	sb.append(siteData.getEmail());
        	}
    		break;
    	}

 
    	sb.append("</div>");
    	    	
    	PSPContent pc = new PSPContent();
    	pc.setContentName("address");
    	pc.setContentText(sb.toString());
    	pc.setColumn(siteData.getTemplate().getAddrColNo());
    	
    	return pc;
    }
	
	
	// get disclaimer template
	public String getDisclaimer(String filePath) throws FileNotFoundException, IOException {
		StringBuilder fb = new StringBuilder();
		String strIn = "";
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		
		try {
			while((strIn = br.readLine()) != null)
				fb.append(strIn).append("\n");

		} finally {
			try { br.close(); } catch (Exception e) {}
		}
		
		return fb.toString();
	}
	
	/**
	 * Returns CSS template path for specific content type
	 */
	public abstract String getCSSPath();
	
	// get base CSS template
	public PSPContent getCSS(String filePath) throws FileNotFoundException, IOException {
		StringBuilder fb = new StringBuilder();
		String strIn = "";
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		
		try {
			while((strIn = br.readLine()) != null)
				fb.append(strIn).append("\n");
		
		} finally {
			try { br.close(); } catch (Exception e) {}
		}
		
		//add a style override for a certain profile type's image style
		if(siteData.getTemplate().getProfileTypeFlg() == 4) {
			fb.append("\n");
			fb.append("/* style override for type 4 profile image */\n");
			fb.append(".pspProfileImage {\n");
			fb.append("margin: 30px 10px 0 0;\n");
			fb.append("}\n");
		}
   		
   		PSPContent pc = new PSPContent();
    	pc.setContentName("css");
    	pc.setContentText(fb.toString());
    	pc.setColumn(siteData.getTemplate().getContColNo());
		
		return pc;
	}
	
	/**
	 * Creates logo image content for certain templates that display the
	 * company logo in the content column.
	 * @return
	 */
	public PSPContent createLogoContent() {
		StringBuilder sb = new StringBuilder();
		String logoImage = siteData.getPageImages().get("logocustom");
		if (logoImage != null && logoImage.length() > 0) {
			sb.append("<div id=\"pspLogoImage\">");
			sb.append("<img src=\"").append(orgImagePath);
			sb.append(logoImage).append("\" alt=\"\">");
			sb.append("</div>");
		}
		
		PSPContent pc = new PSPContent();
		pc.setContentName("logo_image");
		pc.setContentText(sb.toString());
		pc.setColumn(siteData.getTemplate().getLogoColNo());
		
		return pc;
	}
	
	/**
	 * Creates one of five types of profile content
	 * @param profile
	 * @param type
	 */
	public PSPContent createProfileContent(List<PspPhysVO> profiles, Integer type) {
		StringBuilder sb = new StringBuilder();
		boolean useTitles = false;
		useTitles = Convert.formatBoolean(siteData.getTemplate().getTitlesFlg());
		
		//set page title if required
    	if(useTitles) {
    		sb.append("<h2>Physician Profiles</h2>");
    	}   	
    	
    	if (profiles != null && profiles.size() > 0) {
	    	//build the rest of the profile content
	    	switch(type) {
		    	case 1:
		    		sb.append("<br/><hr size=\"1\">");
	        		for (PspPhysVO p : profiles) {
	        			sb.append("<p>");
	        			//add the physician's name if available
	        			if(p.getName() != null && p.getName().length() > 0) {
	        				sb.append("<b>").append(p.getName()).append("</b><br/>");
	        			}
	            		
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
	            		if (p.getProfile() != null && p.getProfile().length() > 0) {
	                		sb.append(p.getProfile());	
	            		}
	            		sb.append("</p>");
	            		
		        		//close the content with a horizontal rule
		        		sb.append("<hr size=\"1\">");	
	        		}
	        		break;
	        		
		    	case 2:
		    	case 3:
		    	case 4:
		    		sb.append("<br/><hr size=\"1\">");
	        		for (PspPhysVO p : profiles) {
	            		//add the physician's photo if available
	        			if(type == 2) {
	        				//type2 - use vspace/hspace attributes
	        				if (p.getPhotoCustom() != null && p.getPhotoCustom().length() > 0) {
		            			sb.append("<img src=\"").append(orgImagePath);
		            			sb.append(p.getPhotoCustom()).append("\"");
		            			sb.append(" vspace=\"5\" hspace=\"5\"><br/>");
		            		}	        				
	        			} else if(type == 3) {
		            		//type3 - use style class in img tag
		            		if (p.getPhotoCustom() != null && p.getPhotoCustom().length() > 0) {
		            			sb.append("<img src=\"").append(orgImagePath);
		            			sb.append(p.getPhotoCustom()).append("\"");
		            			sb.append(" class=\"pspProfileImage\"><br/>");
		            		}	
	        			} else if(type == 4) {
		            		//type4 - wrap image in div block
		            		if (p.getPhotoCustom() != null && p.getPhotoCustom().length() > 0) {
		            			sb.append("<div class=\"pspProfileImage\"><img src=\"").append(orgImagePath);
		            			sb.append(p.getPhotoCustom()).append("\">");
		            			sb.append("</div><br/>");
		            		}	        				
	        			}
	        			
	        			//add opening paragraph tag
	        			sb.append("<p>");
	        			
	        			//add the physician's name if available
	        			if(p.getName() != null && p.getName().length() > 0) {
	        				sb.append("<b>").append(p.getName()).append("</b><br/>");
	        			}
	        			
	            		//add link to the physician's CV if available
	            		if (p.getCvPath() != null && p.getCvPath().length() > 0) {
	            			sb.append("<a href\"").append(orgBinaryDocPath).append(p.getCvPath());
	            			sb.append("\" target=\"_blank\">");
	            			sb.append(p.getCvPath().substring(0, p.getCvPath().length() - 4));
	            			sb.append("</a><br/><br/>");
	            		}
	            		
	            		//add physician's profile if available
	            		if (p.getProfile() != null && p.getProfile().length() > 0) {
	                		sb.append(p.getProfile());	
	            		}
	            		//close each paragraph
	            		sb.append("</p>");
	            		
		        		//close the phys paragraph with a horizontal rule
		        		sb.append("<hr size=\"1\">");	
	        		}
	        		break;
	        		
		    	case 5:
		    		//type 5
		    		sb.append("<br/>");
		    		sb.append("<div id=\"pspProfileHr\"><hr size=\"1\"></div>");
		    		
	        		for (PspPhysVO p : profiles) {
	            		//add the physician's photo if available
	            		if (p.getPhotoCustom() != null && p.getPhotoCustom().length() > 0) {
	            			sb.append("<div id=\"pspProfileLeft\">");
	            			sb.append("<img src=\"").append(orgImagePath);
	            			sb.append(p.getPhotoCustom()).append("\"");
	            			sb.append(" class=\"pspProfileImage\"><br/>");
	            			sb.append("</div>");
	            		}
	            		
	            		//set up the right-hand div block and opening paragraph
	            		sb.append("<div id=\"pspProfileRight\">");
	            		sb.append("<p>");
	            		
	        			//add the physician's name if available
	        			if(p.getName() != null && p.getName().length() > 0) {
	        				sb.append("<b>").append(p.getName()).append("</b><br/>");
	        			}
	            		
	            		//add link to the physician's CV if available
	            		if (p.getCvPath() != null && p.getCvPath().length() > 0) {
	            			sb.append("<a href\"").append(orgBinaryDocPath).append(p.getCvPath());
	            			sb.append("\" target=\"_blank\">");
	            			sb.append(p.getCvPath().substring(0, p.getCvPath().length() - 4));
	            			sb.append("</a><br/><br/>");
	            		}
	            		
	            		//add physician's profile if available
	            		if (p.getProfile() != null && p.getProfile().length() > 0) {
	                		sb.append(p.getProfile());	
	            		}
	            		//close the paragraph and div block
	            		sb.append("</p>");
	            		sb.append("</div>");
	            		
		        		//append final horizontal rule
	            		sb.append("<div id=\"pspProfileHr\"><hr size=\"1\"></div>");
	        		}
		    		break;
	    	}
	    	
    	}
    	
    	//set the content on the PSPContent object
    	PSPContent pc = new PSPContent();
    	pc.setContentName("profile");
    	pc.setContentText(sb.toString());
    	pc.setColumn(siteData.getTemplate().getContColNo());
    	
    	return pc;
	}
	
	/**
	 * Replaces "pep.ans-medical.com" URL references so that they reference 
	 * the SB binary instead.
	 * @param content
	 * @return
	 */
	public String rewriteDocsPath(String content) {
		String s = "http://pep.ans-medical.com/websites/ans" + siteData.getPracticeId() + "/";
		int index = content.indexOf(s);
		if (index > -1) {
			return content.replace(s, orgBinaryDocPath);
		} else {
			return content;
		}
	}
	
}