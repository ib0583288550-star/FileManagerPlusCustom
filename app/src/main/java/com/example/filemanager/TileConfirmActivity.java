package com.example.filemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class TileConfirmActivity extends Activity {
    private StorageVolume target;
    private String action;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        StorageManager sm=(StorageManager)getSystemService(STORAGE_SERVICE);
        List<StorageVolume> volumes=sm.getStorageVolumes();
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
        action=mounted?"unmount":"mount";
        String message=mounted?"האם להוציא את הכרטיס?":"הכרטיס בחוץ. האם לטעון אותו?";
        new AlertDialog.Builder(this)
            .setTitle("מנהל אחסון")
            .setMessage(message)
            .setPositiveButton("כן",(d,w)->performAction())
            .setNegativeButton("לא",(d,w)->finish())
            .setOnCancelListener(d->finish())
            .show();
    }

    private void performAction() {
        Toast.makeText(this, action.equals("unmount")?"מוציא את הכרטיס…":"טוען את הכרטיס…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            try {
                String uuid=target.getUuid();
                String list=runRoot("sm list-volumes all");
                String id=null;
                if(uuid!=null&&!uuid.trim().isEmpty()) {
                    for(String line:list.split("\\r?\\n")) {
                        String[] parts=line.trim().split("\\s+");
                        for(String token:parts) {
                            if(uuid.equalsIgnoreCase(token)||token.toLowerCase(Locale.US).contains(uuid.toLowerCase(Locale.US))) {
                                id=parts[0]; break;
                            }
                        }
                        if(id!=null) break;
                    }
                } else {
                    for(String line:list.split("\\r?\\n")) {
                        String[] parts=line.trim().split("\\s+");
                        if(parts.length>=2 && parts[0].startsWith("public:") &&
                           (("unmount".equals(action)&&"mounted".equalsIgnoreCase(parts[1])) ||
                            ("mount".equals(action)&&!"mounted".equalsIgnoreCase(parts[1])))) {
                            id=parts[0]; break;
                        }
                    }
                }
                if(id==null) throw new IOException("לא נמצא מזהה לכרטיס.\n"+list);
                String output=runRoot("sm "+action+" "+id);
                runOnUiThread(()->{
                    Toast.makeText(this,action.equals("unmount")?"בקשת ההוצאה נשלחה":"בקשת הטעינה נשלחה",Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch(Exception e) {
                runOnUiThread(()->new AlertDialog.Builder(this)
                    .setTitle("הפעולה נכשלה")
                    .setMessage(String.valueOf(e.getMessage()))
                    .setPositiveButton("אישור",(d,w)->finish())
                    .setOnCancelListener(d->finish())
                    .show());
            }
        }).start();
    }

    private String runRoot(String command) throws Exception {
        Process process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        InputStream input=process.getInputStream();
        byte[] buffer=new byte[1024];
        int count;
        while((count=input.read(buffer))!=-1) output.write(buffer,0,count);
        int code=process.waitFor();
        String result=output.toString("UTF-8").trim();
        if(code!=0) throw new IOException("קוד "+code+"\n"+(result.isEmpty()?"ללא פלט":result));
        return result;
    }
}