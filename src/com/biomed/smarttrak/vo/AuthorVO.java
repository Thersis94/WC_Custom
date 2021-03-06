package com.biomed.smarttrak.vo;

//smt base libs
import com.siliconmtn.action.ActionRequest;

//sb libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;

/****************************************************************************
 * Title: AuthorVO.java <p/>
 * Project: WC_Custom <p/>
 * Description: Data container that handles the common attributes for an Author<p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since May 8, 2017
 ****************************************************************************/
public class AuthorVO extends SecureSolrDocumentVO {

	protected static final String CREATOR_PROFILE_ID = "creatorProfileId";

	protected String creatorProfileId;
	
	public AuthorVO() {
		super(null);
	}

	/**
	 * @param solrIndex
	 */
	public AuthorVO(String solrIndex) {
		super(solrIndex);
	}

	/**
	 * Sets the common data for an author using the request object
	 * @param req
	 */
	protected void setData(ActionRequest req) {
		if (req.hasParameter(CREATOR_PROFILE_ID)) {
			setCreatorProfileId(req.getParameter(CREATOR_PROFILE_ID));
		} else { //attempt to retrieve from the session if not on request
			UserVO vo = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
			if(vo != null)
				setCreatorProfileId(vo.getProfileId());
		}
	}

	/**
	 * @return the creatorProfileId
	 */
	public String getCreatorProfileId() {
		return creatorProfileId;
	}

	/**
	 * @param creatorProfileId the creatorProfileId to set
	 */
	public void setCreatorProfileId(String creatorProfileId) {
		this.creatorProfileId = creatorProfileId;
	}
}
