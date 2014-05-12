package org.openseamap.openseamapviewer.viewer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.rendertheme.InternalRenderTheme;
import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.MainActivity;
import org.openseamap.openseamapviewer.R;
import org.openseamap.openseamapviewer.R.id;
import org.openseamap.openseamapviewer.R.layout;
import org.openseamap.openseamapviewer.R.menu;
import org.openseamap.openseamapviewer.R.string;
import org.openseamap.openseamapviewer.downloader.OpenSeamapTileAndSeamarksDownloader;
import org.openseamap.openseamapviewer.environment.Environment2;
import org.openseamap.openseamapviewer.filefilter.FilterByFileExtension;
import org.openseamap.openseamapviewer.filefilter.ValidRouteGmlFile;
import org.openseamap.openseamapviewer.filepicker.FilePicker;
import org.openseamap.openseamapviewer.locationlistener.MyLocationListenerWithDownload;
import org.openseamap.openseamapviewer.overlay.CenterCircleOpenSeaMapOverlay;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class DownloadOpenSeamapTileAndSeamarksViewer extends MapActivity {
	private static final String TAG = "DownloadOpenSeamapTileAndSeamarksViewer";
	private NotificationManager mNM;
	private static final int NOTIFICATION_ID = OpenSeamapTileAndSeamarksDownloader.NOTIFICATION_ID;
	private MapView mMapView;
	private MapController mMapController;
	private TextView mTextViewCenterCoordinates;
	private CenterCircleOpenSeaMapOverlay mCircleOverlay = null;
	private static Handler mCenterPointHandler = new Handler();
	int mTextViewCenterCoordinatedSavedHeight;
	private boolean showMapInfo;
	
	SharedPreferences prefs;
	
 // deal with routes
	
	private static final FileFilter FILE_FILTER_EXTENSION_GML = new FilterByFileExtension(".gml");
	private static final FileFilter FILE_FILTER_EXTENSION_GPX = new FilterByFileExtension(".gpx");
	
	private static final int SELECT_ROUTE_GML_FILE = 3;
	private static final int SELECT_ROUTE_GPX_FILE = 4;
	

	
	public ArrayList<GeoPoint> mRouteList = null;
	public boolean mShowRoute = false;
	
// deal with location
	
	public boolean showMyLocation;
	private boolean snapToLocation;
	private LocationManager locationManager;
	private MyLocationListenerWithDownload myLocationListener;
	public  boolean mMyLocationChanged;
	public GeoPoint mMyLocationPoint = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Context myContext = getApplicationContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		OpenSeamapTileAndSeamarksDownloader aOpenSeamapTileAndSeamarksDownLoader = new OpenSeamapTileAndSeamarksDownloader(myContext);
		setContentView(R.layout.activity_download_openseamap_map_viewer);
		this.mMapView = (MapView) findViewById(R.id.mapView);
		mTextViewCenterCoordinates = (TextView) findViewById(R.id.OSM_Info_textview);
		MapGenerator mapGenerator = aOpenSeamapTileAndSeamarksDownLoader;
		this.mMapView.setMapGenerator(mapGenerator);
		configureMapView();
		
		mCircleOverlay = new CenterCircleOpenSeaMapOverlay(this);
		mCircleOverlay.setMustShowCenter(true);
		mMapView.getOverlays().add(mCircleOverlay);
		showMapInfo = true;
		
		// get the pointers to different system services
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		this.myLocationListener = new MyLocationListenerWithDownload(this);
		this.showMyLocation= false;
		this.mMyLocationChanged= false;
		mNM = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		if (mCircleOverlay != null && mCircleOverlay.getMustShowCenter()){
			GeoPoint geoPoint = this.mMapView.getMapPosition().getMapCenter();
			
			double lat = geoPoint.getLatitude();
			double lon = geoPoint.getLongitude();
			String aMsg = "Mapcenter "  + "LAT: " + lat + " LON: " + lon ;
			
			mTextViewCenterCoordinates.append(aMsg);
			mCenterPointHandler.postDelayed(centerPointRefresh, 1000); 
		}
	}
	
	
	private void configureMapView() {
		// configure the MapView and activate the zoomLevel buttons
		this.mMapView.setClickable(true);
		this.mMapView.setBuiltInZoomControls(true);
		this.mMapView.setFocusable(true);
		
		boolean drawTileFrames = false;
		boolean drawTileCoordinates = false;
		boolean highlightWaterTiles = false;
		DebugSettings debugSettings = new DebugSettings(drawTileCoordinates, drawTileFrames, highlightWaterTiles);
		this.mMapView.setDebugSettings(debugSettings);
		// set the localized text fields
		MapScaleBar mapScaleBar = this.mMapView.getMapScaleBar();
		mapScaleBar.setText(TextField.KILOMETER, getString(R.string.unit_symbol_kilometer));
		mapScaleBar.setText(TextField.METER, getString(R.string.unit_symbol_meter));

		// get the map controller for this MapView
		this.mMapController = this.mMapView.getController();
	}
	
	
	private Runnable centerPointRefresh = new Runnable() {
		public void run() {
			
			GeoPoint geoPoint = mMapView.getMapPosition().getMapCenter();
			byte zoom = mMapView.getMapPosition().getZoomLevel();
			double lat = geoPoint.getLatitude();
			double lon = geoPoint.getLongitude();
			
			String aMsg = "Mapcenter "  + "LAT: " + lat + " LON: " + lon + " Zoom " + zoom ;
			    
				mTextViewCenterCoordinates.setText("");
				mTextViewCenterCoordinates.append(aMsg);
				
			mCenterPointHandler.postDelayed(this,1000);
		}
	};
	
	private void toggleShowCenterOverlay() {
		if (mCircleOverlay.getMustShowCenter()){
			mCircleOverlay.setMustShowCenter(false);
			mCircleOverlay.requestRedraw();
		}else {
			mCircleOverlay.setMustShowCenter(true);
			mCircleOverlay.requestRedraw();
		}
	}
	
	private void toggleShowMapInfo(){
		if(showMapInfo){
			mCenterPointHandler.removeCallbacks(centerPointRefresh);
			mTextViewCenterCoordinates.setText("");
			mTextViewCenterCoordinates.setVisibility(View.INVISIBLE);
			mTextViewCenterCoordinatedSavedHeight = mTextViewCenterCoordinates.getHeight();
			mTextViewCenterCoordinates.setHeight(0);
			/*GeoPoint geoPoint = mMapView.getMapPosition().getMapCenter();
			GeoPoint movedPoint = new GeoPoint(geoPoint.latitudeE6 + 100, geoPoint.longitudeE6);
			mMapView.setCenter(movedPoint);*/
			showMapInfo = false;
			
		} else {
			showMapInfo = true;
			mTextViewCenterCoordinates.setVisibility(View.VISIBLE);
			mTextViewCenterCoordinates.setHeight(mTextViewCenterCoordinatedSavedHeight);
			mCenterPointHandler.postDelayed(centerPointRefresh, 1000); 
			
		}
		mCircleOverlay.requestRedraw();
		
	}
	
	/**
	 * Returns the status of the "show my location" mode.
	 * 
	 * @return true if the "show my location" mode is enabled, false otherwise.
	 */
	public boolean isShowMyLocationEnabled() {
		return this.showMyLocation;
	}

	/**
	 * Returns the status of the "snap to location" mode.
	 * 
	 * @return true if the "snap to location" mode is enabled, false otherwise.
	 */
	public boolean isSnapToLocationEnabled() {
		return this.snapToLocation;
	}
	
	/**
	 * Enables the "show my location" mode.
	 * 
	 * @param centerAtFirstFix
	 *            defines whether the map should be centered to the first fix.
	 */
	private void enableShowMyLocation(boolean centerAtFirstFix) {
		if (!this.showMyLocation) {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			String bestProvider = this.locationManager.getBestProvider(criteria, true);
			this.showMyLocation = true;
			this.mCircleOverlay.setShowLocation(this.showMyLocation);
			this.myLocationListener.setCenterAtFirstFix(centerAtFirstFix);
			this.locationManager.requestLocationUpdates(bestProvider, 1000, 0, this.myLocationListener);
			

		}
	}
	

	/**
	 * Disables the "show my location" mode.
	 */
	void disableShowMyLocation() {
		if (this.showMyLocation) {
			this.showMyLocation = false;
			this.mCircleOverlay.setShowLocation(this.showMyLocation);
			//disableSnapToLocation(false);
			this.locationManager.removeUpdates(this.myLocationListener);
			
		}
	}
	
	private void toggleShowLocation(){
		if(showMyLocation){
			
			disableShowMyLocation();
		} else {
			enableShowMyLocation(true);
		}
		mCircleOverlay.requestRedraw();
	}
	
	public void requestOverlayRedraw(GeoPoint newPoint) {
		this.mMyLocationPoint = newPoint;
		this.mMyLocationChanged = true;
		mCircleOverlay.requestRedraw();
		// if the redraw is done in the overlay, mMyLocationChanged is set to false 
		// see SeamarksOverlay.showMyUpdatedPosition
	}
	
	
	/**
	 * Sets all file filters and starts the FilePicker to select an gml file.
	 */
	private void startRouteGMLPicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_GML);
		FilePicker.setFileSelectFilter(new ValidRouteGmlFile());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_ROUTE_GML_FILE);
	}
	
	
	/**
	 * Sets all file filters and starts the FilePicker to select an gml file.
	 */
	private void startRouteGPXPicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_GPX);
		FilePicker.setFileSelectFilter(new ValidRouteGmlFile());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_ROUTE_GPX_FILE);
	}
	
	private void readGPXFile(){
		File cardDirectory = Environment2.getCardDirectory();
		String aPath = cardDirectory.getAbsolutePath();
        String aGMLFilename =aPath + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + "route.gpx";
       
		File aFile = new File(aGMLFilename);
		loadGPXRouteData(aFile);

	}
	
	/**
	 * 
	 * @param aFile a file that contains the route, selected via the filepicker
	 *    load the route  from aFile to the datbase
	 *    uses a thread 
	 *    visual feedback to the user with aprogress bar
	 */

   private void loadGPXRouteData(File aFile) {
        boolean test = true;
		final String aFilename = aFile.getAbsolutePath();
		
	    ArrayList<GeoPoint> pointsList = new ArrayList<GeoPoint>();
	    // We don't use namespaces
	    final String ns = null;
		try {
			
			XmlPullParserFactory parserCreator;

			parserCreator = XmlPullParserFactory.newInstance();

			XmlPullParser parser = parserCreator.newPullParser();
			FileReader myReader = new FileReader(aFilename);
			parser.setInput(myReader);
			int parserEvent = parser.getEventType();
			// Parse the XML returned from the file
			int pointCounter = 0;
			while (parserEvent != XmlPullParser.END_DOCUMENT) {
				switch (parserEvent) {
					case XmlPullParser.START_TAG:
						String tag = parser.getName();
						if (tag.compareTo("rtept") == 0) {
							pointCounter++;
							String aLATStr = parser.getAttributeValue(null, "lat");
							String aLONStr = parser.getAttributeValue(null, "lon");
							double lon = Double.parseDouble(aLONStr);
            				
            				double lat = Double.parseDouble(aLATStr);
            				GeoPoint geoPoint = new GeoPoint(lat,lon);
            				pointsList.add(geoPoint);

							if (test){
								Log.i(TAG, "   routepoint=" + pointCounter + " latitude=" + aLATStr
										+ " longitude=" + aLONStr);
							}
						}
						break;
					} //switch
				parserEvent = parser.next();
			   } //while
				
		} catch (FileNotFoundException e) {
			Log.d(TAG, "File not found");
		} 
		catch (XmlPullParserException e) {
			Log.d(TAG, "Parser exception");
		} catch (Exception e) {
			Log.i(TAG, "Failed in parsing XML", e);
			//showToastOnUiThread(aUIMsg);
		}
        /*
		int countParsedPoints = pointsList.size();
		Log.d(TAG,"begin list points");
		for (int index = 0; index < countParsedPoints;index++){
			GeoPoint geoPoint = pointsList.get(index);
			String geoPointStr = "x = " + geoPoint.longitudeE6 + " y= "+ geoPoint.latitudeE6;
			Log.d(TAG, geoPointStr);
		}
		Log.d(TAG,"end of list points");*/
		if (mRouteList != null ){
			mRouteList.clear();
		}
		mRouteList = pointsList;
	}
   
    private void toggleShowRoute(){
		if(mShowRoute){
			mShowRoute = false;
			mCircleOverlay.setShowRoute(this.mShowRoute);
		} else {
			mShowRoute = true;
			mCircleOverlay.setShowRoute(this.mShowRoute);
		}
        mCircleOverlay .requestRedraw();
	}
	
	
	private void readGMLFile(){
		File cardDirectory = Environment2.getCardDirectory();
		String aPath = cardDirectory.getAbsolutePath();
        String aGMLFilename =aPath + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + "route.gml";
       
		File aFile = new File(aGMLFilename);
		loadGMLRouteData(aFile);

	}
	
	/**
	 * 
	 * @param aFile a file that contains the route, selected via the filepicker
	 *    load the route  from aFile to the datbase
	 *    uses a thread 
	 *    visual feedback to the user with aprogress bar
	 */

   private void loadGMLRouteData(File aFile) {

		final String aFilename = aFile.getAbsolutePath();
		
	    ArrayList<GeoPoint> pointsList = new ArrayList<GeoPoint>();
	    // We don't use namespaces
	    final String ns = null;
		try {
			
			XmlPullParserFactory parserCreator;

			parserCreator = XmlPullParserFactory.newInstance();

			XmlPullParser parser = parserCreator.newPullParser();
			FileReader myReader = new FileReader(aFilename);
			parser.setInput(myReader);
			int parserEvent = parser.getEventType();
			// Parse the XML returned from the file
			while (parserEvent != XmlPullParser.END_DOCUMENT) {
				switch (parserEvent) {
					case XmlPullParser.START_TAG:
						String tag = parser.getName();

						if (tag.compareTo("gml:coordinates") == 0) {
							String decimal = parser.getAttributeValue(null, "decimal");
							String cs = parser.getAttributeValue(null, "cs");
							String ts = parser.getAttributeValue(null, "ts");
							parser.require(XmlPullParser.START_TAG, ns, "gml:coordinates");
							String result = "";
						    if (parser.next() == XmlPullParser.TEXT) {
						        result = parser.getText();
						        parser.nextTag();
						    }
						    
							parser.require(XmlPullParser.END_TAG, ns, "gml:coordinates");
                            //Log.d(TAG,result);
                            String[] pointsStr = result.split(ts);
                            int countPoints = pointsStr.length;
                            for (int index =0; index < countPoints;index++){
                            	if (pointsStr[index]!= null){
                            		String aPointStr = pointsStr[index];
                            		String coordStr[] = aPointStr.split(cs);
                            		if (coordStr.length == 2) { 
                            			try {
                            				String lonStr = coordStr[0];
                            				double lon = Double.parseDouble(lonStr);
                            				String latStr = coordStr[1];
                            				double lat = Double.parseDouble(latStr);
                            				GeoPoint geoPoint = new GeoPoint(lat,lon);
                            				pointsList.add(geoPoint);
                            			} catch (Exception e){
                            				Log.d(TAG," invalid point coordinates " + aPointStr);
                            			}
                            		}
                            	}
                            }
                          
						} 
						break;
				}

				parserEvent = parser.next();
			}

		} catch (FileNotFoundException e) {
			Log.d(TAG, "File not found");
		} 
		catch (XmlPullParserException e) {
			Log.d(TAG, "Parser exception");
		} catch (Exception e) {
			Log.i(TAG, "Failed in parsing XML", e);
			//showToastOnUiThread(aUIMsg);
		}
        
		/*int countParsedPoints = pointsList.size();
		Log.d(TAG,"begin list points");
		for (int index = 0; index < countParsedPoints;index++){
			GeoPoint geoPoint = pointsList.get(index);
			String geoPointStr = "x = " + geoPoint.longitudeE6 + " y= "+ geoPoint.latitudeE6;
			Log.d(TAG, geoPointStr);
		}
		Log.d(TAG,"end of list points");*/
		if (mRouteList != null ){
			mRouteList.clear();
		}
		mRouteList = pointsList;
	}

	@Override
	protected void onResume() {
		super.onResume(); 
		MapScaleBar mapScaleBar = this.mMapView.getMapScaleBar();
		mapScaleBar.setShowMapScaleBar(true);
		int lat = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LAT, 90);
		int lon = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LON, 180);
		int aZoomLevel = prefs.getInt(MainActivity.PREF_ZOOM_FACTOR,-1);
		if (lat != 90 && lon != 180 && aZoomLevel > 0) {
			// we got valid values
			this.mMapView.getController().setZoom(aZoomLevel);
			GeoPoint geoPoint = new GeoPoint(lat,lon);
	 		this.mMapView.getController().setCenter(geoPoint);
		}
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onPause() {
		GeoPoint aGp = mMapView.getMapPosition().getMapCenter();
     	int aZoomLevel =mMapView.getMapPosition().getZoomLevel();
     	int aLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
	    int aLON = (int) aGp.longitudeE6;
	    prefs.edit()
	     	.putInt(MainActivity.PREV_LAST_GEOPOINT_LAT, aLAT)
	     	.putInt(MainActivity.PREV_LAST_GEOPOINT_LON, aLON)
	     	.putInt(MainActivity.PREF_ZOOM_FACTOR,aZoomLevel)
	     	.commit();
		super.onPause();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		mNM.cancel(NOTIFICATION_ID);
		this.disableShowMyLocation();
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu__osm_download_viever, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		MenuItem aMenuItem = menu.findItem(R.id.menu_show_overlay_center);
		String info ;
		if(mCircleOverlay.getMustShowCenter()) {
			//aMenuItem.setTitle("hide infos");
			info = getResources().getString(R.string.osm_download_viever_hide_center);
			aMenuItem.setTitle(info);
		} else {
			//aMenuItem.setTitle("show infos");
			info = getResources().getString(R.string.osm_download_viever_show_center);
			aMenuItem.setTitle(info);
		}
		aMenuItem = menu.findItem(R.id.menu_show_location);
		if (showMyLocation){
			//aMenuItem.setTitle("hide location");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_hide_location));
		} else {
			//aMenuItem.setTitle("show location");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_show_location));
		}
		MenuItem aInfoMenuItem = menu.findItem(R.id.menu_download_show_map_info);
		if (showMapInfo){
			//aMenuItem.setTitle("hide map info");
			aInfoMenuItem.setTitle(getResources().getString(R.string.osm_download_viever_hide_map_info));
		} else {
			//aMenuItem.setTitle("show map info");
			aInfoMenuItem.setTitle(getResources().getString(R.string.osm_download_viever_show_map_info));
		}
		aMenuItem = menu.findItem(R.id.menu_show_hide_route);
		 if (mRouteList == null) {
			 aMenuItem.setVisible(false);
		 } else {
			 aMenuItem.setVisible(true);
		 }
		 if (mShowRoute) {
			 aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_route_hide ));
		 }else {
			 aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_route_show ));
		 }
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_show_overlay_center:
				toggleShowCenterOverlay();
				return true;
			case R.id.menu_download_show_map_info:
				toggleShowMapInfo();
				return true;
			case R.id.menu_read_route_gpx_file:
				startRouteGPXPicker();
				return true;
			case R.id.menu_read_route_gml_file:
				startRouteGMLPicker();
				return true;
			case R.id.menu_show_hide_route:
				toggleShowRoute();
				return true;
			case R.id.menu_show_location:
				toggleShowLocation();
				return true;
			default:
				return false;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SELECT_ROUTE_GPX_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			
		     String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
		     File selectedFile = new File(aFilename);
		     loadGPXRouteData(selectedFile);
		     if (mRouteList!= null && mRouteList.size()> 0) {
		    	 this.mShowRoute = true;
		    	 mCircleOverlay.setRouteList(mRouteList) ;
		    	 mCircleOverlay.setShowRoute(this.mShowRoute);
		    	 mCircleOverlay.requestRedraw();
		     }
		     
		     
          } 
	}
	
	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
   
		int myKeyCode = keyCode;
		KeyEvent myKeyEvent = event;
    
        if  (keyCode == KeyEvent.KEYCODE_BACK)
        {
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	String aQuestion = getResources().getString(R.string.osmviewer_exit_question);
        	String yesMsg = getResources().getString(R.string.osmviewer_exit_yes);
        	String noMsg = getResources().getString(R.string.osmviewer_exit_no);
        	builder.setMessage(aQuestion)
        	       .setCancelable(false)       
        	       .setPositiveButton(yesMsg, new DialogInterface.OnClickListener() {           
        	    	   public void onClick(DialogInterface dialog, int id) {               
        	    		   finish();           
        	    		   }       
        	    	   })       
        	    	   .setNegativeButton(noMsg, new DialogInterface.OnClickListener() {          
        	    		   public void onClick(DialogInterface dialog, int id) {                
        	    			   dialog.cancel();           
        	    			   }       
        	    		   });
        	AlertDialog alert = builder.create();
        	alert.show();
            return  true;
        } 
        
        return super.onKeyUp(keyCode, event);
        //return false;
    }
	
	
}
