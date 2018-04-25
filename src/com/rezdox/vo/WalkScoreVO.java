package com.rezdox.vo;

// JDK 1.8.x
import java.io.Serializable;
import java.util.Date;

// GSON 2.8.x
import com.google.gson.annotations.SerializedName;

// SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: WalkScoreVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object storing the data returned from the walkscore api
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 1, 2018
 * @updates:
 ****************************************************************************/

public class WalkScoreVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4714069265723551494L;
	
	// Member Variables
	private int status;
	private int walkscore;
	private String description;
	private Date updated;
	private Transport transit;
	private Transport bike;
	
	@SerializedName("logo_url")
	private String logoUrl;
	
	@SerializedName("more_info_icon")
	private String moreInfoIcon;
	
	@SerializedName("more_info_link")
	private String moreInfoLink;
	
	@SerializedName("ws_link")
	private String wsLink;
	
	@SerializedName("help_link")
	private String helpLink;
	
	@SerializedName("snapped_lat")
	private String snappedLatitude;
	
	@SerializedName("snapped_lon")
	private String snappedLongitude;

	/**
	 * 
	 */
	public WalkScoreVO() {
		super();
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * @return the walkscore
	 */
	public int getWalkscore() {
		return walkscore;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the updated
	 */
	public Date getUpdated() {
		return updated;
	}

	/**
	 * @return the logoUrl
	 */
	public String getLogoUrl() {
		return logoUrl;
	}

	/**
	 * @return the moreInfoIcon
	 */
	public String getMoreInfoIcon() {
		return moreInfoIcon;
	}

	/**
	 * @return the moreInfoLink
	 */
	public String getMoreInfoLink() {
		return moreInfoLink;
	}

	/**
	 * @return the wsLink
	 */
	public String getWsLink() {
		return wsLink;
	}

	/**
	 * @return the helpLink
	 */
	public String getHelpLink() {
		return helpLink;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @param walkscore the walkscore to set
	 */
	public void setWalkscore(int walkscore) {
		this.walkscore = walkscore;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param updated the updated to set
	 */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/**
	 * @param logoUrl the logoUrl to set
	 */
	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	/**
	 * @param moreInfoIcon the moreInfoIcon to set
	 */
	public void setMoreInfoIcon(String moreInfoIcon) {
		this.moreInfoIcon = moreInfoIcon;
	}

	/**
	 * @param moreInfoLink the moreInfoLink to set
	 */
	public void setMoreInfoLink(String moreInfoLink) {
		this.moreInfoLink = moreInfoLink;
	}

	/**
	 * @param wsLink the wsLink to set
	 */
	public void setWsLink(String wsLink) {
		this.wsLink = wsLink;
	}

	/**
	 * @param helpLink the helpLink to set
	 */
	public void setHelpLink(String helpLink) {
		this.helpLink = helpLink;
	}
	

	/**
	 * @return the transit
	 */
	public Transport getTransit() {
		return transit;
	}

	/**
	 * @return the bike
	 */
	public Transport getBike() {
		return bike;
	}

	/**
	 * @return the snappedLatitude
	 */
	public String getSnappedLatitude() {
		return snappedLatitude;
	}

	/**
	 * @return the snappedLongitude
	 */
	public String getSnappedLongitude() {
		return snappedLongitude;
	}

	/**
	 * @param transit the transit to set
	 */
	public void setTransit(Transport transit) {
		this.transit = transit;
	}

	/**
	 * @param bike the bike to set
	 */
	public void setBike(Transport bike) {
		this.bike = bike;
	}

	/**
	 * @param snappedLatitude the snappedLatitude to set
	 */
	public void setSnappedLatitude(String snappedLatitude) {
		this.snappedLatitude = snappedLatitude;
	}

	/**
	 * @param snappedLongitude the snappedLongitude to set
	 */
	public void setSnappedLongitude(String snappedLongitude) {
		this.snappedLongitude = snappedLongitude;
	}
	
	/**
	 * 
	 */
	public class Transport implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4185511498472831751L;
		
		// Members
		private int score;
		private String description;
		private String summary;
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return StringUtil.getToString(this);
		}
		
		/**
		 * @return the score
		 */
		public int getScore() {
			return score;
		}
		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
		/**
		 * @return the summary
		 */
		public String getSummary() {
			return summary;
		}
		/**
		 * @param score the score to set
		 */
		public void setScore(int score) {
			this.score = score;
		}
		/**
		 * @param description the description to set
		 */
		public void setDescription(String description) {
			this.description = description;
		}
		/**
		 * @param summary the summary to set
		 */
		public void setSummary(String summary) {
			this.summary = summary;
		}
	}
}



