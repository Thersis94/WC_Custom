package com.rezdox.vo;

import java.util.Date;

import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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
@Table(name="REZDOX_MEMBER_REWARD")
public class MemberRewardVO {

	private String memberRewardId;
	private String memberId;
	private String rewardId;
	private int pointsNo;
	private double currencyValueNo;
	private int approvalFlg;
	private RewardVO reward;
	private Date createDate;
	private Date updateDate;

	public MemberRewardVO() {
		super();
	}

	/**
	 * 
	 * @param reward
	 * @param memberId
	 * @return
	 */
	public static MemberRewardVO instanceOf(RewardVO reward, String memberId) {
		MemberRewardVO vo = new MemberRewardVO();
		vo.setReward(reward);
		vo.setMemberId(memberId);
		// dollar and point values get captured/preserved "at the time of redemption" for historical purposes.
		vo.setCurrencyValueNo(reward.getCurrencyValueNo());
		vo.setPointsNo(reward.getPointsNo());

		return vo;
	}

	@Column(name="member_reward_id", isPrimaryKey=true)
	public String getMemberRewardId() {
		return memberRewardId;
	}

	@Column(name="member_id")
	public String getMemberId() {
		return memberId;
	}

	@Column(name="reward_id")
	public String getRewardId() {
		return rewardId;
	}

	@Column(name="point_value_no")
	public int getPointsNo() {
		return pointsNo;
	}

	@Column(name="currency_value_no")
	public double getCurrencyValueNo() {
		return currencyValueNo;
	}

	@Column(name="approval_flg")
	public int getApprovalFlg() {
		return approvalFlg;
	}

	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	public Date getRewardDate() {
		return updateDate != null ? updateDate : createDate;
	}

	public RewardVO getReward() {
		return reward;
	}

	public void setMemberRewardId(String memberRewardId) {
		this.memberRewardId = memberRewardId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public void setRewardId(String rewardId) {
		this.rewardId = rewardId;
	}

	public void setPointsNo(int pointsNo) {
		this.pointsNo = pointsNo;
	}

	public void setCurrencyValueNo(double currencyValueNo) {
		this.currencyValueNo = currencyValueNo;
	}

	public void setApprovalFlg(int approvalFlg) {
		this.approvalFlg = approvalFlg;
	}

	@BeanSubElement
	public void setReward(RewardVO reward) {
		this.reward = reward;
		setRewardId(reward.getRewardId());
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}