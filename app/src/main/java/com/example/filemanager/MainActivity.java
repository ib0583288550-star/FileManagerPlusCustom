package com.example.filemanager;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.storage.*;
import android.view.*;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout body;
    SharedPreferences p;
    static final int PICK = 77;
    int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+.5f); }

    int navy=Color.rgb(7,24,45), blue=Color.rgb(0,140,255), cyan=Color.rgb(21,183,255);
    int text=Color.rgb(25,35,48), muted=Color.rgb(100,115,130), bg=Color.rgb(246,248,251);

    TextView tv(String s,float size,int color){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); v.setPadding(dp(16),dp(8),dp(16),dp(8));
        return v;
    }
    GradientDrawable shape(int color,float radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp((int)radius)); return g;
    }
    Button button(String label,int color,int txtColor){
        Button b=new Button(this); b.setText(label); b.setTextColor(txtColor); b.setTextSize(15);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackground(shape(color,16));
        b.setPadding(dp(14),0,dp(14),0); return b;
    }
    LinearLayout card(){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16),dp(14),dp(16),dp(14)); c.setBackground(shape(Color.WHITE,20));
        return c;
    }
    TextView line(String s){ TextView v=tv(s,14,muted); v.setPadding(dp(16),dp(4),dp(16),dp(4)); return v; }

    @Override public void onCreate(Bundle b){ super.onCreate(b); p=getSharedPreferences("storage",0); home(); }

    void home(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg);
        LinearLayout header=new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL); header.setPadding(dp(20),dp(18),dp(20),dp(18));
        header.setBackground(shape(navy,0));
        TextView title=tv("מנהל אחסון",26,Color.WHITE); title.setTypeface(null,1); title.setPadding(0,dp(4),0,0); header.addView(title,new LinearLayout.LayoutParams(-1,dp(44)));
        TextView sub=tv("שליטה פשוטה באחסון של המכשיר",14,Color.rgb(190,210,230)); sub.setPadding(0,0,0,dp(4)); header.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));
        root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(14),dp(14),dp(14),dp(24));
        scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); render();
    }

    void addGap(int h){ Space s=new Space(this); body.addView(s,new LinearLayout.LayoutParams(1,dp(h))); }

    void render(){
        body.removeAllViews();
        String target=p.getString("target","internal");
        boolean external=target.equals("external");

        LinearLayout status=card();
        TextView st=tv("יעד ברירת מחדל",16,text); st.setTypeface(null,1); status.addView(st,new LinearLayout.LayoutParams(-1,dp(34)));
        TextView current=tv(external?"💳  כרטיס SD / אחסון חיצוני":"💾  אחסון פנימי",20,navy); current.setTypeface(null,1);
        status.addView(current,new LinearLayout.LayoutParams(-1,dp(42)));
        TextView hint=tv(external?"קבצים חדשים שהאפליקציה יוצרת יישמרו בתיקייה שבחרת.":"קבצים חדשים יישמרו באחסון הפנימי.",13,muted);
        status.addView(hint,new LinearLayout.LayoutParams(-1,dp(38)));
        body.addView(status); addGap(12);

        TextView section=tv("בחירת יעד",18,text); section.setTypeface(null,1); section.setPadding(dp(4),0,dp(4),dp(8)); body.addView(section);
        LinearLayout actions=card();

        Button in=button("💾   אחסון פנימי",external?Color.rgb(242,245,249):Color.rgb(225,241,255),external?text:navy);
        in.setOnClickListener(v->{p.edit().putString("target","internal").remove("tree").apply();render();});
        actions.addView(in,new LinearLayout.LayoutParams(-1,dp(52)));

        Space gap=new Space(this); actions.addView(gap,new LinearLayout.LayoutParams(1,dp(8)));
        Button sd=button("💳   בחר כרטיס SD / חיצוני",blue,Color.WHITE);
        sd.setOnClickListener(v->pick()); actions.addView(sd,new LinearLayout.LayoutParams(-1,dp(52)));
        body.addView(actions); addGap(12);

        Button test=button("✍️   צור קובץ בדיקה ביעד הנבחר",Color.WHITE,navy);
        test.setOnClickListener(v->writeTest());
        body.addView(test,new LinearLayout.LayoutParams(-1,dp(54))); addGap(18);

        TextView sec2=tv("אמצעי אחסון",18,text); sec2.setTypeface(null,1); sec2.setPadding(dp(4),0,dp(4),dp(8)); body.addView(sec2);
        for(StorageVolume v:vols()) addVolume(v);
    }

    void addVolume(StorageVolume v){
        LinearLayout c=card();
        boolean rem=v.isRemovable();
        TextView name=tv((rem?"💳  אחסון חיצוני":"📱  אחסון פנימי"),17,text); name.setTypeface(null,1);
        c.addView(name,new LinearLayout.LayoutParams(-1,dp(38)));
        c.addView(line("מצב: "+v.getState()));
        c.addView(line(label(v)));
        if(rem){
            addGapIn(c,8);
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            Button eject=button("⏏️  הוצאה בטוחה",Color.rgb(255,244,244),Color.rgb(170,45,45));
            eject.setOnClickListener(x->rootCmd("unmount",v));
            Button mount=button("🔌  טעינה מחדש",Color.rgb(235,248,255),navy);
            mount.setOnClickListener(x->rootCmd("mount",v));
            row.addView(eject,new LinearLayout.LayoutParams(0,dp(48),1));
            Space s=new Space(this); row.addView(s,new LinearLayout.LayoutParams(dp(8),1));
            row.addView(mount,new LinearLayout.LayoutParams(0,dp(48),1));
            c.addView(row);
        }
        body.addView(c); addGap(10);
    }
    void addGapIn(LinearLayout c,int h){Space s=new Space(this);c.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}

    ArrayList<StorageVolume> vols(){
        StorageManager m=(StorageManager)getSystemService(STORAGE_SERVICE);
        return new ArrayList<>(m.getStorageVolumes());
    }
    String label(StorageVolume v){
        String u=v.getUuid(); String path=v.getDirectory()==null?"":v.getDirectory().getAbsolutePath();
        return (u==null?"כונן":u)+"  •  "+path;
    }
    void pick(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,PICK);
    }
    @Override protected void onActivityResult(int q,int r,Intent d){
        super.onActivityResult(q,r,d);
        if(q==PICK&&r==RESULT_OK&&d!=null){
            Uri u=d.getData();
            try{getContentResolver().takePersistableUriPermission(u,d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception e){}
            p.edit().putString("target","external").putString("tree",u.toString()).apply(); render();
        }
    }
    void writeTest(){
        try{
            String x=p.getString("tree",null);
            DocumentFile d=x==null?DocumentFile.fromFile(getFilesDir()):DocumentFile.fromTreeUri(this,Uri.parse(x));
            if(d==null) throw new IOException("לא נמצאה תיקיית יעד");
            DocumentFile f=d.createFile("text/plain","StorageTest.txt");
            if(f==null) throw new IOException("לא ניתן ליצור קובץ");
            OutputStream o=getContentResolver().openOutputStream(f.getUri());
            o.write(("נוצר "+new Date()).getBytes("UTF-8")); o.close();
            Toast.makeText(this,"✓ הקובץ נוצר בהצלחה",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"כתיבה נכשלה: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void rootCmd(String a,StorageVolume v){
        String u=v.getUuid(); if(u==null){Toast.makeText(this,"אין UUID לכונן",Toast.LENGTH_LONG).show();return;}
        new Thread(()->{try{
            java.lang.Process q=Runtime.getRuntime().exec(new String[]{"su","-c","sm "+a+" "+u});
            int c=q.waitFor();
            runOnUiThread(()->new AlertDialog.Builder(this).setTitle(a.equals("unmount")?"הוצאה בטוחה":"טעינה מחדש")
                .setMessage("פקודת Root הסתיימה. קוד: "+c).setPositiveButton("אישור",null).show());
        }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Root לא זמין: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
    }
}