package com.ansmed.datafeed;

// JDK 1.6
import java.io.*;

/*
Original Source: www.javaworld.com
Author: Michael C. Daconta
Original Class name: StreamGobbler.java
Author's Creation Date: 12/29/2000
Source url: http://www.javaworld.com/jw-12-2000/jw-1229-traps.html?page=4
Modified 11/14/2012: DBargerhuff, removed PrintWriter and associated calls.
*/
/****************************************************************************
 * <b>Title</b>:RemoteExecStreamManager.java<p/>
 * <b>Description: </b> This class is used to 'eat' the output and error streams resulting from running
 * a remotely executed process.  Handling the output and error streams in this manner ensures that the 
 * remote process will not hang when attempting to exit on success or failure.  This class is sourced 
 * from www.javaworld.com as specified above.  I commented out the references to PrintWriter.
 * 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 12, 2012
 ****************************************************************************/
public class RemoteExecStreamManager extends Thread
{
    InputStream is;
    String type;
    OutputStream os;
    
    public RemoteExecStreamManager(InputStream is, String type)
    {
        this(is, type, null);
    }
    public RemoteExecStreamManager(InputStream is, String type, OutputStream redirect)
    {
        this.is = is;
        this.type = type;
        this.os = redirect;
    }
    
    public void run()
    {
        try
        {
            //PrintWriter pw = null;
            //if (os != null) pw = new PrintWriter(os);
                
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            @SuppressWarnings("unused")
			String line=null;
            while ( (line = br.readLine()) != null)
            {
	            // eat the stream by doing nothing but reading it
                //if (pw != null) pw.println(line);
                //System.out.println(type + ">" + line);    
            }
            //if (pw != null) pw.flush();
        } catch (IOException ioe) {
            System.out.println(ioe);
        }
    }
}