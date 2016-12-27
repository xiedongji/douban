package com.simon.app.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.htmlparser.jericho.Source;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.simon.app.AppConfig;
import com.simon.app.R;
import com.simon.app.util.ILog;
import com.simon.app.util.ITips;
import com.simon.app.util.UtilNet;

public class ActLogin extends ActBase implements OnClickListener {
	
	private static final int LOGIN_SUCCESS = 0x01;
	private static final int LOGIN_FAIL = 0x02;
	
	private EditText mUsernameET, mPwdET, mCaptchaET;
	private Button mLoginBtn, mExitBtn;
	private ImageView mCaptchaIV;
	private Bitmap  mPicCode;

	private ProgressDialog mProgressDialog;

	@Override
	protected void proLogic() {

	}

	@Override
	protected void initView() {
		setContentView(R.layout.act_login);
		mUsernameET = (EditText) findViewById(R.id.unameET);
		mPwdET = (EditText) findViewById(R.id.pwdET);
		mCaptchaET = (EditText) findViewById(R.id.captchaET);

		mCaptchaIV = (ImageView) findViewById(R.id.captchaIV);

		mLoginBtn = (Button) findViewById(R.id.btnLogin);
		mExitBtn = (Button) findViewById(R.id.btnExit);
	}

	@Override
	protected void setListener() {
		mLoginBtn.setOnClickListener(this);
		mExitBtn.setOnClickListener(this);

		mCaptchaIV.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnLogin:
			final String name = mUsernameET.getText().toString();
			final String pwd = mPwdET.getText().toString();
			

			if ("".equals(name) || "".equals(pwd)) {
				ITips.toast(this, "用户名或者密码不能为空");
				return;
			} else {
				this.login(name, pwd);
			}
			break;

		case R.id.btnExit:
			finish();
			break;

		case R.id.captchaIV:
			this.getCaptcha();
			break;
		}
	}

	// 密码登陆
	private void login(final String name, final String pwd) {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage("正在登陆");
		mProgressDialog.show();

		new Thread() {
			@Override
			public void run() {
				// 发送登陆请求
				@SuppressWarnings("deprecation")
				HttpPost httpPost = new HttpPost(AppConfig.URL_LOGIN);
				
				ILog.show("LOGIN", AppConfig.URL_LOGIN);
				// 拼装数据
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("uname", name));
				nameValuePairs.add(new BasicNameValuePair("pwd", pwd));

				try {
					UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
					httpPost.setEntity(entity);
					// 创建一个浏览器
					DefaultHttpClient client = new DefaultHttpClient();
					// 开始请求
					HttpResponse response = client.execute(httpPost);

					ILog.show("LOGIN","responseCode:"+response.getStatusLine().getStatusCode());
					// 解析成html
					Source jsonRes = new Source(response.getEntity().getContent());
					ILog.show("LOGIN","responseContent:"+jsonRes.toString());
					
					//解析json
					JSONTokener jsonParser = new JSONTokener(jsonRes.toString());
					JSONObject loginRes = (JSONObject) jsonParser.nextValue();
					String code = loginRes.getString("code");
					String msg = loginRes.getString("msg");
					
					ILog.show("LOGIN","responseParse:"+code+":"+msg);
					Message handlerMsg = new Message();
					if ("1".equals(code)) {
						handlerMsg.what = LOGIN_SUCCESS;
						//登陆状态 保存到SP
						SharedPreferences sp = getSharedPreferences(AppConfig.SP_KEY, Context.MODE_PRIVATE);
						Editor editor = sp.edit();
						editor.putString(AppConfig.SESSION_ID, "1");
						editor.commit();
					}else{
						handlerMsg.what = LOGIN_FAIL;
					}
					handlerMsg.obj = msg;
					handler.sendMessage(handlerMsg);

					// 获取登陆成功的cookie
					CookieStore cookie = client.getCookieStore();
					ILog.show("LOGIN","responseCookie:"+cookie);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}

	// 获取图形验证码
	private void getCaptcha() {
		
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				ILog.show("LOGIN", AppConfig.URL_CAPTCHA);
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				mCaptchaIV.setImageBitmap(mPicCode);
			}

			@Override
			protected Void doInBackground(Void... params) {
				try {
					mPicCode = UtilNet.getImage(AppConfig.URL_CAPTCHA);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
			
			
		}.execute();
	}
	
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mProgressDialog.dismiss();
			
			switch (msg.what) {
				case LOGIN_SUCCESS:
					ITips.toast(ActLogin.this, msg.obj.toString());
					redirectTo(ActHome.class);
					break;
				case LOGIN_FAIL:
					ITips.toast(ActLogin.this, msg.obj.toString());
					break;
			}
		}
	};

}
