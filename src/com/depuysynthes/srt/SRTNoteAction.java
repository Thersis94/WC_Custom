package com.depuysynthes.srt;

import java.util.Arrays;
import java.util.List;

import com.depuysynthes.srt.vo.SRTNoteVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
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
		String projectId = req.getParameter("projectId");

		List<SRTNoteVO> notes = loadNotes(projectId);

		putModuleData(notes);
	}

	/**
	 * @param projectId
	 * @return
	 */
	private List<SRTNoteVO> loadNotes(String projectId) {
		String sql = buildNoteLoadSql();

		List<SRTNoteVO> data = new DBProcessor(dbConn).executeSelect(sql, Arrays.asList(projectId), new SRTNoteVO());

		new NameComparator().decryptNames(data, (String)getAttribute(Constants.ENCRYPT_KEY));

		return data;
	}

	/**
	 * Build the Note Loading Sql.
	 * @return
	 */
	private String buildNoteLoadSql() {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select n.*, p.first_nm, p.last_nm from ").append(custom);
		sql.append("SRT_NOTE n inner join ").append(custom);
		sql.append("SRT_ROSTER r on n.ROSTER_ID = r.ROSTER_ID ");
		sql.append("inner join PROFILE p on r.profile_id = p.profile_id ");
		sql.append("where n.project_id = ?");

		return sql.toString();
	}

	@Override
	public void build(ActionRequest req) throws ActionException {
		SRTNoteVO note = new SRTNoteVO(req);

		//Save the note
		saveNote(note);

		/*
		 * Place the note back on moduleData.  This is intended to be
		 * called via ajax and we wanna return the proper note.
		 */
		putModuleData(note);
	}

	/**
	 * Save the Note.  If is Insert, the id will be populated on the save.
	 * @param note
	 */
	private void saveNote(SRTNoteVO note) {
		try {
			new DBProcessor(dbConn).save(note);
		} catch(Exception e) {
			log.error("Unable to save Note.", e);
		}
	}
}