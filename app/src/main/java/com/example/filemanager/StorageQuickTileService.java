package com.example.filemanager;

import android.app.AlertDialog;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class StorageQuickTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening();
        Tile tile=getQsTile();
        if(tile!=null){
            tile.setLabel("הוצאת כרטיס");
            tile.setIcon(Icon.createWithResource(this, R.drawable.qs_storage_icon));
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }

    @Override public void onClick() {
        super.onClick();
        boolean ask=getSharedPreferences("storage_settings",MODE_PRIVATE).getBoolean("tile_ask",true);
        StorageVolume target=findTarget();
        if(target==null){
            showDialog(new AlertDialog.Builder(this).setMessage("לא זוהה כרטיס חיצוני או OTG.").setPositiveButton("אישור",null).create());
            return;
        }
        String action="mounted".equalsIgnoreCase(target.getState())?"unmount":"mount";
        if(!ask){perform(target,action);return;}
        String message="unmount".equals(action)?"האם להוציא את הכרטיס?":"הכרטיס בחוץ. האם לטעון אותו?";
        showDialog(new AlertDialog.Builder(this).setMessage(message)
            .setPositiveButton("כן",(d,w)->perform(target,action))
            .setNegativeButton("לא",null).create());
    }

    private StorageVolume findTarget(){
        StorageManager sm=(StorageManager)getSystemService(STORAGE_SERVICE);
        List<StorageVolume> volumes=sm.getStorageVolumes();
        StorageVolume fallback=null;
        for(StorageVolume v:volumes){
            if(!v.isRemovable())continue;
            if(fallback==null)fallback=v;
            if("mounted".equalsIgnoreCase(v.getState()))return v;
        }
        return fallback;
    }

    private void perform(StorageVolume target,String action){
        new Thread(()->{
            try{
                String uuid=target.getUuid();
                String list=runRoot("sm list-volumes all");
                String id=null;
                if(uuid!=null&&!uuid.trim().isEmpty()){
                    for(String line:list.split("\\r?\\n")){
                        String[] parts=line.trim().split("\\s+");
                        for(String token:parts){
                            if(uuid.equalsIgnoreCase(token)||token.toLowerCase(Locale.US).contains(uuid.toLowerCase(Locale.US))){id=parts[0];break;}
                        }
                        if(id!=null)break;
                    }
                }else{
                    for(String line:list.split("\\r?\\n")){
                        String[] parts=line.trim().split("\\s+");
                        if(parts.length>=2&&parts[0].startsWith("public:")&&
                           (("unmount".equals(action)&&"mounted".equalsIgnoreCase(parts[1]))||
                            ("mount".equals(action)&&!"mounted".equalsIgnoreCase(parts[1])))){id=parts[0];break;}
                    }
                }
                if(id==null)throw new IOException("לא נמצא מזהה לכרטיס.");
                runRoot("sm "+action+" "+id);
            }catch(Exception ignored){
                // Keep the quick-settings tile action silent and do not launch the app.
            }
        }).start();
    }

    private String runRoot(String command)throws Exception{
        Process process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        InputStream input=process.getInputStream();
        byte[] buffer=new byte[1024];int count;
        while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
        int code=process.waitFor();String result=output.toString("UTF-8").trim();
        if(code!=0)throw new IOException("קוד "+code+" "+result);
        return result;
    }
}