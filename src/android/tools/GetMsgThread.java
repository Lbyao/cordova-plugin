package com.lmr.screenshare.tools;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 获取后台发来的消息的线程
 */
public class GetMsgThread implements Runnable {
	private static final int GETMSG_SUCCESS=0x123;
	private static final int GETMSG_FAILD=0x122;
	private static final int GETMSG_SERVER_ERROR=0x121;
	private String urlStr;
	private Map<String,Object> paramMap;
	private Handler mHandler;
	public GetMsgThread(Handler handler, String urlStr, Map<String,Object> paramMap){
		super();
		this.urlStr=urlStr;
		this.paramMap=paramMap;
		this.mHandler=handler;
	}
	@Override
	public void run(){
		HttpClient httpClient=CustomerHttpClient.getHttpClient();
		HttpPost post=new HttpPost(urlStr);
		List<NameValuePair> params=new ArrayList<NameValuePair>();
		if(paramMap!=null){
			for(String key: paramMap.keySet()){
				params.add(new BasicNameValuePair(key,paramMap.get(key).toString()));
			}
		}
		try {
			post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse response=httpClient.execute(post);
			if(response.getStatusLine().getStatusCode()==200){
				String result= EntityUtils.toString(response.getEntity());
				Message msg=new Message();
				msg.what=GETMSG_SUCCESS;
				msg.obj=result;
				Log.v("aaa",result);
				mHandler.sendMessage(msg);
			}else{
				Message msg=new Message();
				msg.what=GETMSG_FAILD;
				mHandler.sendMessage(msg);
			}
		} catch (Exception e) {
			Message msg=new Message();
			msg.what=GETMSG_FAILD;
			mHandler.sendMessage(msg);
		}
	}
}
