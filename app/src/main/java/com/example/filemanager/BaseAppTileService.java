package com.example.filemanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

abstract class BaseAppTileService extends TileService {
    protected abstract int slot();

    @Override public void onStartListening() {
        super.onStartListening();
        updateAppTile();
    }

    @Override public void onTileAdded() {
        super.onTileAdded();
        updateAppTile();
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

    private void updateAppTile(){
        Tile tile=getQsTile();
        if(tile==null)return;
        String key="app_tile_"+slot();
        String pkg=getSharedPreferences("app_tiles",MODE_PRIVATE).getString(key,"");
        String label=getSharedPreferences("app_tiles",MODE_PRIVATE).getString(key+"_label","");
        tile.setLabel(label.isEmpty()?"בחר אפליקציה "+slot():label);
        try {
            Drawable appIcon=pkg.isEmpty()?getDrawable(android.R.drawable.sym_def_app_icon):getPackageManager().getApplicationIcon(pkg);
            if(appIcon==null)throw new IllegalStateException("missing icon");
            int size=(int)(48*getResources().getDisplayMetrics().density);
            int iconSize=(int)(36*getResources().getDisplayMetrics().density);
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);
            int inset=(size-iconSize)/2;
            appIcon.setBounds(inset,inset,inset+iconSize,inset+iconSize);
            appIcon.draw(canvas);
            tile.setIcon(Icon.createWithBitmap(bitmap));
        } catch(Exception e) {
            tile.setIcon(Icon.createWithResource(this,android.R.drawable.sym_def_app_icon));
        }
        tile.setState(Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
