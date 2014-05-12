package org.openseamap.openseamapviewer.viewer;

import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapScaleBar.TextField;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.MainActivity;
import org.openseamap.openseamapviewer.R;
import org.openseamap.openseamapviewer.R.id;
import org.openseamap.openseamapviewer.R.layout;
import org.openseamap.openseamapviewer.R.string;
import org.openseamap.openseamapviewer.downloader.EniroAerialTileDownloader;
import org.openseamap.openseamapviewer.downloader.EniroNauticalTileDownloader;
import org.openseamap.openseamapviewer.downloader.OpenSeamapTileAndSeamarksDownloader;
import org.openseamap.openseamapviewer.overlay.CenterCircleOverlay;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.widget.LinearLayout;
import android.widget.TextView;


public class SplitScreenViewer extends MapActivity {
	
	private static final String TAG = "SplitScreenViewer";
	private NotificationManager mNM;
	
	private MapView mMapViewLeft;
	private MapView mMapViewRight;
	private MapController mMapControllerLeft;
	private MapController mMapControllerRight;
	
	private byte mZoomLevelLeft;
	private byte mZoomLevelRight;
	private GeoPoint mMapCenterLeft;
	private GeoPoint mMapCenterRight;
	
	
	private TextView mTextViewCenterCoordinates;
	private CenterCircleOverlay mCircleOverlayLeft = null;
	private CenterCircleOverlay mCircleOverlayRight = null;
	private static Handler mCenterPointHandler = new Handler();
	int mTextViewCenterCoordinatedSavedHeight;
	private boolean showMapInfo;
	
	SharedPreferences prefs;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		Context myContext = getApplicationContext();
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		//EniroAerialTileDownloader aEniroAerialDownLoader = new EniroAerialTileDownloader(myContext);
		
		setContentView(R.layout.activity_render_openseamap_split_map_viewer);
		this.mTextViewCenterCoordinates = (TextView) findViewById(R.id.OSM_Info_textview);
		
		// left side
		this.mMapViewLeft = (MapView) findViewById(R.id.mapViewLeft);
		MapGenerator mapGeneratorLeft = null;
		Bundle extras = getIntent().getExtras();
		String downloadTypeLeft = extras.getString(MainActivity.VIEW_LEFT_KEY);
		if (downloadTypeLeft.equals(MainActivity.VIEW_MAPTYPE_ENIRO_AERIAL)) {
			mapGeneratorLeft= new EniroAerialTileDownloader(myContext);
		} else if (downloadTypeLeft.equals(MainActivity.VIEW_MAPTYPE_ENIRO_NAUTICAL)){
			mapGeneratorLeft= new EniroNauticalTileDownloader(myContext);
		} else if (downloadTypeLeft.equals(MainActivity.VIEW_MAPTYPE_OPENSEAMAP)){
			mapGeneratorLeft= new OpenSeamapTileAndSeamarksDownloader(myContext);
		}
		
		this.mMapViewLeft.setMapGenerator(mapGeneratorLeft);
		this.mMapControllerLeft = this.mMapViewLeft.getController();
		configureMapView(this.mMapViewLeft);
		mCircleOverlayLeft = new CenterCircleOverlay(this);
		mCircleOverlayLeft.setMustShowCenter(true);
		mMapViewLeft.getOverlays().add(mCircleOverlayLeft);
		//right side
		
		this.mMapViewRight = (MapView) findViewById(R.id.mapViewRight);
		MapGenerator mapGeneratorRight = null;
		String downloadTypeRight = extras.getString(MainActivity.VIEW_RIGHT_KEY);
		if (downloadTypeRight.equals(MainActivity.VIEW_MAPTYPE_ENIRO_AERIAL)) {
			mapGeneratorRight= new EniroAerialTileDownloader(myContext);
		} else if (downloadTypeRight.equals(MainActivity.VIEW_MAPTYPE_ENIRO_NAUTICAL)){
			mapGeneratorRight= new EniroNauticalTileDownloader(myContext);
		} else if (downloadTypeRight.equals(MainActivity.VIEW_MAPTYPE_OPENSEAMAP)){
			mapGeneratorRight= new OpenSeamapTileAndSeamarksDownloader(myContext);
		}
	
