package com.fastsigns.action.franchise.vo.pages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.menu.MenuObj;



/****************************************************************************
 * <b>Title</b>: FranchiseMenuObj.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 27, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class PageContainerVO_en_US extends PageContainerVO{

	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(PageContainerVO_en_US.class);
	protected final String loginModId = "c0a802232cbe218db59504f1d6757df7";
	
	public PageContainerVO_en_US() {
		assignTypeVals();
		sharedId = "FTS_SHARED";
	}
	
	/**
	 * Assigns the page to the appropriate collection
	 * @param page
	 */
	public void addPages(List<MenuObj> pages) {
		for (MenuObj page : pages) {
			if (pages.size() == 1) {
				selectedPage.setKey(page);
			}
			if (page.isVisible()) {
				//menu pages
				if (bizRules.containsKey(page.getDisplayName())) {
					bizRules.put(page.getDisplayName(), page);
				} else if (bizRules.get("Custom Link One").getPageId() == null && "link1".equalsIgnoreCase(page.getAliasName())) {
					bizRules.put("Custom Link One", page);
				} else if (bizRules.get("Custom Link Two").getPageId() == null && "link2".equalsIgnoreCase(page.getAliasName())) {
					bizRules.put("Custom Link Two", page);
				} else if (bizRules.get("Custom Page One").getPageId() == null) {
					bizRules.put("Custom Page One", page);
				} else if (bizRules.get("Custom Page Two").getPageId() == null) {
					bizRules.put("Custom Page Two", page);
				}
				menuPages.add(page);
			} else {
				//non-menu pages
				nonMenuPages.add(page);
			}
		}
		menus.addAll(new Tree(castNodeList()).preorderList());
	}

	/**
	 * Enter custom data that is required in center page views.
	 * @return
	 */
	public Map<String, String> getCustomVals(){
		Map<String, String> vals = new HashMap<String, String>();
		vals.put("loginPmid", "c0a802232cbe218db59504f1d6757df7");	//Pmid for parent org's login module.
		return vals;
	}
	
