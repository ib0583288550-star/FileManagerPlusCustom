package com.example.filemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;

public class TileConfirmActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        new AlertDialog.Builder(this)
            .setTitle("מנהל אחסון")
            .setMessage("האם לפתוח את מנהל האחסון?")
            .setPositiveButton("כן", (d,w) -> {
                Intent i=new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            })
            .setNegativeButton("לא", (d,w) -> finish())
            .setOnCancelListener(d -> finish())
            .show();
    }
}