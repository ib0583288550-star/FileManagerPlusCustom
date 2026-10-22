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
    LinearLayout body, tabBar;
    int selectedTab = 0;
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
        LinearLayout titleBox=new LinearLayout(this); titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(4),0,0,0);
        TextView title=text("מנהל אחסון",21,dark); title.setTypeface(null,1);
        TextView subtitle=text("הכרטיסים והכוננים שלך",12,muted);
        titleBox.addView(title); titleBox.addView(subtitle);
        header.addView(titleBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView menu=centered("⋮",27,dark); menu.setOnClickListener(v->showMenu(menu));
        header.addView(menu,new LinearLayout.LayoutParams(dp(42),dp(48)));
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(66)));

        tabBar=new LinearLayout(this); tabBar.setPadding(dp(14),dp(4),dp(14),dp(8)); tabBar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(tabBar,new LinearLayout.LayoutParams(-1,dp(58)));

        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16),dp(8),dp(16),dp(20));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView credit=centered("y.b apps  •  מנהל אחסון",11,muted);
        root.addView(credit,new LinearLayout.LayoutParams(-1,dp(32)));
        setContentView(root); renderTabs(); render();
    }

    void renderTabs(){
        tabBar.removeAllViews();
        String[] labels={"▤  מנהל אחסון","▦  אריחי אפליקציות"};
        for(int i=0;i<labels.length;i++){
            final int tab=i;
            TextView item=centered(labels[i],13,selectedTab==i?Color.WHITE:muted);
            item.setTypeface(null,1);
            item.setBackground(shape(selectedTab==i?Color.rgb(53,75,145):card,16));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);
            p.leftMargin=dp(4); p.rightMargin=dp(4);
            tabBar.addView(item,p);
            item.setOnClickListener(v->{selectedTab=tab;renderTabs();render();});
        }
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
            new AlertDialog.Builder(this).setTitle("מנהל אחסון 1.26")
                .setMessage("כלים מהירים לניהול כרטיסי SD וחיבורי OTG.\n\ny.b apps")
                .setPositiveButton("סגירה",null).show(); return true;
        });
        popup.show();
    }

    void render(){
        body.removeAllViews();
        if(selectedTab==1){ renderAppTiles(); return; }
        renderStorage();
    }

    void renderStorage(){
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

    void renderAppTiles(){
        TextView intro=text("הפוך אפליקציות לאריחים מהירים",22,dark); intro.setTypeface(null,1);
        body.addView(intro);
        TextView desc=text("בחר אפליקציה לכל משבצת, ואז הוסף את האריח המתאים למסך ההגדרות המהירות של Android.",13,muted);
        desc.setPadding(0,dp(8),0,dp(16)); body.addView(desc);

        if(Build.VERSION.SDK_INT<24){
            LinearLayout notice=new LinearLayout(this); notice.setOrientation(LinearLayout.VERTICAL);
            notice.setPadding(dp(16),dp(18),dp(16),dp(18)); notice.setBackground(shape(card,20));
            TextView n=text("האריחים של Android זמינים מ־Android 7.0 ומעלה. במכשיר Android 5 האפליקציה תמשיך לעבוד בכרטיסיית מנהל האחסון, אך המערכת לא תומכת באריחים מותאמים.",14,dark);
            n.setGravity(Gravity.CENTER_VERTICAL); notice.addView(n); body.addView(notice); return;
        }

        String[] keys={"app_tile_1","app_tile_2","app_tile_3","app_tile_4"};
        String[] names={"אריח אפליקציה 1","אריח אפליקציה 2","אריח אפליקציה 3","אריח אפליקציה 4"};
        android.content.SharedPreferences prefs=getSharedPreferences("app_tiles",MODE_PRIVATE);
        for(int i=0;i<keys.length;i++){
            final int slot=i+1; final String key=keys[i];
            String pkg=prefs.getString(key,"");
            String appName=prefs.getString(key+"_label","");
            LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(15),dp(14),dp(15),dp(14)); box.setBackground(shape(card,20));
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2); bp.bottomMargin=dp(11);
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            android.widget.ImageView icon=new android.widget.ImageView(this);
            icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            icon.setPadding(dp(4),dp(4),dp(4),dp(4));
            if(!pkg.isEmpty()){
                try { icon.setImageDrawable(getPackageManager().getApplicationIcon(pkg)); }
                catch(Exception ignored){ icon.setImageResource(android.R.drawable.sym_def_app_icon); }
                icon.setBackground(shape(Color.rgb(34,45,67),14));
            }else{
                icon.setImageResource(android.R.drawable.sym_def_app_icon);
                icon.setBackground(shape(Color.rgb(34,45,67),14));
            }
            row.addView(icon,new LinearLayout.LayoutParams(dp(46),dp(46)));
            LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(12),0,dp(8),0);
            TextView title=text(names[i],15,dark); title.setTypeface(null,1);
            TextView state=text(pkg.isEmpty()?"לא נבחרה אפליקציה":appName,12,pkg.isEmpty()?muted:green);
            info.addView(title); info.addView(state); row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            box.addView(row);
            LinearLayout actions=new LinearLayout(this); actions.setPadding(0,dp(12),0,0);
            TextView choose=centered(pkg.isEmpty()?"בחירת אפליקציה":"שינוי אפליקציה",13,Color.WHITE);
            choose.setTypeface(null,1); choose.setBackground(gradient(blue,Color.rgb(105,83,255),13));
            actions.addView(choose,new LinearLayout.LayoutParams(0,dp(42),1));
            TextView clear=centered("איפוס",13,muted); clear.setBackground(shape(Color.rgb(37,49,72),13));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(78),dp(42)); cp.leftMargin=dp(8); actions.addView(clear,cp);
            box.addView(actions);
            choose.setOnClickListener(v->chooseApp(slot,key));
            clear.setOnClickListener(v->{prefs.edit().remove(key).remove(key+"_label").apply();refreshAppTile(slot);render();});
            body.addView(box,bp);
        }
        LinearLayout hint=new LinearLayout(this); hint.setOrientation(LinearLayout.VERTICAL);
        hint.setPadding(dp(15),dp(14),dp(15),dp(14)); hint.setBackground(shape(Color.rgb(24,39,65),18));
        TextView h=text("איך מוסיפים אריח למעלה?",15,Color.rgb(156,190,255)); h.setTypeface(null,1);
        TextView steps=text("1. בחר אפליקציה במשבצת כאן.\n2. משוך את וילון ההתראות פעמיים.\n3. לחץ על עריכה (עיפרון) וגרור את „אריח אפליקציה” לרשימת האריחים הפעילים.\n4. חזור על הפעולה עבור משבצות נוספות.",13,dark);
        steps.setPadding(0,dp(8),0,0); hint.addView(h); hint.addView(steps); body.addView(hint);
    }

    void chooseApp(int slot,String key){
        Intent query=new Intent(Intent.ACTION_MAIN,null); query.addCategory(Intent.CATEGORY_LAUNCHER);
        android.content.pm.PackageManager pm=getPackageManager();
        List<android.content.pm.ResolveInfo> apps=pm.queryIntentActivities(query,0);
        Collections.sort(apps,(a,b)->String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        ArrayList<android.content.pm.ResolveInfo> filtered=new ArrayList<>();
        for(android.content.pm.ResolveInfo app:apps) if(!getPackageName().equals(app.activityInfo.packageName)) filtered.add(app);
        if(filtered.isEmpty()){Toast.makeText(this,"לא נמצאו אפליקציות לבחירה",Toast.LENGTH_SHORT).show();return;}

        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(10),dp(6),dp(10),dp(6)); scroll.addView(list);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("בחר אפליקציה לאריח "+slot)
            .setView(scroll).setNegativeButton("ביטול",null).create();

        for(android.content.pm.ResolveInfo app:filtered){
            LinearLayout item=new LinearLayout(this); item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(10),dp(8),dp(10),dp(8));
            item.setBackground(shape(Color.rgb(24,35,56),14));
            LinearLayout.LayoutParams itemParams=new LinearLayout.LayoutParams(-1,dp(58));
            itemParams.bottomMargin=dp(5);
            android.widget.ImageView appIcon=new android.widget.ImageView(this);
            appIcon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            try { appIcon.setImageDrawable(app.loadIcon(pm)); }
            catch(Exception ignored){ appIcon.setImageResource(android.R.drawable.sym_def_app_icon); }
            item.addView(appIcon,new LinearLayout.LayoutParams(dp(38),dp(38)));
            TextView appLabel=text(String.valueOf(app.loadLabel(pm)),15,dark);
            appLabel.setPadding(dp(12),0,dp(8),0);
            item.addView(appLabel,new LinearLayout.LayoutParams(0,-1,1));
            list.addView(item,itemParams);
            item.setOnClickListener(v->{
                prefsSaveApp(key,app,pm);
                refreshAppTile(slot);
                dialog.dismiss();
                render();
                Toast.makeText(this,"האריח עודכן. אם כבר הוספת אותו למעלה, הסר והוסף אותו מחדש.",Toast.LENGTH_LONG).show();
            });
        }
        dialog.show();
        dialog.getWindow().setLayout(-1,Math.min(getResources().getDisplayMetrics().heightPixels-dp(120),dp(560)));
    }

    void refreshAppTile(int slot){
        if(Build.VERSION.SDK_INT>=24){
            android.content.ComponentName component;
            switch(slot){
                case 1: component=new android.content.ComponentName(this,AppTileService1.class); break;
                case 2: component=new android.content.ComponentName(this,AppTileService2.class); break;
                case 3: component=new android.content.ComponentName(this,AppTileService3.class); break;
                default: component=new android.content.ComponentName(this,AppTileService4.class); break;
            }
            android.service.quicksettings.TileService.requestListeningState(this,component);
        }
    }

    void prefsSaveApp(String key,android.content.pm.ResolveInfo chosen,android.content.pm.PackageManager pm){
        getSharedPreferences("app_tiles",MODE_PRIVATE).edit()
            .putString(key,chosen.activityInfo.packageName)
            .putString(key+"_label",String.valueOf(chosen.loadLabel(pm))).apply();
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