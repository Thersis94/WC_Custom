package com.fastsigns.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ChangeLogVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

public class LogAction extends SBActionAdapter{
	
	public static enum LogTypeKey {
		ctrWhiteBrd("com.fastsigns.action.approval.vo.WhiteBoardLogVO"),
		sitePg("com.fastsigns.action.approval.vo.PageLogVO"),
		ctrPgModule("com.fastsigns.action.approval.vo.ModuleLogVO"),
		ctrImage("com.fastsigns.action.approval.vo.CenterImageLogVO"),
		jobPosting("com.fastsigns.action.approval.vo.CareerLogVO"),
		pageModule("com.fastsigns.action.approval.vo.PageModuleLogVO");
		  
		LogTypeKey (String classPath) {
		   this.classPath = classPath;
		  }
		  
		  String classPath;

		  public String getClassPath() {
		   return classPath;
		  }

		 }

	public LogAction(){
		
	}
	
	public LogAction(ActionInitVO actionInit){
		this.actionInit = actionInit;

	}
	
	private void updateVO(AbstractChangeLogVO vo, SMTServletRequest req){
		HttpSession ses = req.getSession();
    	UserRoleVO role = (UserRoleVO) ses.getAttribute(Constants.ROLE_DATA);
    	vo.setReviewerId(role.getProfileId());
    	if(req.hasParameter("revComments"))
    		vo.setResolutionTxt(StringUtil.checkVal(req.getParameter("revComments")));
    	if(vo.getResolutionTxt() == null || vo.getResolutionTxt().length() == 0)
    		vo.setResolutionTxt("Approved");
    	vo.setFranchiseId(req.getParameter("apprFranchiseId"));
	}
	
	/**
	 * Updates the changelog when a franchise updates or an admin approves/denys
	 * a centerimage, centerpage or page module.
	 * @param vo
	 * @param req
	 * @return
	 */
	public void updateChangeLogs(List<AbstractChangeLogVO> vos, SMTServletRequest req){
		log.debug("Beginning update ChangeLog...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(customDb).append("FTS_CHANGELOG set REVIEWER_ID = ?, RESOLUTION_TXT = ?, ");
		sb.append("REVIEW_DT = ?, STATUS_NO = ?, UPDATE_DT = ? where FTS_CHANGELOG_ID = ?");
		PreparedStatement ps = null;
		for(AbstractChangeLogVO vo : vos){
			if(vo.getStatusNo() != AbstractChangeLogVO.Status.PENDING.ordinal())
				updateVO(vo, req);
		}
		try{
			ps = dbConn.prepareStatement(sb.toString());
			for(AbstractChangeLogVO vo : vos){
				ps.setString(1, vo.getReviewerId());
				ps.setString(2, vo.getResolutionTxt());
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.setInt(4, vo.getStatusNo());
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.setString(6, vo.getFtsChangelogId());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
	        log.error(sqle);
		} finally {
			try { ps.close();} catch(Exception e) {}
	    }
	}
	
	public void logChange(SMTServletRequest req, AbstractChangeLogVO vo){
		List<AbstractChangeLogVO> list = new ArrayList<AbstractChangeLogVO>();
		list.add(vo);
		logChange(req, list);
	}
	/**
	 * Checks to see if a changelog exists and then forwards to either the create
	 * or update methods below.
	 * @param req
	 * @return
	 */
	public void logChange(SMTServletRequest req, List<AbstractChangeLogVO> vos){
		log.debug("Editing changelog...");
		List<AbstractChangeLogVO> uploads = new ArrayList<AbstractChangeLogVO>();
		List<AbstractChangeLogVO> creates = new ArrayList<AbstractChangeLogVO>();
		if(vos != null){
			Map<String, AbstractChangeLogVO> vals = convertList(vos);
				String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
				StringBuilder sb = new StringBuilder();
				sb.append("select * from ").append(customDb).append("FTS_CHANGELOG where COMPONENT_ID = ? and STATUS_NO = 0 and TYPE_ID = ? ");
				for(int i = 1; i < vos.size(); i++){
					sb.append("union ");
					sb.append("select * from ").append(customDb).append("FTS_CHANGELOG where COMPONENT_ID = ? and STATUS_NO = 0 and TYPE_ID = ? ");
				}
				PreparedStatement ps = null;
				log.debug(sb.toString());
				try{
					int i = 1;
					ps = dbConn.prepareStatement(sb.toString());
					for(AbstractChangeLogVO vo : vos){
					ps.setString(i++, vo.getComponentId());
					ps.setString(i++, vo.getTypeId());
					ps.addBatch();
					}
					ResultSet rs = ps.executeQuery();
					while(rs.next()){
						AbstractChangeLogVO v = vals.get(rs.getString("COMPONENT_ID"));
						if(v != null){
							log.debug(rs.getString("FTS_CHANGELOG_ID"));
							v.setData(rs);
							uploads.add(v);
							vals.remove(rs.getString("COMPONENT_ID"));
						}
					}
					for(AbstractChangeLogVO vo : vals.values())
						creates.add(vo);
					
				}catch(SQLException sqle){
					log.error(sqle);
				} finally{
					try{ps.close();}
					catch(Exception e){
						log.debug(e);
					}
					}
				if(uploads.size() > 0)
					updateChangeLogs(uploads, req);
				if(creates.size() > 0)
					createChangeLogs(creates);
			}
		}
	/**
	 * Creates a changelog entry for the provided AbstractChangeLogVO
	 * @param vo
	 * @return
	 */
	public void createChangeLogs(List<AbstractChangeLogVO> vos){
		log.debug("Beginning create ChangeLog...");
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb.append("insert into ").append(customDb).append("FTS_CHANGELOG (FTS_CHANGELOG_ID, COMPONENT_ID, TYPE_ID,");
		sb.append(" SUBMITTER_ID, STATUS_NO, DESC_TXT, SUBMITTED_DT) values (?,?,?,?,?,?,?)");
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			for(AbstractChangeLogVO vo : vos){
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, vo.getComponentId());
			ps.setString(3, vo.getTypeId());
			ps.setString(4, vo.getSubmitterId());
			ps.setInt(5, vo.getStatusNo());
			ps.setString(6, vo.getDescTxt());
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.addBatch();
			}
			ps.executeBatch();
	
		} catch (SQLException sqle) {
	           log.debug(sqle);
	           return;
		} finally {
			try { ps.close(); } catch(Exception e) {}
	    }
	}
	
