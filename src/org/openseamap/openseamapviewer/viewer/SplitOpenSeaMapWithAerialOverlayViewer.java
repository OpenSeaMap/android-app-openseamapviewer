package org.openseamap.openseamapviewer.viewer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.mapsforge.android.AndroidUtils;
import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.TileCache;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.android.maps.rendertheme.InternalRenderTheme;
import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.openseamap.openseamapviewer.MainActivity;
import org.openseamap.openseamapviewer.OverlayOpenSeaMapDatabaseRenderer;
import org.openseamap.openseamapviewer.R;
import org.openseamap.openseamapviewer.R.drawable;
import org.openseamap.openseamapviewer.R.id;
import org.openseamap.openseamapviewer.R.layout;
import org.openseamap.openseamapviewer.R.menu;
import org.openseamap.openseamapviewer.R.string;
import org.openseamap.openseamapviewer.downloader.BingAerialTileDownloader;
import org.openseamap.openseamapviewer.downloader.EniroAerialTileDownloader;
import org.openseamap.openseamapviewer.downloader.EniroNauticalTileDownloader;
import org.openseamap.openseamapviewer.environment.Environment2;
import org.openseamap.openseamapviewer.filefilter.FilterByFileExtension;
import org.openseamap.openseamapviewer.filefilter.ValidMapFile;
import org.openseamap.openseamapviewer.filefilter.ValidRenderTheme;
import org.openseamap.openseamapviewer.filefilter.ValidRouteGmlFile;
import org.openseamap.openseamapviewer.filefilter.ValidSeamarksFile;
import org.openseamap.openseamapviewer.filepicker.FilePicker;
import org.openseamap.openseamapviewer.locationlistener.MySplitLocationListernerWithAerialOverlay;
import org.openseamap.openseamapviewer.overlay.CenterCircleOverlay;
import org.openseamap.openseamapviewer.overlay.SplitSeamarksWithAerialOverlay;
import org.openseamap.openseamapviewer.seamarks.SeamarkNode;
import org.openseamap.openseamapviewer.seamarks.SeamarkOsm;
import org.openseamap.openseamapviewer.seamarks.SeamarkSymbol;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SplitOpenSeaMapWithAerialOverlayViewer extends MapActivity {
	private static final String TAG = "SplitOpenSeaMapWithAerialOverlayViewer";
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
	public int  mCountSeamarksUpdate = 0;
	private static boolean mInUpdateSeamarkNodesOnOverlay = false;
	
	public static final int seamarkNameNotVisible = 0;
	public static final int seamarkShortNameVisible= 1;
	public static final int seamarkLongNameVisible= 2;
	
	
	public static byte mMinZoomForSeamarks= 8;
	//private ScreenshotCapturerOpenSeaMapWithPois screenshotCapturer;
	MapController mMapController1;
	MapView mMapView1;
	public SplitSeamarksWithAerialOverlay mSplitSeamarksWithAerialOverlay = null;
	
	MapController mMapController2;
	MapView mMapView2;
	private CenterCircleOverlay mCircleOverlay = null;
	
	//private OpenSeamapTestDatabaseRenderer mMapGenerator;
	TextView mTextViewCenterCoordinates = null;
	int mTextViewCenterCoordinatedSavedHeight;
	private String mRenderThemeName = "";
	private static Handler mCenterPointHandler = new Handler();
	private SeamarkOsm mSeamarkOsm = null;
	private String mSeamarkFilePath = "";
	//private LinkedHashMap<String,SeamarkNode> mSeamarksDictionary = null;
	private ArrayList<SeamarkNode> mSeamarksNodeList = null;
	private static Handler mReadPoisHandler = new Handler();
	
	//public SeamarkItemizedOverlay mSeamarkItemizedOverlay = null;
	
	private Bitmap mDefaultSeamark = null;
	
	
	private ArrayList<SeamarkNode> otherSeamarksInfoList = null;
	//private ArrayList<SeamarkOverlayItem> seamarksItemList = null;
	public boolean showOtherSeamarks = false;
	public boolean showSectorFires = true;
	public ArrayList<SeamarkNode> mNodeListNumberedFires;
	public ArrayList<SeamarkNode> mNodeListSingleLights;
	private int mCountDisplayedSeamarks =0;
	public byte mLastZoom;
	private boolean mShowMapInfo = true;
	private float mDisplayFactor = 1.0f;
    // deal with routes
	
	public ArrayList<GeoPoint> mRouteList = null;
	public boolean mShowRoute = false;
	
	// deal with location
	
	public boolean showMyLocation;
	private boolean snapToLocation;
	private LocationManager locationManager;
	private MySplitLocationListernerWithAerialOverlay mySplitLocationListener;
	private boolean mFillDirectionalSector;
	public  boolean mMyLocationChanged;
	public GeoPoint mMyLocationPoint = null;
	
	
	// deal with the aerial Overlay
	
	private int mTransparencyLevel ;
	private OverlayOpenSeaMapDatabaseRenderer mOverlayOpenSeaMapDatabaseRenderer = null;
	
	
	// deal with prefs 
	
	SharedPreferences prefs;
	
	// deal with synchronizing the two views
	private GeoPoint mMapCenter1;
	private GeoPoint mMapCenter2;
	private byte mZoomLevel1;
	private byte mZoomLevel2;
	
	
	
	private static Handler mSyncronizeViewRefreshhandler = new Handler();
	
	/**
	 * the runnable, that is executed from mAISOverlayRefreshhandle 
	 */
	private Runnable syncronizeViewsRefresh = new Runnable() {
		public void run() {
			syncronizeDualViews();
			mSyncronizeViewRefreshhandler.postDelayed(this, 1000);
		}
	};
	
	private void syncronizeDualViews() {
		GeoPoint  mapCenter1= this.mMapView1.getMapPosition().getMapCenter();
		GeoPoint mapCenter2 = this.mMapView2.getMapPosition().getMapCenter();
		byte zoomLevel1 = this.mMapView1.getMapPosition().getZoomLevel();
		byte zoomLevel2 = this.mMapView2.getMapPosition().getZoomLevel();
		// figure out which view has moved
		if (this.mMapCenter1.compareTo(mapCenter1)!= 0){
			// mapView1 moved
			this.mMapCenter1 = mapCenter1;
			this.mMapCenter2= mapCenter1;
			this.mMapView2.getController().setCenter(mapCenter1);
		} else  if (this.mMapCenter2.compareTo(mapCenter2)!= 0){
			// mapView2 moved
			this.mMapCenter1 = mapCenter2;
			this.mMapCenter2 = mapCenter2;
			this.mMapView1.getController().setCenter(mapCenter2);
		}
		// figure out if zoom changed
		if (this.mZoomLevel1!= zoomLevel1) {
			this.mZoomLevel1 = zoomLevel1;
			this.mZoomLevel2 = zoomLevel1;
			this.mMapView2.getController().setZoom(zoomLevel1);
		} else if (this.mZoomLevel2!= zoomLevel2){
			this.mZoomLevel1 = zoomLevel2;
			this.mZoomLevel2 = zoomLevel2;
			this.mMapView1.getController().setZoom(zoomLevel2);
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS + Window.FEATURE_ACTION_BAR);
		mActivityManager =(ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		 if (metrics.densityDpi == 320) {
		    	// we have a nexus 7 nexus 7
		    	mDisplayFactor = 1.5f;
		    	Log.d(TAG,"nexus 7 with 320 dpi"); 
		    } 
		//this.screenshotCapturer = new ScreenshotCapturerOpenSeaMapWithPois(this);
		//this.screenshotCapturer.start();
		SeamarkSymbol.preloadFromDefsFile(MainActivity.OPENSEAMAP_STANDARDSYMBOLDEFS);
		//SeamarkSymbol.preloadFromDefsFile("symbols5.defs");
		mSeamarkOsm = new SeamarkOsm(this);
		setContentView(R.layout.activity_render_openseamap_split_map_viewer);
		this.mMapView1 = (MapView) findViewById(R.id.mapViewLeft);
		mTextViewCenterCoordinates = (TextView) findViewById(R.id.OSM_Info_textview); 
		
		configureMapView(this.mMapView1);
		this.mMapController1 = this.mMapView1.getController();
		/*
		 * MapGeneratorInternal mapGeneratorInternalNew = MapGeneratorInternal.DATABASE_RENDERER;
		MapGenerator mapGenerator = MapGeneratorFactory.createMapGenerator(mapGeneratorInternalNew);
		this.mapView.setMapGenerator(mapGenerator);*/
		
		// ask the prefs if there is a map file
		String mapfileName = prefs.getString(MainActivity.PREV_LAST_MAPFILE_PATH,"");
		if (!mapfileName.equals("")){
			// we have a mapfileName from the prefs
			File mapFile = new File (mapfileName);
			FileOpenResult aFileOpenResult = this.mMapView1.setMapFile(mapFile);
		}
		Context context = this.getApplicationContext();
		mTransparencyLevel = 112;
		TileDownloader aUnderlayAerialTileDownloader = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null ) {
			String downloadType = extras.getString(MainActivity.DOWNLOAD_AERIAL_UNDERLAY);
			if (downloadType != null && downloadType.equals(MainActivity.DOWNLOAD_AERIAL_TYPE_BING)) {
				aUnderlayAerialTileDownloader= new BingAerialTileDownloader(context);
			}
			if (downloadType != null && downloadType.equals(MainActivity.DOWNLOAD_AERIAL_TYPE_ENIRO)) {
				// the EniroTile are downloaded to a subdirectory with the mapfileName
				EniroAerialTileDownloader aEnirioEarialTileDownloader = new EniroAerialTileDownloader(context);
	            if (!mapfileName.equals("")){
	            	String[] fields = mapfileName.split("/");
	            	int countFields = fields.length;
					String mapName = fields[countFields-1];
					if (mapName !=null && mapName.endsWith(".map")) {
							mapName = mapName.substring(0,mapName.lastIndexOf("."));
							aEnirioEarialTileDownloader.setTileCacheSubDirName(mapName);
						}
					aUnderlayAerialTileDownloader = aEnirioEarialTileDownloader;	
				}
			}
		}
		OverlayOpenSeaMapDatabaseRenderer aOverlayOpenSeaMapDatabaseRenderer = new OverlayOpenSeaMapDatabaseRenderer(context,mTransparencyLevel,aUnderlayAerialTileDownloader);
		this.mMapView1.setMapGenerator(aOverlayOpenSeaMapDatabaseRenderer);
		
		
		
		//this.mapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
		
		// set the Render theme
		setStandardRendererTheme();
		
		// set the tile cache to false ( otherwise we get some file not found exceptions due to a problem in mapsforge)
		TileCache fileSystemTileCache1 = this.mMapView1.getFileSystemTileCache();
		boolean persistent1 = false;
		fileSystemTileCache1.setPersistent(persistent1);
		
        // load the seamarks file into aSeamarkOsmObject and create the dictionary
		mDefaultSeamark = BitmapFactory.decodeResource(getResources(),R.drawable.star); 
		mFillDirectionalSector = true;
		mSplitSeamarksWithAerialOverlay = new SplitSeamarksWithAerialOverlay(new BitmapDrawable(getResources(),mDefaultSeamark),this,mSeamarkOsm,mFillDirectionalSector, mDisplayFactor);
		otherSeamarksInfoList = new ArrayList<SeamarkNode>();
		mMapView1.getOverlays().add(mSplitSeamarksWithAerialOverlay);
		
		mNodeListNumberedFires = new ArrayList<SeamarkNode>();
		mNodeListSingleLights = new ArrayList<SeamarkNode>();
		
		// get the pointers to different system services
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		this.mySplitLocationListener = new MySplitLocationListernerWithAerialOverlay(this);
		this.showMyLocation= false;
		this.mMyLocationChanged= false;
		
		File currentMapFile = this.mMapView1.getMapFile();
		if (currentMapFile != null ) {
			String currentMapFilePath = currentMapFile.getAbsolutePath();
		    setSeamarkFilePathAndRead(currentMapFilePath);
		    mReadPoisHandler.postDelayed(readPois, 1);  // it takes some time to read the seamarks file 
		}
		
		
		
		//mapController.setZoom(12);
		
		//GeoPoint newPoint = new GeoPoint(54.00,11.37);
		//this.mapController.setCenter(newPoint);
		if (!this.mMapView1.getMapGenerator().requiresInternetConnection() && this.mMapView1.getMapFile() == null) {
			startMapFilePicker();
		}
		mLastZoom = this.mMapView1.getMapPosition().getZoomLevel();
		mTextViewCenterCoordinates = (TextView) findViewById(R.id.OSM_Info_textview);
		
		// set the mapview2
		this.mMapView2 = (MapView) findViewById(R.id.mapViewRight);
		MapGenerator mapGenerator2= new EniroNauticalTileDownloader(context);
		this.mMapView2.setMapGenerator(mapGenerator2);
		configureMapView(this.mMapView2);
		this.mMapController2 = this.mMapView2.getController();
		TileCache fileSystemTileCache2 = this.mMapView2.getFileSystemTileCache();
		boolean persistent2 = false;
		fileSystemTileCache2.setPersistent(persistent2);
		mCircleOverlay = new CenterCircleOverlay(this);
		mCircleOverlay.setMustShowCenter(true);
		mMapView2.getOverlays().add(mCircleOverlay);
		
		
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.mapViewMain);
		// if the device orientation is portrait, change the orientation to vertical
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			linearLayout.setOrientation(LinearLayout.VERTICAL);
		}
		
		mMapCenter1 = this.mMapView1.getMapPosition().getMapCenter();
		mMapCenter2 = this.mMapView2.getMapPosition().getMapCenter();
		mZoomLevel1 = this.mMapView1.getMapPosition().getZoomLevel();
		mZoomLevel2 = this.mMapView2.getMapPosition().getZoomLevel();
		mSyncronizeViewRefreshhandler.postDelayed(syncronizeViewsRefresh, 1000);
		
		if (mShowMapInfo){
			GeoPoint geoPoint = this.mMapView1.getMapPosition().getMapCenter();
			
			double lat = geoPoint.getLatitude();
			double lon = geoPoint.getLongitude();
			String aMsg = "Mapcenter "  + "LAT: " + lat + " LON: " + lon ;
			
			mTextViewCenterCoordinates.append(aMsg);
			mCenterPointHandler.postDelayed(centerPointRefresh, 1000); 
		}
		
		this.setTitle(getResources().getString(R.string.title_activity_main));
		
	}
	
	
	
	
	private void setStandardRendererTheme() {
		File cardDirectory = Environment2.getCardDirectory();
		String aPath = cardDirectory.getAbsolutePath();
        String aRenderThemeFilename =aPath + MainActivity.OPENSEAMAP_STANDARDDIRECTORYNAME + "/" + MainActivity.OPENSEAMAP_STANDARDRENDERERNAME;
        mRenderThemeName = aRenderThemeFilename;
		File aFile = new File(aRenderThemeFilename);
		try {
		this.mMapView1.setRenderTheme(aFile);
		} catch (IOException e) {
			//String aMsg = "Standard RenderTheme not found" + e.toString();
			String aMsg = getResources().getString(R.string.osmviewer_start_stdrender_not_found);
			mTextViewCenterCoordinates.append(aMsg);
			Log.d(TAG, e.toString());
		}
	}
	
	private void configureMapView(MapView mapView) {
		// configure the MapView and activate the zoomLevel buttons
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		mapView.setFocusable(true);
		
		boolean drawTileFrames = false;
		boolean drawTileCoordinates = false;
		boolean highlightWaterTiles = false;
		DebugSettings debugSettings = new DebugSettings(drawTileCoordinates, drawTileFrames, highlightWaterTiles);
		mapView.setDebugSettings(debugSettings);
		// set the localized text fields
		MapScaleBar mapScaleBar = mapView.getMapScaleBar();
		mapScaleBar.setText(TextField.KILOMETER, getString(R.string.unit_symbol_kilometer));
		mapScaleBar.setText(TextField.METER, getString(R.string.unit_symbol_meter));
        mapScaleBar.setShowMapScaleBar(true);
		// get the map controller for this MapView
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		int lat = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LAT, 90);
		int lon = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LON,180);
		int aZoomLevel = prefs.getInt(MainActivity.PREF_ZOOM_FACTOR,-1);
		if (lat != 90 && lon != 180 && aZoomLevel > 0) {
			// we got valid values
			this.mMapView1.getController().setZoom(aZoomLevel);
			GeoPoint geoPoint = new GeoPoint(lat,lon);
	 		this.mMapView1.getController().setCenter(geoPoint);
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
		File aMapFile = mMapView1.getMapFile();
		if (aMapFile!=null) {
			String mapfileName = aMapFile.getAbsolutePath();
			GeoPoint aGp = mMapView1.getMapPosition().getMapCenter();
	     	int aZoomLevel = mMapView1.getMapPosition().getZoomLevel();
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
			this.mMapView1.setClickable(true);
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
			this.mMapView1.setClickable(false);
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
		mSeamarksNodeList = null;  // the node list will be build in SeamarkOsm,we check if we have a valid list in updateSeamarks
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
	
	
	private void checkAndDisplayMemory() {
		//int countMemory = mActivityManager.getMemoryClass();
        StringBuffer aBuf = new StringBuffer();
       /* aBuf.append("Memory ");
        aBuf.append(countMemory);
        aBuf.append(" Mb" );*/
        int countPois = 0;
	    if (mSeamarksNodeList!= null) {
	    	countPois = mSeamarksNodeList.size();
	    }
	    int countOthers = 0;
	    if (otherSeamarksInfoList != null ) {
	    	countOthers = otherSeamarksInfoList.size();
	    }
	    
        aBuf.append("    seamarks updates: " );
        aBuf.append(mCountSeamarksUpdate);
        aBuf.append(" displayed seamarks ");
        aBuf.append(mCountDisplayedSeamarks);
        aBuf.append(" all seamarks ");
        aBuf.append( countPois);
        aBuf.append("    info seamarks: ");
        aBuf.append(countOthers);
        aBuf.append("\n");
        mTextViewCenterCoordinates.append(aBuf.toString());
	}
	
	
	private Runnable centerPointRefresh = new Runnable() {
		public void run() {
			
			GeoPoint geoPoint = mMapView1.getMapPosition().getMapCenter();
			byte zoom = mMapView1.getMapPosition().getZoomLevel();
			double lat = geoPoint.getLatitude();
			double lon = geoPoint.getLongitude();
			String mapFileName = "";
			File mapFile = mMapView1.getMapFile();
			if (mapFile != null) {
				mapFileName = mapFile.getName();
			}
			String renderFileName = "";
			String names[] = mRenderThemeName.split("/");
			int count = names.length;
			if (count > 0) {
				renderFileName = names[count-1];
			}
			
			
			String aMsg = "Mapcenter "  + "LAT: " + lat + " LON: " + lon + " Zoom " + zoom 
			               
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
				checkAndDisplayMemory();
				mTextViewCenterCoordinates.append(aMsg);
				
			mCenterPointHandler.postDelayed(this,1000);
		}
	};
	
	
	private synchronized void clearSeamarkListsIfNecessary() {
		if (mSeamarksNodeList != null) {
			int count = mSeamarksNodeList.size();
			for (int index=0;index < count; index ++){
				  SeamarkNode aSeamarkNode = mSeamarksNodeList.get(index);
				  if (aSeamarkNode != null) removeSeamarkNodeFromOverlay(aSeamarkNode);
		}
		
		}
	}
	
	private Runnable readPois = new Runnable() {
		public void run() {
			if (mSeamarkOsm.getSeamarkFileReadComplete()){
				//mSeamarksDictionary = mSeamarkOsm.getSeamarksAsDictionary();
				mSeamarksNodeList = mSeamarkOsm.getSeamarksAsArrayList();
				//updateSeamarkNodesOnOverlay();
				mSplitSeamarksWithAerialOverlay.requestRedraw();
			} else {
				// try again
				mReadPoisHandler.postDelayed(this,1000);
			}
		}
	};
	
	public void updateSeamarkNodesOnOverlay() {
		// maybe we are called before the seamarksfileRead is completed
		if (!mSeamarkOsm.getSeamarkFileReadComplete()) return;
		if (mSeamarksNodeList== null) return;  // the list may not be set by readpois()
		
		// we only update the own position
		if (this.mMyLocationChanged) return;
		// maybe we get a new update, but we process an older one
		if (mInUpdateSeamarkNodesOnOverlay) return;
		
		mInUpdateSeamarkNodesOnOverlay= true;
		mCountSeamarksUpdate++;
		int sleeptime = 200;
		//Log.d(TAG,"update SeamarkNodesOnOverlay");
		byte zoom = this.mMapView1.getMapPosition().getZoomLevel();
		if (mLastZoom != zoom ){  //&& (8 <= zoom  && zoom <=13 )) 
			int count = mSeamarksNodeList.size();
			//Log.d(TAG,"nodes: " + count);
			for (int index=0;index < count; index ++){
				if (mSeamarksNodeList!= null){
				  //Log.d(TAG,count + "remove:  "+ index);
			      SeamarkNode aSeamarkNode = mSeamarksNodeList.get(index);
			      removeSeamarkNodeFromOverlay(aSeamarkNode);
				}
			}
			mLastZoom = zoom;
			sleeptime = sleeptime * 2;
		}
		/*try {
		Thread.sleep(sleeptime);
		} catch (Exception e) {
			
		}*/
		Projection aProjection = this.mMapView1.getProjection();
		int aLatSpan = aProjection.getLatitudeSpan(); // bottom to top
		int aLonSpan = aProjection.getLongitudeSpan(); // left right
		GeoPoint geoPoint = this.mMapView1.getMapPosition().getMapCenter();
		int minlat = geoPoint.latitudeE6 - aLatSpan /2;
		int minlon = geoPoint.longitudeE6 - aLonSpan/2;
		int maxlat = geoPoint.latitudeE6 + aLatSpan /2;
		int maxlon = geoPoint.longitudeE6 + aLonSpan /2;
		BoundingBox aBoundingBox = new BoundingBox(minlat,minlon,maxlat,maxlon);
		if (mSeamarksNodeList != null) {
			int count = mSeamarksNodeList.size();
			//Log.d(TAG,"SeamarkNodeList count" + count);
			for (int index=0;index < count; index ++){
				if (mSeamarksNodeList != null){
				  SeamarkNode aSeamarkNode = mSeamarksNodeList.get(index);
				  if (aSeamarkNode != null) {
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
			}
		}
		mInUpdateSeamarkNodesOnOverlay= false;
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
	
	private void putSeamarkNodeOnOverlay (SeamarkNode aSeamarkNode) {
		if (!aSeamarkNode.getVisibility()){
			if (isOtherSeamark(aSeamarkNode)){
				if( !otherSeamarksInfoList.contains(aSeamarkNode)){
					otherSeamarksInfoList.add(aSeamarkNode);
				}
				
				if (showOtherSeamarks){
					  mSplitSeamarksWithAerialOverlay.addNode(aSeamarkNode);
					  aSeamarkNode.setVisibility(true);
					  mCountDisplayedSeamarks++;
					
				}
			}
			if (!isOtherSeamark(aSeamarkNode)){
				  mSplitSeamarksWithAerialOverlay.addNode(aSeamarkNode);
				  aSeamarkNode.setVisibility(true);
				  mCountDisplayedSeamarks++;
				  preparePaintSingleLightsIfNecessary(aSeamarkNode);
				  preparePaintNumberedLightsIfNecessary( aSeamarkNode);
				
			}
		}
		
		
		
		
	}
	
	
	private synchronized void removeSeamarkNodeFromOverlay (SeamarkNode aSeamarkNode){
		if (otherSeamarksInfoList.contains(aSeamarkNode)){
			otherSeamarksInfoList.remove(aSeamarkNode);
			//aSeamarkNode.setVisibility(false);
		}
		if (aSeamarkNode.getVisibility()) {
			mSplitSeamarksWithAerialOverlay.removeNode(aSeamarkNode);
			mNodeListNumberedFires.remove(aSeamarkNode);
			mNodeListSingleLights.remove(aSeamarkNode);
			mCountDisplayedSeamarks--;
			aSeamarkNode.setVisibility(false);
		} else {
			
		}
	    
	
	}
	
	
	
	private void preparePaintSingleLightsIfNecessary(SeamarkNode aSeamarkNode) {
		 String seamarkType = aSeamarkNode.getValueToKey("seamark:type");
		 if (seamarkType != null &&seamarkType.equals("landmark")) {
			   /*String seamarkStr = aSeamarkNode.getValueToKey("seamark") ;
			   if (seamarkStr != null && seamarkStr.equals("lighthouse")) {
				   String longname = aSeamarkNode.getValueToKey("seamark:longname");
			    	Log.d(TAG,"lighthouse found "+ longname);
					mNodeListMainLights.add(aSeamarkNode);	  
		       }*/
			   String lightColor = aSeamarkNode.getValueToKey("seamark:light:colour");
			   if (lightColor != null ) {
				   String longname = aSeamarkNode.getValueToKey("seamark:longname");
			    	Log.d(TAG,"landmark found "+ longname);
					mNodeListSingleLights.add(aSeamarkNode);	  
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
				     mNodeListNumberedFires.add(aSeamarkNode); 
			   } else {
				   String light1Color = aSeamarkNode.getValueToKey("seamark:light:1:colour");
				    if (light1Color != null ){
				    	//we have a other sector fire
				    	String longname = aSeamarkNode.getValueToKey("seamark:name");
				    	//Log.d(TAG,"Sector fire found "+ longname);
						mNodeListNumberedFires.add(aSeamarkNode);	
				    }
			   } // else
			    
		   } // if type != null
	     
   }
	
	private boolean isOtherSeamark(SeamarkNode aSeamarkNode){
		boolean result = false;
		String seamarkType = aSeamarkNode.getValueToKey("seamark:type");
		if ( seamarkType != null) {
			if ((seamarkType.contains("small_craft_facility")
					||(seamarkType.contains("mooring"))
					||(seamarkType.contains("harbour"))
					|| seamarkType.contains("bridge"))
					|| seamarkType.contains("cable")) {
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
		if(showOtherSeamarks){
			int count = otherSeamarksInfoList.size();
			for (int index = 0; index < count; index++){
				SeamarkNode seamarkNode = otherSeamarksInfoList.get(index);
				mSplitSeamarksWithAerialOverlay.removeNode(seamarkNode);
				seamarkNode.setVisibility(false);
				mCountDisplayedSeamarks--;
			}
			showOtherSeamarks = false;
		} else {
			int count = otherSeamarksInfoList.size();
			for (int index = 0; index < count; index++){
				SeamarkNode seamarkNode = otherSeamarksInfoList.get(index);
				mSplitSeamarksWithAerialOverlay.addNode(seamarkNode);
				seamarkNode.setVisibility(true);
				mCountDisplayedSeamarks++;
			}
			showOtherSeamarks = true;
		}
		mSplitSeamarksWithAerialOverlay.requestRedraw();
	}
	
	private void toggleShowSectorFires(){
		if(showSectorFires){
			
			showSectorFires = false;
		} else {
			
			showSectorFires = true;
		}
		mSplitSeamarksWithAerialOverlay.requestRedraw();
	}
	
	private void toggleShowLocation(){
		if(showMyLocation){
			
			disableShowMyLocation();
		} else {
			enableShowMyLocation(true);
		}
		mSplitSeamarksWithAerialOverlay.requestRedraw();
	}
	
	private void toggleShowRoute(){
		if(mShowRoute){
			mShowRoute = false;
		} else {
			mShowRoute = true;
		}
		mSplitSeamarksWithAerialOverlay .requestRedraw();
	}
	
	private void toggleShowMapInfo(){
		if(mShowMapInfo){
			mCenterPointHandler.removeCallbacks(centerPointRefresh);
			mTextViewCenterCoordinates.setText("");
			mTextViewCenterCoordinates.setVisibility(View.INVISIBLE);
			mTextViewCenterCoordinatedSavedHeight = mTextViewCenterCoordinates.getHeight();
			Log.d(TAG,"TextView height " + mTextViewCenterCoordinatedSavedHeight);
			mTextViewCenterCoordinates.setHeight(0);
			GeoPoint geoPoint = mMapView1.getMapPosition().getMapCenter();
			GeoPoint movedPoint = new GeoPoint(geoPoint.latitudeE6 + 100, geoPoint.longitudeE6);
			mMapView1.setCenter(movedPoint);
			mShowMapInfo = false;
			
		} else {
			mShowMapInfo = true;
			mTextViewCenterCoordinates.setVisibility(View.VISIBLE);
			mTextViewCenterCoordinates.setHeight(mTextViewCenterCoordinatedSavedHeight);
			mCenterPointHandler.postDelayed(centerPointRefresh, 1000); 
			
		}
		mSplitSeamarksWithAerialOverlay.requestRedraw();
		
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
			this.mySplitLocationListener.setCenterAtFirstFix(centerAtFirstFix);
			this.locationManager.requestLocationUpdates(bestProvider, 1000, 0, this.mySplitLocationListener);
			

		}
	}
	

	/**
	 * Disables the "show my location" mode.
	 */
	void disableShowMyLocation() {
		if (this.showMyLocation) {
			this.showMyLocation = false;
			disableSnapToLocation(false);
			this.locationManager.removeUpdates(this.mySplitLocationListener);
			
		}
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
		getMenuInflater().inflate(R.menu.options_menu__osm_viever, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		MenuItem aMenuItem = menu.findItem(R.id.menu_show_other_seamarks);
		if(showOtherSeamarks) {
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
				this.mMapView1.setRenderTheme(InternalRenderTheme.OSMARENDER);
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
			case R.id.menu_show_other_seamarks:
				toggleShowOtherSeamarks();
				return true;
			case R.id.menu_show_sector_fires:
				toggleShowSectorFires();
				return true;
			case R.id.menu_show_location:
				toggleShowLocation();
				return true;
			case R.id.menu_show_map_info:
				toggleShowMapInfo();
				return true;
			case R.id.menu_show_seamark_names:
				return true;
			case R.id.menu_show_no_names_info:
				mSplitSeamarksWithAerialOverlay.setShowNameStatus(seamarkNameNotVisible);
				return true;
			case R.id.menu_show_long_names_info:
				mSplitSeamarksWithAerialOverlay.setShowNameStatus(seamarkLongNameVisible);
				return true;
			case R.id.menu_show_short_names_info:
				mSplitSeamarksWithAerialOverlay.setShowNameStatus(seamarkShortNameVisible);
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
					this.mMapView1.setMapFile(selectedFile);
					mLastZoom = this.mMapView1.getMapPosition().getZoomLevel();
					String selectedFilePath = selectedFile.getAbsolutePath();
					this.setSeamarkFilePathAndRead(selectedFilePath);
					mReadPoisHandler.postDelayed(readPois, 1);  // it takes some time to read the seamarks file
			
				}
			} else if (resultCode == RESULT_CANCELED && !this.mMapView1.getMapGenerator().requiresInternetConnection()
					&& this.mMapView1.getMapFile() == null) {
				finish();
			}
		} else if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			try {
				String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
				mRenderThemeName = aFilename;
				File aFile = new File(aFilename);
				this.mMapView1.setRenderTheme(aFile);
			} catch (FileNotFoundException e) {
				showToastOnUiThread(e.getLocalizedMessage());
			}
		} else if (requestCode == SELECT_SEAMARKS_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			
				String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
				File selectedFile = new File(aFilename);
				this.setSeamarkFilePathAndRead(selectedFile.getAbsolutePath());
				mReadPoisHandler.postDelayed(readPois, 1000);  // it takes some time to read the seamarks file
				
			
		} else if (requestCode == SELECT_ROUTE_GML_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			
		     String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
		     File selectedFile = new File(aFilename);
		     loadGMLRouteData(selectedFile);
		     this.mShowRoute = true;
		     mSplitSeamarksWithAerialOverlay.requestRedraw();
        } else if (requestCode == SELECT_ROUTE_GPX_FILE && resultCode == RESULT_OK && intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			
		     String aFilename = intent.getStringExtra(FilePicker.SELECTED_FILE);
		     File selectedFile = new File(aFilename);
		     loadGPXRouteData(selectedFile);
		     this.mShowRoute = true;
		     mSplitSeamarksWithAerialOverlay.requestRedraw();
          } 
	}
	
	/**
	 * Uses the UI thread to display the given text message as toast notification.
	 * 
	 * @param text
	 *            the text message to display
	 */
	void showToastOnUiThread(final String text) {

		if (AndroidUtils.currentThreadIsUiThread()) {
			Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
			toast.show();
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast toast = Toast.makeText( SplitOpenSeaMapWithAerialOverlayViewer.this, text, Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	}
	
	public void requestOverlayRedraw(GeoPoint newPoint) {
		this.mMyLocationPoint = newPoint;
		this.mMyLocationChanged = true;
		mSplitSeamarksWithAerialOverlay.requestRedraw();
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
