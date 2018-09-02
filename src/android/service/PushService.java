package com.lmr.screenshare.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import com.lmr.screenshare.MainActivity;
import com.lmr.screenshare.R;
import com.tencent.rtmp.ITXLivePushListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePushConfig;
import com.tencent.rtmp.TXLivePusher;

public class PushService extends Service implements ITXLivePushListener {

    private TXLivePushConfig mLivePushConfig;
    private TXLivePusher mLivePusher;
    private Bitmap mBitmap;

    public PushService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLivePusher = new TXLivePusher(this);
        mLivePushConfig = new TXLivePushConfig();
        mLivePusher.setConfig(mLivePushConfig);
        //水印
        mBitmap = decodeResource(getResources(), R.drawable.logo);
    }

    private Bitmap decodeResource(Resources resources, int id) {
        TypedValue value = new TypedValue();
        resources.openRawResource(id, value);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inTargetDensity = value.density;
        return BitmapFactory.decodeResource(resources, id, opts);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startPublishRtmp(intent);
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPublishRtmp();
    }

    private  boolean startPublishRtmp(Intent intent) {
        String rtmpUrl = intent.getStringExtra("RTMP_URL");
        if (TextUtils.isEmpty(rtmpUrl) || (!rtmpUrl.trim().toLowerCase().startsWith("rtmp://"))) {
            Toast.makeText(getApplicationContext(), "推流地址不合法，目前支持rtmp推流!", Toast.LENGTH_SHORT).show();
            return false;
        }
        mLivePushConfig.setWatermark(mBitmap, 0.02f, 0.05f, 0.2f);
        int customModeType = 0;
        mLivePushConfig.setCustomModeType(customModeType);
        mLivePusher.setPushListener(this);
        mLivePushConfig.enableNearestIP(false);
        mLivePushConfig.setVideoResolution(TXLiveConstants.VIDEO_RESOLUTION_TYPE_960_540);
//        mLivePusher.setVideoQuality(TXLiveConstants.VIDEO_QUALITY_HIGH_DEFINITION,true,true);
        mLivePushConfig.setConnectRetryCount(1);
        mLivePusher.setConfig(mLivePushConfig);
        mLivePusher.startScreenCapture();
        mLivePusher.stopBGM();
        mLivePusher.startPusher(rtmpUrl.trim());
//        Toast.makeText(getApplicationContext(), "开启屏幕共享！", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void beginPublishRtmp(){
        final Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION_UPDATEUI);
        Log.e("sign","success");
        intent.putExtra("sign", 1);
        getApplicationContext().sendBroadcast(intent);
    }

    private void stopPublishRtmp() {
        mLivePusher.stopBGM();
        mLivePusher.stopCameraPreview(true);
        mLivePusher.stopScreenCapture();
        mLivePusher.setPushListener(null);
        mLivePusher.stopPusher();
        if(mLivePushConfig != null) {
            mLivePushConfig.setPauseImg(null);
        }

        final Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION_UPDATEUI);
        intent.putExtra("sign", -1);
        getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onPushEvent(int event, Bundle param) {
        String msg = param.getString(TXLiveConstants.EVT_DESCRIPTION);
        Log.e("event",event+"是");
        if(event!=11017){
//            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
        //错误还是要明确的报一下
        if (event < 0) {
            if(event == TXLiveConstants.PUSH_ERR_OPEN_CAMERA_FAIL || event == TXLiveConstants.PUSH_ERR_OPEN_MIC_FAIL){
                stopPublishRtmp();
                stopSelf();
            }
        }
        if(event == TXLiveConstants.PUSH_EVT_PUSH_BEGIN){
            beginPublishRtmp();
        }

        if (event == TXLiveConstants.PUSH_ERR_NET_DISCONNECT) {
            stopPublishRtmp();
            stopSelf();
        }
        else if (event == TXLiveConstants.PUSH_WARNING_HW_ACCELERATION_FAIL) {
            mLivePushConfig.setHardwareAcceleration(TXLiveConstants.ENCODE_VIDEO_SOFTWARE);
            mLivePusher.setConfig(mLivePushConfig);
        }
        else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_UNSURPORT) {
            stopPublishRtmp();
            stopSelf();
        }
        else if (event == TXLiveConstants.PUSH_ERR_SCREEN_CAPTURE_START_FAILED) {
            stopPublishRtmp();
            stopSelf();
        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_RESOLUTION) {

        } else if (event == TXLiveConstants.PUSH_EVT_CHANGE_BITRATE) {
        } else if (event == TXLiveConstants.PUSH_WARNING_NET_BUSY) {
            Toast.makeText(getApplicationContext(), "当前网络质量很糟糕，建议您拉近离路由器的距离，避免WiFi穿墙！", Toast.LENGTH_SHORT).show();
        } else if (event == TXLiveConstants.PUSH_EVT_START_VIDEO_ENCODER) {

        }
    }

    @Override
    public void onNetStatus(Bundle status) {

    }


}
