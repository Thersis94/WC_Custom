package com.depuysynthes.huddle;

import java.text.SimpleDateFormat;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;

public class BriefcaseItemVO {
	
	private String itemId;
	private String type;
	private String imageUrl;
	private String name;
	private String url;
	private String vtt;
	private String checksum;
	private String createDt;
	
	public BriefcaseItemVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		setItemId(db.getStringVal("PROFILE_FAVORITE_ID", rs));
		setType(db.getStringVal("ASSET_TYPE", rs));
		setName(db.getStringVal("ASSET_NM", rs));

		setImageUrl(db.getStringVal("ASSET_TYPE", rs));
		setUrl(db.getStringVal("PROFILE_FAVORITE_ID", rs));
		setVtt(db.getStringVal("PROFILE_FAVORITE_ID", rs));
		
		setChecksum(db.getDateVal("CREATE_DT", rs) + "||" + db.getIntVal("ORIG_FILE_SIZE_NO", rs));
		setCreateDt(new SimpleDateFormat("MM/dd/yyyy KK:mm:ss a Z").format(db.getDateVal("CREATE_DT", rs)));
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getVtt() {
		return vtt;
	}

	public void setVtt(String vtt) {
		this.vtt = vtt;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getCreateDt() {
		return createDt;
	}

	public void setCreateDt(String createDt) {
		this.createDt = createDt;
	}
}
