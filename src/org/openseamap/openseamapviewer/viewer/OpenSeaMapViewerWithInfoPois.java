package org.openseamap.openseamapviewer.viewer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorFactory;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
import org.mapsforge.android.maps.mapgenerator.TileCache;
import org.mapsforge.android.maps.overlay.ItemizedOverlay;
import org.mapsforge.android.maps.rendertheme.InternalRenderTheme;
import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.Tag;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.openseamap.openseamapviewer.MainActivity;
import org.openseamap.openseamapviewer.R;
import org.openseamap.openseamapviewer.ScreenshotCapturerOpenSeaMapWithPois;
import org.openseamap.openseamapviewer.ViewerGlobals;
import org.openseamap.openseamapviewer.environment.Environment2;
import org.openseamap.openseamapviewer.filefilter.FilterByFileExtension;
import org.openseamap.openseamapviewer.filefilter.ValidMapFile;
import org.openseamap.openseamapviewer.filefilter.ValidRenderTheme;
import org.openseamap.openseamapviewer.filefilter.ValidRouteGmlFile;
import org.openseamap.openseamapviewer.filefilter.ValidSeamarksFile;
import org.openseamap.openseamapviewer.filepicker.FilePicker;
import org.openseamap.openseamapviewer.locationlistener.MyLocationListenerWithInfoPois;
import org.openseamap.openseamapviewer.overlay.SeamarkItemizedOverlayWithInfoPois;
import org.openseamap.openseamapviewer.seamarks.SeamarkDrawable;
import org.openseamap.openseamapviewer.seamarks.SeamarkNode;
import org.openseamap.openseamapviewer.seamarks.SeamarkOsm;
import org.openseamap.openseamapviewer.seamarks.SeamarkSymbol;
import org.openseamap.openseamapviewer.seamarks.SeamarkWay;
import org.openseamap.openseamapviewer.seamarks.SeamarkWithPoisOverlayItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class OpenSeaMapViewerWithInfoPois extends MapActivity {
	private static final String TAG = "RenderOpenSeaMapViewerWithInfoPois";
	private static final FileFilter FILE_FILTER_EXTENSION_MAP = new FilterByFileExtension(".map");
	
	private static final FileFilter FILE_FILTER_EXTENSION_XML = new FilterByFileExtension(".xml");
	private static final FileFilter FILE_FILTER_EXTENSION_GML = new FilterByFileExtension(".gml");
	private static final FileFilter FILE_FILTER_EXTENSION_GPX = new FilterByFileExtension(".gpx");
	
	
	
	private static final int SELECT_MAP_FILE = 0;
	private static final int SELECT_RENDER_THEME_FILE = 1;
	private static final int SELECT_SEAMARKS_FILE = 2;
	private static final int SELECT_ROUTE_GML_FILE = 3;
	private static final int SELECT_ROUTE_GPX_FILE = 4;
	
	private  ActivityManager mActivityManager = null;
	private int  mCountSeamarksUpdate = 0;
	private static boolean mInUpdateSeamarkNodesOnOverlay = false;
	public static byte mMinZoomForSeamarks= 8;
	private ScreenshotCapturerOpenSeaMapWithPois screenshotCapturer;
	MapController mapController;
	public MapView mapView;
	
	//private OpenSeamapTestDatabaseRenderer mMapGenerator;
	TextView mTextViewCenterCoordinates = null;
	int mTextViewCenterCoordinatedSavedHeight;
	private String mRenderThemeName = "";
	private static Handler mCenterPointHandler = new Handler();
	private SeamarkOsm mSeamarkOsm = null;
	private String mSeamarkFilePath = "";
	private LinkedHashMap<String,SeamarkNode> mSeamarksDictionary = null;
	private ArrayList<SeamarkNode> mSeamarksList = null;
	private static Handler mReadPoisAndWaysHandler = new Handler();
	
	public SeamarkItemizedOverlayWithInfoPois mSeamarkWithInfoPoisAndWaysItemizedOverlay = null;
	private Bitmap mDefaultSeamark = null;
	private boolean mFillDirectionalSector;
	
	private ArrayList<SeamarkWithPoisOverlayItem> infoSeamarksList = null;
	private ArrayList<SeamarkWay>mSeamarkWayList  = null;
	private float mDisplayFactor = 1.0f; // for displays with high density we change this 
	//private ArrayList<SeamarkWithPoisOverlayItem> seamarksItemList = null;
	public boolean showInfoSeamarks = false;
	public boolean showSectorFires = true;
	public ArrayList<SeamarkNode> mNodeListSectorFire;
	public ArrayList<SeamarkNode> mNodeListMainLights;
	private ArrayList<SeamarkNode> seamarksList = null;
	
	private int countDisplayedSeamarks =0;
	public byte mLastZoom;
	private boolean mShowMapInfo = true;
	
	// deal with routes
	
	public ArrayList<GeoPoint> mRouteList = null;
	public boolean mShowRoute = false;
	// deal with location
	
	public boolean showMyLocation;
	private boolean snapToLocation;
	private LocationManager locationManager; 
	private MyLocationListenerWithInfoPois myLocationListener;
	public  boolean mMyLocationChanged;
	public GeoPoint mMyLocationPoint = null;
	
    // deal with prefs 
	
	SharedPreferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS + Window.FEATURE_ACTION_BAR);
		mActivityManager =(ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//this.screenshotCapturer = new ScreenshotCapturerOpenSeaMapWithPois(this);
		//this.screenshotCapturer.start();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		 if (metrics.densityDpi == 320) {
		    	// we have a nexus 7 nexus 7
		    	mDisplayFactor = 1.5f;
		    	Log.d(TAG,"nexus 7 with 320 dpi"); 
		    } 
		SeamarkSymbol.preloadFromDefsFile(MainActivity.OPENSEAMAP_STANDARDSYMBOLDEFS);
		//SeamarkSymbol.preloadFromDefsFile("symbols5.defs");
		mSeamarkOsm = new SeamarkOsm(this);
		setContentView(R.layout.activity_renderopenstreet_map_viewer);
		this.mapView = (MapView) findViewById(R.id.mapView);
		mTextViewCenterCoordinates = (TextView) findViewById(R.id.OSM_Info_textview);
		
		configureMapView();
		MapGeneratorInternal mapGeneratorInternalNew = MapGeneratorInternal.DATABASE_RENDERER;
		MapGenerator mapGenerator = MapGeneratorFactory.createMapGenerator(mapGeneratorInternalNew);
		this.mapView.setMapGenerator(mapGenerator);
		
		//this.mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
		
		// set the Render theme
		setStandardRendererTheme();
		
        // load the seamarks file into aSeamarkOsmObject and create the dictionary
		mDefaultSeamark = BitmapFactory.decodeResource(getResources(),R.drawable.star); 
		mFillDirectionalSector = true;
		mSeamarkWithInfoPoisAndWaysItemizedOverlay = new SeamarkItemizedOverlayWithInfoPois(new BitmapDrawable(getResources(),mDefaultSeamark),this,mFillDirectionalSector, mDisplayFactor);
		
		mapView.getOverlays().add(mSeamarkWithInfoPoisAndWaysItemizedOverlay);
		
		infoSeamarksList = new ArrayList<SeamarkWithPoisOverlayItem>();
		mNodeListSectorFire = new ArrayList<SeamarkNode>();
		mNodeListMainLights = new ArrayList<SeamarkNode>();
		
		// get the pointers to different system services
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		this.myLocationListener = new MyLocationListenerWithInfoPois(this);
		this.showMyLocation= false;
		this.mMyLocationChanged= false;
		String mapfileName = prefs.getString(MainActivity.PREV_LAST_MAPFILE_PATH,"");
		if (!mapfileName.equals("")){
			// we have a mapfileName from the prefs
			File mapFile = new File (mapfileName);
			FileOpenResult aFileOpenResult = this.mapView.setMapFile(mapFile);
			
		}
		
		File currentMapFile = this.mapView.getMapFile();
		if (currentMapFile != null ) {
			String currentMapFilePath = currentMapFile.getAbsolutePath();
		    setSeamarkFilePathAndRead(currentMapFilePath);
		    mReadPoisAndWaysHandler.postDelayed(readPoisAndWays, 1000);  // it takes some time to read the seamarks file
		}
		
		
		
		//mapController.setZoom(12);
		mLastZoom = this.mapView.getMapPosition().getZoomLevel();
		//GeoPoint newPoint = new GeoPoint(54.00,11.37);
		//this.mapController.setCenter(newPoint);
		if (!this.mapView.getMapGenerator().requiresInternetConnection() && this.mapView.getMapFile() == null) {
			startMapFilePicker();
		}
		mTextViewCenterCoordinates = (TextView) findViewById(R.id.OSM_Info_textview);
		
		if (mShowMapInfo){
			GeoPoint geoPoint = this.mapView.getMapPosition().getMapCenter();
			
			double lat = geoPoint.getLatitude();
			double lon = geoPoint.getLongitude();
			String aMsg = "Mapcenter "  + "LAT: " + lat + " LON: " + lon ;
			
			mTextViewCenterCoordinates.append(aMsg);
			mCenterPointHandler.postDelayed(centerPointRefresh, 1000); 
		}
		
		this.setTitle(getResources().getString(R.string.title_activity_main));
		
	}
	
	private void XcheckAndDisplayMemory() {
		int countMemory = mActivityManager.getMemoryClass();
        StringBuffer aBuf = new StringBuffer();
       /* aBuf.append("Memory ");
        aBuf.append(countMemory);
        aBuf.append(" Mb" );*/
        aBuf.append("    seamarks updates: " );
        aBuf.append(mCountSeamarksUpdate);
        aBuf.append("\n");
        mTextViewCenterCoordinates.append(aBuf.toString());
	}
	
	
	private void setStandardRendererTheme() {
		File cardDirectory = Environment2.getCardDirectory();
		String aPath = cardDirectory.getAbsolutePath();
        String aRenderThemeFilename =aPath + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + MainActivity.OPENSEAMAP_STANDARDRENDERERNAME;
        mRenderThemeName = aRenderThemeFilename;
		File aFile = new File(aRenderThemeFilename);
		try {
		this.mapView.setRenderTheme(aFile);
		} catch (IOException e) {
			//String aMsg = "Standard RenderTheme not found" + e.toString();
			String aMsg = getResources().getString(R.string.osmviewer_start_stdrender_not_found);
			mTextViewCenterCoordinates.append(aMsg);
			Log.d(TAG, e.toString());
		}
	}
	
	private void configureMapView() {
		// configure the MapView and activate the zoomLevel buttons
		this.mapView.setClickable(true);
		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setFocusable(true);
		TileCache fileSystemTileCache = this.mapView.getFileSystemTileCache();
		fileSystemTileCache.setPersistent(false);
		boolean drawTileFrames = false;
		boolean drawTileCoordinates = false;
		boolean highlightWaterTiles = false;
		DebugSettings debugSettings = new DebugSettings(drawTileCoordinates, drawTileFrames, highlightWaterTiles);
		this.mapView.setDebugSettings(debugSettings);
		// set the localized text fields
		MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
		mapScaleBar.setText(TextField.KILOMETER, getString(R.string.unit_symbol_kilometer));
		mapScaleBar.setText(TextField.METER, getString(R.string.unit_symbol_meter));

		// get the map controller for this MapView
		this.mapController = this.mapView.getController();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
		mapScaleBar.setShowMapScaleBar(true);
		int lat = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LAT, 90);
		int lon = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LON, 180);
		int aZoomLevel = prefs.getInt(MainActivity.PREF_ZOOM_FACTOR,-1);
		if (lat != 90 && lon != 180 && aZoomLevel > 0) {
			// we got valid values
			this.mapView.getController().setZoom(aZoomLevel);
			GeoPoint geoPoint = new GeoPoint(lat,lon);
	 		this.mapView.getController().setCenter(geoPoint);
		}
		/*ActionBar actionBar = getActionBar();
		if (actionBar!= null){
			actionBar.show();
		}*/
		/*mTextViewCenterCoo {
		 * rdinatedSavedHeight = mTextViewCenterCoordinates.getHeight() +5;
		mTextViewCenterCoordinates.setHeight(0);
		mTextViewCenterCoordinates.setVisibility(View.INVISIBLE);*/
		 
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onPause() {
		File aMapFile = mapView.getMapFile();
		if (aMapFile!=null) {
			String mapfileName = mapView.getMapFile().getAbsolutePath();
			GeoPoint aGp = mapView.getMapPosition().getMapCenter();
	     	int aZoomLevel = mapView.getMapPosition().getZoomLevel();
	     	int aLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
		    int aLON = (int) aGp.longitudeE6;
		    prefs.edit()
		        .putString(MainActivity.PREV_LAST_MAPFILE_PATH, mapfileName)
		     	.putInt(MainActivity.PREV_LAST_GEOPOINT_LAT, aLAT)
		     	.putInt(MainActivity.PREV_LAST_GEOPOINT_LON, aLON)
		     	.putInt(MainActivity.PREF_ZOOM_FACTOR,aZoomLevel)
		     	.commit();
		}
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.disableShowMyLocation();
		//this.screenshotCapturer.interrupt();
	}
	
	
	/**
	 * Disables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void disableSnapToLocation(boolean showToast) {
		if (this.snapToLocation) {
			this.snapToLocation = false;
			//this.snapToLocationView.setChecked(false);
			this.mapView.setClickable(true);
			if (showToast) {
				//showToastOnUiThread(getString(R.string.snap_to_location_disabled));
			}
		}
	}

	/**
	 * Enables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void enableSnapToLocation(boolean showToast) {
		if (!this.snapToLocation) {
			this.snapToLocation = true;
			this.mapView.setClickable(false);
			if (showToast) {
				//showToastOnUiThread(getString(R.string.snap_to_location_enabled));
			}

		}
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
	
	public void setSeamarkFilePathAndRead ( String aPath ) {
		mSeamarkOsm.clear();
		mSeamarkFilePath = aPath;
		if (aPath.endsWith(".map")) {
	    	aPath = aPath.substring(0, aPath.length()-4);
	    	String aSeamarksPath = aPath +"_seamarks"+ ".xml";
	    	File xmlFile = new File (aSeamarksPath);
	    	if (xmlFile.exists()) {
	    		mSeamarkFilePath = aSeamarksPath;
	    		mSeamarkOsm.readSeamarkFile(aSeamarksPath);
	    	} else {
	    		String datFilepath = aPath +"_seamarks"+ ".dat";
	    		File datFile = new File (datFilepath);
	    		if(datFile.exists()) {
	    			mSeamarkFilePath = datFilepath;
		    		mSeamarkOsm.readSeamarkFile(datFilepath);	
	    		} else {
	    			String info = "no seamarks file: " + aPath;
		    		//String info = mContext.getResources().getString(R.string.osmviewer_seamarks_file_not_found);
		    		showToastOnUiThread(info);
	    		}
	    	}
		}
		/*if (aPath.endsWith(".map")) {
	    	aPath = aPath.substring(0, aPath.length()-4);
	    	String aSeamarksPath = aPath +"_seamarks"+ ".xml";
	    	File xmlFile = new File (aSeamarksPath);
	    	if (xmlFile.exists()) {
	    		mSeamarkFilePath = aSeamarksPath;
	    		mSeamarkOsm.readSeamarkFile(aSeamarksPath);
	    	} else {
	    		//String info = "no seamarks file: " + aPath;
	    		String info = getResources().getString(R.string.osmviewer_seamarks_file_not_found);
	    		showToastOnUiThread(info);
	    	}
		}*/
		if (aPath.endsWith(".xml")){
			File xmlFile = new File (aPath);
	    	if (xmlFile.exists()) {
	    		mSeamarkFilePath = aPath;
	    		mSeamarkOsm.readSeamarkFile(aPath);
	    	} else {
	    		//String info = "no seamarks file: " + aPath;
	    		String info = getResources().getString(R.string.osmviewer_seamarks_file_not_found);
	    		showToastOnUiThread(info);
	    	}
		}
	}
	
	
	
	
	
	private Runnable centerPointRefresh = new Runnable() {
		public void run() {
			
			GeoPoint geoPoint = mapView.getMapPosition().getMapCenter();
			byte zoom = mapView.getMapPosition().getZoomLevel();
			double lat = geoPoint.getLatitude();
			double lon = geoPoint.getLongitude();
			String mapFileName = "";
			File mapFile = mapView.getMapFile();
			if (mapFile != null) {
				mapFileName = mapFile.getName();
			}
			String renderFileName = "";
			String names[] = mRenderThemeName.split("/");
			int count = names.length;
			if (count > 0) {
				renderFileName = names[count-1];
			}
			int countPois = 0;
		    if (mSeamarksList!= null) {
		    	countPois = mSeamarksList.size();
		    }
		    int countWays = 0;
		    if (mSeamarkWayList != null ){
		    	countWays = mSeamarkWayList.size();
		    }
		    int countDisplayedWays = 0;
		    if (mSeamarkWithInfoPoisAndWaysItemizedOverlay != null) {
		    	countDisplayedWays = mSeamarkWithInfoPoisAndWaysItemizedOverlay.getDispayedWayListSize();
		    }
			
			String aMsg = "Mapcenter "  + "LAT: " + lat + " LON: " + lon + " Zoom " + zoom 
			               //+ " displayed pois " + countDisplayedSeamarks 
			              // + " all ways " + countWays + " displayed ways " + countDisplayedWays + "\n"
			               + " all pois " + countPois
			               + "\n Map: " + mapFileName;
			aMsg = aMsg    + "   Renderer: " + renderFileName; 
			               
			/*if (mapFile != null){
				try {
			    Projection aProjection = mapView.getProjection();
				int aLatSpan = aProjection.getLatitudeSpan(); // bottom to top
				int aLonSpan = aProjection.getLongitudeSpan(); // left right
				float lonSpan = aLonSpan /1E6f;
				float latSpan = aLatSpan /1E6f;
				aMsg = aMsg + "  LonSpan " + lonSpan + "° LatSpan " + latSpan +"°";
				} catch (Exception e ){
					
				}
			}*/
			    
				mTextViewCenterCoordinates.setText("");
				//checkAndDisplayMemory();
				mTextViewCenterCoordinates.append(aMsg);
				
			mCenterPointHandler.postDelayed(this,1000);
		}
	};
	
	private Runnable readPoisAndWays = new Runnable() {
		public void run() {
			if (mSeamarkOsm.getSeamarkFileReadComplete()){
				mSeamarksDictionary = mSeamarkOsm.getSeamarksAsDictionary();
				mSeamarksList = mSeamarkOsm.getSeamarksAsArrayList();
				mSeamarkWayList = mSeamarkOsm.getSeamarkWaysAsArrayList();
				updateSeamarkNodesOnOverlay();
				mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
			} else {
				// try again
				mReadPoisAndWaysHandler.postDelayed(this,1000);
			}
		}
	};
	
	public void updateSeamarkNodesOnOverlay() {
		// maybe we are called before the seamarksfileRead is completed
		if (!mSeamarkOsm.getSeamarkFileReadComplete()) return;
		// we only update the own position
		if (this.mMyLocationChanged) return;
		// maybe we get a new update, but we process an older one
		if (mInUpdateSeamarkNodesOnOverlay) return;
		
		mInUpdateSeamarkNodesOnOverlay= true;
		mCountSeamarksUpdate++;
		int sleeptime = 200;
		byte zoom = this.mapView.getMapPosition().getZoomLevel();
		if (mLastZoom != zoom ){  //&& (8 <= zoom  && zoom <=13 )) 
			if (mSeamarksList!= null){
				int count = mSeamarksList.size();
				for (int index=0;index < count; index ++){
					if (mSeamarksList!= null){
				       SeamarkNode aSeamarkNode = mSeamarksList.get(index);
				       removeSeamarkNodeFromOverlay(aSeamarkNode);
					}
				}
				mLastZoom = zoom;
				sleeptime = sleeptime * 2;
			}
		}
		try {
		Thread.sleep(sleeptime);
		} catch (Exception e) {
			
		}
		Projection aProjection = this.mapView.getProjection();
		int aLatSpan = aProjection.getLatitudeSpan(); // bottom to top
		int aLonSpan = aProjection.getLongitudeSpan(); // left right
		GeoPoint geoPoint = this.mapView.getMapPosition().getMapCenter();
		int minlat = geoPoint.latitudeE6 - aLatSpan /2;
		int minlon = geoPoint.longitudeE6 - aLonSpan/2;
		int maxlat = geoPoint.latitudeE6 + aLatSpan /2;
		int maxlon = geoPoint.longitudeE6 + aLonSpan /2;
		BoundingBox aBoundingBox = new BoundingBox(minlat,minlon,maxlat,maxlon);
		if (mSeamarksList != null){
			int count = mSeamarksList.size();
			for (int index=0;index < count; index ++){
			  SeamarkNode aSeamarkNode = mSeamarksList.get(index);
			  GeoPoint aNodePoint = new GeoPoint(aSeamarkNode.getLatitudeE6(),aSeamarkNode.getLongitudeE6());
			  if (aBoundingBox.contains(aNodePoint))  {
				  if (zoom >= mMinZoomForSeamarks) { // zoom 8 
					  if (zoom <= mMinZoomForSeamarks + 2) { // 8<= zoom <= 10
						  if (checkDisplay(aSeamarkNode)){
							  putSeamarkNodeOnOverlay(aSeamarkNode);
						  }
					  } else {
						  putSeamarkNodeOnOverlay(aSeamarkNode); 
					  }
				  }
			  } else {
				  removeSeamarkNodeFromOverlay(aSeamarkNode);
			  }
			}
		}
		if (mSeamarkWayList != null) {
			updateSeamarkWaysOnOverlay(aBoundingBox);
		}
		mInUpdateSeamarkNodesOnOverlay= false;
	}
	
	private void updateSeamarkWaysOnOverlay(BoundingBox boundingBox){
		if (mSeamarkWayList != null ){
			int countNavLines = mSeamarkWayList.size();
			for (int lineIndex = 0; lineIndex < countNavLines; lineIndex ++){
				SeamarkWay seamarkWay = mSeamarkWayList.get(lineIndex);
				if (seamarkWay.belongsToBoundingBox(boundingBox)){
					putSeamarkWayOnOverlay(seamarkWay);
				} else {
					removeSeamarkWayFromOverlay(seamarkWay);
				}
			}
		}
	}
	
	private void putSeamarkWayOnOverlay(SeamarkWay seamarkWay){
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.addSeamarkWay(seamarkWay);
	}
	
	private void removeSeamarkWayFromOverlay(SeamarkWay seamarkWay){
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.removeSeamarkWay(seamarkWay);
	}
	
	private boolean checkDisplay(SeamarkNode aSeamarkNode){
		boolean result = false;
		
		String type = aSeamarkNode.getValueToKey("seamark:type");
		if (type != null && type.equals("buoy_safe_water")) result = true;
		if (type != null && type.equals("light_minor")) result = true;
		if (type != null && type.equals("light_major")) result = true;
		if (type!= null && type.equals("landmark")) {
			 String seamarkStr = aSeamarkNode.getValueToKey("seamark");
			 if ((seamarkStr != null) && (seamarkStr.equals("lighthouse"))) {
				 result = true;
			 }
			 if ((seamarkStr != null) && (seamarkStr.equals("landmark"))) {
				 result = true;
			 }
			 String lightRangeStr = aSeamarkNode.getValueToKey("seamark:light:range");
				float range = 10.0f;
				try {
						if (lightRangeStr != null)  range = Float.parseFloat(lightRangeStr);
					 } catch (Exception e) {}
				if (range > 10.1f) {  // Lighthouse in NL may only have  the range set
					result = true;
				}
				String light1RangeStr = aSeamarkNode.getValueToKey("seamark:light:1:range");
				float range1 = 10.0f;
				try {
						if (light1RangeStr != null)  range1 = Float.parseFloat(light1RangeStr);
					 } catch (Exception e) {}
				if (range1 > 10.1f) { // Lighthouse in NL may only have  the range set
					result = true;
				}
		}
		  
		return result;
		
	}
	
