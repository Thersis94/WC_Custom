package com.depuysynthes.action;

import java.util.List;

import com.depuysynthes.action.MediaBinDistChannels.DistChannel;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.content.MediaListAction;
import com.smt.sitebuilder.action.content.MediaListQueryIntfc;
import static com.smt.sitebuilder.action.content.MediaListAction.SEARCH_PARAM;

/****************************************************************************
 * <b>Title</b>: MediaBinMediaListQuery.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 16, 2017
 ****************************************************************************/
public class MediaBinMediaListQuery implements MediaListQueryIntfc {

	public MediaBinMediaListQuery() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.content.MediaListQueryIntfc#addSelectFields(java.lang.StringBuilder)
	 */
	@Override
	public void addSelectFields(StringBuilder sql) {
		sql.append("select 'mediabin' as type, dpy_syn_mediabin_id as id, file_nm as name, title_txt as description, "); 
		sql.append("orig_file_size_no as file_size, dpy_syn_mediabin_id as secondary_id, ");
		sql.append("modified_dt as last_modified, '/json{0}amid=MEDIA_BIN_AJAX&mbid=' + dpy_syn_mediabin_id as file_path, ");
		sql.append("cast(coalesce(substring(NULLIF(dimensions_txt, '') from 0 for position('~' in NULLIF(dimensions_txt, ''))), '0') as integer) as width, ");
		sql.append("cast(coalesce(substring(NULLIF(dimensions_txt, '') from position('~' in NULLIF(dimensions_txt, '')) + 1), '0') as integer) as height, ");
		sql.append("'video/mp4' as mime_type, null as poster_url ");
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.content.MediaListQueryIntfc#addTableNames(java.lang.StringBuilder, String customSchema)
	 */
	@Override
	public void addTableNames(StringBuilder sql, String customSchema) {
		sql.append("from ").append(customSchema).append("dpy_syn_mediabin ");
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.content.MediaListQueryIntfc#addWhereClause(java.lang.StringBuilder, com.siliconmtn.action.ActionRequest, java.util.List, java.util.List)
	 */
	@Override
	public void addWhereClause(StringBuilder sql, ActionRequest req, List<Object> params, List<String> linkFilter) {
		sql.append("where 1=1 ");

		// Add a filter for the image dialog
		if (Type.VIDEO.toString().equalsIgnoreCase(req.getParameter(MediaListAction.FILE_TYPE))) {
			sql.append(" and lower(asset_type) in (");
			for (int x=MediaBinAdminAction.VIDEO_ASSETS.length; x > 0; x--) {
				sql.append( x == MediaBinAdminAction.VIDEO_ASSETS.length ? "?" : ",?");
				params.add(MediaBinAdminAction.VIDEO_ASSETS[x-1]);
			}
			sql.append(")");
		} else if  (linkFilter.contains(Filter.PDF.reqVal())) {
			sql.append(" and lower(asset_type) in (");
			for (int x=MediaBinAdminAction.PDF_ASSETS.length; x > 0; x--) {
				sql.append( x == MediaBinAdminAction.PDF_ASSETS.length ? "?" : ",?");
				params.add(MediaBinAdminAction.PDF_ASSETS[x-1]);
			}
			sql.append(")");
		}

		//filter by dist channel & opcoNm
		DistChannel channel = MediaBinDistChannels.getByOrgId(req.getParameter("organizationId"));
		sql.append("and import_file_cd=? and opco_nm like ? ");
		params.add(channel.getTypeCd());
		params.add("%" + channel.getChannel() + "%");

		// Implement the search filter
		if (! StringUtil.isEmpty(req.getParameter(SEARCH_PARAM))) {
			String searchVal = "%" + StringUtil.checkVal(req.getParameter(SEARCH_PARAM)).toLowerCase() + "%";

			sql.append("and (lower(tracking_no_txt) like ? or lower(prod_nm) like ? or lower(prod_family) like ?) ");
			params.add(searchVal);
			params.add(searchVal);
			params.add(searchVal);
		}

		//implement the opco filter
		if (req.hasParameter("opco") && req.getParameter("opco").indexOf("Companies") == -1) {
			sql.append("and lower(business_unit_nm)=? ");
			params.add(req.getParameter("opco").trim().toLowerCase());
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.content.MediaListQueryIntfc#matchesFilter(java.lang.String, java.util.List)
	 */
	@Override
	public boolean matchesFilter(String fileType, List<String> linkFilters) {
		return linkFilters.contains(Filter.DMB.reqVal()) && !Type.IMAGE.toString().equalsIgnoreCase(fileType);
	}
}