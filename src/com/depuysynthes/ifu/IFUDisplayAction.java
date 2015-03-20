package com.depuysynthes.ifu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: IFUSearchAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Searches the database for all items pertaining to the given 
 * search parameters and creates a list of IFU documents from those results.
 * If the language being searched does not have a complete list of IFUs then 
 * any missing documents will be loaded from the default language/</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUDisplayAction extends SBActionAdapter {
	
	private static final String DEFAULT_LANG = "en"; //we use this to load the list of IFUs

	public IFUDisplayAction() {
		super();
	}
	
	public IFUDisplayAction(ActionInitVO init) {
		super(init);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {		
		// Get the default language - give the user a list to choose from if one wasn't passed
		String language = StringUtil.checkVal(req.getParameter("lang"), null);
		if (language == null) {
			req.setAttribute("languages", this.loadLanguages());
			return;
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		
		String keyword = "";
		if (req.hasParameter("keyword")) {
			req.setValidateInput(false); //turn off validation before grabbing the term, issues with UTF & HTML characters.
			keyword = StringEncoder.urlDecode(req.getParameter("keyword")).trim();
			req.setValidateInput(true);
			log.debug("searching for " + keyword);
		}
		
		//load the list of IFUs - favor the language provided
		Collection<IFUDocumentVO> data = loadIFUs(language, req.hasParameter("archive"), 
				page.isPreviewMode(), keyword);
				
		//store the data and return
		super.putModuleData(data);
	}
	
	
	/**
	 * loads the master list of IFUs in the language requests
	 * also unions the default language to fill in any gaps in the data.
	 * @param lang
	 * @return
	 */
	private Collection<IFUDocumentVO> loadIFUs(String lang, boolean isArchive, 
			boolean isPreviewMode, String keyword) {
		Map<String, IFUDocumentVO> data = new HashMap<>();
		keyword = "%" + keyword + "%";
		String sql = getIFUQuery(lang, isArchive, isPreviewMode, (keyword.length() > 2));
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (keyword.length() > 2) {
				ps.setString(1, lang);
				ps.setString(2, keyword);
				ps.setString(3, keyword);
				ps.setString(4, keyword);				
				ps.setString(5, keyword);
				ps.setString(6, keyword);
				ps.setString(7, lang);
			} else {
				ps.setString(1, lang);
				ps.setString(2, lang);
				ps.setString(3, DEFAULT_LANG);
			}	

			boolean isNativeLang = false;
			String ifuId = null;
			IFUDocumentVO vo = null;
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ifuId = rs.getString("depuy_ifu_id");
				if (data.containsKey(ifuId)) {
					vo = data.get(ifuId);
				} else {
					vo = new IFUDocumentVO(rs);
					if (keyword.length() > 2)
						vo.setKeywordMatched(Convert.formatBoolean(rs.getInt("keyword_matched")));
				}
				//determine if the TG belongs to the this language or the default language
				isNativeLang = (StringUtil.checkVal(vo.getImplId()).equals(rs.getString("xr_impl_id")));
				
				//add the TG to the IFU
				vo.addTg(new IFUTechniqueGuideVO(rs), isNativeLang);
				data.put(ifuId,  vo);
			}
			
		} catch (SQLException sqle) {
			log.error("could not load IFUs", sqle);
		}

		List<IFUDocumentVO> list;
		//apply keyword search as a post-query filter, so we can display the complete IFU instead of snippets
		if (keyword.length() > 2) {
			list = new ArrayList<>(keywordFilter(data.values(), keyword));
		} else {
			list = new ArrayList<>(data.values());
		}
		
		Collections.sort(list, new IFUDisplayComparator());
		log.debug("cnt=" + list.size());
		return list;
	}
	
	
	/**
	 * apply keyword filtering to the TG names if the IFU did not (itself) match the keyword search
	 * @param data
	 * @param keyword
	 * @return
	 */
	private List<IFUDocumentVO> keywordFilter(Collection<IFUDocumentVO> data, String keyword) {
		List<IFUDocumentVO> newList = new ArrayList<>(data.size());
		
		for (IFUDocumentVO vo : data) {
			if (vo.isKeywordMatched()) {
				newList.add(vo);
				continue;
			}
			//test for TG names for a match since we didn't match the IFU
			//if none of the TGs match they keyword, and the IFU didn't match, we won't display this record.
			for (IFUTechniqueGuideVO tg : vo.getTgList()) {
				if (StringUtils.containsIgnoreCase(tg.getTgName(), keyword.substring(1, keyword.length()-1))) {
					newList.add(vo);
					break;
				}
			}
		}
		return newList;
	}
	
	
	
	/**
	 * builds the complex union query that loads the language-specific IFUs
	 * as well as the default-language IFUs (in their absense)
	 * @param lang
	 * @return
	 */
	private String getIFUQuery(String lang, boolean isArchive, boolean isPreviewMode, 
			boolean isKeyword) {
		StringBuilder sql = new StringBuilder(300);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select case b.language_cd when ? then 0 else 1 end as precedence, ");
		sql.append("a.BUSINESS_UNIT_NM, a.order_no, a.version_txt, a.business_unit_nm,  ");
		sql.append("isnull(a.depuy_ifu_group_id, a.depuy_ifu_id) as depuy_ifu_id, ");  //use pending records in place of approved ones
		sql.append("b.title_txt, b.url_txt, b.dpy_syn_mediabin_id, ");
		sql.append("b.language_cd, b.create_dt, b.default_msg_txt, ");
		sql.append("b.depuy_ifu_impl_id, xr.depuy_ifu_impl_id as xr_impl_id, ");
		sql.append("tg.DEPUY_IFU_TG_ID, tg.tg_nm, tg.url_txt as tg_url, tg.dpy_syn_mediabin_id as tg_mediabin_id ");
		if (isKeyword) {
			//sequence here is important for performance, we save evaluating article_txt (the blob) for last
			sql.append(", case when a.title_txt like ? then 1 ");
			sql.append("when b.title_txt like ? then 1 ");
			sql.append("when b.part_no_txt like ? then 1 ");
			sql.append("when tg.tg_nm like ? then 1 ");
			sql.append("when b.article_txt like ? then 1 else 0 end as keyword_matched ");
		}
		sql.append("from ").append(customDb).append("DEPUY_IFU a ");
		sql.append("inner join ").append(customDb).append("DEPUY_IFU_IMPL b on a.depuy_ifu_id=b.depuy_ifu_id and (b.language_cd=? ");
		if (!isKeyword) sql.append(" or b.language_cd=? ");
		sql.append(") ");
		sql.append("left outer join ").append(customDb).append("DEPUY_IFU_TG_XR xr on b.depuy_ifu_impl_id=xr.depuy_ifu_impl_id ");
		sql.append("left outer join ").append(customDb).append("DEPUY_IFU_TG tg on xr.depuy_ifu_tg_id=tg.depuy_ifu_tg_id ");
		sql.append("where a.archive_flg=").append((isArchive) ? 1 : 0).append(" ");
		if (!isPreviewMode) sql.append("and a.depuy_ifu_group_id is null ");
		
		//order by
		if (isPreviewMode) {
			sql.append("order by precedence asc, order_no asc, depuy_ifu_group_id desc, b.title_txt asc, xr.order_no, tg.tg_nm");
		} else {
			sql.append("order by precedence asc, order_no asc, b.title_txt asc, xr.order_no, tg.tg_nm");
		}
		return sql.toString();
	}
	
	
	/**
	 * loads a set of languages to display on the default page
	 * @return
	 */
	private Map<String, String> loadLanguages() {
		Map<String, String> langs = new LinkedHashMap<>();
		StringBuilder sql = new StringBuilder(100);
		sql.append("select distinct a.language_cd, a.language_nm from language a inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_IFU_IMPL b on a.language_cd=b.language_cd ");
		sql.append("order by a.language_nm");
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