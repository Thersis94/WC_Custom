package com.fastsigns.action.franchise.approval;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpSession;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: WhiteBoardLogVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Dec 30, 2011<p/>
 * <b>Changes: Migrated all Approval action code to new package and updated code 
 * to use new workflow.  </b>
 ****************************************************************************/
@Deprecated
public class WhiteBoardLogVO extends ChangeLogVO {

	public WhiteBoardLogVO(){
		
	}
	
	public WhiteBoardLogVO(SMTServletRequest req) {
		HttpSession ses = req.getSession();
    	UserRoleVO role = (UserRoleVO) ses.getAttribute(Constants.ROLE_DATA);
     	componentId = ApprovalAction.getComponentId(req)[0];
    	typeId = req.getParameter("cmpType");
    	submitterId = role.getProfileId();
    	descTxt = req.getParameter("subComments");
    	statusNo = Status.PENDING.ordinal();
    }

	public WhiteBoardLogVO(ResultSet rs) throws SQLException {
		ftsChangelogId = rs.getString("FTS_CHANGELOG_ID");
		componentId = rs.getString("COMPONENT_ID");
		typeId = rs.getString("TYPE_ID");
		submitterId = rs.getString("SUBMITTER_ID");
		descTxt = rs.getString("DESC_TXT");
		submittedDate = rs.getDate("SUBMITTED_DT");
		statusNo = rs.getInt("STATUS_NO");
	}
}
