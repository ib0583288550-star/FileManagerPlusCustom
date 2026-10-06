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
    LinearLayout body; SharedPreferences p; static final int PICK=77;
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    int bg=Color.rgb(245,247,250), dark=Color.rgb(18,27,38), blue=Color.rgb(20,126,230), gray=Color.rgb(105,116,128);
    GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView t(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(12),0,dp(12),0);return v;}
    TextView action(String s,boolean selected){
        TextView v=t(s,14,selected?Color.WHITE:dark);v.setGravity(Gravity.CENTER);v.setClickable(true);v.setFocusable(true);
        v.setBackground(box(selected?blue:Color.WHITE,12));v.setMinHeight(dp(44));v.setElevation(dp(1));return v;
    }
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(10),dp(12),dp(10));c.setBackground(box(Color.WHITE,14));c.setElevation(dp(1));return c;}
    void gap(LinearLayout c,int h){Space s=new Space(this);c.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    @Override public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("storage",0);build();}
    void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);
        LinearLayout top=new LinearLayout(this);top.setPadding(dp(14),dp(10),dp(14),dp(10));top.setBackgroundColor(dark);
        TextView title=t("מנהל אחסון",21,Color.WHITE);title.setTypeface(null,1);top.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView refresh=t("↻",25,Color.WHITE);refresh.setGravity(Gravity.CENTER);refresh.setClickable(true);refresh.setOnClickListener(v->render());top.addView(refresh,new LinearLayout.LayoutParams(dp(48),dp(44)));
        root.addView(top);
        ScrollView sc=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(10),dp(10),dp(10),dp(16));sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);render();
    }
    void render(){
        body.removeAllViews();boolean ext="external".equals(p.getString("target","internal"));
        LinearLayout status=card();
        TextView a=t("יעד נוכחי",13,gray);status.addView(a,new LinearLayout.LayoutParams(-1,dp(25)));
        TextView cur=t(ext?"💳  SD / חיצוני":"💾  פנימי",17,dark);cur.setTypeface(null,1);status.addView(cur,new LinearLayout.LayoutParams(-1,dp(34)));
        body.addView(status);gap(body,8);
        TextView h=t("בחירת יעד",15,dark);h.setTypeface(null,1);body.addView(h,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        TextView in=action("💾  פנימי",!ext);in.setOnClickListener(v->{p.edit().putString("target","internal").remove("tree").apply();Toast.makeText(this,"יעד: אחסון פנימי",Toast.LENGTH_SHORT).show();render();});
        TextView sd=action("💳  SD / חיצוני",ext);sd.setOnClickListener(v->pick());
        row.addView(in,new LinearLayout.LayoutParams(0,dp(44),1));Space sp=new Space(this);row.addView(sp,new LinearLayout.LayoutParams(dp(7),1));row.addView(sd,new LinearLayout.LayoutParams(0,dp(44),1));body.addView(row);
        gap(body,8);
        TextView test=action("✍  בדיקת כתיבה",false);test.setOnClickListener(v->writeTest());body.addView(test,new LinearLayout.LayoutParams(-1,dp(44)));
        gap(body,12);
        TextView h2=t("אחסון במכשיר",15,dark);h2.setTypeface(null,1);body.addView(h2,new LinearLayout.LayoutParams(-1,dp(30)));
        for(StorageVolume v:vols())addVolume(v);
    }
    void addVolume(StorageVolume v){
        boolean rem=v.isRemovable();LinearLayout c=card();
        TextView n=t(rem?"💳  אחסון חיצוני":"📱  אחסון פנימי",15,dark);n.setTypeface(null,1);c.addView(n,new LinearLayout.LayoutParams(-1,dp(30)));
        c.addView(t("מצב: "+v.getState(),12,gray),new LinearLayout.LayoutParams(-1,dp(25)));
        if(rem){
            LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
            TextView e=action("⏏  הוצאה",false);e.setTextColor(Color.rgb(175,55,55));e.setOnClickListener(x->rootCmd("unmount",v));
            TextView m=action("↻  טעינה",false);m.setOnClickListener(x->rootCmd("mount",v));
            r.addView(e,new LinearLayout.LayoutParams(0,dp(40),1));Space s=new Space(this);r.addView(s,new LinearLayout.LayoutParams(dp(7),1));r.addView(m,new LinearLayout.LayoutParams(0,dp(40),1));c.addView(r);
        }
        body.addView(c);gap(body,7);
    }
    ArrayList<StorageVolume> vols(){return new ArrayList<>(((StorageManager)getSystemService(STORAGE_SERVICE)).getStorageVolumes());}
    void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK);}
    @Override protected void onActivityResult(int q,int r,Intent d){super.onActivityResult(q,r,d);if(q==PICK&&r==RESULT_OK&&d!=null){Uri u=d.getData();try{getContentResolver().takePersistableUriPermission(u,d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Exception e){}p.edit().putString("target","external").putString("tree",u.toString()).apply();render();}}
    void writeTest(){try{String x=p.getString("tree",null);DocumentFile d=x==null?DocumentFile.fromFile(getFilesDir()):DocumentFile.fromTreeUri(this,Uri.parse(x));if(d==null)throw new IOException("לא נמצאה תיקייה");DocumentFile f=d.createFile("text/plain","StorageTest.txt");if(f==null)throw new IOException("לא ניתן ליצור קובץ");OutputStream o=getContentResolver().openOutputStream(f.getUri());o.write(("נוצר "+new Date()).getBytes("UTF-8"));o.close();Toast.makeText(this,"✓ הקובץ נוצר",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"כתיבה נכשלה: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    void rootCmd(String a,StorageVolume v){
        new Thread(()->{
            try{
                String uuid=v.getUuid();
                String list=runRoot("sm list-volumes all");
                String id=null;
                if(uuid!=null && !uuid.trim().isEmpty()){
                    for(String line:list.split("\\r?\\n")){
                        String[] z=line.trim().split("\\s+");
                        if(z.length>=1){
                            for(String token:z){
                                if(uuid.equalsIgnoreCase(token) || token.toLowerCase(Locale.US).contains(uuid.toLowerCase(Locale.US))){
                                    id=z[0]; break;
                                }
                            }
                        }
                        if(id!=null) break;
                    }
                }
                if(id==null) throw new IOException("לא נמצא מזהה sm לכונן.\n"+list);
                String cmd="sm "+a+" "+id;
                String out=runRoot(cmd);
                runOnUiThread(()->new AlertDialog.Builder(this)
                    .setTitle(a.equals("unmount")?"הוצאה בטוחה":"טעינה מחדש")
                    .setMessage("בוצע בהצלחה: "+cmd+(out.isEmpty()?"":"\n\n"+out))
                    .setPositiveButton("אישור",null).show());
            }catch(Exception e){
                runOnUiThread(()->new AlertDialog.Builder(this)
                    .setTitle("פקודת Root נכשלה")
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
        int code=q.waitFor();
        String out=b.toString("UTF-8").trim();
        if(code!=0) throw new IOException("קוד "+code+"\n"+(out.isEmpty()?"ללא פלט":out));
        return out;
    }
}