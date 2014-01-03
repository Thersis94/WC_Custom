package com.ansmed.sb.psp;

/****************************************************************************
 * <b>Title</b>:PspPageQueries.java<p/>
 * <b>Description</b>: Contains methods that format the mySQL queries for
 * retrieving content info for each PSP site page.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 2.0
 * @since Oct 2, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PspSiteQueries {
	
	public PspSiteQueries() {
		
	}
	
	public static String retrieveSitesSql() {
		StringBuffer sb = new StringBuffer();
		sb.append("select practice_id from site_alias order by practice_id ");
		return sb.toString();
	}
	
	/**
	 * Builds and returns misc info SQL query string
	 * @return
	 */
	public static String retrieveSiteDataSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select a.practice_id, a.status, x.template, b.message as home_welcome,");
		sb.append("c.message as overview_welcome, d.map, d.direction,");
		sb.append("d.direction2, d.direction3, d.contactform, e.hours,");
		sb.append("e.insurance, e.payment, e.app, e.emergencies, e.addinfo,");
		sb.append("e.pdf, e.pdf_file, f.name as company, f.address1, f.address2, f.city,");
		sb.append("f.state, f.zip, f.phone, f.fax, f.email ");
		sb.append("from status a inner join design x on a.practice_id = x.practice_id ");
		sb.append("inner join homepage b on x.practice_id = b.practice_id ");
		sb.append("inner join overview c on b.practice_id = c.practice_id ");
		sb.append("inner join location d on c.practice_id = d.practice_id ");
		sb.append("inner join general e on d.practice_id = e.practice_id ");
		sb.append("inner join basic f on e.practice_id = f.practice_id ");
		sb.append("where 1 = 1 and a.status = 'Approved' and a.practice_id = ? ");
		
		return sb.toString();
	}
	
	/**
	 * Sql for retrieving image names used on particular site pages
	 * @return
	 */
	public static String retrieveImagesSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select logocustom, mapcustom, map2custom, map3custom, home1, ");
		sb.append("home1custom, overview1, overview1custom, overview2, overview2custom, ");
		sb.append("service1, service1custom, service2, service2custom, link1, ");
		sb.append("link1custom from image where practice_id = ? ");
		
		return sb.toString();
	}
	
	/**
	 * Links page sql
	 * @return
	 */
	public static String retrieveLinksSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select link, linkname from link where practice_id = ? order by link_id ");
		
		return sb.toString();
	}
	
	/**
	 * Sql for retrieving documents for General Info page
	 * @return
	 */
	public static String retrieveDocumentsSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select doc_path from document where practice_id = ? order by order_no ");
		
		return sb.toString();
	}
	
	/**
	 * Contact Us page header sql
	 * @return
	 */
	public static String retrieveContactSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select name, email from contact where practice_id = ? ");
		sb.append("and name <> '' order by contact_id ");
		
		return sb.toString();
	}
	
	/**
	 * Services page sql
	 * @return
	 */
	public static String retrieveServicesSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select type, description from service where practice_id = ? order by service_id ");
		
		return sb.toString();
	}
	
	/**
	 * Physician Profiles page sql
	 * @return
	 */
	public static String retrieveProfileSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select name, profile, photo, photocustom, cv, cv_path ");
		sb.append("from profile where practice_id = ? order by profile_id ");
		
		return sb.toString();
	}
	
	/**
	 * Template info sql
	 * @return
	 */
	public static String retrieveTemplateSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select b.* from design a inner join template_master b ");
		sb.append("on a.template = b.theme_path where a.practice_id = ? ");
		
		return sb.toString();
	}
	
	/**
	 * Site alias sql
	 * @return
	 */
	public static String retrieveAliasesSql() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("select * from site_alias where practice_id = ? order by alias_id ");
		
		return sb.toString();
	}

}
