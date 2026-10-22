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
    int bg=Color.rgb(8,13,25), dark=Color.rgb(242,246,255), muted=Color.rgb(151,166,190);
    int blue=Color.rgb(76,112,255), cyan=Color.rgb(43,211,205), card=Color.rgb(19,29,49);
    int green=Color.rgb(66,220,156), red=Color.rgb(255,111,132);

    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable shape(int color,int radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    GradientDrawable gradient(int first,int second,int radius){
        GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{first,second});
        g.setCornerRadius(dp(radius)); return g;
    }
    TextView text(String s,float size,int color){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    TextView centered(String s,float size,int color){
        TextView v=text(s,size,color); v.setGravity(Gravity.CENTER); return v;
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        build();
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},4102);
        }
        String action=getIntent().getStringExtra("tile_action");
        if(action!=null){
            StorageVolume v=findVolumeForAction(action);
            if(v!=null) rootCmd(action,v);
            else new AlertDialog.Builder(this).setTitle("לא נמצא כרטיס")
                .setMessage("לא זוהה כרטיס חיצוני זמין. חבר את הכרטיס ולחץ על רענון.")
                .setPositiveButton("אישור",null).show();
            getIntent().removeExtra("tile_action");
        }
    }

    void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18),dp(4),dp(12),dp(4));
        TextView mark=centered("▦",25,Color.WHITE); mark.setBackground(gradient(blue,Color.rgb(126,82,255),16));
        header.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout titleBox=new LinearLayout(this); titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12),0,0,0);
        TextView title=text("מנהל אחסון",21,dark); title.setTypeface(null,1);
        TextView subtitle=text("הכרטיסים והכוננים שלך",12,muted);
        titleBox.addView(title); titleBox.addView(subtitle);
        header.addView(titleBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=centered("⋮",27,dark); menu.setOnClickListener(v->showMenu(menu));
        header.addView(menu,new LinearLayout.LayoutParams(dp(42),dp(48)));
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(66)));

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16),dp(8),dp(16),dp(20));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView credit=centered("y.b apps  •  מנהל אחסון",11,muted);
        root.addView(credit,new LinearLayout.LayoutParams(-1,dp(32)));
        setContentView(root); render();
    }

    void showMenu(View anchor){
        PopupMenu popup=new PopupMenu(this,anchor);
        popup.getMenu().add("רענון כוננים").setOnMenuItemClickListener(i->{render();return true;});
        popup.getMenu().add("איך זה עובד?").setOnMenuItemClickListener(i->{
            new AlertDialog.Builder(this).setTitle("מנהל אחסון")
                .setMessage("האפליקציה מציגה אחסון חיצוני ו-OTG בלבד.\n\nטעינה והוצאה בטוחות עשויות לדרוש הרשאת Root. לפני הוצאה, עצור העתקת קבצים וסגור קבצים פתוחים.")
                .setPositiveButton("הבנתי",null).show(); return true;
        });
        popup.getMenu().add("אודות").setOnMenuItemClickListener(i->{
            new AlertDialog.Builder(this).setTitle("מנהל אחסון 1.24")
                .setMessage("כלים מהירים לניהול כרטיסי SD וחיבורי OTG.\n\ny.b apps")
                .setPositiveButton("סגירה",null).show(); return true;
        });
        popup.show();
    }

    void render(){
        body.removeAllViews();
        ArrayList<StorageVolume> removable=new ArrayList<>();
        for(StorageVolume v:vols()) if(v.isRemovable()) removable.add(v);

        LinearLayout hero=new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20),dp(18),dp(20),dp(18)); hero.setBackground(gradient(Color.rgb(35,57,120),Color.rgb(70,49,139),24));
        TextView eyebrow=text("ניהול חכם ופשוט",12,Color.rgb(205,218,255));
        TextView heading=text("האחסון שלך,\nבשליטה שלך",25,Color.WHITE); heading.setTypeface(null,1);
        heading.setPadding(0,dp(8),0,dp(12));
        TextView count=centered(removable.size()+" כוננים חיצוניים זוהו",13,Color.WHITE);
        count.setBackground(shape(Color.argb(40,255,255,255),13));
        count.setPadding(dp(10),dp(4),dp(10),dp(4));
        hero.addView(eyebrow); hero.addView(heading);
        LinearLayout.LayoutParams countP=new LinearLayout.LayoutParams(-2,dp(32)); countP.gravity=Gravity.RIGHT;
        hero.addView(count,countP);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.bottomMargin=dp(20);
        body.addView(hero,hp);

        LinearLayout section=new LinearLayout(this); section.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionTitle=text("הכוננים שלי",18,dark); sectionTitle.setTypeface(null,1);
        section.addView(sectionTitle,new LinearLayout.LayoutParams(0,dp(34),1));
        TextView refresh=centered("↻  רענון",13,Color.rgb(126,164,255));
        refresh.setPadding(dp(10),0,dp(10),0);
        refresh.setOnClickListener(v->{render(); Toast.makeText(this,"רשימת הכוננים עודכנה",Toast.LENGTH_SHORT).show();});
        section.addView(refresh,new LinearLayout.LayoutParams(-2,dp(34)));
        body.addView(section);
        TextView note=text("ניהול אחסון חיצוני בלבד · SD / USB / OTG",11,muted);
        note.setPadding(0,0,0,dp(12)); body.addView(note);

        if(removable.isEmpty()){
            LinearLayout empty=new LinearLayout(this); empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER); empty.setPadding(dp(20),dp(24),dp(20),dp(24));
            empty.setBackground(shape(card,22));
            TextView icon=centered("▤",42,Color.rgb(123,150,255));
            TextView emptyTitle=centered("עוד לא חיברת כונן",17,dark); emptyTitle.setTypeface(null,1);
            TextView desc=centered("חבר כרטיס SD או התקן USB / OTG\nואז לחץ על רענון",13,muted);
            desc.setGravity(Gravity.CENTER); desc.setPadding(0,dp(8),0,dp(16));
            TextView retry=centered("↻    בדיקת חיבורים",14,Color.WHITE);
            retry.setTypeface(null,1); retry.setBackground(gradient(blue,Color.rgb(105,83,255),15));
            empty.addView(icon,new LinearLayout.LayoutParams(-1,dp(56)));
            empty.addView(emptyTitle,new LinearLayout.LayoutParams(-1,dp(34)));
            empty.addView(desc,new LinearLayout.LayoutParams(-1,-2));
            empty.addView(retry,new LinearLayout.LayoutParams(-1,dp(46)));
            retry.setOnClickListener(v->{render();});
            body.addView(empty);
            return;
        }

        for(StorageVolume v:removable){
            LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL);
            outer.setPadding(dp(14),dp(14),dp(14),dp(14)); outer.setBackground(shape(card,22));
            outer.setElevation(dp(2));
            LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,-2); op.bottomMargin=dp(12);
            LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
            TextView icon=centered("▤",24,Color.WHITE); icon.setBackground(gradient(blue,cyan,16));
            top.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(12),0,dp(8),0);
            TextView name=text(volumeName(v),15,dark); name.setTypeface(null,1);
            TextView state=text(stateText(v),12,stateColor(v));
            info.addView(name); info.addView(state);
            top.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            TextView more=centered("ⓘ",22,Color.rgb(151,176,255));
            top.addView(more,new LinearLayout.LayoutParams(dp(34),dp(42)));
            more.setOnClickListener(x->showVolumeInfo(v));
            outer.addView(top);

            LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.CENTER_VERTICAL);
            actions.setPadding(0,dp(14),0,0);
            LinearLayout mount=actionButton("↻","טעינה",blue,Color.WHITE);
            LinearLayout eject=actionButton("⏏","הוצאה בטוחה",Color.rgb(37,49,72),dark);
            LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(46),1); ap.leftMargin=dp(5); ap.rightMargin=dp(5);
            actions.addView(mount,ap); actions.addView(eject,new LinearLayout.LayoutParams(0,dp(46),1));
            mount.setOnClickListener(x->confirmAction(v,"mount"));
            eject.setOnClickListener(x->confirmAction(v,"unmount"));
            outer.addView(actions); body.addView(outer,op);
        }

        TextView tip=text("טיפ: לפני הוצאה בטוחה, ודא שאין העברת קבצים פעילה.",12,muted);
        tip.setPadding(dp(4),dp(8),dp(4),dp(4)); body.addView(tip);
    }

    LinearLayout actionButton(String icon,String title,int color,int fg){
        LinearLayout b=new LinearLayout(this); b.setGravity(Gravity.CENTER); b.setOrientation(LinearLayout.HORIZONTAL);
        b.setBackground(shape(color,14)); b.setClickable(true); b.setFocusable(true);
        TextView i=centered(icon,19,fg); TextView t=centered(title,12,fg); t.setTypeface(null,1);
        b.addView(i,new LinearLayout.LayoutParams(dp(30),-1));
        b.addView(t,new LinearLayout.LayoutParams(0,-1,1)); return b;
    }

    void showVolumeInfo(StorageVolume v){
        String uuid=v.getUuid(); String path="";
        try { File f=v.getDirectory(); if(f!=null) path=f.getAbsolutePath(); } catch(Exception ignored){}
        String message="שם: "+volumeName(v)+"\nמצב: "+stateText(v)+"\nמזהה: "+(uuid==null?"לא זמין":uuid)+"\nנתיב: "+(path.isEmpty()?"לא זמין":path);
        new AlertDialog.Builder(this).setTitle("פרטי האחסון").setMessage(message)
            .setPositiveButton("סגירה",null)
            .setNeutralButton("העתקת פרטים",(d,w)->{
                android.content.ClipboardManager cb=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                cb.setPrimaryClip(ClipData.newPlainText("פרטי אחסון",message));
                Toast.makeText(this,"הפרטים הועתקו",Toast.LENGTH_SHORT).show();
            }).show();
    }

    void confirmAction(StorageVolume v,String action){
        String message="unmount".equals(action)?"האם להוציא בבטחה את "+volumeName(v)+"?":"האם לטעון את "+volumeName(v)+"?";
        new AlertDialog.Builder(this).setTitle("אישור פעולה").setMessage(message)
            .setNegativeButton("ביטול",null)
            .setPositiveButton("המשך",(d,w)->rootCmd(action,v)).show();
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
        if("mounted".equals(s)) return "● מחובר וזמין";
        if("unmounted".equals(s)||"ejected".equals(s)||"removed".equals(s)) return "○ מנותק";
        if("checking".equals(s)) return "בודק את הכרטיס…";
        if("mounted_ro".equals(s)) return "מחובר · קריאה בלבד";
        return s.isEmpty()?"המצב לא ידוע":"מצב: "+s;
    }
    int stateColor(StorageVolume v){return "mounted".equalsIgnoreCase(v.getState())?green:muted;}
    String volumeName(StorageVolume v){
        String desc=""; try{desc=v.getDescription(this);}catch(Exception ignored){}
        String d=desc==null?"":desc.toLowerCase(Locale.ROOT);
        if(d.contains("usb")||d.contains("otg")) return "אחסון USB / OTG";
        if(d.contains("sd")) return "כרטיס SD חיצוני";
        if(v.getUuid()!=null) return "כרטיס SD חיצוני";
        return "אחסון USB / OTG";
    }

    void rootCmd(String a,StorageVolume v){
        new Thread(()->{
            try{
                String uuid=v.getUuid(); String list=runRoot("sm list-volumes all"); String id=null;
                if(uuid!=null&&!uuid.trim().isEmpty()){
                    for(String line:list.split("\\r?\\n")){
                        String[] z=line.trim().split("\\s+");
                        for(String token:z) if(uuid.equalsIgnoreCase(token)||token.toLowerCase(Locale.US).contains(uuid.toLowerCase(Locale.US))){id=z[0];break;}
                        if(id!=null)break;
                    }
                }else{
                    for(String line:list.split("\\r?\\n")){
                        String[] z=line.trim().split("\\s+");
                        if(z.length>=2&&z[0].startsWith("public:")&&
                           (("unmount".equals(a)&&"mounted".equalsIgnoreCase(z[1]))||("mount".equals(a)&&!"mounted".equalsIgnoreCase(z[1])))){id=z[0];break;}
                    }
                }
                if(id==null)throw new IOException("לא נמצא מזהה לכונן.\n"+list);
                runRoot("sm "+a+" "+id);
                runOnUiThread(()->{
                    Toast.makeText(this,"unmount".equals(a)?"הפקודה להוצאה נשלחה":"הפקודה לטעינה נשלחה",Toast.LENGTH_SHORT).show();
                    render();
                    Handler h=new Handler(Looper.getMainLooper());
                    h.postDelayed(()->render(),700); h.postDelayed(()->render(),1600); h.postDelayed(()->render(),3000);
                });
            }catch(Exception e){
                runOnUiThread(()->new AlertDialog.Builder(this).setTitle("הפעולה נכשלה")
                    .setMessage(String.valueOf(e.getMessage())).setPositiveButton("אישור",null).show());
            }
        }).start();
    }
    String runRoot(String cmd)throws Exception{
        java.lang.Process q=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream b=new ByteArrayOutputStream(); InputStream is=q.getInputStream(); byte[] buf=new byte[1024]; int n;
        while((n=is.read(buf))!=-1)b.write(buf,0,n);
        int code=q.waitFor(); String out=b.toString("UTF-8").trim();
        if(code!=0)throw new IOException("קוד "+code+"\n"+(out.isEmpty()?"ללא פלט":out));
        return out;
    }
}