private void putSeamarkNodeOnOverlayOld (SeamarkNode aSeamarkNode) {
		// until 13_04_15 
		int latE6 = aSeamarkNode.getLatitudeE6();
		int lonE6 = aSeamarkNode.getLongitudeE6();
		GeoPoint aGeoPoint = new GeoPoint(latE6,lonE6);
		
		int count = aSeamarkNode.getTagListSize();
		/*
		LinkedHashMap<String,String> tagDictionary = aSeamarkNode.getTagDictionary();
		Set<String> aKeySet = tagDictionary.keySet();
		Collection<String>  aValueCollection = tagDictionary.values();*/
		byte zoomLevel = this.mapView.getMapPosition().getZoomLevel();
		SeamarkDrawable aSeamarkDrawable = new SeamarkDrawable(aSeamarkNode,zoomLevel,mDisplayFactor);
		Drawable marker = aSeamarkDrawable.getDrawable();
		// collect the info for the seamark
		StringBuilder b = new StringBuilder();
		b.append("LAT ");
		b.append(aGeoPoint.getLatitude());
		b.append("\n");
		b.append("LON ");
		b.append(aGeoPoint.getLongitude());
		b.append("\n");
		for (int index = 0; index < count; index++){
			
			Tag aTag = aSeamarkNode.getTag(index);
			String key = aTag.key;
			if (key.startsWith("seamark:")){
				key = key.substring("seamark:".length(),key.length());
			}
			String value = aSeamarkNode.getValueToKey(aTag.key);
			b.append(key); b.append(": ");b.append(value);b.append("\n");
		}
		String aInfo = b.toString();
		String aTitle = "Seamark";
		String seamarkName = aSeamarkNode.getValueToKey("seamark:name");
		String seamarkLongName = aSeamarkNode.getValueToKey("seamark:longname");
		if (seamarkName != null) aTitle = seamarkName;
		if (seamarkLongName != null) aTitle = seamarkLongName;
		SeamarkWithPoisOverlayItem aSeamarkItem = null;
		if (marker != null) {  // we only process if we have a valid symbol
			aSeamarkItem = new SeamarkWithPoisOverlayItem(aGeoPoint,aTitle,aInfo,ItemizedOverlay.boundCenter(marker),aSeamarkNode);
			if (isInfoSeamark(aSeamarkNode)){
				infoSeamarksList.add(aSeamarkItem);
				if (showInfoSeamarks){
					if (aSeamarkNode.getSeamarkWithPoisOverlayItem()== null){
					  mSeamarkWithInfoPoisAndWaysItemizedOverlay.addItem(aSeamarkItem);
					  aSeamarkNode.setSeamarkWithPoisOverlayItem(aSeamarkItem);
					  countDisplayedSeamarks++;
					}
				}
			}
			if (!isInfoSeamark(aSeamarkNode)){
				if (aSeamarkNode.getSeamarkWithPoisOverlayItem()== null){
				  //mSeamarkItemizedOverlay.addItem(aSeamarkItem);
					mSeamarkWithInfoPoisAndWaysItemizedOverlay.addNode(aSeamarkNode);
				  aSeamarkNode.setSeamarkWithPoisOverlayItem(aSeamarkItem);
				  countDisplayedSeamarks++;
				  preparePaintSingleLightsIfNecessary(aSeamarkNode);
				  preparePaintNumberedLightsIfNecessary( aSeamarkNode);
				}
			} // marker != null
		}else {
			// marker == null, we have no defined Symbol
			/*if (aSeamarkNode.getSeamarkWithPoisOverlayItem()== null){
				if (showInfoSeamarks){ // display the unknown symbol only on demand
					aSeamarkItem = new SeamarkWithPoisOverlayItem(aGeoPoint,aTitle,aInfo,null,aSeamarkNode);
					mSeamarkItemizedOverlay.addItem(aSeamarkItem);
					infoSeamarksList.add(aSeamarkItem);
					aSeamarkNode.setSeamarkWithPoisOverlayItem(aSeamarkItem);
					countDisplayedSeamarks++;
				} // showInfoSeamarks
			} // 
*/		}
	}

