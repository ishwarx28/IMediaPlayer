package com.ishwar.imediaplayer;

import java.io.File;
import java.io.FileOutputStream;

public class StreamFile
{
	private String url;
	
   /**
	* @link tempCacheFile store temp cache until it get completed
	*
	* @link cacheFile is permanent cache file of @link url
	*/
	private File tempCacheFile, cacheFile;
	
   /**
	* Flag that represents file was already cached
	*
	* or
	*
	* @link url is available as @link cacheFile
	*/
	private boolean isCached;
	
   /**
    *
	* Flag that represent a condition where @link IMediaPlayer complete stream download
	* of @link url in @link tempCacheFile
	*
	*/
	private boolean isDownloadCompleted = false;
	
   /**
	* OutputStream that writes to @link tempCacheFile
	*/
	private FileOutputStream fos;
	
	public StreamFile(String url){
		this.url=url;
		cacheFile = new File(Constants.cacheDir, Utils.generateName(url));
		isCached = cacheFile.exists() && cacheFile.isFile() && cacheFile.length()>0;
	}
	
	public String getUrl(){
		return url;
	}
	
	public File getCacheFile(){
		return cacheFile;
	}
	
	public boolean isCached(){
		return isCached;
	}
	
	public boolean isDownloadCompleted(){
		return isDownloadCompleted;
	}
	
	public void setDownloaded(boolean downloaded){
		this.isDownloadCompleted = downloaded;
	}
	
	public FileOutputStream getOutputStream(){
		if(fos == null){
			try{
				isDownloadCompleted = false;
				tempCacheFile = new File(Constants.tempCacheDir, cacheFile.getName());
				tempCacheFile.deleteOnExit();
				fos = new FileOutputStream(tempCacheFile);
			}catch(Exception e){}
		}
		return fos;
	}
	
	public void close(){
		try{
			fos.close();
		}catch(Exception e){}
		try{
			if(isDownloadCompleted){
			   /**
				* stream dowloaded completely, write it to mainCacheFile
				*/
				tempCacheFile.renameTo(cacheFile);
				isCached = true;
			}else{
			   /**
				* stream isn't downloaded completely, release temp cache
				*/
				tempCacheFile.delete();
			}
		}catch(Exception e){}
	}
}
