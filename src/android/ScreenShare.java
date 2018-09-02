package com.lmr.screenshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.lmr.screenshare.service.PushService;
import com.lmr.screenshare.tools.Constants;
import com.lmr.screenshare.tools.GetMsgThread;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Administrator on 2017-11-22.
 */

public class ScreenShare extends CordovaPlugin {

    private Activity activity;

    public static final String ACTION_UPDATEUI = "action.updateUI";
    //    192.168.137.1

    private int SENDTIME = 1000;
    private int TIME = 5000;
    private long stopTime = -5000;

    private Intent pushIntent;
    private String screenShareCheckURLPath;
    private String screenListenerURLPath;
    private String upscreenShareTimeURLPath;

    private String meetingId;
    private String orgId;
    //    自定义广播接收器
    private UpdateUIBroadcastReceiver broadcastReceiver;

    private Boolean flag = false;
    private String ip = "172.16.22.74";
    private String port = "8080";

    private String url = "http://" + ip + ":8087/x5/UI2/wzhhy/index.w";
    private String RTMP_URL = "rtmp://" + ip + "/live/abc";
//    rtmp://172.16.22.74/live/abc

    private CallbackContext callbackContext;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();
        //设置访问后台的链接
        screenListenerURLPath = "/wzhhy/login/screenShareParameter.html";
        upscreenShareTimeURLPath = "/wzhhy/login/upScreenShareTime.html";

//        pushIntent = new Intent(MainActivity.this, PushService.class);

        Constants.server_URL = "http://" + ip + ":" + port;

