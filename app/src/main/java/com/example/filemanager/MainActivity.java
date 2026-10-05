package com.example.filemanager;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.view.*;import android.widget.*;import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, list; ArrayList<String> cats=new ArrayList<>(); android.content.SharedPreferences prefs;
    String[] defaults={"Apps","Images","Videos","Audio","Documents","Downloads","New files"};
    int pad=16;
    @Override public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("cats",0); load(); build();}
    void load(){String s=prefs.getString("list",""); if(s.isEmpty()) cats.addAll(Arrays.asList(defaults)); else cats.addAll(Arrays.asList(s.split("\\|",-1)));}
    void save(){prefs.edit().putString("list",String.join("|",cats)).apply();}
    TextView tv(String t,int sp){TextView v=new TextView(this);v.setText(t);v.setTextSize(sp);v.setTextColor(Color.DKGRAY);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(dp(pad),dp(8),dp(pad),dp(8));return v;}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    void build(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(8),dp(8),dp(8),dp(8));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL); TextView title=tv("File Manager",22);bar.addView(title,new LinearLayout.LayoutParams(0,dp(56),1));
        Button edit=new Button(this);edit.setText("עריכת קטגוריות");edit.setOnClickListener(v->editMode());bar.addView(edit,new LinearLayout.LayoutParams(-2,dp(52)));root.addView(bar);
        ScrollView sv=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        render();setContentView(root);}
    void render(){list.removeAllViews(); for(String c:cats){TextView row=tv("▣   "+c,18);row.setBackgroundColor(Color.WHITE);row.setOnClickListener(v->Toast.makeText(this,c,Toast.LENGTH_SHORT).show());list.addView(row,new LinearLayout.LayoutParams(-1,dp(62)));}}
    void editMode(){
        list.removeAllViews(); TextView info=tv("גרור עם החצים כדי לשנות סדר. מחק או הוסף קטגוריות.",15);list.addView(info,new LinearLayout.LayoutParams(-1,dp(55)));
        for(int i=0;i<cats.size();i++){ final int pos=i; LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL); TextView name=tv(cats.get(i),17);row.addView(name,new LinearLayout.LayoutParams(0,dp(58),1));
            Button up=new Button(this);up.setText("◀");up.setOnClickListener(v->{if(pos>0){Collections.swap(cats,pos,pos-1);save();editMode();}});row.addView(up,new LinearLayout.LayoutParams(dp(54),dp(52)));
            Button down=new Button(this);down.setText("▶");down.setOnClickListener(v->{if(pos<cats.size()-1){Collections.swap(cats,pos,pos+1);save();editMode();}});row.addView(down,new LinearLayout.LayoutParams(dp(54),dp(52)));
            Button del=new Button(this);del.setText("מחק");del.setOnClickListener(v->{cats.remove(pos);save();editMode();});row.addView(del,new LinearLayout.LayoutParams(dp(72),dp(52)));list.addView(row);}
        LinearLayout actions=new LinearLayout(this); Button add=new Button(this);add.setText("+ הוסף קטגוריה");add.setOnClickListener(v->addCategory());actions.addView(add,new LinearLayout.LayoutParams(0,dp(56),1)); Button done=new Button(this);done.setText("סיום");done.setOnClickListener(v->build());actions.addView(done,new LinearLayout.LayoutParams(0,dp(56),1));list.addView(actions);
    }
    void addCategory(){final EditText input=new EditText(this);input.setHint("שם הקטגוריה"); new AlertDialog.Builder(this).setTitle("קטגוריה חדשה").setView(input).setNegativeButton("ביטול",null).setPositiveButton("הוסף",(d,w)->{String s=input.getText().toString().trim();if(!s.isEmpty()){cats.add(s);save();editMode();}}).show();}
}
