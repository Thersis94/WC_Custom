package com.mts.hootsuite;

import java.util.ArrayList;
import java.util.List;

/****************************************************************************
 * <b>Title</b>: HootsuitePostsVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO for holding PostVOs and delivering post information in hootsuite apropriate formats
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since Jun 18, 2020
 * @updates:
 ****************************************************************************/
public class HootsuitePostsVO {

	List<PostVO> posts = new ArrayList<>();

	/**
	 * @return the posts
	 */
	public List<PostVO> getPosts() {
		return posts;
	}

	/**
	 * @param posts the posts to set
	 */
	public void setPosts(List<PostVO> posts) {
		this.posts = posts;
	}
	
	/**
	 * Adds a post to the posts List
	 * @param post
	 */
	public void addPost(PostVO post) {
		posts.add(post);
	}
	
	
	
	
}
