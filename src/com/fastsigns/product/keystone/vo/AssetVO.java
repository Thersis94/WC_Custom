package com.fastsigns.product.keystone.vo;

import java.io.Serializable;

/****************************************************************************
 * <b>Title</b>: ProofVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 13, 2012
 ****************************************************************************/
public class AssetVO extends ImageVO implements Serializable {
	private static final long serialVersionUID = -7247504603290550856L;
	private String proof_id = null;
	private String status = null;
	private String description = null;
	
	public String getProof_id() {
		return proof_id;
	}
	public void setProof_id(String proof_id) {
		this.proof_id = proof_id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
