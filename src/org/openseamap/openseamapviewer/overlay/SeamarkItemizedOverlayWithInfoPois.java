package org.openseamap.openseamapviewer.overlay;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.R;
import org.openseamap.openseamapviewer.ViewerGlobals;
import org.openseamap.openseamapviewer.seamarks.SeamarkDrawable;
import org.openseamap.openseamapviewer.seamarks.SeamarkNode;
import org.openseamap.openseamapviewer.seamarks.SeamarkWay;
import org.openseamap.openseamapviewer.seamarks.SeamarkWithPoisOverlayItem;
import org.openseamap.openseamapviewer.viewer.OpenSeaMapViewerWithInfoPois;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class SeamarkItemizedOverlayWithInfoPois extends ArrayItemizedOverlay {
	private static final String TAG="SeamarkItemizedOverlay";
	/**
	 * Minimum distance in pixels before the way name is repeated.
	 */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = 300;

	/**
	 * Distance in pixels to skip from both ends of a segment.
	 */
	private static final int SEGMENT_SAFETY_DISTANCE = 30;
	
	private final OpenSeaMapViewerWithInfoPois mContext;
	private boolean mMustShowCenter;
	private boolean mFillDirectionalSector;
	private byte mShowFilledDirectionalSectorZoom;
	private int mLastItemIndex = -1;
	private int mShowNameStatus = 0;  // 0 no show, 1 short name, 2 long Name
	private ArrayList<SeamarkNode> mDisplayedSeamarkNodeList;
	private ArrayList<SeamarkWay> mDisplayedSeamarkWays = null;
    private float mDisplayFactor = 1.0f;
	/**
	 * Constructs a new SeamarkItemizedOverlay.
	 * 
	 * @param defaultMarker
	 *            the default marker (may be null).
	 * @param context
	 *            the reference to the application context.
	 */
	public SeamarkItemizedOverlayWithInfoPois(Drawable defaultMarker, OpenSeaMapViewerWithInfoPois context, boolean fillDirectionalSector, float pDisplayFactor) {
		super(defaultMarker);
		this.mContext = context;
		mMustShowCenter = true;
		mFillDirectionalSector = fillDirectionalSector;
		mShowFilledDirectionalSectorZoom= 13;
		this.mDisplayFactor = pDisplayFactor;
		this.mDisplayedSeamarkNodeList = new ArrayList<SeamarkNode>();
		this.mDisplayedSeamarkWays = new ArrayList<SeamarkWay>();
	}
	
	public int getDispayedWayListSize() {
		return mDisplayedSeamarkWays.size();
	}
	
	public SeamarkWithPoisOverlayItem getItem (int index) {
		return (SeamarkWithPoisOverlayItem)createItem(index);
	}
	
	/**
	 * Handles a tap event on the given item.
	 * 
	 */
	@Override
	protected boolean onTap(int index) {
		SeamarkWithPoisOverlayItem item = (SeamarkWithPoisOverlayItem) getItem(index);
		GeoPoint aItemPoint = item.getPoint();
		String aInfo = item.getSnippet();
		// String harbourInfo=null;
		mLastItemIndex = index;
		if (aInfo.contains("harbour")) {
			//harbourInfo = getHarbourInfo(aItemPoint); // old direct Version
			
			getHarbourInfoWithAsyncTask(aItemPoint);
			// we must set up a asyc Task, as since OS 3.0 we cannot call a network query from the UIThread
			// the info is computed and the result ist displays in a Dialog
			
			// used Classes and functions:
			// getHarbourInfoWithAsyncTask(aItemPoint); computes the network query url and 
			// creates a new DownloadHarborInfoTask
			// this executes doInBackground
			// within this task downloadUrlAsString is called and retrieves a info from the OSM-harbor server
			//  the url String of the harbour is extracted from the answer
			// the result is delivered to onPostExecute which calls showtapDialog 
			
		}
		else {
			showTapDialog(null); // we have no info, and we check again null in showTapDialog
		}
		
		return true;
	}

	/**
	 * harbourInfoUrlStr is a valid Url in skipperguide 
	 * is called from the asysc DownloadHarborInfoTask when the urlstr is retrieved
	 * @param harbourInfoUrlStr
	 */
	private void showTapDialog(String harbourInfoUrlStr) {
		SeamarkWithPoisOverlayItem item = (SeamarkWithPoisOverlayItem) getItem(mLastItemIndex);
		GeoPoint aItemPoint = item.getPoint();
		String aTitle = item.getTitle();
		String aInfo = item.getSnippet();
		final String harbourUrl = harbourInfoUrlStr;
		
		Builder builder = new AlertDialog.Builder(mContext);
		builder.setIcon(android.R.drawable.ic_menu_info_details);
		builder.setTitle(item.getTitle());
		if (harbourInfoUrlStr != null) {
			aInfo = aInfo + "\n" + "Webseite;\n" +harbourInfoUrlStr;
		}
		builder.setMessage(aInfo);
		builder.setPositiveButton(R.string.cancel, null);
		if (harbourInfoUrlStr != null ) {
			builder.setNeutralButton(R.string.osmviewer_seamarks_harbour_url, new OnClickListener(){
				public void onClick(DialogInterface dialog , int which) {
					if (harbourUrl != null){
						startBrowser(harbourUrl);
					}	
				}
		    });
		}
		builder.show(); 
	}
	
	private void startBrowser(String aUrl){
		Uri webpage = Uri.parse(aUrl);
		Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);
		PackageManager packageManager = mContext.getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities(webIntent, 0);
		boolean isIntentSafe = activities.size() > 0;
		  
		// Start an activity if it's safe
		if (isIntentSafe) {
		    mContext.startActivity(webIntent);
		}
	}
	
	/**
	 * 
	 * @param pattern  use a pattern like "000.00"
	 * @param value    the value to convert  45.34523
	 * @return  aString with the value formatted  045.34
	 */
	public  String customFormat(String pattern, double value ) {
		  DecimalFormatSymbols decimalFormatSymbolsUS = new DecimalFormatSymbols(Locale.US);
	      DecimalFormat myFormatter = new DecimalFormat(pattern,decimalFormatSymbolsUS);
	      String output = myFormatter.format(value);
	      return output;
	  }
	/**
	 * 
	 * @author vkADM
	 * without this we get  a crash on Devices with OS > 3.0 networkOnMainThreadException
	 * how to Avoid ANRs  http://developer.android.com/training/articles/perf-anr.html
	 *  http://developer.android.com/reference/android/os/AsyncTask.html
	 * it works on the DEll Streak7 with 2.2 and on the SAMSUNG Tablet with 4.03
	 */
     private class DownloadHarborInfoTask extends AsyncTask<URL,Void,String> {
		
		protected String doInBackground(URL...urls ) {
			String downloadResult = null;
			String result = null;
			int count = urls.length;
			if (count== 1) {
				try {
					//Log.d(TAG,"Infotask download "+ urls[0]);
					downloadResult = downloadUrlAsString(urls[0]);
				} catch (IOException e) {
					Log.d(TAG,"IOException "+ e.toString());
		
				} catch (Exception e ) {
					Log.d(TAG,"NetWorkException " + e.toString());  // without this we get  a crash on Devices with OS > 3.0 networkOnMainThreadException
					                                                // how to Avoid ANRs  http://developer.android.com/training/articles/perf-anr.html
					                                                // http://developer.android.com/reference/android/os/AsyncTask.html
				}                                                   // it works on the DEll Streak7 with 2.2
			}
			if (downloadResult != null) {
				//Log.d(TAG,"download task result " + downloadResult);
				// putHarbourMarker(1275, 11.375, 53.991666666667, 'Timmendorf_-_Poel', 'http://www.skipperguide.de/wiki/Timmendorf_-_Poel', '5'); 
				String[] fields = downloadResult.split(",");
				if (fields.length >= 4) {
					String harborUrl = fields[4];
					if (harborUrl.contains("www.skipperguide.de/wiki")){
						if (harborUrl.startsWith(" ")) {
							harborUrl = harborUrl.substring(1);
						}
						if (harborUrl.startsWith("'")) {
							harborUrl = harborUrl.substring(1);
						}
						if (harborUrl.endsWith("'")) {
							harborUrl = harborUrl.substring(0,harborUrl.length()-1);
						}
						result = harborUrl;
					}
				}
			}
			if (result != null) {
				Log.d(TAG,"found harbor info: " + result); 
			}else {
				Log.d(TAG," no harbour info: ");
			}
			return result;
		}
		
		protected void onPostExecute(String aString) {
			 showTapDialog(aString); 
		}
	}
     // till 2014_01_16
	private void getHarbourInfoWithAsyncTask_oldUrl(GeoPoint geoPoint){  
	//  example Timmendorf_-_Poel LAT 53.992036 LON 11.375013
		//  http://harbor.openseamap.org/getHarbours.php?b=53.9&t=54.0&l=11.36&r=11.38&ucid=113&maxSize=5
        // new url
		//  http://dev.openseamap.org/website/map/api/getHarbours.php?b=43.16098&t=43.46375&l=16.23863&r=17.39219&ucid=0&maxSize=5&zoom=11
		// calculate a bounding box
		double lon = geoPoint.getLongitude();
		double lat  = geoPoint.getLatitude();
		double rad = 0.01;
		double left = lon - rad;
		double right = lon +rad;
		double bottom= lat - rad;
		double top = lat + rad;
		String protocoll = "http";
		String host = "harbor.openseamap.org";
		StringBuffer buf = new StringBuffer();
		buf.append("/getHarbours.php?");
		buf.append("b=");
		buf.append(customFormat("00.00000",bottom));
		buf.append("&t=");
		buf.append(customFormat("00.00000",top));
		buf.append("&l=");
		buf.append(customFormat("00.00000",left));
		buf.append("&r=");
		buf.append(customFormat("00.00000",right));
		buf.append("&ucid=113&maxSize=5");
		String aUrlStr = buf.toString();
		URL aUrl = null;
		try {
			   aUrl = new URL(protocoll,host, aUrlStr);
			   String myUrlString = aUrl.toString();
	           Log.d(TAG,myUrlString);
			} catch (MalformedURLException e) {
				Log.d(TAG,"Unknown Exception");
			}
			if ( aUrl != null) {
				new DownloadHarborInfoTask().execute(aUrl);
				/*try {
					downloadResult = downloadUrlAsString(aUrl);
				} catch (IOException e) {
		
				} catch (Exception e ) {
					Log.d(TAG,"NetWorkException " + e.toString());  // without this we get  a crash on Devices with OS > 3.0 networkOnMainThreadException
					                                                // how to Avoid ANRs  http://developer.android.com/training/articles/perf-anr.html
                 	                                                // http://developer.android.com/reference/android/os/AsyncTask.html
				}                                                   // it works on the DEll Streak7 with 2.2
*/		    }
	}
	private void getHarbourInfoWithAsyncTask(GeoPoint geoPoint){
		//  example Timmendorf_-_Poel LAT 53.992036 LON 11.375013
			//  http://harbor.openseamap.org/getHarbours.php?b=53.9&t=54.0&l=11.36&r=11.38&ucid=113&maxSize=5
	        // new url
			//  http://dev.openseamap.org/website/map/api/getHarbours.php?b=43.16098&t=43.46375&l=16.23863&r=17.39219&ucid=0&maxSize=5&zoom=11
		    // also http://dev.openseamap.org/website/map/api/getHarbours.php?b=53.9&t=54.0&l=11.36&r=11.38&ucid=113&maxSize=5&maxSize=5&zoom=11
		    // ergibt putHarbourMarker(1275, 11.375, 53.991666666667, 'Timmendorf_-_Poel', 'http://www.skipperguide.de/wiki/Timmendorf_-_Poel', '5'); 
		    //        putHarbourMarker(1276, 11.3752, 53.992133333333, 'Timmendorf_auf_Poel', 'http://www.skipperguide.de/wiki/Timmendorf_auf_Poel', '5'); 
			// calculate a bounding box
		    // String testUrl = "http://dev.openseamap.org/website/map/api/getHarbours.php?b=53.98204&t=54.00204&l=11.36501&r=11.38501&ucid=113&maxSize=5"; 2014_01_29
			double lon = geoPoint.getLongitude();
			double lat  = geoPoint.getLatitude();
			double rad = 0.01;
			double left = lon - rad;
			double right = lon +rad;
			double bottom= lat - rad;
			double top = lat + rad;
			byte zoom = mContext.mLastZoom;
			String protocoll = "http";
			//String host = "harbor.openseamap.org";
			String host = "dev.openseamap.org";
			StringBuffer buf = new StringBuffer();
			buf.append("/website/map/api/getHarbours.php?");
			buf.append("b=");
			buf.append(customFormat("00.00000",bottom));
			buf.append("&t=");
			buf.append(customFormat("00.00000",top));
			buf.append("&l=");
			buf.append(customFormat("00.00000",left));
			buf.append("&r=");
			buf.append(customFormat("00.00000",right));
			//buf.append("&ucid=113&maxSize=5");
			buf.append("&ucid=113&maxSize=5");
			//buf.append("&zoom="+zoom);
			String aUrlStr = buf.toString(); 
			URL aUrl = null;
			try {
				   aUrl = new URL(protocoll,host, aUrlStr); 
				   String myUrlString = aUrl.toString();
		           //Log.d(TAG,myUrlString);
				} catch (MalformedURLException e) {
					Log.d(TAG,"Unknown Exception");
				}
				if ( aUrl != null) {
					new DownloadHarborInfoTask().execute(aUrl);
					/*try {
						downloadResult = downloadUrlAsString(aUrl);
					} catch (IOException e) {
			
					} catch (Exception e ) {
						Log.d(TAG,"NetWorkException " + e.toString());  // without this we get  a crash on Devices with OS > 3.0 networkOnMainThreadException
						                                                // how to Avoid ANRs  http://developer.android.com/training/articles/perf-anr.html
	                 	                                                // http://developer.android.com/reference/android/os/AsyncTask.html
					}                                                   // it works on the DEll Streak7 with 2.2
	*/		    }
		}
	
	/**
	 *  we get the harbor info from the osm.harbour 
	 *  does throw a  catched exception  Android device with os > 3.0 , cuase we attempt the network in zthe UI
	 * @param geoPoint
	 * @return
	 */
	private String getHarbourInfo(GeoPoint geoPoint){
		String result=null;
		//  example Timmendorf_-_Poel LAT 53.992036 LON 11.375013
		//  http://harbor.openseamap.org/getHarbours.php?b=53.9&t=54.0&l=11.36&r=11.38&ucid=113&maxSize=5
        // 
		// new Version Example: http://dev.openseamap.org/website/map/api/getHarbours.php?b=43.16098&t=43.46375&l=16.23863&r=17.39219&ucid=0&maxSize=5&zoom=11
		// calculate a bounding box
		double lon = geoPoint.getLongitude();
		double lat  = geoPoint.getLatitude();
		double rad = 0.01;
		double left = lon - rad;
		double right = lon +rad;
		double bottom= lat - rad;
		double top = lat + rad;
		String protocoll = "http";
		String host = "harbor.openseamap.org";
		StringBuffer buf = new StringBuffer();
		buf.append("/getHarbours.php?");
		buf.append("b=");
		buf.append(customFormat("00.00000",bottom));
		buf.append("&t=");
		buf.append(customFormat("00.00000",top));
		buf.append("&l=");
		buf.append(customFormat("00.00000",left));
		buf.append("&r=");
		buf.append(customFormat("00.00000",right));
		buf.append("&ucid=113&maxSize=5");
		String aUrlStr = buf.toString();
		URL aUrl = null;
		String downloadResult = null;
		try {
			   aUrl = new URL(protocoll,host, aUrlStr);
			   String myUrlString = aUrl.toString();
	           Log.d(TAG,myUrlString);
			} catch (MalformedURLException e) {
				Log.d(TAG,"Unknown Exception");
				return "";
			}
		if ( aUrl != null) {
			try {
				downloadResult = downloadUrlAsString(aUrl);
			} catch (IOException e) {
	
			} catch (Exception e ) {
				Log.d(TAG,"NetWorkException " + e.toString());  // without this we get  a crash on Devices with OS > 3.0 networkOnMainThreadException
				                                                // how to Avoid ANRs  http://developer.android.com/training/articles/perf-anr.html
				                                                // http://developer.android.com/reference/android/os/AsyncTask.html
			}                                                   // it works on the DEll Streak7 with 2.2, on OS > 3.0 we can't get the downloadResult
	    }
		if (downloadResult != null) {
			String[] fields = downloadResult.split(",");
			if (fields.length >= 4) {
				String harborUrl = fields[4];
				if (harborUrl.contains("www.skipperguide.de/wiki")){
					if (harborUrl.startsWith(" ")) {
						harborUrl = harborUrl.substring(1);
					}
					if (harborUrl.startsWith("'")) {
						harborUrl = harborUrl.substring(1);
					}
					if (harborUrl.endsWith("'")) {
						harborUrl = harborUrl.substring(0,harborUrl.length()-1);
					}
					result = harborUrl;
				}
			}
		}
		Log.d(TAG,"found harbor " + result);
		return result;
	}
	
	// Reads an InputStream and converts it to a String.
	public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
	    Reader reader = null;
	    reader = new InputStreamReader(stream, "UTF-8");        
	    char[] buffer = new char[len];
	    reader.read(buffer);
	    return new String(buffer);
	}
	
	private String downloadUrlAsStringMario(URL testurl) throws IOException{
		// Thanks to Mario Avalone
		URL url;
		HttpURLConnection connection = null;  
		try {
			//Create connection
			connection = (HttpURLConnection)testurl.openConnection();
			connection.setRequestMethod("GET");
				
			connection.setUseCaches (false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
		
			//Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
			wr.flush ();
			wr.close ();
		
			//Get Response	
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
			response.append('\r');
			}
			rd.close();
			return response.toString();
		
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if(connection != null) {
				connection.disconnect();
			}
		}
	}
	private String downloadUrlAsString(URL testurl) throws IOException{
		InputStream   testinputStream = null;
		try {
			
			HttpURLConnection testcon = (HttpURLConnection) testurl.openConnection();
	        testcon.setConnectTimeout(5000);
	        testcon.setReadTimeout(5000);
	        testcon.setRequestMethod("GET");
	        testcon.connect(); 
	        int response = testcon.getResponseCode();
	        Log.d(TAG,"the request code: " + response);
	        testinputStream = testcon.getInputStream();
	        String contentAsString = readIt(testinputStream, 200);
			//Log.d(TAG,contentAsString);
			return contentAsString;
		}
		
		finally {
			   if (testinputStream != null) {
				   testinputStream.close();
			   }
		   }
		
	}
	
	 @Override
    protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
		   Paint paint = new Paint();
		   paint.setStyle(Paint.Style.STROKE);
		   paint.setStrokeWidth(4);
		   paint.setColor(Color.RED);
		 if (mMustShowCenter) {
				int aWidth = canvas.getWidth();
				int aHeight = canvas.getHeight();
				Point aP = new Point(aWidth / 2, aHeight / 2);
				// projection.toPoint(drawPosition, aPixelPoint, drawZoomLevel);
				RectF oval = new RectF(aP.x - 5, aP.y - 5, aP.x + 5, aP.y + 5);
				canvas.drawOval(oval, paint);
			}
		    if (!mContext.mMyLocationChanged){
		    	mContext.updateSeamarkNodesOnOverlay(); 
		    	super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
		    	if (mContext.showSectorFires )  {
		    		drawMainFires(canvas,drawPosition, projection, drawZoomLevel);
					drawSectorFires(canvas,drawPosition, projection, drawZoomLevel);
				}
		    	drawSeamarkWays(canvas,drawPosition, projection, drawZoomLevel);
		    	drawSeamarks(canvas,drawPosition, projection, drawZoomLevel);
		    	drawRoute(canvas,drawPosition, projection, drawZoomLevel);
		    	showMyUpdatedPosition(canvas,drawPosition, projection, drawZoomLevel);
		    }
		    else {
		    	super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
		    	if (mContext.showSectorFires )  {
		    		drawMainFires(canvas,drawPosition, projection, drawZoomLevel);
					drawSectorFires(canvas,drawPosition, projection, drawZoomLevel);
				}
		    	drawSeamarkWays(canvas,drawPosition, projection, drawZoomLevel);
		    	drawSeamarks(canvas,drawPosition, projection, drawZoomLevel);
		    	drawRoute(canvas,drawPosition, projection, drawZoomLevel);
		    	showMyUpdatedPosition(canvas,drawPosition, projection, drawZoomLevel);
		    	mContext.mMyLocationChanged = false;
		    }
			
	 } // drawOverlayBitmap
	 
 private void showMyUpdatedPosition(Canvas canvas, Point drawPosition, Projection projection , byte drawZoomLevel){
	 
	 if ( mContext.mMyLocationPoint != null && mContext.showMyLocation){
		 Paint paint = new Paint();
		 paint.setStrokeWidth(5);
		 paint.setColor(Color.BLUE);
		 Point aPixelPoint = new Point();
		 GeoPoint myPoint = mContext.mMyLocationPoint;
		 projection.toPoint(myPoint, aPixelPoint, drawZoomLevel);
		 float x = aPixelPoint.x;
	     x = x - drawPosition.x;
	     float y = aPixelPoint.y;
	     y = y - drawPosition.y;
		 canvas.drawCircle(x, y, 10, paint);
	 }
 }
 
 private void drawSeamarkWays (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		if (mDisplayedSeamarkWays != null) {
			int countLines =mDisplayedSeamarkWays.size();
 		for (int index= 0;index < countLines;index++){
 			SeamarkWay aSeamarkWay = mDisplayedSeamarkWays.get(index);
 			drawSeamarkWay(aSeamarkWay, canvas,drawPosition, projection, drawZoomLevel);
 		}
		}
	}
 
 private float floatValueOfString (String aValueStr) {
	 float result = 0.0f;
	 try {
		float valueF = Float.valueOf(aValueStr) ;
		result = valueF;
	 } catch (NumberFormatException e) {
		 Log.d(TAG,e.toString());
	 }
	 return result;
 }
 
 private void calculateRenderTextRepeater(String textKey, Paint paint, Paint outline, float[][] coordinates,
			List<WayTextContainerOSM> wayNames) {
		// calculate the way name length plus some margin of safety
		float wayNameWidth = paint.measureText(textKey) + 10;

		int skipPixels = 0;

		// get the first way point coordinates
		float previousX = coordinates[0][0];
		float previousY = coordinates[1][0];
        int l = coordinates[0].length;
		// find way segments long enough to draw the way name on them
		for (int i = 0; i < coordinates[0].length; i ++) {
			// get the current way point coordinates
			float currentX = coordinates[0][i];
			float currentY = coordinates[1][i];

			// calculate the length of the current segment (Euclidian distance)
			float diffX = currentX - previousX;
			float diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);

			if (skipPixels > 0) {
				skipPixels -= segmentLengthInPixel;
			} else if (segmentLengthInPixel > wayNameWidth) {
				float[] wayNamePath = new float[4];
				// check to prevent inverted way names
				if (previousX <= currentX) {
					wayNamePath[0] = previousX;
					wayNamePath[1] = previousY;
					wayNamePath[2] = currentX;
					wayNamePath[3] = currentY;
				} else {
					wayNamePath[0] = currentX;
					wayNamePath[1] = currentY;
					wayNamePath[2] = previousX;
					wayNamePath[3] = previousY;
				}
				wayNames.add(new WayTextContainerOSM(wayNamePath, textKey, paint));
				if (outline != null) {
					wayNames.add(new WayTextContainerOSM(wayNamePath, textKey, outline));
				}

				skipPixels = DISTANCE_BETWEEN_WAY_NAMES;
			}

			// store the previous way point coordinates
			previousX = currentX;
			previousY = currentY;
		}
	}

	
	private void drawSeamarkWay (SeamarkWay seamarkWay, Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		if (seamarkWay != null){
			float[][] coordinates;  // see DatabaseRenderer
			ArrayList<WayTextContainerOSM> wayNames = new ArrayList<WayTextContainerOSM>();
			int paintColor = Color.BLACK;
			float strokeWidth = 2.0f;
			boolean mustDraw = false;
			String navLineOrientationStr = null;
			float navLineOrientation = 0.0f;
			boolean isContour = false;
			String depthValueStr = null;
			float depthValue = 0.0f;
			String dredgedAreaWidthStr = null;
			float dredgedAreaWidth = 0.0f;
			boolean isDredgedArea = false;
			String dredgedArea_minimum_depthStr = null;
			float dredgedArea_minimum_depth = 0.0f;
			String seamarkType = seamarkWay.getValueToKey("seamark:type");
			float[] intervalls = {15.0f,15.0f};
			DashPathEffect aDashPathEffect = new DashPathEffect (intervalls,0);
			if (seamarkType != null) {
				if (seamarkType.equals("fairway")) {
					paintColor = Color.CYAN;
					mustDraw = true;
				}
				if (seamarkType.equals("dredged_area")) {
					dredgedAreaWidthStr=seamarkWay.getValueToKey("seamark:dredged_area:width");
					dredgedArea_minimum_depthStr = seamarkWay.getValueToKey("seamark:dredged_area:minimum_depth");
					if (dredgedAreaWidthStr != null){
						isDredgedArea = true;
						dredgedAreaWidth = floatValueOfString(dredgedAreaWidthStr);
					}
					if (dredgedArea_minimum_depthStr != null){
						dredgedArea_minimum_depth = floatValueOfString(dredgedArea_minimum_depthStr);
					}
					paintColor = Color.CYAN;
					strokeWidth = 10;
					mustDraw = true;
				}
				if (seamarkType.equals("navigation_line")) {
					paintColor = Color.BLACK;
					navLineOrientationStr = seamarkWay.getValueToKey("seamark:navigation_line:orientation");
					if (navLineOrientationStr != null){
						// there may be a Problem see example v="180°18&#39;" from Wismarbucht4 way id="139699229" 
						// or v="006°24&#39;"  from way id="139691959" 
					}
					mustDraw = true;
				}
				if (seamarkType.equals("depth_contour")) {
					float[]  depthContourintervalls = {5.0f,5.0f};
					aDashPathEffect = new DashPathEffect (depthContourintervalls,0);
					depthValueStr = seamarkWay.getValueToKey("seamark:depth_contour:depth");
					paintColor = Color.GRAY;
					if (depthValueStr != null ) {
						depthValue = floatValueOfString(depthValueStr);
						if (depthValue < 3.5f ) {
							paintColor = Color.BLUE;
						}
					}
					
					mustDraw = true;
					isContour = true;
				}
			}
			ArrayList<SeamarkNode> nodeList = seamarkWay.getNodeList();
			if (nodeList != null && mustDraw) {
				Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				//Paint paint = new Paint();
				
				paint.setPathEffect(aDashPathEffect);
			    paint.setStrokeWidth(strokeWidth);
			    paint.setStyle(Style.STROKE);
				paint.setColor(paintColor);
				Path aPath = new Path();
				int countNodes = nodeList.size(); 
				coordinates = new float[2][countNodes];
				if (countNodes > 1) {
					int latE6= nodeList.get(0).getLatitudeE6();
					int lonE6= nodeList.get(0).getLongitudeE6();
					GeoPoint prevPoint = new GeoPoint(latE6,lonE6);
					Point aPrevPixelPoint = new Point();
					
					projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
					float x = aPrevPixelPoint.x -drawPosition.x;
					float y = aPrevPixelPoint.y - drawPosition.y;
					
					aPath.moveTo(x, y);
					for (int index = 1;index < countNodes; index ++){
						  latE6= nodeList.get(index).getLatitudeE6();
						  lonE6= nodeList.get(index).getLongitudeE6();
						  GeoPoint nextPoint = new GeoPoint(latE6,lonE6);
						  Point nextPixelPoint = new Point();
						  projection.toPoint(nextPoint, nextPixelPoint, drawZoomLevel);
						  x = nextPixelPoint.x -drawPosition.x;
						  y=  nextPixelPoint.y- drawPosition.y;
						  aPath.lineTo(x, y);
						  coordinates[0][index] = x;
						  coordinates[1][index] = y;
						  
						    
						
						  /*aPrevPixelPoint.x = aPrevPixelPoint.x - drawPosition.x;
						  aPrevPixelPoint.y = aPrevPixelPoint.y - drawPosition.y;
						  nextPixelPoint.x = nextPixelPoint.x - drawPosition.x;
						  nextPixelPoint.y = nextPixelPoint.y - drawPosition.y;
						  //canvas.drawLine(aPrevPixelPoint.x,aPrevPixelPoint.y, nextPixelPoint.x,nextPixelPoint.y, paint);
						  prevPoint = nextPoint;
						  projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);*/
						  
					}
					
					//aPath.close();
					aPath.setFillType(FillType.EVEN_ODD);
					canvas.drawPath(aPath, paint);// does not paint why??? canvas.drawLine???, x and y must be calculate with aPrevPixelPoint.x -drawPosition.x; 13_04_24
					// now we deal with the tetxt on the path
					if (isContour) {
						depthValueStr = seamarkWay.getValueToKey("seamark:depth_contour:depth");
						if (depthValueStr != null ) {
							paint.setTextSize(12);
							paint.setColor(paintColor);
							paint.setPathEffect(null);  // this is crucial, otherwise the text is drawn with the effect 13_05_15
							strokeWidth = 1.0f;
							paint.setStrokeWidth(strokeWidth);
							calculateRenderTextRepeater(depthValueStr, paint, null,coordinates, wayNames);
							int countWayNames = wayNames.size();
							for (int wayNamesIndex =0; wayNamesIndex < countWayNames; wayNamesIndex++){
								WayTextContainerOSM wayTextContainer = wayNames.get(wayNamesIndex);
								aPath.rewind();

								float[] textCoordinates = wayTextContainer.coordinates;
								aPath.moveTo(textCoordinates[0], textCoordinates[1]);
								for (int i = 2; i < textCoordinates.length; i += 2) {
									aPath.lineTo(textCoordinates[i], textCoordinates[i + 1]);
								}
								canvas.drawTextOnPath(wayTextContainer.text, aPath, 0, -3, wayTextContainer.paint);
							}
							
							//canvas.drawText(depthValue, x, y, paint);
				            // if we want to draw more text on the path, see mapsforge.map.databaserenderer.WayDecorator.renderText
							// CanvasRasterer.drawWayNames and WayTextContainer , see also DatabaseRenderer.renderway
							// there is the way length calculated from the coordinates of the way, like x,y 
							/*canvas.drawTextOnPath(depthValueStr, aPath, 0, -3, paint);
							canvas.drawTextOnPath(depthValueStr, aPath, 500.0f, -3, paint); //we try to draw a second time
							canvas.drawTextOnPath(depthValueStr, aPath, 1000.0f, -3, paint);
							canvas.drawTextOnPath(depthValueStr, aPath, 1500.0f, -3, paint);*/
							paint.setColor(paintColor);
						}
					}
					
					if (isDredgedArea && dredgedArea_minimum_depthStr != null ){
						
						paint.setTextSize(14);
						paint.setColor(Color.BLACK);
						paint.setPathEffect(null);  // this is crucial, otherwise the text is drawn with the effect 13_05_15
						strokeWidth = 1.0f;
						paint.setStrokeWidth(strokeWidth);
						canvas.drawTextOnPath(dredgedArea_minimum_depthStr, aPath, 0, 5, paint);
						paint.setColor(paintColor);
					}
					
				}
			}
		}
	}
	 
  private void drawRoute (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
	    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	    paint.setStrokeWidth(2);
		  paint.setColor(Color.RED);
		if (mContext.mRouteList != null) {
		  int count = mContext.mRouteList.size();
		   if (count > 1 && mContext.mShowRoute){
			   GeoPoint prevPoint = mContext.mRouteList.get(0);
			   Point aPrevPixelPoint = new Point();
			   projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
			   for (int index = 1;index < count; index ++){
				 
				  GeoPoint nextPoint = mContext.mRouteList.get(index) ;
				  Point nextPixelPoint = new Point();
				  projection.toPoint(nextPoint, nextPixelPoint, drawZoomLevel);
				 
				  aPrevPixelPoint.x = aPrevPixelPoint.x - drawPosition.x;
				  aPrevPixelPoint.y = aPrevPixelPoint.y - drawPosition.y;
				  nextPixelPoint.x = nextPixelPoint.x - drawPosition.x;
				  nextPixelPoint.y = nextPixelPoint.y - drawPosition.y;
				  canvas.drawLine(aPrevPixelPoint.x,aPrevPixelPoint.y, nextPixelPoint.x,nextPixelPoint.y, paint);
				  prevPoint = nextPoint;
				  projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
				  
			   }
		   }
		}
  }
  
  private void drawSeamarks(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		int count = mDisplayedSeamarkNodeList.size();
		Point aPixelPoint = new Point();
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.BLACK);
		//paint.setStyle(Paint.Style.STROKE);
		//paint.setStrokeWidth(1);
		paint.setTextSize(16);
		paint.setTypeface(Typeface.MONOSPACE);
		for (int index = 0; index < count; index++ ) {
			SeamarkNode seamarkNode = mDisplayedSeamarkNodeList.get(index);
			SeamarkDrawable aSeamarkDrawable = new SeamarkDrawable(seamarkNode,drawZoomLevel,mDisplayFactor);
			Bitmap aSymbolBitmap = aSeamarkDrawable.getBitmap();
			if (aSymbolBitmap != null){
				GeoPoint geoPoint = new GeoPoint(seamarkNode.getLatitudeE6(),seamarkNode.getLongitudeE6());
				projection.toPoint(geoPoint, aPixelPoint, drawZoomLevel);
				int x = aPixelPoint.x;
			    x = x - drawPosition.x;
			    int y = aPixelPoint.y;
			    y = y - drawPosition.y;
			    float left = x - aSymbolBitmap.getWidth() / 2.0f;
			    float right = x + aSymbolBitmap.getWidth() / 2.0f;
			    float bottom = y + aSymbolBitmap.getHeight() / 2.0f;
			    float top = y - aSymbolBitmap.getHeight() /2.0f;
			    RectF rectF = new RectF();
			    rectF.set(left, top, right, bottom);
			    //drawCenterAndFrame(canvas,rectF);
				canvas.drawBitmap(aSymbolBitmap, left,top, null);
				if (drawZoomLevel > 13) {
					   String nameStr = seamarkNode.getValueToKey("seamark:name");
					   float drawNamePosY = top + aSymbolBitmap.getHeight() * 0.55f;
					   if (drawZoomLevel > 15){
						   // make room for the light description
						   drawNamePosY = top + aSymbolBitmap.getHeight() * 0.25f;
					   }
					   if(nameStr!=null){
						        paint.setTypeface(Typeface.DEFAULT_BOLD);
								switch (mShowNameStatus) {
								case ViewerGlobals.seamarkLongNameVisible:
									 paint.setTextSize(14);
									canvas.drawText(nameStr, left + aSymbolBitmap.getWidth() * 0.65f,drawNamePosY, paint);
									break;
								case ViewerGlobals.seamarkShortNameVisible:
									String shortText = nameStr;
									if (nameStr.length()> 5) {
										shortText = nameStr.substring(0, 5) +"..";
									}
									paint.setTextSize(14);
									canvas.drawText(shortText, left + aSymbolBitmap.getWidth() * 0.65f,drawNamePosY, paint);
									break;
								default:
									break;
								}
					   }
				   }
				
				
				
				if (drawZoomLevel > 15){
					
					
					String lightDescription = seamarkNode.getValueToKey("light:description");
					paint.setTypeface(Typeface.DEFAULT);
					if (lightDescription != null) {
						paint.setTextSize(14);
						canvas.drawText(lightDescription, left + aSymbolBitmap.getWidth() * 0.65f,top + aSymbolBitmap.getHeight() * 0.55f, paint);
					} else {
						String characterStr = seamarkNode.getValueToKey("seamark:light:character");
						String colorStr = seamarkNode.getValueToKey("seamark:light:colour");
						String heightStr = seamarkNode.getValueToKey("seamark:light:height");
						String periodStr = seamarkNode.getValueToKey("seamark:light:period");
						String rangeStr = seamarkNode.getValueToKey("seamark:light:range");
						StringBuffer buf = new StringBuffer();
						if (characterStr != null) {
							buf.append(characterStr);
							buf.append(".");
							if (colorStr != null) {
								colorStr = colorStr.substring(0,1);
								colorStr = colorStr.toUpperCase(Locale.US);
								buf.append(colorStr);
								buf.append(".");
							}
							if (periodStr!= null) {
								buf.append(periodStr);
								buf.append("s");
							}
							if (rangeStr != null) {
								buf.append(rangeStr);
								buf.append("m");
							}
							if (heightStr != null){
								buf.append(heightStr);
								buf.append("M");
							}
						}
						String info = buf.toString();
						paint.setTextSize(14);
						canvas.drawText(info, left + aSymbolBitmap.getWidth() * 0.65f,top + aSymbolBitmap.getHeight() * 0.55f, paint);
					}
					
					
				}
			}
		}
	}
  
  
    public void setShowNameStatus(int aStatus){
		if (aStatus != mShowNameStatus ){
			mShowNameStatus = aStatus;
			this.requestRedraw();
		}
		
	}
	public int getShowNameStatus(){
		return mShowNameStatus;
	}
  
    public void addNode (SeamarkNode seamarkNode){
		if (!mDisplayedSeamarkNodeList.contains(seamarkNode)){
		   mDisplayedSeamarkNodeList.add(seamarkNode);
		}
	}
  
    
	
	public void removeNode (SeamarkNode seamarkNode) {
		if (mDisplayedSeamarkNodeList.contains(seamarkNode)){
	         mDisplayedSeamarkNodeList.remove(seamarkNode);
		}
	}
	
	public void addSeamarkWay(SeamarkWay seamarkWay){
		if (!mDisplayedSeamarkWays.contains(seamarkWay)){
		    mDisplayedSeamarkWays.add(seamarkWay);
		}
	}
	
	public void removeSeamarkWay(SeamarkWay seamarkWay){
		if (mDisplayedSeamarkWays.contains(seamarkWay)){
		    mDisplayedSeamarkWays.remove(seamarkWay);
		}
	}
  
  private void drawMainFires (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
	    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		float factor = 1.0f;
		if (drawZoomLevel > 12) factor = (float)(drawZoomLevel - 12);
		Point aPixelPoint = new Point();
		int count = mContext.mNodeListMainLights.size();
		for (int index = 0; index < count; index++ ) {
			SeamarkNode aSeamark = mContext.mNodeListMainLights.get(index);
			
			if (aSeamark != null) {
			     String aType = aSeamark.getValueToKey("seamark:type");
			     int aLatE6 = aSeamark.getLatitudeE6();
				 int aLonE6 = aSeamark.getLongitudeE6();
				 GeoPoint geoPoint = new GeoPoint(aLatE6,aLonE6);
			     projection.toPoint(geoPoint, aPixelPoint, drawZoomLevel);
			     
			     float x = aPixelPoint.x;
			     x = x - drawPosition.x;
			     float y = aPixelPoint.y;
			     y = y - drawPosition.y;
			    // first we check if we have an unnumbered fire
			    String lightColor = aSeamark.getValueToKey("seamark:light:colour"); 
			    if (lightColor!= null) {
			    	String lightRange = aSeamark.getValueToKey("seamark:light:range");
					 float range = 10.0f;
					 try {
							if (lightRange != null)  range = Float.parseFloat(lightRange);
						 } catch (Exception e) {}
					 float sweepAngle = 360.0f;
					 float dx = 80.0f  * factor *(range / 10.0f);
				     float dy = 80.0f * factor * (range / 10.0f);
				     RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
				     byte zoom = mContext.mLastZoom;
				     int baseStroke = 5;
					   if (zoom < 12 ) {
						   baseStroke = 3;
					   }
					   if (zoom < 11 ) {
						   baseStroke = 2;
					   }
					   if (zoom < 10 ) {
						   baseStroke = 1;
					   }
				     paint.setStrokeWidth(baseStroke);
				    /* paint.setColor(Color.BLUE);
					 paint.setStyle(Style.STROKE);
				     canvas.drawRect(aRectF, paint);*/
				     paint.setStyle(Style.STROKE);
				     paint.setColor(getColor(lightColor));
				     canvas.drawArc(aRectF, 0.0f + 90, - sweepAngle, false,paint);		 
			    }
			  } // seamark != null
	     } // for  
  }

  
  private void drawSectorFires (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
	    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		float factor = 1.0f;
		if (drawZoomLevel > 12) factor = (float)(drawZoomLevel - 12);
		Point aPixelPoint = new Point();
		int count = mContext.mNodeListSectorFire.size();
		for (int index = 0; index < count; index++ ) {
			SeamarkNode aSeamark = mContext.mNodeListSectorFire.get(index);
			
			if (aSeamark != null) {
			     String aType = aSeamark.getValueToKey("seamark:type");
			     int aLatE6 = aSeamark.getLatitudeE6();
				 int aLonE6 = aSeamark.getLongitudeE6();
				 GeoPoint geoPoint = new GeoPoint(aLatE6,aLonE6);
			     projection.toPoint(geoPoint, aPixelPoint, drawZoomLevel);
			     
			     float x = aPixelPoint.x;
			     x = x - drawPosition.x;
			     float y = aPixelPoint.y;
			     y = y - drawPosition.y;
			    /*// first we check if we have an unnumbered fire
			    String lightColor = aSeamark.getValueToKey("seamark:light:colour"); 
			    if (lightColor!= null) {
			    	String lightRange = aSeamark.getValueToKey("seamark:light:range");
					 float range = 10.0f;
					 try {
							if (lightRange != null)  range = Float.parseFloat(lightRange);
						 } catch (Exception e) {}
					 float sweepAngle = 360.0f;
					 float dx = 80.0f  * factor *(range / 10.0f);
				     float dy = 80.0f * factor * (range / 10.0f);
				     RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
				     byte zoom = context.mLastZoom;
				     int baseStroke = 5;
					   if (zoom < 12 ) {
						   baseStroke = 3;
					   }
					   if (zoom < 11 ) {
						   baseStroke = 2;
					   }
					   if (zoom < 10 ) {
						   baseStroke = 1;
					   }
				     paint.setStrokeWidth(baseStroke);
				     paint.setColor(Color.BLUE);
					 paint.setStyle(Style.STROKE);
				     canvas.drawRect(aRectF, paint);
				     paint.setStyle(Style.STROKE);
				     paint.setColor(getColor(lightColor));
				     canvas.drawArc(aRectF, 0.0f + 90, - sweepAngle, false,paint);		 
			    }*/
			    // we check if we have numbered fires 
			    int lightnr = 1;
			    String nrStr = Integer.toString(lightnr);
			    String lightNrColor = aSeamark.getValueToKey("seamark:light:"+nrStr+":colour");
				while ( lightNrColor != null ){
					 nrStr = Integer.toString(lightnr);
					 String category = aSeamark.getValueToKey("seamark:light:"+nrStr+":category");
					 if (category != null && category.equals("directional")){
						 // directional fire  we simulate a sector with the orientation
						
						 String orientationStr = aSeamark.getValueToKey("seamark:light:"+nrStr+"orientation");
						 float orientation = 0;
						 try {
								if (orientationStr != null)  orientation = Float.parseFloat(orientationStr);
							 } catch (Exception e) {}
						 String lightRange = aSeamark.getValueToKey("seamark:light:"+nrStr+":range");
						 float range = 10.0f;
						 try {
								if (lightRange != null)  range = Float.parseFloat(lightRange);
							 } catch (Exception e) {}
						 orientation = orientation - 45f;
						 float startAngle = orientation -0.5f;
						 float endAngle = orientation + 0.5f;
						 float sweepAngle = 0.0f;
						 if ( endAngle >= startAngle ) {
								 sweepAngle =  Math.abs(endAngle - startAngle);
							 } else {
								 sweepAngle = Math.abs(endAngle + 360 - startAngle);
							 }
							 
						 float dx = 40.0f  * factor *(range / 2.5f);
					     float dy = 40.0f * factor * (range / 2.5f);
					     boolean useCenter = false;
					     paint.setStyle(Style.STROKE);
					     if (mFillDirectionalSector && (drawZoomLevel > mShowFilledDirectionalSectorZoom )
								  && lightNrColor.equals("white") && (Math.abs(sweepAngle)< 15.0f)) {
								// we have a directional sector
								dx= dx*2;
								dy= dy*2;
								useCenter = true;
								paint.setStyle(Style.FILL);
							 }
						 RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
						 byte zoom = mContext.mLastZoom;
						 int baseStroke = 5;
						 if (zoom < 12 ) {
							   baseStroke = 3;
						   }
						   if (zoom < 11 ) {
							   baseStroke = 2;
						   }
						   if (zoom < 10 ) {
							   baseStroke = 1;
						   }
						  paint.setStrokeWidth(baseStroke);
						  
						  paint.setColor(getColor(lightNrColor));
						  canvas.drawArc(aRectF, endAngle + 90, -sweepAngle, useCenter,paint);
						  double aRad = Math.toRadians(endAngle +90); 
						  float endLineX = dx* (float) Math.cos(aRad);
						  float endLineY = dy *(float) Math.sin(aRad);
						  paint.setColor(Color.BLACK);
						  paint.setStrokeWidth(0.5f);
						  canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
						  aRad = Math.toRadians(startAngle +90); 
						  endLineX = dx* (float) Math.cos(aRad);
						  endLineY = dy *(float) Math.sin(aRad);
						  paint.setColor(Color.BLACK);
						  paint.setStrokeWidth(0.5f);
						  canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
						   
					 } // if category directional
					 else {
					 String lightSectorStart = aSeamark.getValueToKey("seamark:light:"+nrStr+":sector_start");
					 if (lightSectorStart != null){ // we have a sector fire
						 String lightSectorEnd = aSeamark.getValueToKey("seamark:light:"+nrStr+":sector_end");
						 if (lightSectorEnd != null && !lightSectorStart.equals("shore") && !lightSectorEnd.equals("shore")) {
								// we cannot calculate the sector with the parameter shore
							 String lightRange = aSeamark.getValueToKey("seamark:light:"+nrStr+":range");
							 float startAngle = 0.0f;
							 float endAngle = 0.0f;
							 float range = 10.0f;
							 try {
								 startAngle = Float.parseFloat(lightSectorStart);
								 endAngle = Float.parseFloat(lightSectorEnd);
							 } catch ( Exception e) {}
							 try {
								if (lightRange != null)  range = Float.parseFloat(lightRange);
							 } catch (Exception e) {}
							 float sweepAngle = 0.0f;
							 if ( endAngle >= startAngle ) {
								 sweepAngle =  Math.abs(endAngle - startAngle);
							 } else {
								 sweepAngle = Math.abs(endAngle + 360 - startAngle);
							 }
							 
							 float dx = 40.0f  * factor *(range / 2.5f);
						     float dy = 40.0f * factor * (range / 2.5f);
						     boolean useCenter = false;
						     paint.setStyle(Style.STROKE);
						     if (mFillDirectionalSector && (drawZoomLevel > mShowFilledDirectionalSectorZoom )
									  && lightNrColor.equals("white") && (Math.abs(sweepAngle)< 15.0f)) {
									// we have a directional sector
									dx= dx*2;
									dy= dy*2;
									useCenter = true;
									paint.setStyle(Style.FILL);
								 }
						     RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
						     byte zoom = mContext.mLastZoom;
						     int baseStroke = 5;
							   if (zoom < 12 ) {
								   baseStroke = 3;
							   }
							   if (zoom < 11 ) {
								   baseStroke = 2;
							   }
							   if (zoom < 10 ) {
								   baseStroke = 1;
							   }
						     paint.setStrokeWidth(baseStroke);
						    /* paint.setColor(Color.BLUE);
							 paint.setStyle(Style.STROKE);
						     canvas.drawRect(aRectF, paint);*/
						    
						     paint.setColor(getColor(lightNrColor));
						     canvas.drawArc(aRectF, endAngle + 90, -sweepAngle, useCenter,paint);
						     double aRad = Math.toRadians(endAngle +90); 
						     float endLineX = dx* (float) Math.cos(aRad);
						     float endLineY = dy *(float) Math.sin(aRad);
						     paint.setColor(Color.BLACK);
						     paint.setStrokeWidth(0.5f);
						     canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
						     aRad = Math.toRadians(startAngle +90); 
						     endLineX = dx* (float) Math.cos(aRad);
						     endLineY = dy *(float) Math.sin(aRad);
						     paint.setColor(Color.BLACK);
						     paint.setStrokeWidth(0.5f);
						     canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
							 /*paint.setColor(Color.RED);
							 canvas.drawArc(aRectF, 0, 45, false,paint);*/
						 } 
					 } // if lightSector start
					 else {
					 String lightRange = aSeamark.getValueToKey("seamark:light:"+nrStr+":range");
					 if (lightRange != null) {
						 float range = 10.0f;
						 try {
								if (lightRange != null)  range = Float.parseFloat(lightRange);
							 } catch (Exception e) {}
						 float dx = 80.0f  * factor *(range / 10.0f);
						 float dy = 80.0f * factor * (range / 10.0f);
						 RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
						 byte zoom = mContext.mLastZoom;
						 int baseStroke = 5;
						 if (zoom < 12 ) {
							   baseStroke = 3;
						   }
						   if (zoom < 11 ) {
							   baseStroke = 2;
						   }
						   if (zoom < 10 ) {
							   baseStroke = 1;
						   }
						  paint.setStrokeWidth(baseStroke);
						  paint.setStyle(Style.STROKE);
						  paint.setColor(getColor(lightNrColor));
						  canvas.drawArc(aRectF, 0, 360, false,paint);
						  
					 }
					 
					}
					 
				 }
					 lightnr ++;
					 nrStr = Integer.toString(lightnr);
					 lightNrColor = aSeamark.getValueToKey("seamark:light:"+nrStr+":colour");	 
					 
			   }// while lightNrColor != null
				
					 
	    } // seamark != null
   } // for  
} // drawSectorFires
  
  
  private void drawSectorFiresOld (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
	    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		float factor = 1.0f;
		if (drawZoomLevel > 12) factor = (float)(drawZoomLevel - 12);
		Point aPixelPoint = new Point();
		int count = mContext.mNodeListSectorFire.size();
		for (int index = 0; index < count; index++ ) {
			SeamarkNode aSeamark = mContext.mNodeListSectorFire.get(index);
			
			if (aSeamark != null) {
			     String aType = aSeamark.getValueToKey("seamark:type");
			     int aLatE6 = aSeamark.getLatitudeE6();
				 int aLonE6 = aSeamark.getLongitudeE6();
				 GeoPoint geoPoint = new GeoPoint(aLatE6,aLonE6);
			     projection.toPoint(geoPoint, aPixelPoint, drawZoomLevel);
			     
			     float x = aPixelPoint.x;
			     x = x - drawPosition.x;
			     float y = aPixelPoint.y;
			     y = y - drawPosition.y;
			    /*// first we check if we have an unnumbered fire
			    String lightColor = aSeamark.getValueToKey("seamark:light:colour"); 
			    if (lightColor!= null) {
			    	String lightRange = aSeamark.getValueToKey("seamark:light:range");
					 float range = 10.0f;
					 try {
							if (lightRange != null)  range = Float.parseFloat(lightRange);
						 } catch (Exception e) {}
					 float sweepAngle = 360.0f;
					 float dx = 80.0f  * factor *(range / 10.0f);
				     float dy = 80.0f * factor * (range / 10.0f);
				     RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
				     byte zoom = context.mLastZoom;
				     int baseStroke = 5;
					   if (zoom < 12 ) {
						   baseStroke = 3;
					   }
					   if (zoom < 11 ) {
						   baseStroke = 2;
					   }
					   if (zoom < 10 ) {
						   baseStroke = 1;
					   }
				     paint.setStrokeWidth(baseStroke);
				     paint.setColor(Color.BLUE);
					 paint.setStyle(Style.STROKE);
				     canvas.drawRect(aRectF, paint);
				     paint.setStyle(Style.STROKE);
				     paint.setColor(getColor(lightColor));
				     canvas.drawArc(aRectF, 0.0f + 90, - sweepAngle, false,paint);		 
			    }*/
			    // we check if we have numbered fires 
			    int lightnr = 1;
			    String nrStr = Integer.toString(lightnr);
			    String lightNrColor = aSeamark.getValueToKey("seamark:light:"+nrStr+":colour");
				while ( lightNrColor != null ){
					 nrStr = Integer.toString(lightnr);
					 String category = aSeamark.getValueToKey("seamark:light:"+nrStr+":category");
					 if (category != null && category.equals("directional")){
						 // directional fire  we simulate a sector with the orientation
						
						 String orientationStr = aSeamark.getValueToKey("seamark:light:"+nrStr+"orientation");
						 float orientation = 0;
						 try {
								if (orientationStr != null)  orientation = Float.parseFloat(orientationStr);
							 } catch (Exception e) {}
						 String lightRange = aSeamark.getValueToKey("seamark:light:"+nrStr+":range");
						 float range = 10.0f;
						 try {
								if (lightRange != null)  range = Float.parseFloat(lightRange);
							 } catch (Exception e) {}
						 orientation = orientation - 45f;
						 float startAngle = orientation -0.5f;
						 float endAngle = orientation + 0.5f;
						 float sweepAngle = 0.0f;
						 if ( endAngle >= startAngle ) {
								 sweepAngle =  Math.abs(endAngle - startAngle);
							 } else {
								 sweepAngle = Math.abs(endAngle + 360 - startAngle);
							 }
							 
						 float dx = 80.0f  * factor *(range / 10.0f);
						 float dy = 80.0f * factor * (range / 10.0f);
						 RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
						 byte zoom = mContext.mLastZoom;
						 int baseStroke = 5;
						 if (zoom < 12 ) {
							   baseStroke = 3;
						   }
						   if (zoom < 11 ) {
							   baseStroke = 2;
						   }
						   if (zoom < 10 ) {
							   baseStroke = 1;
						   }
						  paint.setStrokeWidth(baseStroke);
						  paint.setStyle(Style.STROKE);
						  paint.setColor(getColor(lightNrColor));
						  canvas.drawArc(aRectF, endAngle + 90, -sweepAngle, false,paint);
						  double aRad = Math.toRadians(endAngle +90); 
						  float endLineX = dx* (float) Math.cos(aRad);
						  float endLineY = dy *(float) Math.sin(aRad);
						  paint.setColor(Color.BLACK);
						  paint.setStrokeWidth(0.5f);
						  canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
						  aRad = Math.toRadians(startAngle +90); 
						  endLineX = dx* (float) Math.cos(aRad);
						  endLineY = dy *(float) Math.sin(aRad);
						  paint.setColor(Color.BLACK);
						  paint.setStrokeWidth(0.5f);
						  canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
						   
					 } 
					 String lightSectorStart = aSeamark.getValueToKey("seamark:light:"+nrStr+":sector_start");
					 if (lightSectorStart != null){ // we have a sector fire
						 String lightSectorEnd = aSeamark.getValueToKey("seamark:light:"+nrStr+":sector_end");
						 String lightRange = aSeamark.getValueToKey("seamark:light:"+nrStr+":range");
						 float startAngle = 0.0f;
						 float endAngle = 0.0f;
						 float range = 10.0f;
						 try {
							 startAngle = Float.parseFloat(lightSectorStart);
							 endAngle = Float.parseFloat(lightSectorEnd);
						 } catch ( Exception e) {}
						 try {
							if (lightRange != null)  range = Float.parseFloat(lightRange);
						 } catch (Exception e) {}
						 float sweepAngle = 0.0f;
						 if ( endAngle >= startAngle ) {
							 sweepAngle =  Math.abs(endAngle - startAngle);
						 } else {
							 sweepAngle = Math.abs(endAngle + 360 - startAngle);
						 }
						 
						 float dx = 80.0f  * factor *(range / 10.0f);
					     float dy = 80.0f * factor * (range / 10.0f);
					     RectF aRectF = new RectF(x - dx ,y - dy,x + dx ,y + dy );
					     byte zoom = mContext.mLastZoom;
					     int baseStroke = 5;
						   if (zoom < 12 ) {
							   baseStroke = 3;
						   }
						   if (zoom < 11 ) {
							   baseStroke = 2;
						   }
						   if (zoom < 10 ) {
							   baseStroke = 1;
						   }
					     paint.setStrokeWidth(baseStroke);
					    /* paint.setColor(Color.BLUE);
						 paint.setStyle(Style.STROKE);
					     canvas.drawRect(aRectF, paint);*/
					     paint.setStyle(Style.STROKE);
					     paint.setColor(getColor(lightNrColor));
					     canvas.drawArc(aRectF, endAngle + 90, -sweepAngle, false,paint);
					     double aRad = Math.toRadians(endAngle +90); 
					     float endLineX = dx* (float) Math.cos(aRad);
					     float endLineY = dy *(float) Math.sin(aRad);
					     paint.setColor(Color.BLACK);
					     paint.setStrokeWidth(0.5f);
					     canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
					     aRad = Math.toRadians(startAngle +90); 
					     endLineX = dx* (float) Math.cos(aRad);
					     endLineY = dy *(float) Math.sin(aRad);
					     paint.setColor(Color.BLACK);
					     paint.setStrokeWidth(0.5f);
					     canvas.drawLine(x,y, x + endLineX,y +  endLineY, paint);
						 /*paint.setColor(Color.RED);
						 canvas.drawArc(aRectF, 0, 45, false,paint);*/
					 } // else ( sector fire)
					 lightnr ++;
					 nrStr = Integer.toString(lightnr);
					 lightNrColor = aSeamark.getValueToKey("seamark:light:"+nrStr+":colour");
			   }// while lightNrColor 1= null
				
					 
	    } // seamark != null
     } // for  
  } // drawSectorFires
  
  private int getColor (String aColor) {
	  int result = Color.CYAN;
	  if (aColor == null) return result;
	   if (aColor.equals("green")) {
		   result = Color.GREEN;
	   } else 
	   if (aColor.equals("red")) {
		   result = Color.RED; 
	   }
	   else 
	   if (aColor.equals("yellow")) {
		   result = Color.YELLOW;
	   } else 
	   if (aColor.equals("white")) {
		  result =Color.YELLOW;
	   } 
	   return result;
  }
  
}
