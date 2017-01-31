package com.bmg.admin.vo;

import java.util.List;

import com.biomed.smarttrak.vo.NoteVO;

/****************************************************************************
 * <b>Title</b>: BiomedNoteInterface.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> sets up a contract so that all VOs that will accept notes 
 *  in the same way.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Jan 30, 2017<p/>
 * @updates:
 ****************************************************************************/
public interface NoteEntityInterface {
	
	public void setNotes(List<NoteVO> notes);
	public String getId();

}
