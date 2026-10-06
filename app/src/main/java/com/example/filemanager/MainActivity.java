package com.example.filemanager;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, list;
    ArrayList<String> cats = new ArrayList<>();
    SharedPreferences prefs;
    String currentTitle = "File Manager";
    final int REQ_MEDIA = 40, REQ_TREE = 41;

    String[] defaults = {"Apps","Images","Videos","Audio","Documents","Downloads","New files"};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("cats",0);
        load();
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO}, REQ_MEDIA);
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_MEDIA);
        }
        buildHome();
    }

    void load() {
        String s = prefs.getString("list","");
        if (s.isEmpty()) cats.addAll(Arrays.asList(defaults));
        else cats.addAll(Arrays.asList(s.split("\\|",-1)));
    }

    void save() { prefs.edit().putString("list",String.join("|",cats)).apply(); }

    int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }

    TextView tv(String t,int sp){
        TextView v=new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(Color.DKGRAY);
        v.setGravity(Gravity.CENTER_VERTICAL); v.setPadding(dp(16),dp(8),dp(16),dp(8));
        return v;
    }

    void buildHome(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12),dp(10),dp(12),dp(10));

        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=tv("File Manager",23);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        bar.addView(title,new LinearLayout.LayoutParams(0,dp(58),1));
        Button edit=new Button(this); edit.setText("עריכת אריחים");
        edit.setOnClickListener(v->editMode());
        bar.addView(edit,new LinearLayout.LayoutParams(-2,dp(50)));
        root.addView(bar);

        TextView subtitle=tv("קטגוריות",14);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setPadding(dp(6),0,dp(6),dp(8));
        root.addView(subtitle);

        ScrollView sv=new ScrollView(this);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        renderHome(); setContentView(root);
    }

    TextView cardView(String category){
        TextView card=tv(icon(category)+"\\n"+category,15);
        card.setGravity(Gravity.CENTER);
        card.setTypeface(null, android.graphics.Typeface.BOLD);
        card.setTextColor(Color.DKGRAY);
        card.setPadding(dp(4),dp(9),dp(4),dp(9));

        int bgColor=Color.rgb(245,247,250);
        if(category.equals("Images")) bgColor=Color.rgb(232,240,254);
        else if(category.equals("Videos")) bgColor=Color.rgb(252,235,235);
        else if(category.equals("Audio")) bgColor=Color.rgb(239,232,252);
        else if(category.equals("Documents")) bgColor=Color.rgb(255,246,224);
        else if(category.equals("Downloads")) bgColor=Color.rgb(232,247,238);
        else if(category.equals("Apps")) bgColor=Color.rgb(235,244,250);

        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1),Color.rgb(225,228,233));
        card.setBackground(bg);
        card.setElevation(dp(2));

        if(Build.VERSION.SDK_INT>=21){
            android.content.res.ColorStateList ripple=android.content.res.ColorStateList.valueOf(Color.argb(35,0,0,0));
            card.setForeground(new android.graphics.drawable.RippleDrawable(ripple,null,null));
        }
        card.setOnClickListener(v->openCategory(category));
        return card;
    }

    void renderHome(){
        list.removeAllViews();
        int columns=2;
        LinearLayout row=null;
        for(int i=0;i<cats.size();i++){
            if(i%columns==0){
                row=new LinearLayout(this);
                row.setGravity(Gravity.CENTER);
                row.setPadding(0,dp(3),0,dp(3));
                list.addView(row,new LinearLayout.LayoutParams(-1,dp(100)));
            }
            TextView card=cardView(cats.get(i));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(94),1);
            lp.setMargins(dp(4),dp(2),dp(4),dp(2));
            row.addView(card,lp);
        }
    }

    String icon(String c){
        if(c.equals("Images")) return "🖼";
        if(c.equals("Videos")) return "▶";
        if(c.equals("Audio")) return "♫";
        if(c.equals("Documents")) return "▤";
        if(c.equals("Downloads")) return "↓";
        if(c.equals("Apps")) return "▦";
        if(c.equals("New files")) return "✦";
        return "▣";
    }

    void openCategory(String c){
        currentTitle=c;
        if(c.equals("Apps")) showApps();
        else if(c.equals("Images")) queryMedia("Images");
        else if(c.equals("Videos")) queryMedia("Videos");
        else if(c.equals("Audio")) queryMedia("Audio");
        else if(c.equals("Documents")) queryFiles("Documents",false);
        else if(c.equals("Downloads")) queryFiles("Downloads",true);
        else if(c.equals("New files")) queryFiles("New files",false);
        else openFolderForCategory(c);
    }

    void showListScreen(String title, ArrayList<Item> items){
        root.removeAllViews();
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this); back.setText("‹ חזרה"); back.setOnClickListener(v->buildHome());
        bar.addView(back,new LinearLayout.LayoutParams(dp(105),dp(54)));
        TextView t=tv(title,21); bar.addView(t,new LinearLayout.LayoutParams(0,dp(54),1));
        root.addView(bar);

        ScrollView sv=new ScrollView(this);
        list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        if(items.isEmpty()){
            TextView empty=tv("אין פריטים להצגה",17); empty.setGravity(Gravity.CENTER);
            list.addView(empty,new LinearLayout.LayoutParams(-1,dp(100)));
        } else {
            for(Item it:items){
                TextView row=tv(it.icon+"   "+it.name+"\\n"+it.info,16);
                row.setPadding(dp(16),dp(10),dp(12),dp(10));
                row.setOnClickListener(v->openUri(it.uri,it.mime));
                list.addView(row,new LinearLayout.LayoutParams(-1,dp(68)));
            }
        }
        sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    static class Item {
        String name,info,mime,uri,icon;
        Item(String n,String i,String m,String u,String ic){name=n;info=i;mime=m;uri=u;icon=ic;}
    }

    void queryMedia(String type){
        ArrayList<Item> out=new ArrayList<>();
        Uri base; String[] proj={MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,MediaStore.MediaColumns._ID};
        if(type.equals("Images")) base=MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        else if(type.equals("Videos")) base=MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        else base=MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        try(Cursor cur=getContentResolver().query(base,proj,null,null,MediaStore.MediaColumns.DATE_ADDED+" DESC")){
            if(cur!=null) while(cur.moveToNext()){
                String n=cur.getString(0); long size=cur.getLong(1);
                String mime=cur.getString(2); long id=cur.getLong(3);
                Uri u=ContentUris.withAppendedId(base,id);
                out.add(new Item(n,formatSize(size),mime,u.toString(),type.equals("Images")?"🖼":type.equals("Videos")?"▶":"♫"));
            }
        }catch(Exception e){}
        showListScreen(type,out);
    }

    void queryFiles(String type, boolean downloads){
        ArrayList<Item> out=new ArrayList<>();
        Uri base=MediaStore.Files.getContentUri("external");
        String[] proj={MediaStore.Files.FileColumns.DISPLAY_NAME,MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.MIME_TYPE,MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.RELATIVE_PATH};
        String sel=null; ArrayList<String> args=new ArrayList<>();
        if(downloads && Build.VERSION.SDK_INT>=29){
            sel=MediaStore.Files.FileColumns.RELATIVE_PATH+" LIKE ?";
            args.add("Download/%");
        }
        String sort=type.equals("New files") ? MediaStore.Files.FileColumns.DATE_ADDED+" DESC" :
                MediaStore.Files.FileColumns.DISPLAY_NAME+" COLLATE NOCASE ASC";
        try(Cursor cur=getContentResolver().query(base,proj,sel,args.isEmpty()?null:args.toArray(new String[0]),sort)){
            if(cur!=null) while(cur.moveToNext()){
                String n=cur.getString(0); long size=cur.getLong(1);
                String mime=cur.getString(2); long id=cur.getLong(3);
                if(mime==null) mime="";
                if(type.equals("Documents") && (mime.startsWith("image/")||mime.startsWith("video/")||mime.startsWith("audio/"))) continue;
                Uri u=ContentUris.withAppendedId(base,id);
                out.add(new Item(n,formatSize(size),mime,u.toString(),"▤"));
            }
        }catch(Exception e){}
        showListScreen(type,out);
    }

    void showApps(){
        ArrayList<Item> out=new ArrayList<>();
        PackageManager pm=getPackageManager();
        List<ApplicationInfo> apps=pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(apps,(a,b)->pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString()));
        for(ApplicationInfo a:apps){
            String n=pm.getApplicationLabel(a).toString();
            Uri u=Uri.parse("package:"+a.packageName);
            out.add(new Item(n,a.packageName,"application/*",u.toString(),"▦"));
        }
        showListScreen("Apps",out);
    }

    void openFolderForCategory(String category){
        String saved=prefs.getString("folder_"+category,null);
        if(saved==null){
            new AlertDialog.Builder(this).setTitle(category)
                .setMessage("בחר תיקייה שתשמש עבור הקטגוריה הזאת.")
                .setPositiveButton("בחר תיקייה",(d,w)->pickFolder(category))
                .setNegativeButton("ביטול",(d,w)->buildHome()).show();
        } else showDocumentFolder(Uri.parse(saved),category);
    }

    void pickFolder(String category){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        prefs.edit().putString("pending_category",category).apply();
        startActivityForResult(i,REQ_TREE);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_TREE && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            Uri u=data.getData();
            try{getContentResolver().takePersistableUriPermission(u,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception ignored){}
            String c=prefs.getString("pending_category","Custom");
            prefs.edit().putString("folder_"+c,u.toString()).remove("pending_category").apply();
            showDocumentFolder(u,c);
        }
    }

    void showDocumentFolder(Uri uri,String title){
        ArrayList<Item> out=new ArrayList<>();
        DocumentFile dir=DocumentFile.fromTreeUri(this,uri);
        if(dir!=null && dir.exists()){
            DocumentFile[] files=dir.listFiles();
            Arrays.sort(files,(a,b)->a.getName()==null?1:b.getName()==null?-1:a.getName().compareToIgnoreCase(b.getName()));
            for(DocumentFile f:files){
                String n=f.getName()==null?"ללא שם":f.getName();
                String info=f.isDirectory()?"תיקייה":formatSize(f.length());
                out.add(new Item(n,info,f.getType()==null?"":f.getType(),f.getUri().toString(),f.isDirectory()?"📁":"▤"));
            }
        }
        showListScreen(title,out);
    }

    void openUri(String uri,String mime){
        try{
            Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(uri));
            if(mime!=null&&!mime.isEmpty()) i.setDataAndType(Uri.parse(uri),mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        }catch(Exception e){
            Toast.makeText(this,"אין אפליקציה מתאימה לפתיחת הקובץ",Toast.LENGTH_SHORT).show();
        }
    }

    String formatSize(long n){
        if(n<1024) return n+" B";
        if(n<1024*1024) return String.format(Locale.US,"%.1f KB",n/1024.0);
        if(n<1024L*1024*1024) return String.format(Locale.US,"%.1f MB",n/1024.0/1024);
        return String.format(Locale.US,"%.1f GB",n/1024.0/1024/1024);
    }

    void editMode(){
        list.removeAllViews();
        TextView info=tv("שנה סדר, מחק או הוסף קטגוריות.",15);
        list.addView(info,new LinearLayout.LayoutParams(-1,dp(55)));
        for(int i=0;i<cats.size();i++){
            final int pos=i; LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=tv(icon(cats.get(i))+"  "+cats.get(i),17);
            row.addView(name,new LinearLayout.LayoutParams(0,dp(58),1));
            Button up=new Button(this); up.setText("◀"); up.setOnClickListener(v->{if(pos>0){Collections.swap(cats,pos,pos-1);save();editMode();}});
            row.addView(up,new LinearLayout.LayoutParams(dp(54),dp(52)));
            Button down=new Button(this); down.setText("▶"); down.setOnClickListener(v->{if(pos<cats.size()-1){Collections.swap(cats,pos,pos+1);save();editMode();}});
            row.addView(down,new LinearLayout.LayoutParams(dp(54),dp(52)));
            Button del=new Button(this); del.setText("מחק"); del.setOnClickListener(v->{cats.remove(pos);save();editMode();});
            row.addView(del,new LinearLayout.LayoutParams(dp(72),dp(52)));
            list.addView(row);
        }
        LinearLayout actions=new LinearLayout(this);
        Button add=new Button(this); add.setText("+ הוסף קטגוריה"); add.setOnClickListener(v->addCategory());
        actions.addView(add,new LinearLayout.LayoutParams(0,dp(56),1));
        Button done=new Button(this); done.setText("סיום"); done.setOnClickListener(v->buildHome());
        actions.addView(done,new LinearLayout.LayoutParams(0,dp(56),1));
        list.addView(actions);
    }

    void addCategory(){
        final EditText input=new EditText(this); input.setHint("שם הקטגוריה");
        new AlertDialog.Builder(this).setTitle("קטגוריה חדשה").setView(input)
            .setNegativeButton("ביטול",null)
            .setPositiveButton("הוסף",(d,w)->{String s=input.getText().toString().trim();if(!s.isEmpty()){cats.add(s);save();editMode();}}).show();
    }

    @Override public void onBackPressed(){
        if(list!=null && !currentTitle.equals("File Manager")) { currentTitle="File Manager"; buildHome(); }
        else super.onBackPressed();
    }
}
