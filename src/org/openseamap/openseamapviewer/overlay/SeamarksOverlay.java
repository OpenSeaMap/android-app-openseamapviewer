package org.openseamap.openseamapviewer.overlay;

import java.util.ArrayList;
import java.util.Locale;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.ViewerGlobals;
import org.openseamap.openseamapviewer.seamarks.SeamarkDrawable;
import org.openseamap.openseamapviewer.seamarks.SeamarkNode;
import org.openseamap.openseamapviewer.seamarks.SeamarkOsm;
import org.openseamap.openseamapviewer.seamarks.SeamarkWay;
import org.openseamap.openseamapviewer.viewer.OpenSeaMapViewer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class SeamarksOverlay extends Overlay {
	
	private static final boolean test = false;
	private static final String TAG = "SeamarksOverlay";
	private Paint mPaint;
	private RectF mOval;
	private boolean mMustShowCenter = true;
	private int mShowNameStatus = 0;  // 0 no show, 1 short name, 2 long Name
	private ArrayList<SeamarkOverlayItem> mOverlayItemList;
	
	private ArrayList<SeamarkNode> mDisplayedSeamarkNodeList;
	//private ArrayList<NavigationLine> mDisplayedNavigationLines = null;
	private ArrayList<SeamarkWay> mDisplayedSeamarkWays = null;
	
    private final OpenSeaMapViewer mContext;
    private final SeamarkOsm mSeamarkOsm;
    private final Drawable defaultMarker;
    private boolean mFillDirectionalSector;
    private byte mShowFilledDirectionalSectorZoom;
    
    private float mDisplayFactor = 1.0f;
	/**
	 * Constructs a new SeamarkItemizedOverlay.
	 * 
	 * @param defaultMarker
	 *            the default marker (may be null).
	 * @param context
	 *            the reference to the application context.
	 */
	public SeamarksOverlay(Drawable defaultMarker, OpenSeaMapViewer context,SeamarkOsm seamarkOsm, boolean fillDirectionalSector,float  pDisplayFactor) {
		super();
		this.defaultMarker = defaultMarker;
		this.mContext = context;
		this.mSeamarkOsm = seamarkOsm;
		this.mOverlayItemList = new ArrayList<SeamarkOverlayItem>();
		this.mDisplayedSeamarkNodeList = new ArrayList<SeamarkNode>();
		//this.mDisplayedNavigationLines = new ArrayList<NavigationLine>();
		this.mDisplayedSeamarkWays = new ArrayList<SeamarkWay>();
		this.mShowNameStatus = 0;
		this.mFillDirectionalSector = fillDirectionalSector;
		this.mShowFilledDirectionalSectorZoom = 13;
		this.mDisplayFactor = pDisplayFactor;
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
	
	public void setMustShow(boolean pMustShow) {
		mMustShowCenter = pMustShow;
		this.requestRedraw();
	}
	
	
	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition,Projection projection, byte drawZoomLevel) {
		this.mPaint = new Paint();
		this.mPaint.setStyle(Paint.Style.STROKE);
		this.mPaint.setStrokeWidth(4);
		this.mPaint.setColor(Color.RED);
		if (test) Log.d(TAG,"drawOverlayBitmap begin");
		
		if (mMustShowCenter) {
			int aWidth = canvas.getWidth();
			int aHeight = canvas.getHeight();
			Point aP = new Point(aWidth / 2, aHeight / 2);
			// projection.toPoint(drawPosition, aPixelPoint, drawZoomLevel);
			this.mOval = new RectF(aP.x - 5, aP.y - 5, aP.x + 5, aP.y + 5);
			canvas.drawOval(this.mOval, this.mPaint);
		}
		if (mSeamarkOsm.getSeamarkFileReadComplete()){
			if (test) Log.d(TAG,"call updateSeamarks");
			if (mContext.mMyLocationChanged) {
				// this is a only a update of my position, so we can proceed with the old seamarks
				// we do nothing
			}else {
				// this is a real redraw, since the map was moved or zoom changed
				// we must update all the drawing lists
				mContext.updateSeamarkNodesOnOverlay();
			}
				
			
			if (test) Log.d(TAG, "after call updateSeamarks");
	    	if (mContext.showSectorFires )  {
	    		drawMainFires(canvas,drawPosition, projection, drawZoomLevel);
				drawSectorFires(canvas,drawPosition, projection, drawZoomLevel);
			}
	    	//drawNavLines(canvas,drawPosition, projection, drawZoomLevel);
	    	drawSeamarkWays(canvas,drawPosition, projection, drawZoomLevel);
	    	drawSeamarks(canvas,drawPosition, projection, drawZoomLevel);
	    	
	    	drawRoute(canvas,drawPosition, projection, drawZoomLevel);
	    	showMyUpdatedPosition(canvas,drawPosition,projection,drawZoomLevel);
		}
		
		if (test) Log.d(TAG,"drawOverlayBitmap end");	
	}
	
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
			 if (mContext.mMyLocationChanged) {
				 mContext.mMyLocationChanged = false;
				 // we handled it, see OpenSeaMapViewer.requestOverlayRedraw
			 }
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
	
	private void drawSeamarkWay (SeamarkWay seamarkWay, Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		if (seamarkWay != null){
			ArrayList<SeamarkNode> nodeList = seamarkWay.getNodeList();
			if (nodeList != null) {
				Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				//Paint paint = new Paint();
				float[] intervalls = {5.0f,5.0f};
				DashPathEffect aDashPathEffect = new DashPathEffect (intervalls,0);
				paint.setPathEffect(aDashPathEffect);
			    paint.setStrokeWidth(2);
			    paint.setStyle(Style.STROKE);
				paint.setColor(Color.BLUE);
				Path aPath = new Path();
				int countNodes = nodeList.size();
				if (countNodes > 1) {
					int latE6= nodeList.get(0).getLatitudeE6();
					int lonE6= nodeList.get(0).getLongitudeE6();
					GeoPoint prevPoint = new GeoPoint(latE6,lonE6);
					Point aPrevPixelPoint = new Point();
					
					projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
					int x = aPrevPixelPoint.x;
					int y = aPrevPixelPoint.y;
					aPath.moveTo(x, y);
					for (int index = 1;index < countNodes; index ++){
						  latE6= nodeList.get(index).getLatitudeE6();
						  lonE6= nodeList.get(index).getLongitudeE6();
						  GeoPoint nextPoint = new GeoPoint(latE6,lonE6);
						  Point nextPixelPoint = new Point();
						  projection.toPoint(nextPoint, nextPixelPoint, drawZoomLevel);
						  x = nextPixelPoint.x;
						  y=  nextPixelPoint.y;
						  aPath.lineTo(x, y);
						  aPrevPixelPoint.x = aPrevPixelPoint.x - drawPosition.x;
						  aPrevPixelPoint.y = aPrevPixelPoint.y - drawPosition.y;
						  nextPixelPoint.x = nextPixelPoint.x - drawPosition.x;
						  nextPixelPoint.y = nextPixelPoint.y - drawPosition.y;
						  canvas.drawLine(aPrevPixelPoint.x,aPrevPixelPoint.y, nextPixelPoint.x,nextPixelPoint.y, paint);
						  prevPoint = nextPoint;
						  projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
						  
					}
					aPath.close();
					aPath.setFillType(FillType.EVEN_ODD);
					//canvas.drawPath(aPath, paint); does not paint why??? canvas.drawLine???
					/*PathShape aPathShape = new PathShape(aPath,canvas.getWidth(),canvas.getHeight());
		    	    aPathShape.resize(canvas.getWidth() ,canvas.getHeight());
		    	    mPaint.setStrokeWidth(5);
		    	    mPaint.setStyle(Style.STROKE);
		    	    aPathShape.draw(canvas, mPaint);*/
				}
			}
		}
	}
	
	/*private void drawNavLines(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		if (mDisplayedNavigationLines!= null){
    		int countLines = mDisplayedNavigationLines.size();
    		for (int index= 0;index < countLines;index++){
    			NavigationLine aNavLine = mDisplayedNavigationLines.get(index);
    			drawNavigationLine(aNavLine, canvas,drawPosition, projection, drawZoomLevel);
    		}
    	}
	}*/
	
	/*private void drawNavigationLine (NavigationLine navLine, Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		if (navLine != null){
			ArrayList<SeamarkNode> nodeList = navLine.getNodeList();
			if (nodeList != null) {
				Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
				//Paint paint = new Paint();
				float[] intervalls = {5.0f,5.0f};
				DashPathEffect aDashPathEffect = new DashPathEffect (intervalls,0);
				paint.setPathEffect(aDashPathEffect);
			    paint.setStrokeWidth(2);
			    paint.setStyle(Style.STROKE);
				paint.setColor(Color.BLUE);
				Path aPath = new Path();
				int countNodes = nodeList.size();
				if (countNodes > 1) {
					int latE6= nodeList.get(0).getLatitudeE6();
					int lonE6= nodeList.get(0).getLongitudeE6();
					GeoPoint prevPoint = new GeoPoint(latE6,lonE6);
					Point aPrevPixelPoint = new Point();
					
					projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
					int x = aPrevPixelPoint.x;
					int y = aPrevPixelPoint.y;
					aPath.moveTo(x, y);
					for (int index = 1;index < countNodes; index ++){
						  latE6= nodeList.get(index).getLatitudeE6();
						  lonE6= nodeList.get(index).getLongitudeE6();
						  GeoPoint nextPoint = new GeoPoint(latE6,lonE6);
						  Point nextPixelPoint = new Point();
						  projection.toPoint(nextPoint, nextPixelPoint, drawZoomLevel);
						  x = nextPixelPoint.x;
						  y=  nextPixelPoint.y;
						  aPath.lineTo(x, y);
						  aPrevPixelPoint.x = aPrevPixelPoint.x - drawPosition.x;
						  aPrevPixelPoint.y = aPrevPixelPoint.y - drawPosition.y;
						  nextPixelPoint.x = nextPixelPoint.x - drawPosition.x;
						  nextPixelPoint.y = nextPixelPoint.y - drawPosition.y;
						  canvas.drawLine(aPrevPixelPoint.x,aPrevPixelPoint.y, nextPixelPoint.x,nextPixelPoint.y, paint);
						  prevPoint = nextPoint;
						  projection.toPoint(prevPoint, aPrevPixelPoint, drawZoomLevel);
						  
					}
					aPath.close();
					aPath.setFillType(FillType.EVEN_ODD);
					//canvas.drawPath(aPath, paint); does not paint why??? canvas.drawLine???
					PathShape aPathShape = new PathShape(aPath,canvas.getWidth(),canvas.getHeight());
		    	    aPathShape.resize(canvas.getWidth() ,canvas.getHeight());
		    	    mPaint.setStrokeWidth(5);
		    	    mPaint.setStyle(Style.STROKE);
		    	    aPathShape.draw(canvas, mPaint);
				}
			}
		}
	}*/
	
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
	
	
	
	private void drawSymbol(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
		int count = mOverlayItemList.size();
		Point aPixelPoint = new Point();
		Paint paint = new Paint();
		for (int index = 0; index < count; index++ ) {
			SeamarkOverlayItem aOverlayItem = mOverlayItemList.get(index);
			Drawable aMarker = aOverlayItem.getMarker();
			if (aMarker!= null) {
				GeoPoint geoPoint = aOverlayItem.getPoint();
				projection.toPoint(geoPoint, aPixelPoint, drawZoomLevel); 
			    int x = aPixelPoint.x;
			    x = x - drawPosition.x;
			    int y = aPixelPoint.y;
			    y = y - drawPosition.y;
			 // get the position of the marker
				Rect markerBounds = aMarker.copyBounds();
	
				// calculate the bounding box of the marker
				int left = x + markerBounds.left;
				int right = x + markerBounds.right;
				int top = y + markerBounds.top;
				int bottom = y + markerBounds.bottom;
	            //drawCenterAndFrame(canvas,left,top,right,bottom);
				// check if the bounding box of the marker intersects with the canvas
				if (right >= 0 && left <= canvas.getWidth() && bottom >= 0
						&& top <= canvas.getHeight()) {
					// set the position of the marker
					aMarker.setBounds(left, top, right, bottom);
	
					// draw the item marker on the canvas
					aMarker.draw(canvas);
	
					// restore the position of the marker
					aMarker.setBounds(markerBounds);
	
					// add the current item index to the list of visible items
					
				}
			}
		}
	}
	
	private void drawCenterAndFrame(Canvas canvas ,RectF rectF){
	    //a visible Rectangle with a cross to show the center of the rectangle
	    Paint paint = new Paint();
	    paint.setStrokeWidth(2);
        paint.setStyle(Style.STROKE);
	    paint.setColor(Color.BLUE);
	    canvas.drawRect(rectF, paint);
	    canvas.drawLine(rectF.left,rectF.bottom,rectF.right,rectF.top,paint);
	    canvas.drawLine(rectF.left,rectF.top,rectF.right,rectF.bottom,paint); 
    }
	
	private void drawCenterAndFrame(Canvas canvas, int left, int top, int right, int bottom){
	    Rect rect = new Rect(); 
	    rect.set(left,top,right,bottom);
	    Paint paint = new Paint();
	    paint.setStrokeWidth(2);
        paint.setStyle(Style.STROKE);
	    paint.setColor(Color.BLUE);
	    canvas.drawRect(rect, paint);
    }
	
	private void drawMainFires (Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel){
	    Paint paint = new Paint();
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
	    Paint paint = new Paint();
		float factor = 1.0f;
		if (drawZoomLevel > 12) factor = (float)(drawZoomLevel - 12);
		Point aPixelPoint = new Point();
		if (test) Log.d(TAG, "begin  display Sectorfire updatenr: " + mContext.mCountSeamarksUpdate);
		int count = mContext.mNodeListNumberedFires.size();
		for (int index = 0; index < count; index++ ) {
			SeamarkNode aSeamark = mContext.mNodeListNumberedFires.get(index);
			
			if (aSeamark != null) {
			     String aType = aSeamark.getValueToKey("seamark:type");
			     String aName = aSeamark.getValueToKey("seamark:name");
			     if (aName != null ){
			    	 if (test) Log.d(TAG,"Displaying Sectorfire: " + aName);
			     } else {
			    	if (test) Log.d(TAG,"Displaying unnamed Sectorfire: ");
			     }
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
					 if(test)Log.d(TAG,nrStr +" " + lightNrColor);
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
						  
					 } // lightRange != null
					 
					} // else 
					 // lightnr++ hier war der schwere Fehler, lightnr++ wurde nicht immer ausgeführt
				 }  // else  
					 lightnr ++;
					 nrStr = Integer.toString(lightnr);
					 lightNrColor = aSeamark.getValueToKey("seamark:light:"+nrStr+":colour"); 
			   }// while lightNrColor != null	 
	    } // if seamark != null
		if (test) Log.d(TAG, "end display Sectorfire "+ index);
     } // for  
		if (test) Log.d(TAG, "end  display Sectorfire ");
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
	
	public void addItem(SeamarkOverlayItem aItem){
		if (!mOverlayItemList.contains(aItem)){
		    mOverlayItemList.add(aItem);
		}
	}
	
	public void removeItem(SeamarkOverlayItem aItem){
		if(mOverlayItemList.contains(aItem)){
		  mOverlayItemList.remove(aItem);
		}
		
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
	
	/*public void addNavLine(NavigationLine navLine){
		if (!mDisplayedNavigationLines.contains(navLine)){
		    mDisplayedNavigationLines.add(navLine);
		}
	}
	
	public void removeNavLine(NavigationLine navLine){
		if (mDisplayedNavigationLines.contains(navLine)){
		    mDisplayedNavigationLines.remove(navLine);
		}
	}*/
	
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
	
	
	public static Drawable boundCenter(Drawable balloon){
		balloon.setBounds(balloon.getIntrinsicWidth() / -2, balloon.getIntrinsicHeight() / -2,
				balloon.getIntrinsicWidth() / 2, balloon.getIntrinsicHeight() / 2);
		return balloon;
	}
	
	
}
