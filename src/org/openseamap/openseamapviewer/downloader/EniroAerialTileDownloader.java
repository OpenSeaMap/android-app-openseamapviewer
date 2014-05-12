package org.openseamap.openseamapviewer.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.Tile;
import org.openseamap.openseamapviewer.MainActivity;
import org.openseamap.openseamapviewer.R;
import org.openseamap.openseamapviewer.R.drawable;
import org.openseamap.openseamapviewer.R.string;
import org.openseamap.openseamapviewer.environment.Device;
import org.openseamap.openseamapviewer.environment.Environment2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class EniroAerialTileDownloader extends TileDownloader {

	//private static final String HOST_NAME = "map01.eniro.no/geowebcache/service/tms1.0.0/aerial";
	// see the document eniro Kartenanforderung
	// The browser script requests tiles from the four eniro servers
	// we use them in order 1,2,3,4
	// we assume, that a new tile should be loaded from the next host
	private static final String TAG = "EniroAerialTileDownLoader";
	private static final boolean test = false;
	
	
	private static final String HOST_NAME1 = "map01.eniro.no";
	private static final String HOST_NAME2 = "map02.eniro.no";
	private static final String HOST_NAME3 = "map03.eniro.no";
	private static final String HOST_NAME4 = "map04.eniro.no";
	private static final String TILES_DIRECTORY = "/geowebcache/service/tms1.0.0/aerial/";
	private static final String PROTOCOL = "http";
	private static final byte ZOOM_MAX = 18;

	private final StringBuilder stringBuilder;
	private int mHostNumber; 
	private final int[] pixels;
	private Context mContext;
	private NotificationManager mNM;
	private static final int NOTIFICATION_ID = 78;
	// TileCache	
	private String mExternalPathStr = "";
	private File mExternalStorageDir = null;
	private String stdDir =  MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME; // "/openseamapviewer"
	private String mCachePathStr = "/Cachedata/EniroAerial";
	private String mCacheDirPathStr =  stdDir + mCachePathStr;
	// Timeout
	private int mTimeoutForDownload = 3000;
	
	/**
	 * Constructs a new EniroAerialTileDownloader.
	 */
	public EniroAerialTileDownloader(Context context) {
		mContext = context;
		this.pixels = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];
		this.stringBuilder = new StringBuilder();
		Device firstExternalStorageDevice = Environment2.getPrimaryExternalStorage();
        String firstExternalStorageDevicePath = firstExternalStorageDevice.getMountPoint();
        File aDir = firstExternalStorageDevice.getFile();
		mExternalPathStr = firstExternalStorageDevicePath;
		mExternalStorageDir = aDir;
		boolean hasSecondaryStorage = Environment2.isSecondaryExternalStorageAvailable();
		
		//String aInfo = "";
        if (hasSecondaryStorage){
        try {
        	aDir = Environment2.getSecondaryExternalStorageDirectory();
            String aPathStr = aDir.getAbsolutePath();
            mExternalPathStr = aPathStr;
            mExternalStorageDir = aDir;
        } catch (Exception e){
        	Log.d(TAG,e.toString());
        }
        } else {
        	//aInfo = "no Secondary storage on SD-Card: ";
        }
        assertDataDirectoryExists(mCacheDirPathStr);
		
	}
	
	public void setTileCacheSubDirName (String aName){
		if (aName.startsWith("/")){
			mCacheDirPathStr =  stdDir +  aName + mCachePathStr;
		} else {
		   mCacheDirPathStr =  stdDir + "/" + aName + mCachePathStr;
		}
	}
	
	private boolean assertDirectoryExists(String directoryName){
		boolean result = false;
		String mountedStr = Environment2.getCardState();
		if (mountedStr.equals(Environment.MEDIA_MOUNTED)) {
			File file = new File(mExternalStorageDir, directoryName);
			if (file.exists()&& file.isDirectory()){
				result = true;
				return result;
			}
			if (!directoryName.startsWith("/")) {
				directoryName="/" +directoryName;
			}

			try {
				String filePathStr = file.getAbsolutePath();
				if (file.mkdir()) { // here we need android permission in the manifest
						Log.v(TAG, "create directory: " + filePathStr);
				} else {
						Log.v(TAG, "directory not created: " + filePathStr);
				}
				File testFile = new File(mExternalStorageDir, directoryName);
				if (testFile.exists() && testFile.isDirectory()){
					result = true;
				}
			} catch (SecurityException se) {
				se.printStackTrace();
					Log.v("TAG", "Security exception : directory not created: " + se);
			}// try
		}
		return result;
	}
	
	private boolean assertDataDirectoryExists(String directoryName){
		boolean result = false;
		String mountedStr = Environment2.getCardState();
		if (mountedStr.equals(Environment.MEDIA_MOUNTED)) {
			File file = new File(mExternalStorageDir, directoryName);
			if (file.exists()&& file.isDirectory()){
				result = true;
				return result;
			}
			if (directoryName.startsWith("/")) {
				directoryName=directoryName.substring(1);
			}
			String [] dirs = directoryName.split("/");
			int dirCount = dirs.length;
			String newDirPrefix = "/"; 
			String lastDirPath =""; 
		    //  now we know how many dirs we must create
			boolean dirExists = false;
			String lastPathStr = "";
			for (int dirIndex =0;dirIndex < dirCount; dirIndex++){
				StringBuffer buf = new StringBuffer();
				//newDirName = newDirName + dirs[dirIndex]  +"/";
				buf.append(lastDirPath);
				buf.append(newDirPrefix);
				buf.append(dirs[dirIndex]) ;
				String dirNamePath = buf.toString();
				dirExists = assertDirectoryExists(dirNamePath);
				lastDirPath = dirNamePath;
			}
			File testFile = new File(mExternalStorageDir,lastDirPath);
			result = testFile.exists()&& testFile.isDirectory();
		}
		return result;
	}
	
	
	
	/**
	 * get the Tile with cacheFilepathName from the cache
	 * @param cacheFilePathName
	 * @param bitmap the bitmap is filled with the tiles content
	 * @return true if Tile was in the cache
	 */
	
	private boolean getTileFromCache(String cacheFilePathName, Bitmap bitmap){
		boolean result = false;
		// Test if the Tile is in the cache
		File aTestFile = new File(cacheFilePathName);
	    boolean fileExist = aTestFile.exists();
	    if (fileExist) {
	    	Bitmap decodedBitmap;
            if (test) Log.d(TAG,"in Cache: " + cacheFilePathName);
	    	BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inDither = false;   //important here we deal with the transparency  
	        options.inPreferredConfig = Bitmap.Config.ARGB_8888;  // decodeFile must return  a 8888 rgb for transparency
	        decodedBitmap = BitmapFactory.decodeFile(cacheFilePathName , options);

	        if(decodedBitmap == null) {
	           if (test) {
	        	   Log.e(TAG, "unable to decode bitmap");
	           }
	            return false;
	        }
	        decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			decodedBitmap.recycle();

			// copy all pixels from the color array to the tile bitmap
			bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			result = true;
	    } 
	    return result;
	}
	
	private void writeTileToCache (String cacheFilePathName, Bitmap bitmap){
		OutputStream os = null;
		try {
		    os = new FileOutputStream(cacheFilePathName );
		    bitmap.compress(CompressFormat.PNG, 50, os); // PNG: 50 is ignored
		   
		} catch(IOException e) {
		    e.printStackTrace();
		    if (test) {
		    	Log.d(TAG,e.toString());
		    }
		} finally {
			if(os!=null){
				try {
				 os.close();
				} catch (IOException e) {}
			}
		}
	}

	@Override
	public String getHostName() {
		switch (mHostNumber) {
		
		case 1:
			return HOST_NAME1;
		case 2:
			return HOST_NAME2;
		case 3:
			return HOST_NAME3;
		case 4:
			return HOST_NAME4;
		default:
			return HOST_NAME2;
		}
	}

	@Override
	public String getProtocol() {
		return PROTOCOL;
	}

	@Override
	public String getTilePath(Tile tile) {
		this.stringBuilder.setLength(0);
		this.stringBuilder.append(TILES_DIRECTORY);
		this.stringBuilder.append(tile.zoomLevel);
		this.stringBuilder.append('/');
		this.stringBuilder.append(tile.tileX);
		this.stringBuilder.append('/');
	    long eniroTileY = (1<< tile.zoomLevel)- 1 - tile.tileY;
		this.stringBuilder.append(eniroTileY);
		this.stringBuilder.append(".jpeg");
		String myPath = this.stringBuilder.toString();
		if (mHostNumber > 3){
			mHostNumber = 1;
		}else {
			mHostNumber++;
		}
		return this.stringBuilder.toString();
	}

	@Override
	public byte getZoomLevelMax() {
		return ZOOM_MAX;
	}
	
	/*
	 * @Override
	 */
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		
		try {
			Tile tile = mapGeneratorJob.tile;
			int zoomLevel = tile.zoomLevel;
			String zoomLevelStr = String.valueOf(zoomLevel);
			String msg = " x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel ;
			
			String filename = "EniroAerialTile_" + tile.zoomLevel + "_" + tile.tileX+"_"+tile.tileY;
			String aCacheDirPathStr = mCacheDirPathStr +"/"+ zoomLevelStr;
			
			String aCacheFilePathStr = mExternalPathStr + aCacheDirPathStr + "/" + filename + ".png";
			if (getTileFromCache(aCacheFilePathStr, bitmap)){
				return true;
			}
			// the tile is not in the cache
			
			if (test) Log.d(TAG,"loading tile " + msg );
			
			showNotification(true,msg);
			URL url = new URL(getProtocol(), getHostName(), getTilePath(tile));
			if (test) Log.d(TAG,url.getPath());
			URLConnection con =url.openConnection();
	        con.setConnectTimeout(1000);
	        con.setReadTimeout(1000);
	        con.connect(); 
	        InputStream   inputStream = con.getInputStream();
			//InputStream inputStream = url.openStream();
			Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
			inputStream.close();
            cancelNotification();
			// check if the input stream could be decoded into a bitmap
			if (decodedBitmap == null) {
				return false;
			}
			if (test) Log.d(TAG,"tile loaded " + msg );
			// copy all pixels from the decoded bitmap to the color array
			decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			decodedBitmap.recycle();

			// copy all pixels from the color array to the tile bitmap
			bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
			if (this.assertDataDirectoryExists(aCacheDirPathStr)){
			    writeTileToCache(aCacheFilePathStr, bitmap);
			}
			return true;
		} catch (UnknownHostException e) {
			//LOG.log(Level.SEVERE, null, e);
			if (test) Log.d(TAG,e.toString());
			
			showNotification(false,"");
			return false;
		} catch (IOException e) {
			//LOG.log(Level.SEVERE, null, e);
			if (test) Log.d(TAG,e.toString());
			
			showNotification(false,"");
			return false;
		}
	}
	
	private void showNotification(boolean isOn, String msg) {
		mNM = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		String aNotificationTitle = mContext.getResources().getString(R.string.tcp_network_service); 
		String aNotificationMessage;
		String aPendingMessage;
		int aVersion  = Build.VERSION.SDK_INT;  // we ask for the Version of the OS
		if (isOn){
		  aNotificationMessage = mContext.getResources().getString(R.string.mapdownload_runs) + msg;
		  Notification aNotification = new Notification (R.drawable.downloadactive_green, aNotificationMessage,
			        System.currentTimeMillis());
		  /* PendingIntent pendingIntent = 
	      PendingIntent.getActivity(mContext, 0, null, 0);*/
          PendingIntent pendingIntent = null;
          if (aVersion < 14) pendingIntent = PendingIntent.getActivity(mContext, 0, null, 0);
		  aPendingMessage = mContext.getResources().getString(R.string.mapdownload_false) + msg; 
		  aNotification.setLatestEventInfo(mContext, 
			        						aNotificationTitle,
			        						aPendingMessage, pendingIntent); 
			    
			    // Ab Android 2.0: 
		  mNM.notify(NOTIFICATION_ID,aNotification);
		  //mContext.startForeground(NOTIFICATION_ID,aNotification);
		}else {
		  aNotificationMessage = mContext.getResources().getString(R.string.mapdownload_false);
		  Notification aNotification = new Notification (R.drawable.downloadfalse, aNotificationMessage,
			        System.currentTimeMillis());
		  /* PendingIntent pendingIntent = 
	      PendingIntent.getActivity(mContext, 0, null, 0);*/
          PendingIntent pendingIntent = null;
          if (aVersion < 14) pendingIntent = PendingIntent.getActivity(mContext, 0, null, 0);
		  aPendingMessage = mContext.getResources().getString(R.string.mapdownload_false); 
		  aNotification.setLatestEventInfo(mContext, 
				    						aNotificationTitle,
				    						aPendingMessage, pendingIntent); 
			    
			    // Ab Android 2.0: 
		  mNM.notify(NOTIFICATION_ID,aNotification);
	}
		
	}
	
	private void cancelNotification(){
		mNM.cancel(NOTIFICATION_ID);
	}

}
