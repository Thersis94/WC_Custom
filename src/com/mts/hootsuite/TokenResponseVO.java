package com.mts.hootsuite;
/****************************************************************************
 * <b>Title</b>: TokenResponseVO.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO for the Refresh Token response
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 15, 2020
 * @updates:
 ****************************************************************************/
public class TokenResponseVO {
	private String error;
	private String error_description;
	private String error_hint;
	private int status_code;
	private String access_token;
	private int expires_in;
	private String refresh_token;
	private String scope;
	private String token_type;
	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}
	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}
	/**
	 * @return the error_description
	 */
	public String getError_description() {
		return error_description;
	}
	/**
	 * @param error_description the error_description to set
	 */
	public void setError_description(String error_description) {
		this.error_description = error_description;
	}
	/**
	 * @return the error_hint
	 */
	public String getError_hint() {
		return error_hint;
	}
	/**
	 * @param error_hint the error_hint to set
	 */
	public void setError_hint(String error_hint) {
		this.error_hint = error_hint;
	}
	/**
	 * @return the access_token
	 */
	public String getAccess_token() {
		return access_token;
	}
	/**
	 * @param access_token the access_token to set
	 */
	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}
	/**
	 * @return the expires_in
	 */
	public int getExpires_in() {
		return expires_in;
	}
	/**
	 * @param expires_in the expires_in to set
	 */
	public void setExpires_in(int expires_in) {
		this.expires_in = expires_in;
	}
	/**
	 * @return the refresh_token
	 */
	public String getRefresh_token() {
		return refresh_token;
	}
	/**
	 * @param refresh_token the refresh_token to set
	 */
	public void setRefresh_token(String refresh_token) {
		this.refresh_token = refresh_token;
	}
	/**
	 * @return the scope
	 */
	public String getScope() {
		return scope;
	}
	/**
	 * @param scope the scope to set
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}
	/**
	 * @return the token_type
	 */
	public String getToken_type() {
		return token_type;
	}
	/**
	 * @param token_type the token_type to set
	 */
	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}
	/**
	 * @return the status_code
	 */
	public int getStatus_code() {
		return status_code;
	}
	/**
	 * @param status_code the status_code to set
	 */
	public void setStatus_code(int status_code) {
		this.status_code = status_code;
	}
}
