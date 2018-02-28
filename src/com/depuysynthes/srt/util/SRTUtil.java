package com.depuysynthes.srt.util;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.depuysynthes.srt.vo.SRTMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.google.common.collect.Maps;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
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

	//TODO Update with actual Values when decided.  Potentially pull from config.
	public static final String SRT_ORG_ID = "DPY_SYN_HUDDLE";
	public static final String PUBLIC_SITE_ID = "DPY_SYN_HUDDLE_2";
	public static final String REGISTRATION_GRP_ID = "18d2a87d9daef5dfc0a8023743a91557";
	public static final String HOMEPAGE_REGISTER_FIELD_ID = null;

	private SRTUtil() {
		//Hide Default Constructor.
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
	 * Helper method that loads Milestones into a list of Project Records.
	 * @param rowData
	 */
	public static void populateMilestones(List<SRTProjectVO> rowData, Connection dbConn, String schema) {

		//Map Projects by ProjectId
		Map<String, SRTProjectVO> pMap = Maps.uniqueIndex(rowData, SRTProjectVO::getProjectId);

		//Create list of keys via pMap keySet.
		List<Object> vals = new ArrayList<>(pMap.keySet());

		//Retrieve all Milestones for the project Ids in vals.
		List<SRTMilestoneVO> milestones = new DBProcessor(dbConn).executeSelect(buildMilestoneQuery(vals.size(), schema), vals, new SRTMilestoneVO());

		//Add Milestones.  Will reflect in passed rowData by references.
		for(SRTMilestoneVO m : milestones) {
			pMap.get(m.getProjectId()).addMilestone(m);
		}
	}

	/**
	 * Build Milestone Retrieval Sql.
	 * @param size
	 * @return
	 */
	private static String buildMilestoneQuery(int size, String schema) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(schema);
		sql.append("SRT_PROJECT_MILESTONE_XR where PROJECT_ID in (");
		DBUtil.preparedStatmentQuestion(size, sql);
		sql.append(") order by PROJECT_ID");

		return sql.toString();
	}

	/**
	 * Attempt to load the Users OpCo off the Session Object.
	 * @param req
	 * @return
	 */
	public static String getOpCO(ActionRequest req) {
		SRTRosterVO r = (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);
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
		String [] firstLast = name.split(" ");
		if(firstLast == null || firstLast.length == 0) {
			return "";
		} else if(firstLast.length == 1) {
			return se.decrypt(firstLast[0]);
		} else {
			return StringUtil.join(se.decrypt(firstLast[0]), " ", se.decrypt(firstLast[1]));
		}
	}
}