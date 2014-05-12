package org.openseamap.openseamapviewer.seamarks;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

import android.graphics.drawable.Drawable;

public class SeamarkWithPoisOverlayItem extends OverlayItem {
	 private SeamarkNode mSeamarkNode;
	 public SeamarkWithPoisOverlayItem (GeoPoint point, String title, String snippet,
				Drawable marker , SeamarkNode seamarkNode) {
			super(point,title,snippet, marker);
			mSeamarkNode = seamarkNode;
	 }
}