		this.mMapViewRight.setMapGenerator(mapGeneratorRight);
		this.mMapControllerRight = this.mMapViewRight.getController();
		configureMapView(this.mMapViewRight);
		mCircleOverlayRight = new CenterCircleOverlay(this);
		mCircleOverlayRight.setMustShowCenter(true);
		mMapViewRight.getOverlays().add(mCircleOverlayRight);
		
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.mapViewMain);
		// if the device orientation is portrait, change the orientation to vertical
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			linearLayout.setOrientation(LinearLayout.VERTICAL);
		}
		mMapCenterLeft = this.mMapViewLeft.getMapPosition().getMapCenter();
		mMapCenterRight = this.mMapViewRight.getMapPosition().getMapCenter();
		mZoomLevelLeft = this.mMapViewLeft.getMapPosition().getZoomLevel();
		mZoomLevelRight = this.mMapViewRight.getMapPosition().getZoomLevel();
		mSyncronizeViewRefreshhandler.postDelayed(syncronizeViewsRefresh, 1000);
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
		
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		MapScaleBar mapScaleBar = this.mMapViewLeft.getMapScaleBar();
		mapScaleBar.setShowMapScaleBar(true);
		int lat = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LAT, 90);
		int lon = prefs.getInt(MainActivity.PREV_LAST_GEOPOINT_LON,180);
		int aZoomLevel = prefs.getInt(MainActivity.PREF_ZOOM_FACTOR,-1);
		if (lat != 90 && lon != 180 && aZoomLevel > 0) {
			// we got valid values
			this.mMapViewLeft.getController().setZoom(aZoomLevel);
			this.mMapViewRight.getController().setZoom(aZoomLevel);
			GeoPoint geoPoint = new GeoPoint(lat,lon);
	 		this.mMapViewLeft.getController().setCenter(geoPoint);
	 		this.mMapViewRight.getController().setCenter(geoPoint);
		}
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onPause() {
		
		GeoPoint aGp = mMapViewLeft.getMapPosition().getMapCenter();
     	int aZoomLevel =mMapViewLeft.getMapPosition().getZoomLevel();
     	int aLAT = (int) aGp.latitudeE6; // we keep the prefs in Micrograd
	    int aLON = (int) aGp.longitudeE6;
	    prefs.edit()
	     	.putInt(MainActivity.PREV_LAST_GEOPOINT_LAT, aLAT)
	     	.putInt(MainActivity.PREV_LAST_GEOPOINT_LON, aLON)
	     	.putInt(MainActivity.PREF_ZOOM_FACTOR,aZoomLevel)
	     	.commit();
		super.onPause();
	}
	
	
	private void syncronizeDualViews() {
		GeoPoint  mapCenterLeft= this.mMapViewLeft.getMapPosition().getMapCenter();
		GeoPoint mapCenterRight = this.mMapViewRight.getMapPosition().getMapCenter();
		byte zoomLevelLeft = this.mMapViewLeft.getMapPosition().getZoomLevel();
		byte zoomLevelRight = this.mMapViewRight.getMapPosition().getZoomLevel();
		// figure out which view has moved
		if (this.mMapCenterLeft.compareTo(mapCenterLeft)!= 0){
			// mapView1 moved
			this.mMapCenterLeft = mapCenterLeft;
			this.mMapCenterRight= mapCenterLeft;
			this.mMapViewRight.getController().setCenter(mapCenterLeft);
		} else  if (this.mMapCenterRight.compareTo(mapCenterRight)!= 0){
			// mapView2 moved
			this.mMapCenterLeft = mapCenterRight;
			this.mMapCenterRight = mapCenterRight;
			this.mMapViewLeft.getController().setCenter(mapCenterRight);
		}
		// figure out if zoom changed
		if (this.mZoomLevelLeft!= zoomLevelLeft) {
			this.mZoomLevelLeft = zoomLevelLeft;
			this.mZoomLevelRight = zoomLevelLeft;
			this.mMapViewRight.getController().setZoom(zoomLevelLeft);
		} else if (this.mZoomLevelRight!= zoomLevelRight){
			this.mZoomLevelLeft = zoomLevelRight;
			this.mZoomLevelRight = zoomLevelRight;
			this.mMapViewLeft.getController().setZoom(zoomLevelRight);
		}
	}
		
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
}