	/**
	 * Removes all pending changes from the ChangeLog
	 */
	public void cleanPendingChangelog(){
		//cleanup log
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(customDb);
		sb.append("FTS_CHANGELOG where status_no = 0");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			int x = ps.executeUpdate();
			log.debug("purged " + x + " pending changelog records");
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Removes all Changelog Entries for the given ID's
	 * @param ids String [] of ChangeLog Id's that need to be removed.
	 */
	public void deleteFromChangelog(List<String> ids){
		//cleanup log
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(customDb);
		sb.append("FTS_CHANGELOG where component_id =?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for (int x=0; x < ids.size(); x++) {
				ps.setString(1, ids.get(x));
				ps.addBatch();
			}
			ps.executeBatch();
			log.debug("purged " + ids.size() + " changelog records");
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	/**
	 * Returns either a complete list of all logs in the Changelog Table or a 
	 * truncated list of pending changelogs only.
	 * @param pendingOnly return all ChangeLogs or just those pending.
	 * @return
	 */
	public List<AbstractChangeLogVO> getLogs(boolean pendingOnly, SMTServletRequest req){
		Map<String, Object> config = new HashMap<String, Object>();
		config.put("encKey", (String) attributes.get(Constants.ENCRYPT_KEY));
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
		} catch (EncryptionException e1) {
			e1.printStackTrace();
		}
		removeOrphanedEntries();
		StringBuilder sb = new StringBuilder();
		List<AbstractChangeLogVO> vos = new ArrayList<AbstractChangeLogVO>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		log.debug("Retrieving logs");
		sb.append("select a.type_id, a.desc_txt, a.submitted_dt, a.update_dt, ");
		sb.append("a.submitter_id, b.franchise_id as modfranchise_id, b.option_nm, ");
		sb.append("b.option_desc, c.new_center_image_url, c.NEW_WHITE_BOARD_TEXT, ");
		sb.append("c.FRANCHISE_ID as franchise_id, d.SITE_ID, d.PAGE_DISPLAY_NM, ");
		sb.append("e.JOB_TITLE_NM, f.PAGE_MODULE_ID, g.ACTION_NM ");
		sb.append("from ").append(customDb).append("FTS_CHANGELOG a left outer join ").append(customDb);
		sb.append("FTS_CP_MODULE_OPTION b on a.COMPONENT_ID = Cast(b.cp_module_option_id as nvarchar(32)) ");
		sb.append("left outer join PAGE_MODULE f on f.PAGE_MODULE_ID = a.COMPONENT_ID ");
		sb.append("left outer join PAGE d on a.COMPONENT_ID = d.PAGE_ID or f.PAGE_ID = d.PAGE_ID left outer join "); 
		sb.append(customDb).append("FTS_JOB_POSTING e on a.COMPONENT_ID = e.JOB_POSTING_ID ");
		sb.append("left outer join SB_ACTION g on g.ACTION_ID = f.ACTION_ID ");
		sb.append("left outer join ").append(customDb).append("FTS_FRANCHISE c ");
		sb.append("on c.FRANCHISE_ID = b.FRANCHISE_ID ");
		sb.append("or Cast(c.FRANCHISE_ID as nvarchar(32)) = a.COMPONENT_ID ");
		sb.append("or c.FRANCHISE_ID = e.FRANCHISE_ID ");
		
		if(pendingOnly)
			sb.append(" where status_no = " + AbstractChangeLogVO.Status.PENDING.ordinal() + " ");
		sb.append("order by update_dt desc, submitted_dt desc");
		PreparedStatement ps = null;
		log.debug("sql: " + sb);
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				
				AbstractChangeLogVO vo = getChangeLogVO(rs.getString("TYPE_ID"), rs);
				try{
					UserDataVO p = pm.getProfile(vo.getSubmitterId(), dbConn, "profile_id");
					vo.setSubmitterName(StringUtil.checkVal(se.decrypt(p.getFirstName())) + " " + StringUtil.checkVal(se.decrypt(p.getLastName())));
				} catch (EncryptionException e) {
					log.debug(e);
				} catch (IllegalArgumentException e) {
					log.debug(e);
				} catch (DatabaseException e) {
					log.debug(e);
				}
				vos.add(vo);
			}
		} catch(SQLException sqle){
			log.debug(sqle);
		} finally {
			try { ps.close(); } catch(Exception e) {}
	    }
		return vos;
	}
	
	/**
	 * Take the List of Vos and convert them into a Map for ease of access.
	 * @param vos
	 * @return
	 */
	private Map<String, AbstractChangeLogVO> convertList(List<AbstractChangeLogVO> vos){
		Map<String, AbstractChangeLogVO> vals = new HashMap<String, AbstractChangeLogVO>();
		for(AbstractChangeLogVO vo: vos)
			vals.put(vo.getComponentId(), vo);
		return vals;
	}
	
	/**
	 * Factory Method that returns the appropriate ChangeLogVO.
	 * 
	 * @param req
	 * @return
	 */
	private AbstractChangeLogVO getChangeLogVO(String typeId, ResultSet rs) {
		ChangeLogVO cL = null;
		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(LogTypeKey.valueOf(typeId).getClassPath());
			cL = (ChangeLogVO) load.newInstance();
			cL.setData(rs);
		} catch (ClassNotFoundException cnfe) {
			log.error("Unable to find className", cnfe);
		} catch (Exception e) {
			log.error("Unable to create ChangeLog.", e);
		}
		return cL;
	}
	
	/**
	 * String to remove all orphaned Page Changelogs. 
	 * @return
	 */
	private final String orphanPagesRemoval(){
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FTS_CHANGELOG where STATUS_NO=0 and TYPE_ID='sitePg' and not exists");
		sb.append("(select * from PAGE where PAGE_ID = COMPONENT_ID and ");
		sb.append("LIVE_START_DT > '2100-01-01 00:00:00.000')");
		return sb.toString();
	}
	/**
	 * String to remove all orphaned Module Changelogs. 
	 * @return
	 */
	private final String orphanModuleRemoval(){
		StringBuilder sb = new StringBuilder();
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sb.append("delete from ").append(customDb).append("FTS_CHANGELOG where STATUS_NO=0 and ");
		sb.append("TYPE_ID='ctrPgModule' and not exists(select * from  ").append(customDb); 
		sb.append("fts_cp_module_option a inner join ").append(customDb).append("fts_cp_module_franchise_xr b ");
		sb.append("on a.cp_module_option_id=b.cp_module_option_id or a.parent_id=b.cp_module_option_id ");
		sb.append("inner join ").append(customDb).append("fts_cp_location_module_xr c on ");
		sb.append("c.cp_location_module_xr_id=b.cp_location_module_xr_id where a.approval_flg=100 ");
		sb.append("and b.CP_MODULE_OPTION_ID = COMPONENT_ID)");
		
		return sb.toString();
	}
	
	/**
	 * Before we retrieve a list of pending changelogs, we want to remove all 
	 * entries that have been fulfilled through other means such as the
	 * Admintool.
	 */
	private void removeOrphanedEntries(){
		PreparedStatement ps = null;
		try{
				ps = dbConn.prepareStatement(orphanPagesRemoval());
				ps.execute();
				ps.close();
				
				log.debug(ps.getUpdateCount() + " Pages Removed");
				ps = dbConn.prepareStatement(orphanModuleRemoval());
				ps.execute();
				log.debug(ps.getUpdateCount() + " Modules Removed");
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			try{ps.close();}catch(Exception e){}
		}
	}
}
