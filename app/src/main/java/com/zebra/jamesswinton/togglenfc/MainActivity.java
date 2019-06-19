package com.zebra.jamesswinton.togglenfc;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.bumptech.glide.Glide;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.zebra.jamesswinton.togglenfc.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

  // Debugging
  private static final String TAG = "MainActivity";

  // Constants
  private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

  // Static Variables

  // Non-Static Variables
  ActivityMainBinding mDataBinding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Init DataBinding
    mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    mDataBinding.startService.setOnClickListener(v -> {
      startService(new Intent(MainActivity.this, ToggleNfcService.class));
      finish();
    });

    //Check if the application has draw over other apps permission or not?
    //This permission is by default available for API<23. But for API > 23
    //you have to ask for the permission in runtime.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          Uri.parse("package:" + getPackageName()));
      startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
    }

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {

      //Check if the permission is granted or not.
      if (resultCode != RESULT_OK) { //Permission is not available
        Toast.makeText(this,
            "Draw over other app permission not available. Closing the application",
            Toast.LENGTH_SHORT).show();
        finish();
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
