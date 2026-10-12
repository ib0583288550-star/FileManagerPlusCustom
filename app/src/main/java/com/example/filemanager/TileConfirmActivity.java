package com.example.filemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import java.util.List;

public class TileConfirmActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        StorageManager sm=(StorageManager)getSystemService(STORAGE_SERVICE);
        List<StorageVolume> volumes=sm.getStorageVolumes();
        StorageVolume target=null;
        for(StorageVolume v:volumes) {
            if(v.isRemovable()) {
                target=v;
                if("mounted".equalsIgnoreCase(v.getState())) break;
            }
        }
        if(target==null) {
            new AlertDialog.Builder(this)
                .setTitle("מנהל אחסון")
                .setMessage("לא זוהה כרטיס חיצוני או OTG. חבר כרטיס ולחץ על רענון.")
                .setPositiveButton("אישור",(d,w)->finish())
                .setOnCancelListener(d->finish())
                .show();
            return;
        }
        boolean mounted="mounted".equalsIgnoreCase(target.getState());
        String action=mounted?"unmount":"mount";
        String message=mounted?"האם להוציא את הכרטיס?":"הכרטיס בחוץ. האם לטעון אותו?";
        new AlertDialog.Builder(this)
            .setTitle("מנהל אחסון")
            .setMessage(message)
            .setPositiveButton("כן",(d,w)->{
                Intent i=new Intent(this,MainActivity.class);
                i.putExtra("tile_action",action);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            })
            .setNegativeButton("לא",(d,w)->finish())
            .setOnCancelListener(d->finish())
            .show();
    }
}