package com.depuysynthes.scripts.showpad;

import com.siliconmtn.io.FileType;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ShowpadResourceType.java<p/>
 * <b>Description: Return a predefined resource type based on the file extention</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 31, 2016
 ****************************************************************************/
public class ShowpadResourceType {
	
	private ShowpadResourceType() {
		//per sonarqube, hide the public constructor since this class only 
		//has static methods; we don't want people creating this object.
	}

	protected static String getResourceType(FileType fType) {
		switch (StringUtil.checkVal(fType.getFileExtension()).toLowerCase()) {
			case "pdf":
			case "txt":
			case "rtf":
			case "doc":
			case "docx":
			case "xls":
			case "xlsx":
			case "ppt":
			case "pps":
			case "ppsx":
			case "pptx":
				return "document";
			case "m4v":
			case "mp4":
			case "mov":
			case "mpg":
			case "mpeg":
			case "flv":
			case "asf":
			case "3gp":
			case "avi":
			case "wmv":
				return "video";
			case "mp3":
			case "m4a":
			case "wma":
			case "wav":
				return "audio";
			case "jpg":
			case "jpeg":
			case "gif":
			case "png":
			case "tiff": 
				return "image";

			default: return "asset";
		}
	}
}