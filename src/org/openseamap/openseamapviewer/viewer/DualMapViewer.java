/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openseamap.openseamapviewer.viewer;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapScaleBar;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.core.GeoPoint;
import org.openseamap.openseamapviewer.downloader.EniroAerialAndSeamarksTileDownloader;
import org.openseamap.openseamapviewer.downloader.EniroNauticalTileDownloader;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * An application which demonstrates how to use two MapView instances at the same time.
 */
public class DualMapViewer extends MapActivity {
	
	private MapView mapView1;
	private MapController mapController1;
	private GeoPoint mMapCenter1;
	private byte mZoomLevel1 ;
	private MapView mapView2;
	private MapController mapController2;
	private GeoPoint mMapCenter2;
	private byte mZoomLevel2;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		// forward the event to both MapViews for simultaneous movement
		return this.mapView1.onKeyDown(keyCode, event) | this.mapView2.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return this.mapView1.onKeyUp(keyCode, event) | this.mapView2.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return this.mapView1.onTrackballEvent(event) | this.mapView2.onTrackballEvent(event);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// forward the event to both MapViews for simultaneous movement 
		// does not work:  mapview does not propagate the event to the activity
		// we use syncronizeDualView with a runnable
		return this.mapView1.onTouchEvent(event) | this.mapView2.onTouchEvent(event);
	}
	
	
private void syncronizeDualViews() {
	GeoPoint mapCenter1 = this.mapView1.getMapPosition().getMapCenter();
	GeoPoint mapCenter2 = this.mapView2.getMapPosition().getMapCenter();
	byte zoomLevel1 = this.mapView1.getMapPosition().getZoomLevel();
	byte zoomLevel2 = this.mapView2.getMapPosition().getZoomLevel();
	// figure out which view has moved
	if (this.mMapCenter1.compareTo(mapCenter1)!= 0){
		// mapView1 moved
		this.mMapCenter1 = mapCenter1;
		this.mMapCenter2 = mapCenter1;
		this.mapView2.setCenter(mapCenter1);
	} else  if (this.mMapCenter2.compareTo(mapCenter2)!= 0){
		// mapView2 moved
		this.mMapCenter1 = mapCenter2;
		this.mMapCenter2 = mapCenter2;
		this.mapView1.setCenter(mapCenter2);
	}
	// figure out if zoom changed
	if (this.mZoomLevel1!= zoomLevel1) {
		this.mZoomLevel1 = zoomLevel1;
		this.mZoomLevel2 = zoomLevel1;
		this.mapView2.getController().setZoom(zoomLevel1);
	} else if (this.mZoomLevel2!= zoomLevel2){
		this.mZoomLevel1 = zoomLevel2;
		this.mZoomLevel2 = zoomLevel2;
		this.mapView1.getController().setZoom(zoomLevel2);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Context myContext = getApplicationContext();
		this.mapView1 = new MapView(this);
		this.mapView1.setClickable(true);
		this.mapView1.setBuiltInZoomControls(true);
		EniroAerialAndSeamarksTileDownloader aEniroAerialDownLoader = new EniroAerialAndSeamarksTileDownloader(myContext);
		MapGenerator mapGeneratorLeft = aEniroAerialDownLoader;
		this.mapView1.setMapGenerator(mapGeneratorLeft);
		this.mapView1.getMapMover().setMoveSpeedFactor(1);
		this.mapController1 = this.mapView1.getController();
		MapScaleBar mapScaleBar1 = this.mapView1.getMapScaleBar();
		mapScaleBar1.setImperialUnits(false);
		mapScaleBar1.setShowMapScaleBar(true);

		this.mapView2 = new MapView(this);
		this.mapView2.setClickable(true);
		this.mapView2.setBuiltInZoomControls(true);
		EniroNauticalTileDownloader aEniroNauticalDownloader = new EniroNauticalTileDownloader(myContext);
		MapGenerator mapGeneratorRight = aEniroNauticalDownloader;
		this.mapView2.setMapGenerator(mapGeneratorRight);
		this.mapView2.getMapMover().setMoveSpeedFactor(1);
		this.mapController2 = this.mapView2.getController();
		MapScaleBar mapScaleBar2 = this.mapView2.getMapScaleBar();
		mapScaleBar2.setImperialUnits(false);
		mapScaleBar2.setShowMapScaleBar(true);

		// create a LineaLayout that contains both MapViews
		LinearLayout linearLayout = new LinearLayout(this);

		// if the device orientation is portrait, change the orientation to vertical
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			linearLayout.setOrientation(LinearLayout.VERTICAL);
		}

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		this.mapView1.setLayoutParams(layoutParams);
		this.mapView2.setLayoutParams(layoutParams);

		// add both MapViews to the LinearLayout
		linearLayout.addView(this.mapView1);
		linearLayout.addView(this.mapView2);
		setContentView(linearLayout);
		mMapCenter1 = this.mapView1.getMapPosition().getMapCenter();
		mMapCenter2 = this.mapView2.getMapPosition().getMapCenter();
		mZoomLevel1 = this.mapView1.getMapPosition().getZoomLevel();
		mZoomLevel2 = this.mapView2.getMapPosition().getZoomLevel();
		mSyncronizeViewRefreshhandler.postDelayed(syncronizeViewsRefresh, 1000);
	}
}
