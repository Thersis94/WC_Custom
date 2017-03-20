package com.biomed.smarttrak.vo;

import com.biomed.smarttrak.vo.GapCompanyVO.StatusVal;

/****************************************************************************
 * <b>Title</b>: GapCellVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO for managing Cell Data in the GapAnalysis table.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 16, 2017
 ****************************************************************************/
public class GapCellVO {

	private StatusVal usReg;
	private StatusVal ousReg;

	/**
	 * @param usReg
	 * @param ousReg
	 */
	public GapCellVO(StatusVal usReg, StatusVal ousReg) {
		this.usReg = usReg;
		this.ousReg = ousReg;
	}

	/**
	 * @return the usReg
	 */
	public StatusVal getUsReg() {
		return usReg;
	}

	/**
	 * @return the ousReg
	 */
	public StatusVal getOusReg() {
		return ousReg;
	}

	/**
	 * @param usReg the usReg to set.
	 */
	public void setUsReg(StatusVal usReg) {
		this.usReg = usReg;
	}

	/**
	 * @param ousReg the ousReg to set.
	 */
	public void setOusReg(StatusVal ousReg) {
		this.ousReg = ousReg;
	}

	/**
	 * Helper method returns Score.
	 * @return
	 */
	public int getScore() {
		return usReg.getScore() + ousReg.getScore();
	}
}