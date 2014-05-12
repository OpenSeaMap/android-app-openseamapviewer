package org.openseamap.openseamapviewer;

import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.DatabaseRenderer;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.Tile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
/**
 *  Read  aerial tiles from eniro and combine them with the generated tiles from the database renderer 
 * @author vkADM
 *
 */
public class OverlayOpenSeaMapDatabaseRenderer extends DatabaseRenderer {
	private static final String TAG = "OverlayOpenSeaMapDatabaseRenderer";
	private static final float[] TILE_FRAME = new float[] { 0, 0, 0, Tile.TILE_SIZE, 0, Tile.TILE_SIZE, Tile.TILE_SIZE,
		Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, 0 };
	private static final float[] TILE_CENTER_CROSS = new float[] { Tile.TILE_SIZE / 2, 0, Tile.TILE_SIZE / 2, Tile.TILE_SIZE,
	      0, Tile.TILE_SIZE  /2, Tile.TILE_SIZE,Tile.TILE_SIZE / 2 };
	private Context mContext;
	private TileDownloader mOverlayTileDownLoader = null;
	private int mTransparency ;
	
	public OverlayOpenSeaMapDatabaseRenderer(Context context,int transparency, TileDownloader overlayTileDownLoader){
		super();
		mContext = context;
		mOverlayTileDownLoader =  overlayTileDownLoader;
		mTransparency = transparency;
	}
	
	public void setOverlayTransparency( int value){
		if ( value >= 0  && value <= 255) {
			mTransparency = value;
		}
	}
	public int getOverlayTransparency (){
		return mTransparency;
	}
	
	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		boolean result = false;
		boolean underlayResult = false;
		if (mOverlayTileDownLoader != null){
			   underlayResult = mOverlayTileDownLoader.executeJob(mapGeneratorJob, bitmap);
			if (underlayResult){
				Bitmap overlayBitmap = Bitmap.createBitmap(Tile.TILE_SIZE,Tile.TILE_SIZE,Bitmap.Config.ARGB_8888);
				result = super.executeJob(mapGeneratorJob, overlayBitmap);
				if (result) {
					Paint transparentpaint = new Paint();
					transparentpaint.setAlpha(mTransparency);
					Canvas comboImage = new Canvas(bitmap);
					comboImage.drawBitmap(overlayBitmap, 0f, 0f, transparentpaint);
					//Log.d(TAG,"combine Bitmaps success");
					
				}
				result = result && underlayResult;	
			}
		} else {
			result = super.executeJob(mapGeneratorJob, bitmap);
		}
		
		return result;
	}
}
