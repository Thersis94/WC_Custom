package com.biomed.smarttrak.vo;

// Java 7
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;

/*****************************************************************************
 <p><b>Title</b>: SmarttrakUserVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 31, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class SmarttrakUserVO extends UserDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8619730513300299951L;
	private String accountId;
	private String userId;
	private String registerSubmittalId;
	private List<SmarttrakTeamVO> teams;

	/**
	* Constructor
	*/
	public SmarttrakUserVO() {
		teams = new ArrayList<>();
	}

	/**
	* Constructor
	*/
	public SmarttrakUserVO(SMTServletRequest req) {
		super(req);
		teams = new ArrayList<>();
	}

	/**
	* Constructor
	*/
	public SmarttrakUserVO(ResultSet rs) {
		super(rs);
		teams = new ArrayList<>();
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the registerSubmittalId
	 */
	public String getRegisterSubmittalId() {
		return registerSubmittalId;
	}

	/**
	 * @param registerSubmittalId the registerSubmittalId to set
	 */
	public void setRegisterSubmittalId(String registerSubmittalId) {
		this.registerSubmittalId = registerSubmittalId;
	}

	/**
	 * @return the teams
	 */
	public List<SmarttrakTeamVO> getTeams() {
		return teams;
	}

	/**
	 * @param teams the teams to set
	 */
	public void setTeams(List<SmarttrakTeamVO> teams) {
		this.teams = teams;
	}
	
	/**
	 * Helper method for adding a team to the List of teams
	 * @param team
	 */
	public void addTeam(SmarttrakTeamVO team) {
		teams.add(team);
	}

}