        // 动态注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATEUI);
        broadcastReceiver = new UpdateUIBroadcastReceiver();
        activity.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("startScreen")){
            Toast.makeText(activity,args.getString(0),Toast.LENGTH_SHORT).show();
            startShowView();
        }
        return false;
    }

    //开启录屏的方法
    public void startShowView() {
        screenShareCheckURLPath = "/wzhhy/login/checkScreenShare.html?meetId=14&personId=2ebd080e-17b6-48d7-8329-90232574813f";
        startShow();
    }

    //wex5传递过来的ip和port，默认设置的也有
    public Boolean getIPAndPort(String ip, String port) {
        if (ip != null && port != null) {
            flag = true;
            Toast.makeText(activity, ip, Toast.LENGTH_SHORT).show();
            this.ip = ip;
            this.port = port;
        } else {
            flag = false;
        }
        return flag;
    }

    //wex5在进入会议的时候调用这个方法
    public void isScreens() {
        Toast.makeText(activity, "调用播放", Toast.LENGTH_SHORT).show();
        handler.postDelayed(runnable, TIME); //每隔5s执行
    }


    public boolean isPlay() {
        return Constants.isShareScreen;
    }

    private void getMsg(Handler handler, String url) {
        GetMsgThread thread = new GetMsgThread(handler, url, null);
        new Thread(thread).start();
    }


    /**
     * 定义广播接收器（内部类）
     *
     * @author lenovo
     */
    private class UpdateUIBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sign = intent.getIntExtra("sign", -1);
            if (sign == 1) {
                handlerUpShareTime.postDelayed(runnableUpShareTime, SENDTIME); //每隔1s执行
                Toast.makeText(activity, "屏幕共享成功！", Toast.LENGTH_SHORT).show();
            } else {
                Constants.isShareScreen = false;
                Toast.makeText(activity, "屏幕共享失效！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // handler自带方法实现定时器
            try {
                handler.postDelayed(this, TIME);
                getMsg(screenListenerHandler, Constants.server_URL + screenListenerURLPath);
                System.out.println("do...");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("exception...");
            }
        }
    };

    Handler handlerUpShareTime = new Handler();
    Runnable runnableUpShareTime = new Runnable() {
        @Override
        public void run() {
            // handler自带方法实现定时器
            try {
                handlerUpShareTime.postDelayed(runnableUpShareTime, SENDTIME); //每隔1s执行
                getMsg(upScreenShareTimeHandler, Constants.server_URL + upscreenShareTimeURLPath);
                System.out.println("do...");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                System.out.println("exception...");
            }
        }
    };
    //共享时间
    private Handler upScreenShareTimeHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.obj != null) {
                    JSONObject jsonObj = new JSONObject(msg.obj.toString());
                    if (jsonObj.getString("rs").equals("true")) {
                        handlerUpShareTime.postDelayed(runnableUpShareTime, TIME); //每隔5s执行
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    };
    //  监听是否有屏幕共享，在onStart的时候
    Handler screenListenerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.obj != null) {
                    JSONObject jsonObj = new JSONObject(msg.obj.toString());
                    if (jsonObj.getString("rs").equals("true")) {
                        Constants.isShare = true;
                        if (Constants.wantPlay == true && Constants.isShareScreen == false && Constants.isAlertDialog == false && calLastedTime(stopTime) > 5) {
                            Constants.isAlertDialog = true;
                            Constants.wantPlay = false;
                            Constants.isPlay = true;
                            Constants.isAlertDialog = false;
                            play();
                        }
                    } else {
                        Constants.isShare = false;
                        Constants.wantPlay = true;
                        Constants.isAlertDialog = false;
                        if (Constants.isPlay) {
                            Constants.isPlay = false;
                        } else {
                            if (MediaPlayerActivity._instance != null) {
                                MediaPlayerActivity._instance.finish();
                                MediaPlayerActivity._instance = null;
                                Toast.makeText(activity, "连接已断开", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    };
    //    开始推送流的方法
    Handler screenShareHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.obj != null) {
                    JSONObject jsonObj = new JSONObject(msg.obj.toString());
                    if (jsonObj.getString("rs").equals("true")) {
                        Constants.isShareScreen = true;
                        startPush();
                    } else {
                        Toast.makeText(activity, "没有权限或已存在屏幕共享",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    };

    //    View view
//  开始共享屏幕的方法
    public void startShow() {
        if (Constants.isShareScreen) {
            // 1.获取一个对话框的创建器
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            // 设置参数
            builder.setTitle("屏幕共享");// 标题
            builder.setMessage("确认关闭屏幕共享");// 设置提示信息
            builder.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopPush();
                        }
                    });
            builder.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            // 1.获取一个对话框的创建器
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            // 设置参数
            builder.setTitle("屏幕共享");// 标题
            builder.setMessage("确认共享屏幕");// 设置提示信息
            builder.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getMsg(screenShareHandler, Constants.server_URL + screenShareCheckURLPath);
                        }
                    });
            builder.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void startPush() {
        pushIntent.setPackage("com.hnnp.wzhhy.service");
        pushIntent.putExtra("RTMP_URL", RTMP_URL);
        activity.startService(pushIntent);
    }

    private void stopPush() {
        activity.stopService(new Intent(activity, PushService.class));
        handlerUpShareTime.removeCallbacks(runnableUpShareTime);
        Constants.isShare = false;
        Constants.isShareScreen = false;
        stopTime = new Date().getTime();
    }

    private void play() {
        Intent intent = new Intent(activity, MediaPlayerActivity.class);
        intent.putExtra("RTMP_URL", RTMP_URL);
        Log.e("success", "success");
        activity.startActivity(intent);
        Toast.makeText(activity, "播放成功！", Toast.LENGTH_SHORT).show();
    }

    //点击回退键的提醒
    public void onBackPressed() {
        if (Constants.isShareScreen) {
            /// 1.获取一个对话框的创建器
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            // 设置参数
            builder.setTitle("屏幕共享");// 标题
            builder.setMessage("确认关闭屏幕共享");// 设置提示信息
            builder.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopPush();
                            handler.removeCallbacks(runnable);
                            activity.finish();
                        }
                    });
            builder.setNegativeButton("取消",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            handler.removeCallbacks(runnable);
            activity.finish();
        }
    }

    public int calLastedTime(long startDate) {
        long a = new Date().getTime();
        int c = (int) ((a - startDate) / 1000);
        return c;
    }
}