private void removeSeamarkNodeFromOverlayOld (SeamarkNode aSeamarkNode){
	//until 13_04_15
	SeamarkWithPoisOverlayItem aSeamarkItem = aSeamarkNode.getSeamarkWithPoisOverlayItem();
	if (aSeamarkItem != null) {
		//mSeamarkItemizedOverlay.removeItem(aSeamarkItem);
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.removeNode(aSeamarkNode);
		mNodeListSectorFire.remove(aSeamarkNode);
		mNodeListMainLights.remove(aSeamarkNode);
		infoSeamarksList.remove(aSeamarkNode);
		countDisplayedSeamarks--;
	}
    aSeamarkNode.setSeamarkWithPoisOverlayItem(null);
}
private void removeSeamarkNodeFromOverlay (SeamarkNode aSeamarkNode){
	
	if (infoSeamarksList.contains(aSeamarkNode)) {
		// remove the itemizedOverlay 
		SeamarkWithPoisOverlayItem aSeamarkItem = aSeamarkNode.getSeamarkWithPoisOverlayItem();
		if (aSeamarkItem != null) {
			mSeamarkWithInfoPoisAndWaysItemizedOverlay.removeItem(aSeamarkItem);
			infoSeamarksList.remove(aSeamarkNode);
			aSeamarkNode.setSeamarkWithPoisOverlayItem(null);
		}
	} else {
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.removeNode(aSeamarkNode);
		mNodeListSectorFire.remove(aSeamarkNode);
		mNodeListMainLights.remove(aSeamarkNode);
		countDisplayedSeamarks--;
	}
   
}
	
	private void putSeamarkNodeOnOverlay (SeamarkNode aSeamarkNode) {
		if (!isInfoSeamark(aSeamarkNode)){
			mSeamarkWithInfoPoisAndWaysItemizedOverlay.addNode(aSeamarkNode);
			countDisplayedSeamarks++;
			preparePaintSingleLightsIfNecessary(aSeamarkNode);
			preparePaintNumberedLightsIfNecessary( aSeamarkNode);
		} else {
			int latE6 = aSeamarkNode.getLatitudeE6();
			int lonE6 = aSeamarkNode.getLongitudeE6();
			GeoPoint aGeoPoint = new GeoPoint(latE6,lonE6);
			
			int count = aSeamarkNode.getTagListSize();
			byte zoomLevel = this.mapView.getMapPosition().getZoomLevel();
			SeamarkDrawable aSeamarkDrawable = new SeamarkDrawable(aSeamarkNode,zoomLevel,mDisplayFactor);
			Drawable marker = aSeamarkDrawable.getDrawable();
			// collect the info for the seamark
			StringBuilder b = new StringBuilder();
			b.append("LAT ");
			b.append(aGeoPoint.getLatitude());
			b.append("\n");
			b.append("LON ");
			b.append(aGeoPoint.getLongitude());
			b.append("\n");
			for (int index = 0; index < count; index++){
				
				Tag aTag = aSeamarkNode.getTag(index);
				String key = aTag.key;
				if (key.startsWith("seamark:")){
					key = key.substring("seamark:".length(),key.length());
				}
				String value = aSeamarkNode.getValueToKey(aTag.key);
				b.append(key); b.append(": ");b.append(value);b.append("\n");
			}
			String aInfo = b.toString();
			String aTitle = "Seamark";
			String seamarkName = aSeamarkNode.getValueToKey("seamark:name");
			String seamarkLongName = aSeamarkNode.getValueToKey("seamark:longname");
			if (seamarkName != null) aTitle = seamarkName;
			if (seamarkLongName != null) aTitle = seamarkLongName;
			SeamarkWithPoisOverlayItem aSeamarkItem = null;
			if (marker != null) {  // we only process if we have a valid symbol
				aSeamarkItem = new SeamarkWithPoisOverlayItem(aGeoPoint,aTitle,aInfo,ItemizedOverlay.boundCenter(marker),aSeamarkNode);
				infoSeamarksList.add(aSeamarkItem);
				if (showInfoSeamarks){
					if (aSeamarkNode.getSeamarkWithPoisOverlayItem()== null){
					  mSeamarkWithInfoPoisAndWaysItemizedOverlay.addItem(aSeamarkItem);
					  aSeamarkNode.setSeamarkWithPoisOverlayItem(aSeamarkItem);
					  countDisplayedSeamarks++;
					}
				}
			} else {
				// marker == null, we have no defined Symbol
				/*if (aSeamarkNode.getSeamarkWithPoisOverlayItem()== null){
					if (showInfoSeamarks){ // display the unknown symbol only on demand
						aSeamarkItem = new SeamarkWithPoisOverlayItem(aGeoPoint,aTitle,aInfo,null,aSeamarkNode);
						mSeamarkItemizedOverlay.addItem(aSeamarkItem);
						infoSeamarksList.add(aSeamarkItem);
						aSeamarkNode.setSeamarkWithPoisOverlayItem(aSeamarkItem);
						countDisplayedSeamarks++;
					} // showInfoSeamarks
				} // 
	           */	
			}
		}
	}
	
	
	
	private void preparePaintSingleLightsIfNecessary(SeamarkNode aSeamarkNode) {
		 String seamarkType = aSeamarkNode.getValueToKey("seamark:type");
		 if (seamarkType != null &&seamarkType.equals("landmark")) {
			   String seamarkStr = aSeamarkNode.getValueToKey("seamark") ;
			   if (seamarkStr != null && seamarkStr.equals("lighthouse")) {
				   String longname = aSeamarkNode.getValueToKey("seamark:longname");
			    	Log.d(TAG,"light house found "+ longname);
					mNodeListMainLights.add(aSeamarkNode);	  
		       }
			   String lightColor = aSeamarkNode.getValueToKey("seamark:light:colour");
			   if (lightColor != null ) {
				   String longname = aSeamarkNode.getValueToKey("seamark:longname");
			    	Log.d(TAG,"landmark found "+ longname);
					mNodeListMainLights.add(aSeamarkNode);	  
		       }
		 }
	     
  }
	
	 private void preparePaintNumberedLightsIfNecessary(SeamarkNode aSeamarkNode) {
		 String seamarkType = aSeamarkNode.getValueToKey("seamark:type");
		   if (seamarkType != null) {
			    /*String light1SectorStart = aSeamarkNode.getValueToKey("seamark:light:1:sector_start");
			    if (light1SectorStart != null ){
			    	// we have a sector fire
			    	String longname = aSeamarkNode.getValueToKey("seamark:longname");
			    	Log.d(TAG,"Sector fire found "+ longname);
					mNodeListSectorFire.add(aSeamarkNode);	
			    }*/
			   if (seamarkType.equals("light_major") 
					   || seamarkType.equals("light_minor")
					    ) {
				   mNodeListSectorFire.add(aSeamarkNode); 
			   } else {
				   String light1Color = aSeamarkNode.getValueToKey("seamark:light:1:colour");
				    if (light1Color != null ){
				    	//we have a other sector fire
				    	String longname = aSeamarkNode.getValueToKey("seamark:name");
				    	Log.d(TAG,"Sector fire found "+ longname);
						mNodeListSectorFire.add(aSeamarkNode);	
				    }
			   }
			    
		   }
	     
   }
	
	private boolean isInfoSeamark(SeamarkNode aSeamarkNode){
		boolean result = false;
		String seamarkType = aSeamarkNode.getValueToKey("seamark:type");
		
		if ( seamarkType != null) {
			if (  
				   seamarkType.contains("small_craft_facility")
				|| seamarkType.contains("mooring")
				|| seamarkType.contains("harbour")
				|| seamarkType.contains("bridge")
				|| seamarkType.contains("cable_overhead") 
				|| seamarkType.contains("distance_mark")
			   ){
				  result = true;
			    }
		}
		return result;
	}
	  
	
	
	/**
	 * Sets all file filters and starts the FilePicker to select a map file.
	 */
	private void startMapFilePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
		FilePicker.setFileSelectFilter(new ValidMapFile());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
	}
	
	/**
	 * Sets all file filters and starts the FilePicker to select a map file.
	 */
	private void startSeamarksFilePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
		FilePicker.setFileSelectFilter(new ValidSeamarksFile());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_SEAMARKS_FILE);
	}
	
	/**
	 * Sets all file filters and starts the FilePicker to select an XML file.
	 */
	private void startRenderThemePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
		FilePicker.setFileSelectFilter(new ValidRenderTheme());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_RENDER_THEME_FILE);
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
	
	private void toggleShowOtherSeamarks(){
		if(showInfoSeamarks){
			int count = infoSeamarksList.size();
			for (int index = 0; index < count; index++){
				SeamarkWithPoisOverlayItem overlayItem = infoSeamarksList.get(index);
				mSeamarkWithInfoPoisAndWaysItemizedOverlay.removeItem(overlayItem);
			}
			showInfoSeamarks = false;
		} else {
			int count = infoSeamarksList.size();
			for (int index = 0; index < count; index++){
				SeamarkWithPoisOverlayItem overlayItem = infoSeamarksList.get(index);
				mSeamarkWithInfoPoisAndWaysItemizedOverlay.addItem(overlayItem);
			}
			showInfoSeamarks = true;
		}
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
	}
	
	private void toggleShowSectorFires(){
		if(showSectorFires){
			
			showSectorFires = false;
		} else {
			
			showSectorFires = true;
		}
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
	}
	
	private void toggleShowLocation(){
		if(showMyLocation){
			
			disableShowMyLocation();
		} else {
			enableShowMyLocation(true);
		}
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
	}
	
	
	private void toggleShowRoute(){
		if(mShowRoute){
			mShowRoute = false;
		} else {
			mShowRoute = true;
		}
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
	}
	
	private void toggleShowMapInfo(){
		if(mShowMapInfo){
			mCenterPointHandler.removeCallbacks(centerPointRefresh);
			mTextViewCenterCoordinates.setText("");
			mTextViewCenterCoordinates.setVisibility(View.INVISIBLE);
			mTextViewCenterCoordinatedSavedHeight = mTextViewCenterCoordinates.getHeight();
			Log.d(TAG,"TextView height " + mTextViewCenterCoordinatedSavedHeight);
			mTextViewCenterCoordinates.setHeight(0);
			GeoPoint geoPoint = mapView.getMapPosition().getMapCenter();
			GeoPoint movedPoint = new GeoPoint(geoPoint.latitudeE6 + 100, geoPoint.longitudeE6);
			mapView.setCenter(movedPoint);
			mShowMapInfo = false;
			
		} else {
			mShowMapInfo = true;
			mTextViewCenterCoordinates.setVisibility(View.VISIBLE);
			mTextViewCenterCoordinates.setHeight(mTextViewCenterCoordinatedSavedHeight);
			mCenterPointHandler.postDelayed(centerPointRefresh, 1000); 
			
		}
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
		
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
			disableSnapToLocation(false);
			this.locationManager.removeUpdates(this.myLocationListener);
			
		}
	}
	
	
	/**
	 * 
	 * @param aFile a file that contains the route, selected via the filepicker
	 *    load the route  from aFile to the datbase
	 *    uses a thread 
	 *    visual feedback to the user with aprogress bar
	 */

	public void loadGPXRouteDataWithThread(File aFile) {

		final String aFilename = aFile.getAbsolutePath();
		//this.mProgressHorizontal.setVisibility(View.VISIBLE);
		//this.mProgressHorizontal.setProgress(0);
		//final String aUIMsg = getResources().getString(R.string.guiupdate_info_failed_in_parsing_route_gpx_file);
		final boolean test = true;
		final String aUIMsg ="Failed parsing xml-file";
		new Thread(new Runnable() {

			public void run() {

				try {
					int count = 1000;
					int updateProgressbar = count / 500; // The progressbar is 0 to 100,
					// we calculate the counter if we have to update
					int updateCounter = 0;
					XmlPullParserFactory parserCreator;

					parserCreator = XmlPullParserFactory.newInstance();

					XmlPullParser parser = parserCreator.newPullParser();
					FileReader myReader = new FileReader(aFilename);
					parser.setInput(myReader);
					// parser.setInput(text.openStream(), null);

					int parserEvent = parser.getEventType();
					int pointCounter = -1;

					String aLATStr = null;;
					String aLONStr = null;

					// Parse the XML returned from the file
					while (parserEvent != XmlPullParser.END_DOCUMENT) {
						switch (parserEvent) {
							case XmlPullParser.START_TAG:
								String tag = parser.getName();

								if (tag.compareTo("rtept") == 0) {
									pointCounter++;
									aLATStr = parser.getAttributeValue(null, "lat");
									aLONStr = parser.getAttributeValue(null, "lon");

									// we initialize time with currentTimeMillis
									long aUtc = System.currentTimeMillis();
									//mDbAdapter.insertRoutePointToTable(AISPlotterGlobals.DEFAULTROUTE, aLATStr, aLONStr, aUtc);

									if (test)
										Log.i(TAG, "   routepoint=" + pointCounter + " latitude=" + aLATStr
												+ " longitude=" + aLONStr);
									updateCounter++;
									/*if (updateCounter == updateProgressbar) {
										updateCounter = 0;
										mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the UI
																		// in a separate
																		// thread
											public void run() {
												mProgressHorizontal.incrementProgressBy(1);

											}
										});
									}*/
								} else if (tag.compareTo("time") == 0) {

									// we do not handle the time

								} else if (tag.compareTo("wpt") == 0) {

									// we do not handle wpt
								}
								break;
						}

						parserEvent = parser.next();
					}

				} catch (FileNotFoundException e) {
					Log.d(TAG, "File not found");
				} catch (Exception e) {
					Log.i(TAG, "Failed in parsing XML", e);
					showToastOnUiThread(aUIMsg);

				}

				/*mProgressbarHandler.post(new Runnable() { // we must post , as we could not access the UI in a separate
					// thread
					public void run() {
						mProgressHorizontal.setVisibility(View.INVISIBLE);
						// we must not call restoreRouteFromDatabase here
						// there  is a resume() pending, as we selected a file with
						// the file picker activity
						//restoreRouteFromDatabase(DEFAULTROUTE);
					}
				});*/

			}
		}).start();

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
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu_osm_viever_with_info_pois, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		MenuItem aMenuItem = menu.findItem(R.id.menu_show_other_seamarks);
		if(showInfoSeamarks) {
			//aMenuItem.setTitle("hide infos");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_hide_infos));
		} else {
			//aMenuItem.setTitle("show infos");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_show_infos));
		}
		aMenuItem = menu.findItem(R.id.menu_show_sector_fires);
		if (showSectorFires){
			//aMenuItem.setTitle("hide fires");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_hide_fires));
		} else {
			//aMenuItem.setTitle("show fires");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_show_fires));
		}
		
		aMenuItem = menu.findItem(R.id.menu_show_location);
		if (showMyLocation){
			//aMenuItem.setTitle("hide location");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_hide_location));
		} else {
			//aMenuItem.setTitle("show location");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_show_location));
		}
		
		aMenuItem = menu.findItem(R.id.menu_show_map_info);
		if (mShowMapInfo){
			//aMenuItem.setTitle("hide map info");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_hide_map_info));
		} else {
			//aMenuItem.setTitle("show map info");
			aMenuItem.setTitle(getResources().getString(R.string.osmviewer_menu_show_map_info));
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
			case R.id.menu_render_theme_select_file:
				startRenderThemePicker();
				return true;
			case R.id.menu_render_theme_osmarender:
				this.mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
				mRenderThemeName= "Osmarender";
				return true;
			case R.id.menu_render_theme_openseamaprender:
				setStandardRendererTheme();
				return true;
			case R.id.menu_mapfile_select_file:
				startMapFilePicker();
				return true;
			case R.id.menu_mapfile_select_seamarks_file:
				startSeamarksFilePicker();
				return true;	
			case R.id.menu_screenshot_png:
				this.screenshotCapturer.captureScreenShot(CompressFormat.PNG);
				return true;
			case R.id.menu_show_other_seamarks:
				toggleShowOtherSeamarks();
				return true;
			case R.id.menu_show_sector_fires:
				toggleShowSectorFires();
				return true;
			case R.id.menu_show_seamark_names:
				return true;
			case R.id.menu_show_no_names_info:
				mSeamarkWithInfoPoisAndWaysItemizedOverlay.setShowNameStatus(ViewerGlobals.seamarkNameNotVisible);
				return true;
			case R.id.menu_show_long_names_info:
				mSeamarkWithInfoPoisAndWaysItemizedOverlay.setShowNameStatus(ViewerGlobals.seamarkLongNameVisible);
				return true;
			case R.id.menu_show_short_names_info:
				mSeamarkWithInfoPoisAndWaysItemizedOverlay.setShowNameStatus(ViewerGlobals.seamarkShortNameVisible);
				return true;
			case R.id.menu_show_location:
				toggleShowLocation();
				return true;
			case R.id.menu_show_map_info:
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
			default:
				return false;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SELECT_MAP_FILE) {
			if (resultCode == RESULT_OK) {
	
				if (intent != null && intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
					File selectedFile = new File(intent.getStringExtra(FilePicker.SELECTED_FILE));
					this.mapView.setMapFile(selectedFile);
					String selectedFilePath = selectedFile.getAbsolutePath();
					this.setSeamarkFilePathAndRead(selectedFilePath);
					mReadPoisAndWaysHandler.postDelayed(readPoisAndWays, 1000);  // it takes some time to read the seamarks file
			
				}
			} else if (resultCode == RESULT_CANCELED && !this.mapView.getMapGenerator().requiresInternetConnection()
					&& this.mapView.getMapFile() == null) {
				finish();
			}
		} else if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			try {
				String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
				mRenderThemeName = aFilename;
				File aFile = new File(aFilename);
				this.mapView.setRenderTheme(aFile);
			} catch (FileNotFoundException e) {
				showToastOnUiThread(e.getLocalizedMessage());
			}
		} else if (requestCode == SELECT_SEAMARKS_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			
				String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
				File selectedFile = new File(aFilename);
				this.setSeamarkFilePathAndRead(selectedFile.getAbsolutePath());
				mReadPoisAndWaysHandler.postDelayed(readPoisAndWays, 1000);  // it takes some time to read the seamarks file
		} else if (requestCode == SELECT_ROUTE_GML_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			
			     String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
			     File selectedFile = new File(aFilename);
			     loadGMLRouteData(selectedFile);
			     this.mShowRoute = true;
			     mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
	    } else if (requestCode == SELECT_ROUTE_GPX_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			
		     String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
		     File selectedFile = new File(aFilename);
		     loadGPXRouteData(selectedFile);
		     this.mShowRoute = true;
		     mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
          } 
	}
	
	/**
	 * Uses the UI thread to display the given text message as toast notification.
	 * 
	 * @param text
	 *            the text message to display
	 */
	public void showToastOnUiThread(final String text) {

		if (AndroidUtils.currentThreadIsUiThread()) {
			Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
			toast.show();
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast toast = Toast.makeText( OpenSeaMapViewerWithInfoPois.this, text, Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	}
	
	public void requestOverlayRedraw(GeoPoint newPoint) {
		this.mMyLocationPoint = newPoint;
		this.mMyLocationChanged = true;
		mSeamarkWithInfoPoisAndWaysItemizedOverlay.requestRedraw();
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
