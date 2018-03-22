package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTNoteVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SRTNoteAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores SRT Note Information
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 15, 2018
 ****************************************************************************/
public class SRTNoteAction extends SimpleActionAdapter {

	public SRTNoteAction() {
		super();
	}

	public SRTNoteAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String projectId = req.getParameter(SRTProjectAction.SRT_PROJECT_ID);

		if(!StringUtil.isEmpty(projectId)) {
			List<SRTNoteVO> notes = loadNotes(projectId);
			putModuleData(notes, notes.size(), false);
		}
	}

	/**
	 * Loads Notes for a given projectId, optionally filtered to a single
	 * noteId.
	 * @param projectId
	 * @param noteId
	 * @return
	 */
	private List<SRTNoteVO> loadNotes(String projectId) {
		String sql = buildNoteLoadSql(projectId);

		List<Object> vals = new ArrayList<>();
		if(!StringUtil.isEmpty(projectId)) {
			vals.add(projectId);
		}

		List<SRTNoteVO> data = new DBProcessor(dbConn).executeSelect(sql, vals, new SRTNoteVO());

		new NameComparator().decryptNames(data, (String)getAttribute(Constants.ENCRYPT_KEY));

		return data;
	}

	/**
	 * Build the Note Loading Sql.
	 * @param projectId
	 * @return
	 */
	private String buildNoteLoadSql(String projectId) {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select n.*, p.first_nm, p.last_nm from ").append(custom);
		sql.append("DPY_SYN_SRT_NOTE n inner join ").append(custom);
		sql.append("DPY_SYN_SRT_ROSTER r on n.ROSTER_ID = r.ROSTER_ID ");
		sql.append("inner join PROFILE p on r.profile_id = p.profile_id ");
		sql.append(DBUtil.WHERE_1_CLAUSE);
		if(!StringUtil.isEmpty(projectId)) {
			sql.append("and n.project_id = ? ");
		}

		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {

		//Build a Note off the Request.
		SRTNoteVO note = new SRTNoteVO(req);
		SRTRosterVO roster = SRTUtil.getRoster(req);

		//Set RosterData
		note.setRosterId(roster.getRosterId());
		note.setFirstName(roster.getFirstName());
		note.setLastName(roster.getLastName());

		//Save the note
		saveNote(note);

		/*
		 * Place the note back on moduleData.  This is intended to be
		 * called via ajax and we want to return the proper note.
		 */
		if(StringUtil.isEmpty(note.getNoteId())) {
			putModuleData(null, 0, false, "Could not Create Note.");
		} else {
			putModuleData(Arrays.asList(note), 1, false);
		}
	}

	/**
	 * Save the Note.  If is Insert, the id will be populated on the save.
	 * @param note
	 */
	private void saveNote(SRTNoteVO note) {
		try {
			new DBProcessor(dbConn, getCustomSchema()).save(note);
		} catch(Exception e) {
			log.error("Unable to save Note.", e);
		}
	}
}