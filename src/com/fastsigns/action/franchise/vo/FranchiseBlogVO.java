package com.fastsigns.action.franchise.vo;

import com.smt.sitebuilder.action.blog.BlogGroupVO;

/****************************************************************************
 * <b>Title</b>FranchiseBlogVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @since Jul 2, 2015
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseBlogVO extends BlogGroupVO{

	private static final long serialVersionUID = 1L;
	private FranchiseVO franchise = null;

	/**
	 * @return the franchise
	 */
	public FranchiseVO getFranchise() {
		return franchise;
	}

	/**
	 * @param franchise the franchise to set
	 */
	public void setFranchise(FranchiseVO franchise) {
		this.franchise = franchise;
	}
	
	/**
	 * Copy data from a BlogGroupVO into this vo
	 * @param vo
	 */
	public void setData(BlogGroupVO vo){
		this.setBlogs(vo.getBlogs());
		this.setBlogPath(vo.getBlogPath());
		this.setLocale(vo.getLocale());
		this.setBloggerId(vo.getBloggerId());
		this.setArchiveMonth(vo.getArchiveMonth());
		this.setArchiveYear(vo.getArchiveYear());
		this.setCategoryUrl(vo.getCategoryUrl());
		this.setCategoryName(vo.getCategoryName());
		this.setCommentsActionId(vo.getCommentsActionId());
		this.setSearchParams(vo.getSearchParams());
	}
}
