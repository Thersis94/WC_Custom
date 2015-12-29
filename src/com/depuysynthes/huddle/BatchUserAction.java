package com.depuysynthes.huddle;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileRoleManager;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: BatchUserAction.java<p/>
 * <b>Description: performs a bulk account deactivation of websites users.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 28, 2015
 ****************************************************************************/
public class BatchUserAction extends SimpleActionAdapter {

	public BatchUserAction() {
	}

	public BatchUserAction(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * processes the file upload and imports each row as a new event to add to the 
	 * desired event calendar. 
	 * @param req
	 * @throws ActionException
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		Object msg = attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);
		FilePartDataBean fpdb = req.getFile("xlsFile");
		String siteId = req.getParameter("targetSiteId");
		int saveCnt = 0, wwidCnt = 0;
		String[] profileIds = {};
		
		try {
			if (fpdb == null) throw new InvalidDataException("file missing");
			
			//turn the file into a set of WWIDs relevant to our needs
			Set<String> wwids = parseFile(fpdb);
			wwidCnt = wwids.size();
			
			//turn the set of WWIDs into a set of profileIds
			profileIds = loadProfileIds(wwids, siteId);
			
			//call ProfileRoleManager to have the list of profileIds+siteId accounts disabled
			saveCnt = uploadRoles(siteId, profileIds);
			
		} catch (Exception e) {
			log.error("could not process transaction", e);
			msg = attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		//return some stats to the administrator
		super.adminRedirect(req, msg, buildRedirect(saveCnt, profileIds.length, wwidCnt));
	}
	
	
	/**
	 * Append extra parameters to the redirect url so we can display some stats
	 * about the transaction performed
	 * @param req
	 * @return
	 */
	private String buildRedirect(int saveCnt, int profileCnt, int wwidCnt) {
		StringBuilder redirect = new StringBuilder(150);
		redirect.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		redirect.append("?saveCnt=").append(saveCnt).append("&wwidCnt=").append(wwidCnt);
		redirect.append("&profileCnt=").append(profileCnt);
		return redirect.toString();
	}
	
	
	/**
	 * tap into the registration tables to transpose WWIDs to profileIds.
	 * @param wwids
	 * @param siteId
	 * @return
	 */
	private String[] loadProfileIds(Set<String> wwids, String siteId) {
		Set<String> profileIds = new HashSet<>(wwids.size());
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("select rs.profile_id from register_submittal rs ");
		sql.append("inner join register_data rd on rs.register_submittal_id=rd.register_submittal_id and rd.register_field_id=? ");
		sql.append("where cast(rd.value_txt as nvarchar(50)) in ('~'");
		for (@SuppressWarnings("unused") String s : wwids) sql.append(",?");
		sql.append(") and rs.site_id=?");
		log.debug(sql);
		
		int x = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(x++, HuddleConstants.WWID_REGISTER_FIELD_ID);
			for (String s : wwids) ps.setString(x++, s);
			ps.setString(x, siteId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				profileIds.add(rs.getString(1));
			
		} catch (SQLException sqle) {
			log.error("could not load profileIds from WWIDs", sqle);
		}
		log.debug("found " + profileIds.size() + " user accounts in the database");
		return profileIds.toArray(new String[profileIds.size()]);
	}
	
	
	/**
	 * calls ProfileRoleManager to save the new status for the profileIds given.
	 * @param siteId
	 * @param profileIds
	 * @return
	 */
	private int uploadRoles(String siteId, String[] profileIds) {
		int cnt = 0;
		if (profileIds == null || profileIds.length == 0) return cnt;
		
		ProfileRoleManager prm = new ProfileRoleManager();
		try {
			cnt = prm.changeRoleStatus(dbConn, SecurityController.STATUS_DISABLED, siteId, profileIds);
		} catch (DatabaseException de) {
			log.error("could not save profile_role records", de);
		}
		prm = null;
		log.debug("updatd " + cnt + " database records");
		return cnt;
	}

	
	/**
	 * iterates the incoming Excel file and returns a Set<String> that represents
	 * all the WWIDs of accounts that need to be expired.
	 * @param fpdb
	 * @return
	 * @throws InvalidDataException
	 */
	private Set<String> parseFile(FilePartDataBean fpdb) throws InvalidDataException {
		Set<String> wwids = new HashSet<>();
		try (ByteArrayInputStream bais = new ByteArrayInputStream(fpdb.getFileData())) {
			Workbook workbook = new XSSFWorkbook(bais);
			Sheet sheet = workbook.getSheet("Terms");
			if (sheet == null) throw new InvalidDataException("unknown file format");
			Iterator<Row> iterator = sheet.iterator();

			Double d = null;
			while (iterator.hasNext()) {
				Row nextRow = iterator.next();
				Cell wwidCell = nextRow.getCell(0);
				if (wwidCell == null || wwidCell.getCellType() == Cell.CELL_TYPE_BLANK) continue;

				switch (wwidCell.getCellType()) {
					case Cell.CELL_TYPE_STRING:
						d = Convert.formatDouble(wwidCell.getStringCellValue());
						break;
					case Cell.CELL_TYPE_NUMERIC:
						d = Convert.formatDouble(wwidCell.getNumericCellValue());
						break;
				}

				if (d > 0)
					wwids.add(d.toString());
			}
		} catch (Exception e) {
			throw new InvalidDataException(e);
		}
		log.debug("found " + wwids.size() + " rows in the Excel file");
		return wwids;
	}
}