/****************** FASTSIGNS BUSINESS LOGIC **********************************/
	/* Permissions Mapping
	 * @returns a mapping of pages and what level of "editability" the franchises have.
	 * Used by JSTL to determine which forms/fields we can offer the admins (to change).
	 * This logic has no impact on the live site functionality.
	 */
	public Map<String, Integer> getBusinessLogic() {
		//pages not listed here are considered custom (fully customizable)
		//there are duplicates on this list because sometimes lookup is done by name
		//and other times by alias.  Others have duplicates because of the HTML characters
		//(sometimes looked-up escaped, other times un-escaped).  Se-la-vi!
		Map<String, Integer> pgs = new HashMap<String, Integer>();
		pgs.put("About Us", CUSTOM_OR_CORP);
		pgs.put("Promotional Products", EXTERNAL);
		pgs.put("Image Library", EXTERNAL);
		pgs.put("Digital Signs", CORP);
		pgs.put("Sign Needs Analysis", CORP);
		pgs.put("Learning Center", CORP);
		pgs.put("Products", CORP);
		pgs.put("Services", CORP);
		pgs.put("FASTSIGNS&reg; Blog", CORP);
		pgs.put("Sign Materials", CUSTOM_TEMPLATED);
		pgs.put("Product Information", CUSTOM_TEMPLATED);
		pgs.put("3M Attention Software Service", CUSTOM_TEMPLATED);
		pgs.put("Optima", CUSTOM_TEMPLATED);
		pgs.put("Modulex", CUSTOM_TEMPLATED);
			//Product Sub-Pages
			pgs.put("Digital Signage", CUSTOM_TEMPLATED);
			pgs.put("Banners", CUSTOM_TEMPLATED);
			pgs.put("Vehicle Graphics", CUSTOM_TEMPLATED);
			pgs.put("Labels & Decals", CUSTOM_TEMPLATED);
			pgs.put("labels-and-decals", CUSTOM_TEMPLATED);
			pgs.put("Trade Show & Displays", CUSTOM_TEMPLATED);
			pgs.put("trade-show-and-displays", CUSTOM_TEMPLATED);
			pgs.put("Promotional Products", CUSTOM_TEMPLATED);
			pgs.put("Building Signs", CUSTOM_TEMPLATED);
			pgs.put("Yard & Site Signs", CUSTOM_TEMPLATED);
			pgs.put("yard-and-site-signs", CUSTOM_TEMPLATED);
			pgs.put("Point of Sale Signs", CUSTOM_TEMPLATED);
			pgs.put("Regulatory Signs", CUSTOM_TEMPLATED);			
			pgs.put("Unique Signs", CUSTOM_TEMPLATED);
			pgs.put("Business Signs", CUSTOM_TEMPLATED);
		pgs.put("Careers", CORP);
		pgs.put("Custom Link One", EXTERNAL);
		pgs.put("Custom Link Two", EXTERNAL);
		pgs.put("link1", EXTERNAL);
		pgs.put("link2", EXTERNAL);
		return pgs;
	}
	
	/**
	 * This loads a default page list.
	 * All sites CAN have these pages, defaulting to these URLs, displayNames, etc.
	 */
	public void assignTypeVals() {
		bizRules.put("About Us", makePage("About-Us", "About Us", "/About-Us")); //custom content or corp
		bizRules.put("Our Staff",makePage("Our-Staff", "Our Staff", "")); //custom
		bizRules.put("Equipment",makePage("Equipment", "Equipment", "")); //custom
		bizRules.put("Products",makePage("ProductList", "Products", "/products"));  //corp
		bizRules.put("Services",makePage("Services", "Services", "/Project-Services"));  //corp
		bizRules.put("Learning Center",makePage("LearningCenter", "Learning Center", "/LearningCenter"));  //corp
		bizRules.put("Sign Needs Analysis",makePage("SignNeedsAnalysis", "Sign Needs Analysis", "/binary/org/FTS/SignNeedsAnalysis-GEN.pdf", true));  //corp
		bizRules.put("Image Library",makePage("ImageLibrary", "Image Library", null, true)); //external
		bizRules.put("FASTSIGNS&reg; Blog",makePage("FASTSIGNSBlog", "FASTSIGNS&reg; Blog", "/Blog")); //corp
		bizRules.put("Promotional Products",makePage("Promotional-Products", "Promotional Products", null, true)); //external
		bizRules.put("Digital Signs",makePage("DigitalSigns", "Digital Signs", "/DDS")); //corp
		bizRules.put("Sign Materials",makePage("sign-materials", "Sign Materials", "")); //custom
		bizRules.put("Product Information",makePage("products", "Product Information", "")); //custom
		bizRules.put("Careers", makePage("careers", "Careers", "/careers?franchiseId=${franchiseId}"));
		bizRules.put("3M Attention Software Service", makePage("visual-attention-software", "3M Attention Software Service", null)); //custom
		bizRules.put("Optima", makePage("optima", "Optima", null)); //custom
		bizRules.put("Modulex", makePage("modulex","Modulex",null)); //custom
		
		MenuObj p;	
				
		p = makePage("digital-signage", "Digital Signage", "");
		p.setParentPath("/products/");
		p.setOrder(1);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("banners", "Banners", "");
		p.setParentPath("/products/");
		p.setOrder(3);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("vehicle-graphics", "Vehicle Graphics", "");
		p.setParentPath("/products/");
		p.setOrder(5);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("labels-and-decals", "Labels & Decals", "");
		p.setParentPath("/products/");
		p.setOrder(7);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("trade-show-and-displays", "Trade Show & Displays", "");
		p.setParentPath("/products/");
		p.setOrder(9);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("promotional-products", "Promotional Products", "");
		p.setParentPath("/products/");
		p.setOrder(11);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("building-signs", "Building Signs", "");
		p.setParentPath("/products/");
		p.setOrder(13);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("point-of-sale-signs", "Point of Sale Signs", "");
		p.setParentPath("/products/");
		p.setOrder(15);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("yard-and-site-signs", "Yard & Site Signs", "");
		p.setParentPath("/products/");
		p.setOrder(17);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("regulatory-signs", "Regulatory Signs", "");
		p.setParentPath("/products/");
		p.setOrder(19);
		bizRules.put(p.getDisplayName(), p); //custom sub-page

		p = makePage("unique-signs", "Unique Signs", "");
		p.setParentPath("/products/");
		p.setOrder(21);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		p = makePage("business-signs", "Business Signs", "");
		p.setParentPath("/products/");
		p.setOrder(23);
		bizRules.put(p.getDisplayName(), p); //custom sub-page
		
		bizRules.put("Custom Page One", makePage("page1","Custom Page One", ""));
		bizRules.put("Custom Page Two", makePage("page2","Custom Page Two", ""));
		bizRules.put("Custom Link One", makePage("link1","Custom Link One", null)); //external
		bizRules.put("Custom Link Two", makePage("link2","Custom Link Two", null)); //external
	}
	
	/**
	 * returns a list of tags/data that Freemarker will use to populate 
	 * dynamic text on the Center's page.
	 * @return
	 */
	@Override
	public Map<String, Object> getFreemarkerTags(SMTServletRequest req) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("franchiseId", req.getSession().getAttribute("webeditFranId"));

		return data;
	}

/************************ END BUSINESS LOGIC **********************************/
}
