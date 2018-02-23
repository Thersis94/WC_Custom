package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> RewardVO.java<br/>
 * <b>Description:</b> RezDox reward data container - see data model.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 23, 2018
 ****************************************************************************/
@Table(name="REZDOX_REWARD")
public class RewardVO {

	private String rewardId;
	private String rewardTypeCd;
	private String rewardTypeName;
	private String rewardName;
	private String slugTxt;
	private int pointsNo;
	private int orderNo;
	private int activeFlg;
	private String imageUrl;
	private double currencyValueNo;

	public RewardVO() {
		super();
	}


	/**
	 * Create a new VO using data auto-filled off the request.
	 * Request parameter names must match setter method names, sans the "set".
	 * e.g. setFirstName -> req.getParameter("firstName"); 
	 * @param req
	 * @return
	 */
	public static RewardVO instanceOf(ActionRequest req) {
		RewardVO vo = new RewardVO();
		BeanDataMapper.parseBean(vo, req.getParameterMap());
		return vo;
	}


	@Column(name="reward_id", isPrimaryKey=true)
	public String getRewardId() {
		return rewardId;
	}

	@Column(name="reward_type_cd")
	public String getRewardTypeCd() {
		return rewardTypeCd;
	}

	@Column(name="reward_nm")
	public String getRewardName() {
		return rewardName;
	}

	@Column(name="action_slug_txt")
	public String getSlugTxt() {
		return slugTxt;
	}

	@Column(name="point_value_no")
	public int getPointsNo() {
		return pointsNo;
	}

	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

	@Column(name="active_flg")
	public int getActiveFlg() {
		return activeFlg;
	}

	@Column(name="image_url")
	public String getImageUrl() {
		return imageUrl;
	}

	@Column(name="currency_value_no")
	public double getCurrencyValueNo() {
		return currencyValueNo;
	}

	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return Convert.getCurrentTimestamp();
	}

	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return Convert.getCurrentTimestamp();
	}

	/**
	 * populated from the rezdox_reward_type table if we join to it.
	 * @return
	 */
	@Column(name="type_nm", isReadOnly=true)
	public String getRewardTypeName() {
		return rewardTypeName;
	}

	public void setRewardId(String rewardId) {
		this.rewardId = rewardId;
	}

	public void setRewardTypeCd(String rewardTypeCd) {
		this.rewardTypeCd = rewardTypeCd;
	}

	public void setRewardName(String rewardName) {
		this.rewardName = rewardName;
	}

	public void setSlugTxt(String slugTxt) {
		this.slugTxt = slugTxt;
	}

	public void setPointsNo(int pointsNo) {
		this.pointsNo = pointsNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	public void setActiveFlg(int activeFlg) {
		this.activeFlg = activeFlg;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public void setCurrencyValueNo(double currencyValueNo) {
		this.currencyValueNo = currencyValueNo;
	}

	public void setRewardTypeName(String rewardTypeName) {
		this.rewardTypeName = rewardTypeName;
	}
}