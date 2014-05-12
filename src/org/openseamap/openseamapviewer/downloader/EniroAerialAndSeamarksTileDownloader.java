package org.openseamap.openseamapviewer.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import android.graphics.Canvas;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

public class EniroAerialAndSeamarksTileDownloader extends TileDownloader {

		private static final String TAG = "EniroAerialAndSeamarksDownloader";
		private static final boolean test = true;
		private static final String HOST_NAME1 = "map01.eniro.no";
		private static final String HOST_NAME2 = "map02.eniro.no";
		private static final String HOST_NAME3 = "map03.eniro.no";
		private static final String HOST_NAME4 = "map04.eniro.no";
		private static final String TILES_DIRECTORY = "/geowebcache/service/tms1.0.0/aerial/";
		private static final String PROTOCOL = "http";
		private static final String HOST_NAME_SEAMARKS = "tiles.openseamap.org";
		
		private static final byte ZOOM_MAX = 18;

		private final StringBuilder stringBuilder;
		private int mHostNumber;
		private final int[] pixels;
		private Context mContext;
		private NotificationManager mNM;
		public static final int NOTIFICATION_ID = 78;
		private int mResponse = 0;
	    
	    private String mExternalPathStr = "";
	    private File mExternalStorageDir = null;
	    private String stdDir =  MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME; // "/openseamapviewer"
		private String mCachePathStr = "/Cachedata/EniroAerialWithSeamarks";
		private String mCacheDirPathStr =  stdDir + mCachePathStr;
		/**
		 * Constructs a new OpenSeamapTilesAndSeamarksTileDownloader.
		 */
		public  EniroAerialAndSeamarksTileDownloader(Context context) {
			super();
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
		
		private void createDataDirOld() {
			// Verzeichnis anlegen
	        File file = new File(mExternalStorageDir, stdDir);
			try {
				String filePathStr = file.getAbsolutePath();
				if (file.mkdir()) { // here we need android permission in the manifest
						Log.v(TAG, "erzeuge Directory: " + filePathStr);
				} else {
						Log.v(TAG, "directory schon vorhanden " + filePathStr);
				}
			} catch (SecurityException se) {
				se.printStackTrace();
					Log.v("TAG", "Security exception : Directory not created " + se);
			}// try
			// Unterverzeichnis anlegen
			File file2 = new File(mExternalStorageDir, mCacheDirPathStr);
			try {
				String file2PathStr = file2.getAbsolutePath();
				if (file2.mkdir()) { // here we need android permission in the manifest
						Log.v(TAG, "erzeuge Directory: " + file2PathStr);
				} else {
						Log.v(TAG, "directory schon vorhanden " + file2PathStr);
				}
			} catch (SecurityException se) {
				se.printStackTrace();
					Log.v("TAG", "Security exception : Directory not created " + se);
			}// try
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
		
		private boolean getTileFromCache(String cacheFilePathStr, Bitmap bitmap){
			boolean result = false;
			// Test if the Tile is in the cache
			File aTestFile = new File(cacheFilePathStr);
		    boolean fileExist = aTestFile.exists();
		    if (fileExist) {
		    	Bitmap decodedBitmap;
	            if (test) Log.d(TAG,"in Cache: " + cacheFilePathStr);
		    	BitmapFactory.Options options = new BitmapFactory.Options();
		        options.inDither = false;   //important here we deal with the transparency  
		        options.inPreferredConfig = Bitmap.Config.ARGB_8888;  // decodeFile must return  a 8888 rgb for transparency
		        decodedBitmap = BitmapFactory.decodeFile(cacheFilePathStr , options);

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
		
		private void writeTileToCache (String cacheFilePathStr, Bitmap bitmap){
			
				try {
					OutputStream os = null;
				    os = new FileOutputStream(cacheFilePathStr );
				    bitmap.compress(CompressFormat.PNG, 50, os); // PNG: 50 is ignored 
				} catch(IOException e) {
				    e.printStackTrace();
				    if (test) {
				    	Log.d(TAG,e.toString());
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
	    private String getSeamarksHostName() {
	    	return HOST_NAME_SEAMARKS;
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
		
		
		
	    
		private String getSeamarksTilePath(Tile tile) {
			this.stringBuilder.setLength(0);
			//this.stringBuilder.append("/tiles/base/");
			this.stringBuilder.append("/seamark/");
			this.stringBuilder.append(tile.zoomLevel);
			this.stringBuilder.append('/');
			this.stringBuilder.append(tile.tileX);
			this.stringBuilder.append('/');
			this.stringBuilder.append(tile.tileY);
			this.stringBuilder.append(".png");
			String myPath = this.stringBuilder.toString();
			try {
			   URL myUrl = new URL(getProtocol(), getSeamarksHostName(),myPath );
			   String myUrlString = myUrl.toString();
	           Log.d(TAG,myUrlString);
			} catch (MalformedURLException e) {
				Log.d(TAG,"Unknown Exception");
				return "";
			}
			
			return this.stringBuilder.toString();
		}
		@Override
		public byte getZoomLevelMax() {
			return ZOOM_MAX;
		}
		
		
		
		
		
		private String downloadUrlAsString(URL testurl) throws IOException{
			InputStream   testinputStream = null;
			try {
				
				HttpURLConnection testcon = (HttpURLConnection) testurl.openConnection();
		        testcon.setConnectTimeout(1000);
		        testcon.setReadTimeout(1000);
		        testcon.setRequestMethod("GET");
		        testcon.connect(); 
		        mResponse = testcon.getResponseCode();
		        Log.d(TAG,"the request code " + mResponse);
		        testinputStream = testcon.getInputStream();
		        String contentAsString = readIt(testinputStream, 100);
				Log.d(TAG,contentAsString);
				return contentAsString;
			   } finally {
				   if (testinputStream != null) {
					   testinputStream.close();
				   }
			   }
			
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
				// Test if the Tile is in the cache
				if (getTileFromCache(aCacheFilePathStr,bitmap)){
					return true;
				}
				if (test) Log.d(TAG,"loading tile " + msg );
				showNotification(true,msg);
				URL url = new URL(getProtocol(), getHostName(), getTilePath(tile));
				URLConnection con =url.openConnection();
		        con.setConnectTimeout(5000);
		        con.setReadTimeout(10000);
		        con.connect(); 
		        InputStream   inputStream = con.getInputStream();
				//InputStream inputStream = url.openStream();
		        BitmapFactory.Options options = new BitmapFactory.Options();
		        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		        options.inDither = false ;
				Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream,null,options);
				//Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
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
				//boolean bitmapHasAlpha = bitmap.hasAlpha();
				//boolean decodedbitmapHasAlpha = decodedBitmap.hasAlpha();
				Bitmap seamarkBitmap = Bitmap.createBitmap(Tile.TILE_SIZE,Tile.TILE_SIZE,Bitmap.Config.ARGB_8888);
			    //boolean seamarkHasAlpha = seamarkBitmap.hasAlpha();
				if (readSeamarksBitmap( mapGeneratorJob,seamarkBitmap)){
					if (test)Log.d(TAG,"try to combine the two bitmaps");
					//seamarkHasAlpha = seamarkBitmap.hasAlpha();
					Canvas comboImage = new Canvas(bitmap);
					comboImage.drawBitmap(seamarkBitmap, 0f, 0f, null);
					
					
					if (test) Log.d(TAG,"combine Bitmaps success");
				}
				if (this.assertDataDirectoryExists(aCacheDirPathStr)){
				    writeTileToCache(aCacheFilePathStr, bitmap);
				}
				return true;
			} catch (UnknownHostException e) {
				//LOG.log(Level.SEVERE, null, e);
				Log.d(TAG,e.toString());
				showNotification(false,"");
				return false;
			} catch (IOException e) {
				//LOG.log(Level.SEVERE, null, e);
				Log.d(TAG,e.toString());
				
				showNotification(false,"");
				return false;
			}
		}
		
		/*
		 * @Override
		 */
		public boolean executeJobOld(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
			
			try {
				Tile tile = mapGeneratorJob.tile;
				String msg = " x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel ;
				String filename = "OpenSeaMapTile_" + tile.zoomLevel + "_" + tile.tileX+"_"+tile.tileY;
				String aCachePathName = mExternalPathStr +  mCacheDirPathStr +"/"+ filename + ".png";
				// Test if the Tile is in the cache
				File aTestFile = new File(aCachePathName);
			   // boolean doExist = aTestFile.exists();
			    if (aTestFile.exists()) {
			    	Bitmap decodedBitmap;
	                Log.d(TAG,"in Cache: " + filename);
			    	BitmapFactory.Options options = new BitmapFactory.Options();
			        options.inDither = false;   //important
			        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			        decodedBitmap = BitmapFactory.decodeFile(aCachePathName , options);

			        if(decodedBitmap == null) {
			            Log.e(TAG, "unable to decode bitmap");
			            return false;
			        }
			        decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
					decodedBitmap.recycle();

					// copy all pixels from the color array to the tile bitmap
					bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
					return true;
			    } 
				Log.d(TAG,"loading tile " + msg );
				showNotification(true,msg);
				URL url = new URL(getProtocol(), getHostName(), getTilePath(tile));
				URLConnection con =url.openConnection();
		        con.setConnectTimeout(3000);
		        con.setReadTimeout(3000);
		        con.connect(); 
		        InputStream   inputStream = con.getInputStream();
				//InputStream inputStream = url.openStream();
		        BitmapFactory.Options options = new BitmapFactory.Options();
		        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		        options.inDither = false ;
				Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream,null,options);
				//Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream);
				inputStream.close();
	            cancelNotification();
				// check if the input stream could be decoded into a bitmap
				if (decodedBitmap == null) {
					return false;
				}
				Log.d(TAG,"tile loaded " + msg );
				
				// copy all pixels from the decoded bitmap to the color array
				decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
				decodedBitmap.recycle();

				// copy all pixels from the color array to the tile bitmap
				bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
				//boolean bitmapHasAlpha = bitmap.hasAlpha();
				//boolean decodedbitmapHasAlpha = decodedBitmap.hasAlpha();
				Bitmap seamarkBitmap = Bitmap.createBitmap(Tile.TILE_SIZE,Tile.TILE_SIZE,Bitmap.Config.ARGB_8888);
			    //boolean seamarkHasAlpha = seamarkBitmap.hasAlpha();
				if (readSeamarksBitmap( mapGeneratorJob,seamarkBitmap)){
					Log.d(TAG,"try to combine the two bitmaps");
					//seamarkHasAlpha = seamarkBitmap.hasAlpha();
					Canvas comboImage = new Canvas(bitmap);
					comboImage.drawBitmap(seamarkBitmap, 0f, 0f, null);
					
					
					Log.d(TAG,"combine Bitmaps success");
				}
				try {
					OutputStream os = null;
				    os = new FileOutputStream(aCachePathName );
				    bitmap.compress(CompressFormat.PNG, 50, os); // PNG: 50 is ignored 
				} catch(IOException e) {
				    e.printStackTrace();
				}
				return true;
			} catch (UnknownHostException e) {
				//LOG.log(Level.SEVERE, null, e);
				Log.d(TAG,e.toString());
				showNotification(false,"");
				return false;
			} catch (IOException e) {
				//LOG.log(Level.SEVERE, null, e);
				Log.d(TAG,e.toString());
				
				showNotification(false,"");
				return false;
			}
		}
		
		
		public boolean readSeamarksBitmap(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
			String aPNGStr = "";
			Tile tile = mapGeneratorJob.tile;
			String aMsg = "";
			String msg = " x= " +tile.tileX + " y= " + tile.tileY + " zoom " + tile.zoomLevel ;
			Log.d(TAG,"loading tile " + msg );
			try {
				URL testurl = new URL(getProtocol(), getSeamarksHostName(), getSeamarksTilePath(tile));
				aMsg = testurl.toString();
				String response = downloadUrlAsString(testurl);
				if (response.length() > 5) {
					aPNGStr = response.substring(1, 4);
				}
				
			} catch (IOException e) {
				//LOG.log(Level.SEVERE, null, e);
				Log.d(TAG,e.toString());
				return false;
			}
			if (mResponse != 200){
				return false;
			}
			
			if (!aPNGStr.equalsIgnoreCase("PNG")) {
				Log.d(TAG,"no PNG " + aMsg);
				return false;
			}
			Log.d(TAG,"Tile is PNG");
			try {
				URL url = new URL(getProtocol(), getSeamarksHostName(), getSeamarksTilePath(tile));
				URLConnection con =url.openConnection();
		        con.setConnectTimeout(3000);
		        con.setReadTimeout(3000);
		        con.connect(); 
		        InputStream   inputStream = con.getInputStream();
				//InputStream inputStream = url.openStream();
		        BitmapFactory.Options options = new BitmapFactory.Options();
		        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		        options.inDither = false ;
				Bitmap decodedBitmap = BitmapFactory.decodeStream(inputStream,null,options);
				//boolean seamarkHasAlpha = decodedBitmap.hasAlpha();
				inputStream.close();
	           
				// check if the input stream could be decoded into a bitmap
				if (decodedBitmap == null) {
					return false;
				}
				Log.d(TAG," Seamarks tile loaded " + msg );
				
				// copy all pixels from the decoded bitmap to the color array
				decodedBitmap.getPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
				decodedBitmap.recycle();

				// copy all pixels from the color array to the tile bitmap
				bitmap.setPixels(this.pixels, 0, Tile.TILE_SIZE, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
				
				return true;
			} catch (UnknownHostException e) {
				//LOG.log(Level.SEVERE, null, e);
				Log.d(TAG,e.toString());
				return false;
			} catch (IOException e) {
				//LOG.log(Level.SEVERE, null, e);
				Log.d(TAG,e.toString());
				
				showNotification(false,"");
				return false;
			}
		}
		
		// Reads an InputStream and converts it to a String.
		public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		    Reader reader = null;
		    reader = new InputStreamReader(stream, "UTF-8");        
		    char[] buffer = new char[len];
		    reader.read(buffer);
		    return new String(buffer);
		}
		
		private void showNotification(boolean isOn, String msg) {
			mNM = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
			String aNotificationTitle = mContext.getResources().getString(R.string.tcp_network_service); 
			String aNotificationMessage;
			String aPendingMessage;
			int version = Build.VERSION.SDK_INT;
			if (isOn){
			  aNotificationMessage = mContext.getResources().getString(R.string.mapdownload_runs) + msg;
			  Notification aNotification = new Notification (R.drawable.downloadactive_green, aNotificationMessage,
				        System.currentTimeMillis());
			  PendingIntent pendingIntent = null;
			  if(version < 14) pendingIntent = PendingIntent.getActivity(mContext, 0, null, 0);
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
			  PendingIntent pendingIntent = null;
			  if(version < 14) pendingIntent = PendingIntent.getActivity(mContext, 0, null, 0);
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
