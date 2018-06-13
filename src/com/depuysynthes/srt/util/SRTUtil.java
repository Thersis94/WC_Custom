package com.depuysynthes.srt.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SRTUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Util class to hold SRT Related Helper methods and
 * constants.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 22, 2018
 ****************************************************************************/
public class SRTUtil {

	public static final String SRT_ORG_ID = "DPY_SYN";
	public static final String PUBLIC_SITE_ID = "DPY_SYN_38";
	public static final String OP_CO_ID = "opCoId";
	public static final String ADMIN_PATH = "/manage";
	public static final String REGISTRATION_GRP_ID = "38ae1841baf997aec0a80255c7bd6f31";
	public static final int DEFAULT_RPP = 25;

	public enum SRTList {SRT_WORKGROUP, REQ_REASON, CHARGE_TO, QUALITY_SYSTEM,
						PRODUCT_TYPE, COMPLEXITY, LABEL_STATUS, PROD_CAT,
						PROD_FAMILY, DEPARTMENT, OBSOLETE, MILESTONE, PROJ_TYPE,
						PROJ_PRIORITY, MAKE_FROM_SCRATCH, PROJ_VENDOR, PROJ_STATUS,
						PROJ_MFG_CHANGE_REASON, SRT_TERRITORIES, SRT_AREA, SRT_REGION}

	public enum SrtPage {MASTER_RECORD("/master-record"), PROJECT("/projects"), REQUEST("/request-form");
		private String urlPath;
		private SrtPage(String urlPath) {
			this.urlPath = urlPath;
		}
		public String getUrlPath() {return urlPath;}
	}

	public enum SrtAdmin {MILESTONE("/milestones");
		private String urlPath;
		private SrtAdmin(String urlPath) {
			this.urlPath = StringUtil.join(ADMIN_PATH, urlPath);
		}
		public String getUrlPath() {return urlPath;}
	}

	private SRTUtil() {
		//Hide Default Constructor.
	}

	/**
	 * Helper method returns proper list_id for an OP_CO.
	 * @param opCoId
	 * @param list
	 * @return
	 */
	public static String getListId(String opCoId, SRTList list) {
		if(!StringUtil.isEmpty(opCoId) && list != null) {
			return StringUtil.join(opCoId, "_", list.name());
		}
		return "";
	}

	/**
	 * takes the pain out of passing Strings in and out of URLs/forms.  Typically these form values arrive HTML encoded.  
	 * Use encodeURIComponent in your JS to compliment what this is doing server-side (at the client).
	 * @param value
	 * @return
	 */
	public static String urlEncode(String value) {
		if (StringUtil.isEmpty(value)) return ""; //going in a URL, we don't want to return a null
		return StringEncoder.urlEncode(StringEscapeUtils.unescapeHtml(value)).replace("+", "%20");
	}

	/**
	 * Attempt to load the Users OpCo off the Session Object.
	 * @param req
	 * @return
	 */
	public static String getOpCO(ActionRequest req) {
		SRTRosterVO r = getRoster(req);
		if(r != null) {
			return r.getOpCoId();
		}
		return null;
	}

	/**
	 * Decrypt given username.  May be empty, just first or first and last
	 * name.
	 * @param qualityEngineerNm
	 * @param se
	 * @return
	 * @throws EncryptionException
	 */
	public static String decryptName(String name, StringEncrypter se) throws EncryptionException {
		String [] firstLast = StringUtil.checkVal(name).split(" ");
		if(firstLast == null || firstLast.length == 0) {
			return "";
		} else if(firstLast.length == 1) {
			return StringUtil.checkVal(se.decrypt(firstLast[0])).trim();
		} else {
			return StringUtil.join(StringUtil.checkVal(se.decrypt(firstLast[0])).trim(), " ", StringUtil.checkVal(se.decrypt(firstLast[1])).trim());
		}
	}

	/**
	 * Retrieve the Roster Records off the Request.
	 * @param req
	 * @return
	 */
	public static SRTRosterVO getRoster(ActionRequest req) {
		return (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);

	}

	/**
	 * Convert list of Projects into map of projects.  Used in places where
	 * we are loading data on Projects and need an efficient means of finding
	 * the relevant SRTProjectVO.
	 * @param projects
	 * @return
	 */
	public static Map<String, SRTProjectVO> mapProjects(List<SRTProjectVO> projects) {
		Map<String, SRTProjectVO> pMap;
		if(projects != null) {
			pMap = projects.stream().collect(Collectors.toMap(SRTProjectVO::getProjectId, Function.identity()));
		} else {
			pMap = new HashMap<>();
		}

		return pMap;
	}

	/**
	 * Builds an outer join query against a list_data element.  Make sure
	 * set PreparedStatementValue for the ListId.
	 * @param sql - Lookup Query
	 * @param alias - Alias for the list_data join
	 * @param columnNm - Column we're matching against
	 */
	public static void buildListJoin(StringBuilder sql, String alias, String columnNm) {
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(" list_data ").append(alias);
		sql.append(" on ").append(alias).append(".value_txt = ").append(columnNm);
		sql.append(" and ").append(alias).append(".list_id = ? ");
	}
}