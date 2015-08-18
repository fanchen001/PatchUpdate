package com.fanchen.update.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.fanchen.update.R;
import com.fanchen.update.jni.PatchUpdate;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.widget.RemoteViews;
import android.widget.Toast;

/***
 * 升级服务
 * 
 * 
 */
public class UpdateService extends Service {
	/******** download progress step *********/
	private static final int down_step_custom = 3;

	private static final int TIMEOUT = 10 * 1000;// 超时
	private static String down_url;
	private static final int DOWN_OK = 1;
	private static final int DOWN_ERROR = 0;

	private String app_name;
	private File updateFile;

	private NotificationManager notificationManager;
	private Notification notification;
	private RemoteViews contentView;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * 方法描述：onStartCommand方法
	 * 
	 * @param Intent
	 *            intent, int flags, int startId
	 * @return int
	 * @see UpdateService
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		app_name = intent.getStringExtra("Key_App_Name");
		down_url = intent.getStringExtra("Key_Down_Url");
		// create file,应该在这个地方加一个返回值的判断SD卡是否准备好，文件是否创建成功，等等！
		if (createFile(app_name,getApplication())) {
			createNotification();
			createThread();
		} else {
			Toast.makeText(this, "未插入sd卡", Toast.LENGTH_SHORT).show();
			/*************** stop service ************/
			stopSelf();
		}
		return super.onStartCommand(intent, flags, startId);
	}
	/** 
	* 方法描述：createFile方法
	* @param   String app_name
	* @return 
	* @see FileUtil
	*/
	public  boolean createFile(String app_name,Context context) {
		if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment.getExternalStorageState())) {
			File updateDir = new File(Environment.getExternalStorageDirectory()+ "/android/data/" + context.getPackageName() +"/update/");
			if(!updateDir.exists()){
				updateDir.mkdirs();
			}
			updateFile = new File(updateDir , app_name );
			if (!updateDir.exists()) {
				updateDir.mkdirs();
			}
			if (!updateFile.exists()) {
				try {
					updateFile.createNewFile();
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				return true;
			}
		}
		return false;
	}
	/**
	 * 安装一个应用程序
	 * 
	 * @param context
	 * @param apkfile
	 */
	public  void installApplication(Context context, File apkfile) {
		Intent intent = new Intent();
		intent.setAction("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.fromFile(apkfile),
				"application/vnd.android.package-archive");
		context.startActivity(intent);
	}

	/********* update UI ******/
	private final Handler handler = new Handler() {
		@SuppressWarnings("deprecation")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DOWN_OK:
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notification.setLatestEventInfo(UpdateService.this, app_name,"下载完成", null);
				/********* 下载完成，点击安装 ***********/
				if(updateFile != null){
					File oldFile = new File("/data/app/"+getApplication().getPackageName()+"-1.apk");
					if(!oldFile.exists()){
						oldFile = new File("/data/app/"+getApplication().getPackageName()+"-2.apk");
					}
					File newDir = new File(Environment.getExternalStorageDirectory() + "/android/data/" + getApplication().getPackageName() + "/update/");
					if(!newDir.exists()){
						newDir.mkdirs();
					}
					File newFile = new File(newDir, SystemClock.currentThreadTimeMillis() + ".apk");
					PatchUpdate.update(oldFile.getAbsolutePath(), newFile.getAbsolutePath(), updateFile.getAbsolutePath());
					installApplication(getApplication(),newFile);
				}
				/***** 安装APK ******/
				/*** stop service *****/
				stopSelf();
				break;

			case DOWN_ERROR:
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notification.setLatestEventInfo(UpdateService.this, app_name,"下载失败", null);

				/*** stop service *****/
				stopSelf();
				break;

			default:
				break;
			}
		}
	};

	/**
	 * 方法描述：createThread方法, 开线程下载
	 * 
	 * @param
	 * @return
	 * @see UpdateService
	 */
	public void createThread() {
		new DownLoadThread().start();
	}

	private class DownLoadThread extends Thread {
		@Override
		public void run() {
			if(updateFile == null ){
				return ;
			}
			Message message = new Message();
			try {
				long downloadSize = downloadUpdateFile(down_url,updateFile.toString());
				if (downloadSize > 0) {
					message.what = DOWN_OK;
					handler.sendMessage(message);
				}
			} catch (Exception e) {
				e.printStackTrace();
				message.what = DOWN_ERROR;
				handler.sendMessage(message);
			}
		}
	}

	/**
	 * 方法描述：createNotification方法
	 * 
	 * @param
	 * @return
	 * @see UpdateService
	 */
	@SuppressWarnings("deprecation")
	public void createNotification() {
		
		notification = new Notification(
				R.drawable.ic_launcher,//应用的图标
				app_name + "正在下载...",
				System.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT; 
		 /*** 自定义  Notification 的显示****/		 
		contentView = new RemoteViews(getPackageName(),R.layout.notification_item);
		contentView.setTextViewText(R.id.notificationTitle, app_name + "正在下载...");
		contentView.setTextViewText(R.id.notificationPercent, "0%");
		contentView.setProgressBar(R.id.notificationProgress, 100, 0, false);
		notification.contentView = contentView;
		
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(R.layout.notification_item, notification);
	}

	/***
	 * down file
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public long downloadUpdateFile(String down_url, String file)
			throws Exception {

		int down_step = down_step_custom;// 提示step
		int totalSize;// 文件总大小
		int downloadCount = 0;// 已经下载好的大小
		int updateCount = 0;// 已经上传的文件大小

		InputStream inputStream;
		OutputStream outputStream;

		URL url = new URL(down_url);
		HttpURLConnection httpURLConnection = (HttpURLConnection) url
				.openConnection();
		httpURLConnection.setConnectTimeout(TIMEOUT);
		httpURLConnection.setReadTimeout(TIMEOUT);
		// 获取下载文件的size
		totalSize = httpURLConnection.getContentLength();

		if (httpURLConnection.getResponseCode() == 404) {
			throw new Exception("fail!");
			// 这个地方应该加一个下载失败的处理，但是，因为我们在外面加了一个try---catch，已经处理了Exception,
			// 所以不用处理
		}

		inputStream = httpURLConnection.getInputStream();
		outputStream = new FileOutputStream(file, false);// 文件存在则覆盖掉

		byte buffer[] = new byte[1024];
		int readsize = 0;

		while ((readsize = inputStream.read(buffer)) != -1) {

			outputStream.write(buffer, 0, readsize);
			downloadCount += readsize;// 时时获取下载到的大小
			/*** 每次增张3% **/
			if (updateCount == 0 || (downloadCount * 100 / totalSize - down_step) >= updateCount) {
				updateCount += down_step;
				// 改变通知栏
				contentView.setTextViewText(R.id.notificationPercent,updateCount + "%");
				contentView.setProgressBar(R.id.notificationProgress, 100,updateCount, false);
				notification.contentView = contentView;
				notificationManager.notify(R.layout.notification_item,notification);
			}
		}
		if (httpURLConnection != null) {
			httpURLConnection.disconnect();
		}
		inputStream.close();
		outputStream.close();
		return downloadCount;
	}

}