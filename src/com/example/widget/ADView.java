package com.example.widget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.example.android_service.R;
import com.utils.FileUtils;
import com.utils.SDCardUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

/**
 * A view for advertisement display
 * 
 * @author Jayin Ton
 * 
 */
public class ADView extends ViewFlipper {
	private static final int ADView_Start = 0;
	private static final int ADView_DownloadFinish = 1;
	private static final int ADView_Faild = 2;
	private static final int ADView_Finish = 3;
	private Handler h;
	private String[] urls;
	private Context context;
	// defaultFilePath : /mnt/sdcard/AD/
	private String FolderPath = SDCardUtils.getSDCardPath() + "AD"
			+ File.separator;
	private boolean isCachePic = false;  //default it's no cache the pic

	public ADView(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public ADView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	/**
	 * init the ADView
	 */
	public void init() {
		isCachePic = false;
		h = new AdViewHandler();
		dismiss();
		this.setInAnimation(context, R.anim.push_left_in);
		this.setOutAnimation(context, R.anim.push_left_out);
		this.setAutoStart(true);
	}

	/**
	 * set thr url of ad
	 * 
	 * @param urls
	 */
	public void setURL(String... urls) {
		this.urls = urls;
	}
	
	public void setIsCachePic(boolean cachePic){
		this.isCachePic = cachePic;
	}

	protected void show() {
		this.setVisibility(View.VISIBLE);
	}

	protected void dismiss() {
		this.setVisibility(View.GONE);
	}

	public void addAD(View v) {
		this.addView(v, this.getChildCount());
	}

	/**
	 * 设置缓存图片文件夹名称
	 * 
	 * @param folder
	 *            name
	 * @return true if set ok
	 */
	public boolean setCachefoldeName(String foldername) {
		if (FileUtils.makeDirs(SDCardUtils.getSDCardPath() + foldername
				+ File.separator)) {
			FolderPath = SDCardUtils.getSDCardPath() + foldername
					+ File.separator;
			return true;
		} else {
			return false;
		}
	}

	public void display() {
		if (FileUtils.isFolderExist(FolderPath)) {
			File file = new File(FolderPath);
			String[] files = file.list();
			if (files != null && files.length > 0) {
				for (String fileName : files) {
					Bitmap bitmap = BitmapFactory.decodeFile(FolderPath
							+ fileName);
					View v = LayoutInflater.from(context).inflate(
							R.layout.item_adview, null);
					ImageView iv = (ImageView) v.findViewById(R.id.iv);
					iv.setImageBitmap(bitmap);
					addAD(v);
				}
				show();
			} else {
				goDownload();
			}
		}
	}

	public void goDownload() {
		try {

			new Thread(new Runnable() {
				@Override
				public void run() {
					if (urls == null || urls.length == 0) {
						return;
					} else {
						if (isCachePic && FileUtils.deleteFile(FolderPath)) {
							FileUtils.makeDirs(FolderPath);
						}
						for (int i = 0; i < urls.length; i++) {
							String url = urls[i];
							Log.i("deubg", "download:" + url);
							byte[] data = doownloadPic(url);
							if (data != null) {
								if(isCachePic)saveToFile(data, FileUtils.getFileName(url));
								Message msg = h.obtainMessage();
								msg.obj = BitmapFactory.decodeByteArray(data,
										0, data.length);
								msg.what = ADView_DownloadFinish;
								h.sendMessage(msg);
							}
						}
						h.sendEmptyMessage(ADView_Finish);
					}

				}
			}).start();
		} catch (Exception e) {
            e.printStackTrace();
            //there is no space to save the cache-pic;
		}
	}

	protected boolean saveToFile(byte[] data, String fileName) {
		return FileUtils.writeFile(FolderPath + fileName, data);
	}

	class AdViewHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ADView_Start:
				Log.i("debug", "ADView_Start");
				break;

			case ADView_Faild:
				Log.i("debug", "ADView_Faild");
				break;
			case ADView_DownloadFinish:
				Log.i("debug", "ADView_Finish");
				View v = LayoutInflater.from(context).inflate(
						R.layout.item_adview, null);
				ImageView iv = (ImageView) v.findViewById(R.id.iv);
				iv.setImageBitmap((Bitmap) msg.obj);
				addAD(v);
				break;
			case ADView_Finish: // all download mission finished
				show();
				break;
			default:
				break;
			}
		}
	}

	protected byte[] doownloadPic(String url) {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse response = null;
		byte[] data = null;
		ByteArrayOutputStream bos = null;
		InputStream in = null;
		try {
			response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == 200) {
				in = response.getEntity().getContent();
				bos = new ByteArrayOutputStream();
				int len = -1;
				byte[] buffer = new byte[1024];
				while ((len = in.read(buffer, 0, buffer.length)) != -1) {
					bos.write(buffer, 0, len);
				}
				return bos.toByteArray();
			} else {
				return null;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (client != null) {
				client.getConnectionManager().shutdown();
			}
		}

	}

}
