package com.zebra.jamesswinton.togglenfc;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import de.hdodenhof.circleimageview.CircleImageView;

public class ToggleNfcService extends Service {

  private static final String TAG = "ToggleNfcService";

  private View mToggleNfcView;
  private WindowManager mWindowManager;

  private static boolean mNfcState;

  private static final String PROFILE_NAME = "ToggleNFC";
  private static CircleImageView chatHeadImage;


  public ToggleNfcService() { }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onCreate() {
    super.onCreate();
    //Inflate the chat head layout we created
    mToggleNfcView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null);

    // Get NFC State
    mNfcState = getNfcState();

    //Add the view to the window.
    final WindowManager.LayoutParams params;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      params = new WindowManager.LayoutParams(
          WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.TYPE_PHONE,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
          PixelFormat.TRANSLUCENT);
    }else{
      params = new WindowManager.LayoutParams(
          WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.WRAP_CONTENT,
          WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
          PixelFormat.TRANSLUCENT);
    }

    // Specify the chat head position
    params.gravity = Gravity.TOP | Gravity.LEFT;
    params.x = 0;
    params.y = 100;

    //Add the view to the window
    mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    mWindowManager.addView(mToggleNfcView, params);

    //Drag and move chat head using user's touch action.
    chatHeadImage = mToggleNfcView.findViewById(R.id.toggle_nfc_imageview);

    // Set Image
    Glide.with(this).load(setToggleButtonState(mNfcState))
        .dontAnimate().into(chatHeadImage);

    GestureDetector gestureDetector = new GestureDetector(this, new SingleTapConfirm());
    chatHeadImage.setOnTouchListener(new View.OnTouchListener() {
      private int lastAction;
      private int initialX;
      private int initialY;
      private float initialTouchX;
      private float initialTouchY;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
          Glide.with(getBaseContext()).load(R.raw.loading).into(chatHeadImage);
          toggleNfc(!mNfcState);
          return true;
        } else {
          switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

              //remember the initial position.
              initialX = params.x;
              initialY = params.y;

              //get the touch location
              initialTouchX = event.getRawX();
              initialTouchY = event.getRawY();

              lastAction = event.getAction();
              return true;
            case MotionEvent.ACTION_UP:
              //As we implemented on touch listener with ACTION_MOVE,
              //we have to check if the previous action was ACTION_DOWN
              //to identify if the user clicked the view or not.
              if (lastAction == MotionEvent.ACTION_DOWN) {
                //Open the chat conversation click.
                Intent intent = new Intent(ToggleNfcService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                //close the service and remove the chat heads
                stopSelf();
              }
              lastAction = event.getAction();
              return true;
            case MotionEvent.ACTION_MOVE:
              //Calculate the X and Y coordinates of the view.
              params.x = initialX + (int) (event.getRawX() - initialTouchX);
              params.y = initialY + (int) (event.getRawY() - initialTouchY);

              //Update the layout with new X & Y coordinate
              mWindowManager.updateViewLayout(mToggleNfcView, params);
              lastAction = event.getAction();
              return true;
          }
        }
        return false;
      }
    });
  }

  private boolean getNfcState() {
    NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
    NfcAdapter adapter = manager.getDefaultAdapter();
    if (adapter != null) {
      return adapter.isEnabled();
    } else {
      return false;
    }
  }

  private Drawable setToggleButtonState(boolean mNfcState) {
    if (mNfcState) {
      return getDrawable(R.drawable.ic_nfc_on);
    } else {
      return getDrawable(R.drawable.ic_nfc_off);
    }
  }

  private void toggleNfc(boolean isChecked) {
    String[] toggleNFCParams = new String[1];
    toggleNFCParams[0] = "<wap-provisioningdoc>\n"
        + "  <characteristic type=\"Profile\">\n"
        + "    <parm name=\"ProfileName\" value=\"ToggleNFC\"/>\n"
        + "    <characteristic type=\"WirelessMgr\" version=\"4.3\">\n"
        + "      <parm name=\"emdk_name\" value=\"\"/>\n"
        + "      <parm name=\"NFCState\" value=\"" + (isChecked ? 1 : 2) + "\"/>\n"
        + "    </characteristic>\n"
        + "  </characteristic>\n"
        + "</wap-provisioningdoc>";

    new ProcessProfile(this).execute(toggleNFCParams);
  }

  private class ProcessProfile extends AsyncTask<String, Void, EMDKResults> {

    private Context context;

    ProcessProfile(Context context) {
      this.context = context;
    }

    @Override
    protected EMDKResults doInBackground(String... params) {
      // Execute Profile
      return App.mProfileManager.processProfile(PROFILE_NAME, ProfileManager.PROFILE_FLAG.SET, params);
    }

    @Override
    protected void onPostExecute(EMDKResults results) {
      super.onPostExecute(results);
      // Log Result
      Log.i(TAG, "Profile Manager Result: " + results.statusCode + " | " + results.extendedStatusCode);
      //
      chatHeadImage.postDelayed(() -> {
        Glide.with(context)
            .load(setToggleButtonState(getNfcState()))
            .dontAnimate()
            .into(chatHeadImage);
        mNfcState = getNfcState();
      }, 2500);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mToggleNfcView != null) mWindowManager.removeView(mToggleNfcView);
  }

  private class SingleTapConfirm extends SimpleOnGestureListener {

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
      return true;
    }
  }
}
