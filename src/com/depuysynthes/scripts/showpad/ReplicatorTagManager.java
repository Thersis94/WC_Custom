package com.depuysynthes.scripts.showpad;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.depuysynthes.scripts.MediaBinDeltaVO;

/****************************************************************************
 * <b>Title</b>: ReplicatorTagManager.java<p/>
 * <b>Description: overrides some superclass methods to adjust behavior for the replicator script's use of Tags.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 12, 2017
 ****************************************************************************/
public class ReplicatorTagManager extends ShowpadTagManager {

	/**
	 * @param apiUrl
	 * @param divisionId
	 * @param divisionUrl
	 * @param util
	 */
	public ReplicatorTagManager(String apiUrl, String divisionId, String divisionUrl, ShowpadApiUtil util) {
		super(apiUrl, divisionId, divisionUrl, util);
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.showpad.ShowpadTagManager#createTag(java.lang.String, java.lang.String)
	 */
	@Override
	protected ShowpadTagVO createTag(String tagNm, String externalId) {
		// we don't want to impose any SMT/Mediabin values - always save these w/null externalId.
		return super.createTag(tagNm, null);
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.showpad.ShowpadTagManager#checkExternalId(com.depuysynthes.scripts.showpad.ShowpadTagVO)
	 */
	@Override
	protected void checkExternalId(ShowpadTagVO tagVo) {
		return; //functionality not needed here
	}


	/*
	 * get desired tags from the SOURCE account
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.showpad.ShowpadTagManager#getDesiredTags(com.depuysynthes.scripts.MediaBinDeltaVO)
	 */
	@Override
	protected Set<String> getDesiredTags(MediaBinDeltaVO vo) {
		return vo.getReplicatorDesiredTags();
	}


	/*
	 * (non-Javadoc)
	 * @see com.depuysynthes.scripts.showpad.ShowpadTagManager#deleteUnwantedMBTags(com.depuysynthes.scripts.MediaBinDeltaVO, java.util.Map, java.util.Set)
	 */
	@Override
	protected void deleteUnwantedMBTags(MediaBinDeltaVO vo, Map<String, ShowpadTagVO> assignedTags,
			Set<String> desiredTags) {
		//if the asset had no existing tags, we're done.  There aren't any we need to worry about removing
		if (assignedTags == null || assignedTags.isEmpty()) return;

		//fire some tag deletions to this existing asset
		//make a 'tagsToDelete' list by subtracting what we want (to add) from what we have
		Map<String, ShowpadTagVO> tagsToDelete = new HashMap<>(assignedTags);

		//don't delete any we want
		for (String tagNm : desiredTags)
			tagsToDelete.remove(tagNm);

		unlinkAssetFromTags(vo.getShowpadId(), tagsToDelete.values());
	}
}