package com.example.filemanager;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.storage.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout body;
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    int bg=Color.rgb(10,15,23), dark=Color.rgb(238,243,250), blue=Color.rgb(30,110,220), gray=Color.rgb(145,158,176), card=Color.rgb(18,26,38);

    GradientDrawable shape(int color,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    TextView label(String s,float size,int color){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.CENTER); return v;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        build();
    }
    void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(bg);

        TextView title=label("כונן חכם",21,dark);
        title.setTypeface(null,1);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(82)));

        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(18),dp(6),dp(18),dp(18));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(root);
        render();
    }

    void render(){
        body.removeAllViews();
        ArrayList<StorageVolume> removable=new ArrayList<>();
        for(StorageVolume v:vols()) if(v.isRemovable()) removable.add(v);

        TextView sub=label(removable.isEmpty()?"לא נמצא כונן נשלף":"כוננים נשלפים",15,gray);
        body.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));

        for(StorageVolume v:removable){
            TextView state=label(volumeName(v)+"\n"+v.getState(),14,dark);
            state.setTypeface(null,1);
            state.setBackground(shape(card,20));
            state.setElevation(dp(2));
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(70));
            sp.bottomMargin=dp(12);
            body.addView(state,sp);

            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            row.setOrientation(LinearLayout.HORIZONTAL);

            LinearLayout eject=roundButton("⏏","הוצאה",Color.rgb(28,38,52),dark);
            eject.setOnClickListener(x->rootCmd("unmount",v));
            LinearLayout mount=roundButton("↻","טעינה",blue,Color.WHITE);
            mount.setOnClickListener(x->rootCmd("mount",v));

            row.addView(eject,new LinearLayout.LayoutParams(dp(104),dp(104)));
            Space gap=new Space(this);
            row.addView(gap,new LinearLayout.LayoutParams(dp(14),1));
            row.addView(mount,new LinearLayout.LayoutParams(dp(104),dp(104)));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(104));
            rp.bottomMargin=dp(20);
            body.addView(row,rp);
        }
    }

    LinearLayout roundButton(String icon,String text,int color,int textColor){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(shape(color,28));
        box.setElevation(dp(3));

        TextView i=label(icon,29,textColor);
        i.setGravity(Gravity.CENTER);
        box.addView(i,new LinearLayout.LayoutParams(-1,dp(46)));

        TextView t=label(text,13,textColor);
        t.setTypeface(null,1);
        box.addView(t,new LinearLayout.LayoutParams(-1,dp(28)));

        box.setClickable(true);
        box.setFocusable(true);
        return box;
    }

    ArrayList<StorageVolume> vols(){
        return new ArrayList<>(((StorageManager)getSystemService(STORAGE_SERVICE)).getStorageVolumes());
    }


    String volumeName(StorageVolume v){
        String uuid=v.getUuid();
        if(uuid!=null && !uuid.trim().isEmpty()) return "אחסון נשלף • "+uuid;
        return "כונן USB / OTG";
    }

    void rootCmd(String a,StorageVolume v){
        new Thread(()->{
            try{
                String uuid=v.getUuid();
                String list=runRoot("sm list-volumes all");
                String id=null;
                if(uuid!=null&&!uuid.trim().isEmpty()){
                    for(String line:list.split("\\r?\\n")){
                        String[] z=line.trim().split("\\s+");
                        for(String token:z){
                            if(uuid.equalsIgnoreCase(token)||token.toLowerCase(Locale.US).contains(uuid.toLowerCase(Locale.US))){
                                id=z[0]; break;
                            }
                        }
                        if(id!=null)break;
                    }
                }
                if(id==null)throw new IOException("לא נמצא מזהה לכונן.\n"+list);
                String cmd="sm "+a+" "+id;
                String out=runRoot(cmd);
                runOnUiThread(()->new AlertDialog.Builder(this)
                    .setTitle(a.equals("unmount")?"הוצאה בטוחה":"טעינה מחדש")
                    .setMessage("בוצע בהצלחה"+(out.isEmpty()?"":"\n\n"+out))
                    .setPositiveButton("אישור",null).show());
            }catch(Exception e){
                runOnUiThread(()->new AlertDialog.Builder(this)
                    .setTitle("הפעולה נכשלה")
                    .setMessage(String.valueOf(e.getMessage()))
                    .setPositiveButton("אישור",null).show());
            }
        }).start();
    }

    String runRoot(String cmd)throws Exception{
        java.lang.Process q=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        InputStream is=q.getInputStream(); byte[] buf=new byte[1024]; int n;
        while((n=is.read(buf))!=-1)b.write(buf,0,n);
        int code=q.waitFor(); String out=b.toString("UTF-8").trim();
        if(code!=0)throw new IOException("קוד "+code+"\n"+(out.isEmpty()?"ללא פלט":out));
        return out;
    }
}