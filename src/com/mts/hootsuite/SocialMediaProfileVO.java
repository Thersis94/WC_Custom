package com.mts.hootsuite;

//SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: SocialMediaProfileVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> Social media profile VO holds descriptive information such as (id, type, socialNetworkId, socialNetworkUsername, avaterUrl, owner, ownerId)
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 19, 2020
 * @updates:
 ****************************************************************************/
public class SocialMediaProfileVO extends BeanDataVO {
	
	private String id;
	private String type;
	private String socialNetworkId;
	private String socialNetworkUsername;
	private String avatarUrl;
	private String owner;
	private String ownerId;
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the socialNetworkId
	 */
	public String getSocialNetworkId() {
		return socialNetworkId;
	}
	/**
	 * @param socialNetworkId the socialNetworkId to set
	 */
	public void setSocialNetworkId(String socialNetworkId) {
		this.socialNetworkId = socialNetworkId;
	}
	/**
	 * @return the socialNetworkUsername
	 */
	public String getSocialNetworkUsername() {
		return socialNetworkUsername;
	}
	/**
	 * @param socialNetworkUsername the socialNetworkUsername to set
	 */
	public void setSocialNetworkUsername(String socialNetworkUsername) {
		this.socialNetworkUsername = socialNetworkUsername;
	}
	/**
	 * @return the avatarUrl
	 */
	public String getAvatarUrl() {
		return avatarUrl;
	}
	/**
	 * @param avatarUrl the avatarUrl to set
	 */
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}
	/**
	 * @return the owner
	 */
	public String getOwner() {
		return owner;
	}
	/**
	 * @param owner the owner to set
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}
	/**
	 * @return the ownerId
	 */
	public String getOwnerId() {
		return ownerId;
	}
	/**
	 * @param ownerId the ownerId to set
	 */
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
}
