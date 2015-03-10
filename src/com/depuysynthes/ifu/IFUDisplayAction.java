package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: IFUSearchAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Searches the database for all items pertaining to the given 
 * search parameters and creates a list of IFU documents from those results.
 * If the language being searched does not have a complete list of IFUs then 
 * any missing documents will be loaded from the default langiage/</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUDisplayAction extends SBActionAdapter {

	public void retrieve(SMTServletRequest req) throws ActionException {		
		// Get the default language - give the user a list to choose from if one wasn't passed
		String language = StringUtil.checkVal(req.getParameter("lang"), null);
		if (language == null) {
			req.setAttribute("languages", this.loadLanguages());
			return;
		}
		
		//TODO check for archive and load archives
		
		
		//load the list of IFUs - favor the language provided
		
	}
	
	
	/**
	 * loads a set of languages to display on the default page
	 * @return
	 */
	private Map<String, String> loadLanguages() {
		Map<String, String> langs = new HashMap<>();
		StringBuilder sql = new StringBuilder(100);
		sql.append("select distinct a.language_cd, a.language_nm from language a inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_IFU_IMPL b on a.language_cd=b.language_cd");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				langs.put(rs.getString(1),  rs.getString(2));
			
		} catch (SQLException sqle) {
			log.error("could not execute langs query", sqle);
		}
		return langs;
	}
}
