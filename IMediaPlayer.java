package com.ishwar.imediaplayer;

import android.content.Context;
import android.media.MediaPlayer;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.io.IOException;
import android.annotation.NonNull;

/**
 *
 * @author Ishwar Meghwal
 * contact me at mrdev.288@gmail.com
 *
 *
 * extended MediaPlayer which support caching with proxy implementation
 *
 * Permissions required :-
 * android.permission.INTERNET
 * 
 * must include android:usesCleartextTraffic="true" in Android-Manifest, or other configuration
 * to enable http support
 *
 */

public class IMediaPlayer extends MediaPlayer
{
	public final static String TAG = IMediaPlayer.class.getSimpleName();
	private final static String URLFORMAT = "http://%s:%d/%s";
	
	private Proxy proxy;
	
   /**
	* recommended to use single instance for application
	*
	*@return IMediaPlayer
	*/
	private volatile static IMediaPlayer instance;

	public static IMediaPlayer getInstance(@NonNull Context context){
		if(instance == null){
			synchronized(IMediaPlayer.class){
				instance = new IMediaPlayer(context.getCacheDir()+"/"+TAG);
			}
		}
		return instance;
	}
	
   /**
	* @Construction
	*
	*/
	private IMediaPlayer(String cacheDir){
		super();
		setCacheDir(cacheDir);
		proxy = new Proxy();
		proxy.start();
	}
	
	@Override
	public void setDataSource(String path) throws SecurityException, IOException, IllegalArgumentException, IllegalStateException{
		releaseSong();
		String url = String.format(URLFORMAT, proxy.getHost(), proxy.getPort(), path);
		super.setDataSource(url);
	}
	
   /**
	* Setting custom cache-directory may require storage read/write permission
	*
	* @param path
	*/
	public void setCacheDir(String path){
		try{
			Constants.cacheDir = new File(path);
			Constants.cacheDir.mkdirs();
			
			Constants.tempCacheDir = new File(path+"/.temp");
			Constants.tempCacheDir.mkdirs();
		}catch(Exception e){
		   /**
			* wrong directory cause unpredictable working of caching
			*/
		}
	}
	
   /**
	* call before setDataSource(Url) to reset player 
	*/
	public void releaseSong(){
		try{
			pause();
		}catch(Exception e){}
		try{
			stop();
		}catch(Exception e){}
		try{
			reset();
		}catch(Exception e){}
	}
	
   /**
	* Must be called when @link instance is no longer being used to release required resources
	*/
	public void destroy(){
		releaseSong();
		try{
			release();
		}catch(Exception e){}
		proxy.destroy();
	}
	
	class Proxy extends Thread
	{
		private static final String LOCAL_HOST = "127.0.0.1";
		private static final int LOCAL_PORT = 2328;
	
	   /**
		* Buffer configuration to boost read/write speed
		*/
		
		private static final int maxBufferLimit = 8192;
		private static final int minBufferLimit = 1024;
		
		private boolean isRunning = false;

		private ServerSocket serverSocket;
		private Socket socket;
		
		/**
		 * declaration on app level to keep single instance
		 */
		private HttpURLConnection conn;
		private StreamFile streamFile;

		public int getPort(){
			return LOCAL_PORT;
		}

		public String getHost(){
			return LOCAL_HOST;
		}

		@Override
		public void run(){
			super.run();
			isRunning = true;
			
			try{
				serverSocket = new ServerSocket(getPort());
			}catch(Exception e){
				throw new IllegalStateException("Proxy initialisation failed ("+e.getLocalizedMessage()+")");
			}

			while(isRunning){
				try{ 
					socket = serverSocket.accept();
				
				   /**
					* received mediaplayer request to connect
					*/
					processRequest(socket);

				}catch(Exception e){
				   /**
				    * timeout
					*
					* No more requests
					*/
				}
			}

	       /**
			* closes socket and streams incase they're still open
			*/
			try{
				serverSocket.close();
				socket.close();
			}catch(Exception e){
				// error while closing
			}
		}

		private void processRequest(Socket socket){
			ByteArrayInputStream bai = null;
			BufferedReader r = null;
			OutputStream out = null;
			InputStream in = null;
			try{
				in = socket.getInputStream();

				String url = null;
				
				byte[] buf = new byte[minBufferLimit];
				int count = in.read(buf,0,buf.length);

				bai = new ByteArrayInputStream(buf,0,count);
				r = new BufferedReader(new InputStreamReader(bai));

				String line;
				while((line = r.readLine())!=null){
					if(line.startsWith("GET ")){
						url = line.split(" ")[1].substring(1);
						break;
					}
				}

				if(url == null){
					throw new RuntimeException("Url missing in request");
				}
				
				out = socket.getOutputStream();
				
				if(streamFile == null || !url.equals(streamFile.getUrl())){
					streamFile = new StreamFile(url);
				}
				
				if(streamFile.isDownloadCompleted() && !streamFile.isCached()){
					streamFile.close();
				}
				
				if(streamFile.isCached()){
					Utils.copy(maxBufferLimit, new FileInputStream(streamFile.getCacheFile()),out);
				}else{
					conn = (HttpURLConnection) new URL(url).openConnection();
					conn.setRequestProperty("User-Agent", "Mozilla/5.0");
					
					Utils.copy(minBufferLimit, buildHeaders(conn), out,streamFile.getOutputStream());
					long downloadedBytes = Utils.copy(maxBufferLimit, conn.getInputStream(), out, streamFile.getOutputStream());
					
					if(conn.getContentLengthLong()==downloadedBytes ){
						streamFile.setDownloaded(true);
					}
				}
				
			}catch(Exception e){
			   /**
				* mysterious errors
				*/
			}finally{
			   /**
				* closing streams
				*/
				try{
					conn.disconnect();
				}catch(Exception e){}
				try{
					bai.close();
				}catch(Exception e){}
				try{
					r.close();
				}catch(Exception e){}
				try{
					out.close();
				}catch(Exception e){}
				try{
					socket.close();
				}catch(Exception e){}
			}
		}
		
		private ByteArrayInputStream buildHeaders(HttpURLConnection conn){
			try{
				StringBuilder headers = new StringBuilder();
				headers.append("HTTP/1.1 206 Partial Content\r\n");
				for(String key : conn.getHeaderFields().keySet()){
					if(key!=null){
						headers.append(key).append(":").append(conn.getHeaderField(key)).append("\r\n");
					}
				}
				return new ByteArrayInputStream(headers.toString().getBytes("UTF-8"));
			}catch(Exception e){
				return null;
			}
		}
		
		public void destroy(){
		   /**
			* stop @Thread and finalize vars
			*
			* after destroying, must not be used
			*/
			isRunning = false;
			try{
				interrupt();
			}catch(Exception e){}	
			try{
				this.join(500);
			}catch(Exception e){}
		}
		
	}

}
