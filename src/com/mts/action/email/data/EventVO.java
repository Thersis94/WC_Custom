package com.mts.action.email.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: EventVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Wrapper class foe the Squarespace Events page
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 18, 2020
 * @updates:
 ****************************************************************************/
public class EventVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1082620256530136626L;
	
	private List<EmailEventVO> upcoming = new ArrayList<>();

	/**
	 * 
	 */
	public EventVO() {
		super();
	}

	/**
	 * @param req
	 */
	public EventVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public EventVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * 
	 * @param vo
	 */
	public void addUpcoming(EmailEventVO vo) {
		if (vo != null) upcoming.add(vo);
	}

	/**
	 * @return the upcoming
	 */
	public List<EmailEventVO> getUpcoming() {
		return upcoming;
	}

	/**
	 * @param upcoming the upcoming to set
	 */
	public void setUpcoming(List<EmailEventVO> upcoming) {
		this.upcoming = upcoming;
	}

}
