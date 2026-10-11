package com.example.filemanager;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class StorageQuickTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening();
        Tile tile=getQsTile();
        if(tile!=null){ tile.setLabel("מנהל אחסון"); tile.setState(Tile.STATE_ACTIVE); tile.updateTile(); }
    }
    @Override public void onClick() {
        super.onClick();
        boolean ask=getSharedPreferences("storage_settings",MODE_PRIVATE).getBoolean("tile_ask",true);
        Intent i=new Intent(this, ask ? TileConfirmActivity.class : MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityAndCollapse(i);
    }
}
