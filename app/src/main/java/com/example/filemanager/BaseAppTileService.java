package com.example.filemanager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

abstract class BaseAppTileService extends TileService {
    protected abstract int slot();
    @Override public void onStartListening() {
        super.onStartListening();
        Tile tile=getQsTile();
        if(tile==null)return;
        String key="app_tile_"+slot();
        String label=getSharedPreferences("app_tiles",MODE_PRIVATE).getString(key+"_label","");
        tile.setLabel(label.isEmpty()?"בחר אפליקציה "+slot():label);
        tile.setIcon(Icon.createWithResource(this,R.drawable.qs_storage_icon));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }
    @Override public void onClick() {
        super.onClick();
        String pkg=getSharedPreferences("app_tiles",MODE_PRIVATE).getString("app_tile_"+slot(),"");
        if(pkg.isEmpty()){
            Toast.makeText(this,"פתח את מנהל האחסון ובחר אפליקציה לאריח "+slot(),Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null)throw new IllegalStateException("לא נמצאה אפשרות לפתוח את האפליקציה");
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(launch);
        } catch(Exception e) {
            Toast.makeText(this,"לא ניתן לפתוח את האפליקציה שנבחרה",Toast.LENGTH_LONG).show();
        }
    }
}