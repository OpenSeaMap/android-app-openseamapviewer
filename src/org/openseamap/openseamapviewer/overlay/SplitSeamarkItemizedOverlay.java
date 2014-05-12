package org.openseamap.openseamapviewer.overlay;

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
import java.util.List;
import java.util.Locale;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.seamarks.SeamarkNode;
import org.openseamap.openseamapviewer.seamarks.SeamarkWithPoisOverlayItem;
import org.openseamap.openseamapviewer.viewer.SplitOpenSeaMapWithAerialOverlayAndPoisViewer;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

public class SplitSeamarkItemizedOverlay extends ArrayItemizedOverlay {
	private static final String TAG="SeamarkItemizedOverlay";
	private final SplitOpenSeaMapWithAerialOverlayAndPoisViewer mContext;
	boolean mMustShowCenter;
	private boolean mFillDirectionalSector;
	private byte mShowFilledDirectionalSectorZoom;
	/**
	 * Constructs a new SeamarkItemizedOverlay.
	 * 
	 * @param defaultMarker
	 *            the default marker (may be null).
	 * @param context
	 *            the reference to the application context.
	 */
	public SplitSeamarkItemizedOverlay(Drawable defaultMarker, SplitOpenSeaMapWithAerialOverlayAndPoisViewer context, boolean fillDirectionalSector) {
		super(defaultMarker);
		this.mContext = context;
		this.mMustShowCenter = true;
		this.mFillDirectionalSector = fillDirectionalSector;
		this.mShowFilledDirectionalSectorZoom = 13;
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
		String aTitle = item.getTitle();
		String aInfo = item.getSnippet();
		String harborInfo=null;
		if (aInfo.contains("harbour")) {
			harborInfo =getHarborInfo(aItemPoint);
			
		}
		
		
		final String harborUrl = harborInfo;
		Builder builder = new AlertDialog.Builder(this.mContext);
		builder.setIcon(android.R.drawable.ic_menu_info_details);
		builder.setTitle(item.getTitle());
		if (harborInfo != null) {
			aInfo = aInfo + "\n" + harborInfo;
		}
		builder.setMessage(aInfo);
		builder.setPositiveButton("cancel", null);
		if (harborInfo != null ) {
			builder.setNeutralButton("call browser", new OnClickListener(){
				public void onClick(DialogInterface dialog , int which) {
					if (harborUrl != null){
						startBrowser(harborUrl);
					}	
				}
		    });
		}
		builder.show();    
		return true;
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
	
	private String getHarborInfo(GeoPoint geoPoint){
		String result=null;
		//  example Timmendorf_-_Poel LAT 53.992036 LON 11.375013
		//  http://harbor.openseamap.org/getHarbours.php?b=53.9&t=54.0&l=11.36&r=11.38&ucid=113&maxSize=5
        // 
		// calculate a bounding box
		double lon = geoPoint.getLongitude();
		double lat  = geoPoint.getLatitude();
		double rad = 0.01;
		double left = lon - rad;
		double right = lon +rad;
		double bottom= lat - rad;
		double top = lat + rad;
		String protocoll = "http";
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
	
			}
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
	private String downloadUrlAsString(URL testurl) throws IOException{
		InputStream   testinputStream = null;
		try {
			
			HttpURLConnection testcon = (HttpURLConnection) testurl.openConnection();
	        testcon.setConnectTimeout(1000);
	        testcon.setReadTimeout(1000);
	        testcon.setRequestMethod("GET");
	        testcon.connect(); 
	        int response = testcon.getResponseCode();
	        Log.d(TAG,"the request code " + response);
	        testinputStream = testcon.getInputStream();
	        String contentAsString = readIt(testinputStream, 200);
			Log.d(TAG,contentAsString);
			return contentAsString;
		   } finally {
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
		    	//drawMajorLights(canvas,drawPosition, projection, drawZoomLevel);
		    	drawRoute(canvas,drawPosition, projection, drawZoomLevel);
		    	showMyUpdatedPosition(canvas,drawPosition, projection, drawZoomLevel);
		    }
		    else {
		    	super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
		    	if (mContext.showSectorFires )  {
		    		drawMainFires(canvas,drawPosition, projection, drawZoomLevel);
					drawSectorFires(canvas,drawPosition, projection, drawZoomLevel);
				}
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
  
  private void drawMainFires (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
	    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		float factor = 1.0f;
		if (drawZoomLevel > 12) factor = (float)(drawZoomLevel - 12);
		Point aPixelPoint = new Point();
		int count = mContext.mNodeListSingleLights.size();
		for (int index = 0; index < count; index++ ) {
			SeamarkNode aSeamark = mContext.mNodeListSingleLights.get(index);
			
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
		int count = mContext.mNodeListNumberedFires.size();
		for (int index = 0; index < count; index++ ) {
			SeamarkNode aSeamark = mContext.mNodeListNumberedFires.get(index);
			
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
						 if (mFillDirectionalSector && (drawZoomLevel > mShowFilledDirectionalSectorZoom) 
								 &&lightNrColor.equals("white") && (Math.abs(sweepAngle)< 15.0f)) {
							// we have a directional sector
							dx= dx*2;
							dy= dy*2;
							useCenter = true;
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
						 if (!lightSectorStart.equals("shore") && !lightSectorEnd.equals("shore")) {
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
							 if (mFillDirectionalSector && (drawZoomLevel > mShowFilledDirectionalSectorZoom) 
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
		int count = mContext.mNodeListNumberedFires.size();
		for (int index = 0; index < count; index++ ) {
			SeamarkNode aSeamark = mContext.mNodeListNumberedFires.get(index);
			
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
