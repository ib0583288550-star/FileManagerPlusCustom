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
        String action=getIntent().getStringExtra("tile_action");
        if(action!=null) {
            StorageVolume v=findVolumeForAction(action);
            if(v!=null) rootCmd(action,v);
            else new AlertDialog.Builder(this).setTitle("לא נמצא כרטיס")
                .setMessage("לא זוהה כרטיס חיצוני זמין לפעולה. חבר את הכרטיס ולחץ על רענון.")
                .setPositiveButton("אישור",null).show();
            getIntent().removeExtra("tile_action");
        }
    }
    void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(bg);

        TextView title=label("מנהל אחסון",21,dark);
        title.setTypeface(null,1);
        LinearLayout header=new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView settings=label("⚙",22,dark);
        settings.setOnClickListener(v->about());
        header.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(62)));
        TextView refresh=label("↻",27,dark);
        refresh.setOnClickListener(v->{render(); Toast.makeText(this,"זיהוי האחסון רוענן",Toast.LENGTH_SHORT).show();});
        header.addView(refresh,new LinearLayout.LayoutParams(dp(48),dp(62)));
        header.addView(title,new LinearLayout.LayoutParams(0,dp(62),1));
        root.addView(header);

        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(14),dp(8),dp(14),dp(18));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(root);
        render();
    }

    void about(){
        new AlertDialog.Builder(this).setTitle("מנהל אחסון")
            .setMessage("זיהוי אחסון חיצוני ו־OTG, רענון הרשימה וטעינה או הוצאה בטוחה.\n\ny.b apps")
            .setPositiveButton("אישור",null).show();
    }

    void render(){
        body.removeAllViews();
        ArrayList<StorageVolume> removable=new ArrayList<>();
        for(StorageVolume v:vols()) if(v.isRemovable()) removable.add(v);

        if(removable.isEmpty()){
            TextView empty=label("לא זוהה אחסון חיצוני או OTG\nחבר כרטיס ולחץ על רענון",15,gray);
            empty.setBackground(shape(card,18));
            body.addView(empty,new LinearLayout.LayoutParams(-1,dp(92)));
            return;
        }

        for(StorageVolume v:removable){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(104));
            rp.bottomMargin=dp(12);

            LinearLayout info=new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setGravity(Gravity.CENTER_VERTICAL);
            info.setPadding(dp(8),dp(8),dp(8),dp(8));
            info.setBackground(shape(card,18));
            info.setElevation(dp(1));
            TextView name=label(volumeName(v),14,dark);
            name.setTypeface(null,1);
            name.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            TextView state=label(stateText(v),12,stateColor(v));
            state.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            info.addView(name,new LinearLayout.LayoutParams(-1,dp(36)));
            info.addView(state,new LinearLayout.LayoutParams(-1,dp(28)));
            row.addView(info,new LinearLayout.LayoutParams(0,dp(88),1));

            LinearLayout controls=new LinearLayout(this);
            controls.setOrientation(LinearLayout.VERTICAL);
            controls.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(78),dp(92));
            cp.leftMargin=dp(8);

            LinearLayout mount=smallButton("↻","טעינה",blue,Color.WHITE);
            mount.setOnClickListener(x->confirmAction(v,"mount"));
            LinearLayout eject=smallButton("⏏","הוצאה",Color.rgb(28,38,52),dark);
            eject.setOnClickListener(x->confirmAction(v,"unmount"));
            controls.addView(mount,new LinearLayout.LayoutParams(-1,dp(42)));
            Space gap=new Space(this);
            controls.addView(gap,new LinearLayout.LayoutParams(1,dp(6)));
            controls.addView(eject,new LinearLayout.LayoutParams(-1,dp(42)));
            row.addView(controls,cp);

            body.addView(row,rp);
        }
    }

    void confirmAction(StorageVolume v,String action){
        boolean mounted="mounted".equalsIgnoreCase(v.getState());
        String actual=action;
        String message;
        if("unmount".equals(action)){
            message="האם להוציא את "+volumeName(v)+"?";
        }else{
            message="האם לטעון את "+volumeName(v)+"?";
        }
        new AlertDialog.Builder(this).setTitle("אישור פעולה").setMessage(message)
            .setPositiveButton("כן",(d,w)->rootCmd(actual,v))
            .setNegativeButton("לא",null).show();
    }

    LinearLayout smallButton(String icon,String text,int color,int textColor){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(shape(color,14));
        TextView i=label(icon,18,textColor);
        TextView t=label(text,11,textColor);
        t.setTypeface(null,1);
        box.addView(i,new LinearLayout.LayoutParams(dp(26),-1));
        box.addView(t,new LinearLayout.LayoutParams(0,-1,1));
        box.setClickable(true);
        box.setFocusable(true);
        return box;
    }

    ArrayList<StorageVolume> vols(){
        return new ArrayList<>(((StorageManager)getSystemService(STORAGE_SERVICE)).getStorageVolumes());
    }

    StorageVolume findVolumeForAction(String action){
        ArrayList<StorageVolume> removable=new ArrayList<>();
        for(StorageVolume v:vols()) if(v.isRemovable()) removable.add(v);
        if(removable.isEmpty()) return null;
        if("unmount".equals(action)){
            for(StorageVolume v:removable) if("mounted".equalsIgnoreCase(v.getState())) return v;
        }else{
            for(StorageVolume v:removable) if(!"mounted".equalsIgnoreCase(v.getState())) return v;
        }
        return removable.get(0);
    }

    String stateText(StorageVolume v){
        String s=v.getState()==null?"":v.getState().toLowerCase(Locale.ROOT);
        if("mounted".equals(s)) return "● מחובר";
        if("unmounted".equals(s) || "ejected".equals(s) || "removed".equals(s)) return "○ בחוץ";
        if("checking".equals(s)) return "בודק כרטיס…";
        if("mounted_ro".equals(s)) return "מחובר • קריאה בלבד";
        return s.isEmpty()?"המצב לא ידוע":"מצב: "+s;
    }
    int stateColor(StorageVolume v){
        return "mounted".equalsIgnoreCase(v.getState())?Color.rgb(80,210,145):gray;
    }

    String volumeName(StorageVolume v){
        String desc="";
        try { desc=v.getDescription(this); } catch(Exception ignored) {}
        String d=desc==null?"":desc.toLowerCase(Locale.ROOT);
        if(d.contains("usb") || d.contains("otg")) return "אחסון OTG / USB";
        if(d.contains("sd")) return "אחסון חיצוני • כרטיס SD";
        if(v.getUuid()!=null) return "אחסון חיצוני • כרטיס SD";
        return "אחסון OTG / USB";
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
                } else {
                    for(String line:list.split("\\r?\\n")){
                        String[] z=line.trim().split("\\s+");
                        if(z.length>=2 && z[0].startsWith("public:") &&
                           (("unmount".equals(a)&&"mounted".equalsIgnoreCase(z[1])) ||
                            ("mount".equals(a)&&!"mounted".equalsIgnoreCase(z[1])))){
                            id=z[0]; break;
                        }
                    }
                }
                if(id==null)throw new IOException("לא נמצא מזהה לכונן.\n"+list);
                String out=runRoot("sm "+a+" "+id);
                runOnUiThread(()->{
                    new AlertDialog.Builder(this)
                        .setTitle(a.equals("unmount")?"הוצאה בטוחה":"טעינה מחדש")
                        .setMessage("הפעולה הסתיימה"+(out.isEmpty()?"":"\n\n"+out))
                        .setPositiveButton("אישור",null).show();
                    render();
                });
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