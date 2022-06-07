package com.ishwar.imediaplayer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * @author Ishwar Meghwal
 *
 * Util class for @link IMediaPlayer
 */

public class Utils
{
	public final static int DEFAULT_BUFFER_LIMIT = 2048;
	
   /**
	* writes one InputStream @link into multiple OutputStreams @link outs
	* 
	* @param in
	* @param outs
	*
	* @return total written bytes
	*/
	
	public static int copy(InputStream in, OutputStream...outs) throws IOException{
		return Utils.copy(Utils.DEFAULT_BUFFER_LIMIT, in, outs);
	}
	
   /**
	* @param bufferLimit
	*/
	
	public static int copy(int bufferLimit, InputStream in, OutputStream...outs) throws IOException{
		int writtenBytes = 0;
		byte[] buff = new byte[bufferLimit];
		int len;
		while(( len = in.read(buff, 0, buff.length ))>-1){
			writtenBytes += len;
			for(OutputStream out : outs){
				if(out != null){
					out.write(buff, 0, len);
				}
			}
		}
		return writtenBytes;
	}
	
	/**
	 * generates unique name of @param url
	 *
	 */
	public static String generateName(String url){
		try{
			return url.trim()
				.replaceAll("(http|https|Http|Https)\\:\\/\\/(www\\.|)","")
				.replaceAll("[/]","_")
				.replaceAll("[:]","-");
		}catch(Exception e){
			return url;
		}
	}
}
