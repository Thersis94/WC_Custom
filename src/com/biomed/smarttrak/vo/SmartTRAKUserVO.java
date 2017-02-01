package com.biomed.smarttrak.vo;

// Java 7
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;

/*****************************************************************************
 <p><b>Title</b>: SmartTRAKUserVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Jan 31, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class SmartTRAKUserVO extends UserDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8619730513300299951L;
	private String userId;
	private List<SmartTRAKTeamVO> teams;

	/**
	* Constructor
	*/
	public SmartTRAKUserVO() {
		teams = new ArrayList<>();
	}

	/**
	* Constructor
	*/
	public SmartTRAKUserVO(SMTServletRequest req) {
		super(req);
		teams = new ArrayList<>();
	}

	/**
	* Constructor
	*/
	public SmartTRAKUserVO(ResultSet rs) {
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
	 * @return the teams
	 */
	public List<SmartTRAKTeamVO> getTeams() {
		return teams;
	}

	/**
	 * @param teams the teams to set
	 */
	public void setTeams(List<SmartTRAKTeamVO> teams) {
		this.teams = teams;
	}
	
	/**
	 * Helper method for adding a team to the List of teams
	 * @param team
	 */
	public void addTeam(SmartTRAKTeamVO team) {
		teams.add(team);
	}

}
