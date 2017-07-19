package com.cf.supervideolibrary;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.cf.supervideolibrary.utils.DateTools;
import com.cf.supervideolibrary.utils.NetUtils;
import com.cf.supervideolibrary.utils.PixelUtil;
import com.cf.supervideolibrary.utils.Tools;

import java.util.Timer;
import java.util.TimerTask;

public class SuperPlayer extends RelativeLayout {

	private Context context;
	private Activity activity;
	private View contentView;

	private static final int MESSAGE_FADE_OUT = 2;
	private int defaultTimeout = 3000;

	/** 视频路径 */
	private String url;
	/** 视频播放控件 */
	private VideoView mVideoView;
	/** 亮度布局 */
	private View mVolumeBrightnessLayout;
	/** 快进快退布局 */
	private View operation_progress;
	/** 当前时间和总时间 */
	private TextView geture_tv_progress_time;
	/** 亮度或音量图标 */
	private ImageView mOperationBg;
	/** 快进快退图标 */
	private ImageView iv_progress;
	/** 音量管理器 */
	private AudioManager mAudioManager;
	/** 静音控制器 */
	private ImageButton ibtn_volume;
	/** 最大声音 */
	private int mMaxVolume;
	/** 当前声音 */
	private int mVolume = -1;
	/** 是否静音 */
	private boolean isMute = false;
	/** 当前亮度 */
	private float mBrightness = -1f;
	/** 手势 */
	private GestureDetector mGestureDetector;
	/** 全屏控制器 */
	private ImageButton fullScreenButton;
	/** 视频加载布局 */
	private View mLoadingView;
	/** 屏幕宽度 */
	private int screenWidth;
	/** 屏幕高度 */
	private int screenHeight;
	//记录播放的位置
	private int position;
	private TextView operation_tv; 
	private int index;
	private boolean isFastOrBack = false;
	private RelativeLayout rl_top;
	private View layout_controller;
	private ImageButton play_pause;
	private TextView current_time;
	private TextView total_time;
	private SeekBar seekBar;
	private int mDuration;
	private boolean isShowing = true;
	private Timer timer = null;
	private final static int WHAT = 0;
	protected int volume;
	private OrientationEventListener mOrientationListener;
	private boolean fullScreenOnly = false;//是否只全屏播放   
	private boolean isShowCenterControl = false;// 是否显示中心控制器
	private boolean isSupportGesture = false;//是否至此手势操作，false ：小屏幕的时候不支持，全屏的支持；true : 小屏幕还是全屏都支持
	private boolean isPrepare = false;// 是否已经初始化播放
	private boolean portrait;
	private RelativeLayout rl_video_box;
	private ImageView iv_back;
	private TextView tv_video_name;
	private int currentPosition;
	private TextView tv_continue;
	private View view_tip_control;
	private View view_center_control;
	private ImageView iv_center_play;

	private boolean isNetListener = true;// 是否添加网络监听 (默认是监听)
	// 网络监听回调
	private NetChangeReceiver netChangeReceiver;
	private OnNetChangeListener onNetChangeListener;

	private OnInfoListener onInfoListener;
	private OnPreparedListener onPreparedListener;
	private OnCompleteListener onCompleteListener;
	private OnErrorListener onErrorListener;

	private Handler handler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case WHAT:
				// 如果正在播放，每1毫秒更新一次进度条
				if (mVideoView != null && mVideoView.isPlaying()) {
					int current = mVideoView.getCurrentPosition();
					if (current > 0) {
						seekBar.setProgress(current);
						current_time.setText(DateTools.generateTime(current));
					} else {
						seekBar.setProgress(0);
					}
					int percent = mVideoView.getBufferPercentage();
					seekBar.setSecondaryProgress(percent*mDuration/100);
				}

				break;
			}
		};
	};

	public SuperPlayer(Context context) {
		this(context, null);
	}

	public SuperPlayer(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SuperPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.context = context;
		activity = (Activity) this.context;
		//初始化控件
		initView();
		//设置监听
		setListener();
	}
	/**
	 * 设置视频名称
	 * @param title
	 * @return
	 */
	public SuperPlayer setTitle(String title) {
		tv_video_name.setText(title);
		return this;
	}
	/**
	 * 设置监听
	 */
	private void setListener() {
		mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				isPrepare = false;
				mLoadingView.setVisibility(View.GONE);
				if (onErrorListener != null) {
					onErrorListener.onError(what, extra);
				}
				return true;
			}
		});

		//设置播放完毕监听
		mVideoView.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				mVideoView.seekTo(0);   //转到第一帧
				mVideoView.start();     //开始播放
				if (onCompleteListener != null) {
					onCompleteListener.onComplete();
				}
			}
		});
		mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {

			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				switch (what) {
				case MediaPlayer.MEDIA_INFO_BUFFERING_START:
					// 开始缓存，暂停播放
					if (mVideoView != null && mVideoView.isPlaying() ) {
						mVideoView.pause();
					}
					mLoadingView.setVisibility(View.VISIBLE);
					break;
				case MediaPlayer.MEDIA_INFO_BUFFERING_END:
					// 缓存完成，继续播放
					if (mVideoView != null){
						mVideoView.start();
					}
					mLoadingView.setVisibility(View.GONE);
					break;
				}
				if (onInfoListener != null) {
					onInfoListener.onInfo(what, extra);
				}
				return true;
			}
		});
		//设置缓冲监听 
		rl_video_box.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mGestureDetector.onTouchEvent(event)){
					return true;
				}
				// 处理手势结束
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_UP:
					endGesture();
					break;
				}
				return true;
			}
		});

		play_pause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//暂停或播放
				playOrPause();
				show(defaultTimeout);
			}
		});

		ibtn_volume.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!isMute){
					volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
					ibtn_volume.setBackgroundResource(R.mipmap.icon_volume2);
					isMute = true;
				} else {
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
					ibtn_volume.setBackgroundResource(R.mipmap.icon_volume1);
					isMute = false;
				}
				show(defaultTimeout);
			}
		});

		fullScreenButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				toggleFullScreen();
			}
		});


		iv_back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!fullScreenOnly && !portrait) {
					activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				} else {
					activity.finish();
				}

			}
		});

		tv_continue.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				isNetListener = false;// 取消网络的监听
				view_tip_control.setVisibility(View.GONE);
				play(url, position);

			}
		});

		iv_center_play.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playOrPause();
				show(defaultTimeout);
			}
		});

	}

	/**
	 * 初始化控件
	 */
	private void initView() {
		contentView = View.inflate(context, R.layout.view_super_player, this);
		rl_video_box = (RelativeLayout) contentView.findViewById(R.id.rl_video_box);

		mVideoView = (VideoView) contentView.findViewById(R.id.videoView);
		mVolumeBrightnessLayout = contentView.findViewById(R.id.operation_volume_brightness);
		operation_progress = contentView.findViewById(R.id.operation_progress);
		geture_tv_progress_time = (TextView) contentView.findViewById(R.id.geture_tv_progress_time);
		mOperationBg = (ImageView) contentView.findViewById(R.id.operation_bg);
		operation_tv = (TextView) contentView.findViewById(R.id.operation_tv);
		iv_progress = (ImageView) contentView.findViewById(R.id.iv_progress);
		mLoadingView = contentView.findViewById(R.id.video_loading);
		rl_top = (RelativeLayout) contentView.findViewById(R.id.rl_top);
		iv_back = (ImageView) contentView.findViewById(R.id.iv_back);
		tv_video_name = (TextView) contentView.findViewById(R.id.tv_video_name);
		view_tip_control = contentView.findViewById(R.id.view_tip_control);
		tv_continue = (TextView) contentView.findViewById(R.id.tv_continue);
		view_center_control = contentView.findViewById(R.id.view_center_control);
		iv_center_play = (ImageView) contentView.findViewById(R.id.iv_center_play);

		layout_controller = contentView.findViewById(R.id.layout_controller);
		play_pause = (ImageButton) contentView.findViewById(R.id.play_pause);
		current_time = (TextView) contentView.findViewById(R.id.current_time);
		total_time = (TextView) contentView.findViewById(R.id.total_time);
		fullScreenButton = (ImageButton) contentView.findViewById(R.id.fullScreenButton);
		seekBar = (SeekBar) contentView.findViewById(R.id.seekbar);
		ibtn_volume = (ImageButton) contentView.findViewById(R.id.ibtn_volume);


		// 为进度条添加进度更改事件
		seekBar.setOnSeekBarChangeListener(change);
		// 获得AudioManager
		mAudioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		//手势监听
		mGestureDetector = new GestureDetector(activity, new MyGestureListener());

		/**
		 * 监听手机重力感应的切换屏幕的方向
		 */
		mOrientationListener = new OrientationEventListener(activity) {
			@Override
			public void onOrientationChanged(int orientation) {
				if (orientation >= 0 && orientation <= 30 || orientation >= 330
						|| (orientation >= 150 && orientation <= 210)) {
					// 竖屏
					if (portrait) {
						activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						mOrientationListener.disable();
					}
				} else if ((orientation >= 90 && orientation <= 120)
						|| (orientation >= 240 && orientation <= 300)) {
					if (!portrait) {
						activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						mOrientationListener.disable();
					}
				}
			}
		};


		if (fullScreenOnly) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		portrait = getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

		if(!portrait){
			setFullScreen(true);
		}
		hideAll();
	}

	/**
	 * 开始播放
	 *
	 * @param url 播放视频的地址
	 */
	public void play(String url) {
		this.url = url;
		play(url, 0);
	}

	/**
	 * @param url             开始播放(可播放指定位置)
	 * @param currentPosition 指定位置的大小
	 * @see （一般用于记录上次播放的位置或者切换视频源）
	 */
	public void play(String url, int currentPosition){
		this.url = url;
		if (!isNetListener) {// 如果设置不监听网络的变化，则取消监听网络变化的广播
			unregisterNetReceiver();
		} else {
			// 注册网路变化的监听
			registerNetReceiver();
		}
		/*if (videoView != null) {
            release();
        }*/
		if (isNetListener && (NetUtils.getNetworkType(activity) == 2 || NetUtils
				.getNetworkType(activity) == 4)) {// 手机网络的情况下
			view_tip_control.setVisibility(View.VISIBLE);
			return;
		} 

		if (url.startsWith("http:")){
			mVideoView.setVideoURI(Uri.parse(url));
		}else {
			mVideoView.setVideoPath(url);
		}

		mVideoView.start();
		mLoadingView.setVisibility(View.VISIBLE);
		//设置准备完毕监听
		mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mediaPlayer) {
				isPrepare = true;
				mLoadingView.setVisibility(View.GONE);

				mDuration = mVideoView.getDuration();
				// 设置进度条的最大进度为视频流的最大播放时长
				if(mDuration>0){
					seekBar.setMax(mDuration);
					total_time.setText(DateTools.generateTime(mDuration));
				}

				if (onPreparedListener != null) {
					onPreparedListener.onPrepared();
				}
			}
		});

		// 初始化定时器
		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				handler.sendEmptyMessage(WHAT);
			}
		}, 0, 1000);

	}

	/**
	 * 隐藏全部的控件
	 */
	private void hideAll() {
		mVolumeBrightnessLayout.setVisibility(View.GONE);
		operation_progress.setVisibility(View.GONE);
		layout_controller.setVisibility(View.GONE);
		rl_top.setVisibility(View.GONE);
		view_center_control.setVisibility(View.GONE);
	}

	/**
	 * @param timeout
	 */
	private void show(int timeout) {
		//updatePausePlay();
		mDismissHandler.removeMessages(MESSAGE_FADE_OUT);
		if (timeout != 0) {
			mDismissHandler.sendMessageDelayed(mDismissHandler.obtainMessage(MESSAGE_FADE_OUT),
					timeout);
		}
	}

	/** 手势结束 */
	private void endGesture() {
		mVolume = -1;
		mBrightness = -1f;
		if(isFastOrBack){
			mVideoView.seekTo(index);
			isFastOrBack = false;
		}
		// 隐藏
		mDismissHandler.removeMessages(MESSAGE_FADE_OUT);
		mDismissHandler.sendMessageDelayed(mDismissHandler.obtainMessage(MESSAGE_FADE_OUT),
				defaultTimeout);
	}

	/** 定时隐藏 */
	private Handler mDismissHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_FADE_OUT:
				hideAll();
				break;
			}
		}
	};


	/**
	 * 暂停
	 */
	public void onPause() {
		if(mVideoView.isPlaying()){
			mVideoView.pause();
			isShowing = true;
		} else{
			isShowing = false;
		}
		position = mVideoView.getCurrentPosition();
	}

	public void onResume() {
		if(position >0){
			if(isShowing){
				mVideoView.start();
			}
			mVideoView.seekTo(position);
			position=0;
		}
	}

	/**
	 * 在activity中的onDestroy中需要回调
	 */
	public void onDestroy() {
		unregisterNetReceiver();// 取消网络变化的监听
		mOrientationListener.disable();
		handler.removeCallbacksAndMessages(null);
		if (mVideoView != null)
			mVideoView.stopPlayback();
		//取消定时器
		timer.cancel();
	}

	public boolean onBackPressed() {
		if (!fullScreenOnly && getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			return true;
		}
		return false;
	}

	/**
	 * 设置播放视频的是否是全屏
	 */
	public void toggleFullScreen() {
		if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {// 转小屏
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		} else {// 转全屏
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		updateFullScreenButton();
	}

	/**
	 * 更新全屏按钮
	 */
	private void updateFullScreenButton() {
		if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {// 全屏幕
			fullScreenButton.setImageResource(R.mipmap.btn_half_screen);
		} else {
			fullScreenButton.setImageResource(R.mipmap.btn_full_screen);
		}
	}

	/**
	 * 监听全屏跟非全屏
	 *
	 * @param newConfig
	 */
	public void onConfigurationChanged(final Configuration newConfig) {
		portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;

		//获得屏幕的宽高(加上虚拟键)
		screenWidth = Tools.getHasVirtualKeyWidth(activity);
		screenHeight = Tools.getHasVirtualKeyHeight(activity);

		//Log.i("TAG", "screenWidth="+screenWidth);
		//Log.i("TAG", "screenHeight="+screenHeight);
		//Log.i("TAG", "portrait="+portrait);

		doOnConfigurationChanged(portrait);    
	}

	private void doOnConfigurationChanged(final boolean portrait) {
		if (mVideoView != null && !fullScreenOnly) {
			mOrientationListener.enable();
		}
		setFullScreen(!portrait);
		if (portrait) {//竖屏
			//设置视频屏幕正常显示
			ViewGroup.LayoutParams params = getLayoutParams();
			params.width = screenWidth;
			params.height = PixelUtil.dp2px(200, activity);
			//mVideoView.setLayoutParams(params);
			setLayoutParams(params);
		} else {//横屏
			//设置视频屏幕全屏显示
			ViewGroup.LayoutParams params = getLayoutParams();
			params.width = screenWidth;
			params.height = screenHeight;
			//mVideoView.setLayoutParams(params);
			setLayoutParams(params);
		}
		updateFullScreenButton();
	}
	/**
	 * 	设置屏幕显示
	 * @param fullScreen
	 */
	private void setFullScreen(boolean fullScreen) {
		if (activity != null) {
			WindowManager.LayoutParams attrs = activity.getWindow()
					.getAttributes();
			if (fullScreen) {
				//去掉虚拟按键全屏显示
				activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION 
						|  View.SYSTEM_UI_FLAG_IMMERSIVE); 
				//全屏显示
				attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
				activity.getWindow().setAttributes(attrs);
				activity.getWindow().addFlags(
						WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			} else {
				//虚拟按键竖屏显示
				activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
				//竖屏显示
				attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
				activity.getWindow().setAttributes(attrs);
				activity.getWindow().clearFlags(
						WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
			}
		}

	}

	private OnSeekBarChangeListener change = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// 当进度条停止修改的时候触发
			// 取得当前进度条的刻度
			int progress = seekBar.getProgress();
			if (mVideoView != null && mVideoView.isPlaying()) {
				// 设置当前播放的位置
				mVideoView.seekTo(progress);
			}
			show(defaultTimeout);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			show(3600000);
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {

		}
	};

	/**
	 * 手势监听
	 */

	private class MyGestureListener extends SimpleOnGestureListener {
		private static final float STEP_PROGRESS = 2f;// 设定进度滑动时的步长，避免每次滑动都改变，导致改变过快
		private boolean firstScroll = false;// 每次触摸屏幕后，第一次scroll的标志
		private int GESTURE_FLAG = 0;// 1,调节进度，2，调节音量,3.调节亮度
		private static final int GESTURE_MODIFY_PROGRESS = 1;
		private static final int GESTURE_MODIFY_VOLUME = 2;
		private static final int GESTURE_MODIFY_BRIGHT = 3;

		/** 双击 */
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (!isPrepare) {// 视频没有初始化点击屏幕不起作用
				return false;
			}
			//暂停或播放
			playOrPause();

			return true;
		}
		/**
		 * 单击确认
		 */
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (!isPrepare) {// 视频没有初始化点击屏幕不起作用
				return false;
			}
			//控制器隐藏或显示
			toggleMediaControlsVisiblity();
			return true;
		}

		/**
		 * 按下
		 */
		@Override
		public boolean onDown(MotionEvent e) {
			firstScroll = true;// 设定是触摸屏幕后第一次scroll的标志
			return true;
		}

		/** 滑动 */
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

			if (!isPrepare) {// 视频没有初始化点击屏幕不起作用
				return false;
			}
			//如果是竖屏，禁止调节亮度和音量
			/*int mCurrentOrientation = activity.getResources().getConfiguration().orientation;  
			if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT){
				return false;
			}*/
			if (!isSupportGesture && portrait) {
				return super.onScroll(e1, e2, distanceX, distanceY);
			}

			float mOldX = e1.getX(), mOldY = e1.getY();
			int x = (int) e2.getRawX();
			int y = (int) e2.getRawY();

			if (firstScroll) {// 以触摸屏幕后第一次滑动为标准，避免在屏幕上操作切换混乱
				// 横向的距离变化大则调整进度，纵向的变化大则调整音量
				if (Math.abs(distanceX) >= Math.abs(distanceY)) {
					GESTURE_FLAG = GESTURE_MODIFY_PROGRESS;
				} else {
					if (mOldX > screenWidth * 3.0 / 5) {// 音量
						GESTURE_FLAG = GESTURE_MODIFY_VOLUME;
					} else if (mOldX < screenWidth * 2.0 / 5) {// 亮度
						GESTURE_FLAG = GESTURE_MODIFY_BRIGHT;
					}
				}
			}
			// 如果每次触摸屏幕后第一次scroll是调节进度，那之后的scroll事件都处理音量进度，直到离开屏幕执行下一次操作
			if (GESTURE_FLAG == GESTURE_MODIFY_PROGRESS) {
				mLoadingView.setVisibility(View.GONE);
				operation_progress.setVisibility(View.VISIBLE);
				mVolumeBrightnessLayout.setVisibility(View.GONE);
				isFastOrBack = true;
				if (Math.abs(distanceX) > Math.abs(distanceY)) {// 横向移动大于纵向移动
					if (distanceX >= PixelUtil.dp2px(STEP_PROGRESS, activity)) {// 快退，用步长控制改变速度，可微调
						//快退
						iv_progress.setImageResource(R.mipmap.video_backward);
					} else if (distanceX <= -PixelUtil.dp2px(STEP_PROGRESS, activity)) {// 快进
						//快进
						iv_progress.setImageResource(R.mipmap.video_forward);
					}
					changePregress(x-mOldX);
				}
			}
			// 如果每次触摸屏幕后第一次scroll是调节音量，那之后的scroll事件都处理音量调节，直到离开屏幕执行下一次操作
			else if (GESTURE_FLAG == GESTURE_MODIFY_VOLUME) {
				mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
				operation_progress.setVisibility(View.GONE);
				onVolumeSlide((mOldY - y) / screenHeight);
			}
			// 如果每次触摸屏幕后第一次scroll是调节亮度，那之后的scroll事件都处理亮度调节，直到离开屏幕执行下一次操作
			else if (GESTURE_FLAG == GESTURE_MODIFY_BRIGHT) {
				mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
				operation_progress.setVisibility(View.GONE);
				onBrightnessSlide((mOldY - y) / screenHeight);
			}

			firstScroll = false;// 第一次scroll执行完成，修改标志

			return super.onScroll(e1, e2, distanceX, distanceY);
		}
	}

	/**  
	 * 播放进度  
	 *   
	 * @param percent  
	 */  
	public void changePregress(float percent) {  
		index = 500*(int)percent + mVideoView.getCurrentPosition(); 
		if (index > mVideoView.getDuration()) {  
			index = mVideoView.getDuration();  
		} else if (index < 0) {  
			index = 0;  
		}  
		geture_tv_progress_time.setText(DateTools.generateTime(index) + "/" + DateTools.generateTime(mVideoView.getDuration()));
	} 

	/**
	 * 控制器显示或隐藏
	 */
	private void toggleMediaControlsVisiblity() {
		if (layout_controller.getVisibility()==View.VISIBLE) {
			layout_controller.setVisibility(View.GONE);
			rl_top.setVisibility(View.GONE);
		} else {
			layout_controller.setVisibility(View.VISIBLE);
			rl_top.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * 播放与暂停
	 */
	private void playOrPause(){
		if (mVideoView != null){
			if (mVideoView.isPlaying()) {
				mVideoView.pause();
			} else {
				mVideoView.start();
			}
			updatePausePlay();
		}
	}

	/**
	 * 更新暂停状态的控件显示
	 */
	private void updatePausePlay() {
		//是否显示中心控制
		view_center_control.setVisibility(isShowCenterControl ? View.VISIBLE : View.GONE);

		if (mVideoView.isPlaying()) {
			play_pause.setImageResource(R.mipmap.btn_play);
			iv_center_play.setImageResource(R.mipmap.ic_center_play);
		} else {
			play_pause.setImageResource(R.mipmap.btn_pause);
			iv_center_play.setImageResource(R.mipmap.ic_center_pause);
		}
	}

	/**
	 * 滑动改变声音大小
	 * 
	 * @param percent
	 */
	private void onVolumeSlide(float percent) {
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0)
				mVolume = 0;
		}

		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume)
			index = mMaxVolume;
		else if (index < 0)
			index = 0;

		if (index >= 10) {
			mOperationBg.setImageResource(R.mipmap.volmn_100);
		} else if (index >= 5 && index < 10) {
			mOperationBg.setImageResource(R.mipmap.volmn_60);
		} else if (index > 0 && index < 5) {
			mOperationBg.setImageResource(R.mipmap.volmn_30);
		} else {
			mOperationBg.setImageResource(R.mipmap.volmn_no);
		}
		operation_tv.setText((int) (((double) index / mMaxVolume)*100)+"%");

		// 变更声音
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

	}

	/**
	 * 滑动改变亮度
	 * 
	 * @param percent
	 */
	private void onBrightnessSlide(float percent) {
		if (mBrightness < 0) {
			mBrightness = activity.getWindow().getAttributes().screenBrightness;
			if (mBrightness <= 0.00f)
				mBrightness = 0.50f;
			if (mBrightness < 0.01f)
				mBrightness = 0.01f;
		}

		WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
		lpa.screenBrightness = mBrightness + percent;
		if (lpa.screenBrightness > 1.0f)
			lpa.screenBrightness = 1.0f;
		else if (lpa.screenBrightness < 0.01f)
			lpa.screenBrightness = 0.01f;
		activity.getWindow().setAttributes(lpa);

		operation_tv.setText((int) (lpa.screenBrightness * 100) + "%");
		if (lpa.screenBrightness * 100 >= 90) {
			mOperationBg.setImageResource(R.mipmap.light_100);
		} else if (lpa.screenBrightness * 100 >= 80 && lpa.screenBrightness * 100 < 90) {
			mOperationBg.setImageResource(R.mipmap.light_90);
		} else if (lpa.screenBrightness * 100 >= 70 && lpa.screenBrightness * 100 < 80) {
			mOperationBg.setImageResource(R.mipmap.light_80);
		} else if (lpa.screenBrightness * 100 >= 60 && lpa.screenBrightness * 100 < 70) {
			mOperationBg.setImageResource(R.mipmap.light_70);
		} else if (lpa.screenBrightness * 100 >= 50 && lpa.screenBrightness * 100 < 60) {
			mOperationBg.setImageResource(R.mipmap.light_60);
		} else if (lpa.screenBrightness * 100 >= 40 && lpa.screenBrightness * 100 < 50) {
			mOperationBg.setImageResource(R.mipmap.light_50);
		} else if (lpa.screenBrightness * 100 >= 30 && lpa.screenBrightness * 100 < 40) {
			mOperationBg.setImageResource(R.mipmap.light_40);
		} else if (lpa.screenBrightness * 100 >= 20 && lpa.screenBrightness * 100 < 20) {
			mOperationBg.setImageResource(R.mipmap.light_30);
		} else if (lpa.screenBrightness * 100 >= 10 && lpa.screenBrightness * 100 < 20) {
			mOperationBg.setImageResource(R.mipmap.light_20);
		}

	}

	private int getScreenOrientation() {
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;
		int height = dm.heightPixels;
		int orientation;
		if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
				&& height > width
				|| (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
				&& width > height) {
			switch (rotation) {
			case Surface.ROTATION_0:
				orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				break;
			case Surface.ROTATION_90:
				orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				break;
			case Surface.ROTATION_180:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
				break;
			case Surface.ROTATION_270:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
				break;
			default:
				orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				break;
			}
		} else {
			switch (rotation) {
			case Surface.ROTATION_0:
				orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				break;
			case Surface.ROTATION_90:
				orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				break;
			case Surface.ROTATION_180:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
				break;
			case Surface.ROTATION_270:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
				break;
			default:
				orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				break;
			}
		}

		return orientation;
	}

	/**
	 * 获取当前播放的currentPosition
	 *
	 * @return
	 */
	public int getCurrentPosition() {
		currentPosition = mVideoView.getCurrentPosition();
		return currentPosition;
	}

	/**
	 * 获取视频的总长度
	 *
	 * @return
	 */
	public int getDuration() {
		return mVideoView.getDuration();
	}


	//----------------------------------------------------------------------
	public interface OnCompleteListener {
		void onComplete();
	}

	public interface OnInfoListener {
		void onInfo(int what, int extra);
	}

	public interface OnPreparedListener {
		void onPrepared();
	}

	public interface OnErrorListener {
		void onError(int what, int extra);
	}

	public interface OnNetChangeListener {
		// wifi
		void onWifi();

		// 手机
		void onMobile();

		// 网络断开
		void onDisConnect();

		// 网路不可用
		void onNoAvailable();
	}

	public SuperPlayer onComplete(OnCompleteListener onCompleteListener) {
		this.onCompleteListener = onCompleteListener;
		return this;
	}

	public SuperPlayer onInfo(OnInfoListener onInfoListener) {
		this.onInfoListener = onInfoListener;
		return this;
	}

	public SuperPlayer onPrepared(OnPreparedListener onPreparedListener) {
		this.onPreparedListener = onPreparedListener;
		return this;
	}

	public SuperPlayer onError(OnErrorListener onErrorListener) {
		this.onErrorListener = onErrorListener;
		return this;
	}

	// 网络监听的回调
	public SuperPlayer setOnNetChangeListener(OnNetChangeListener onNetChangeListener) {
		this.onNetChangeListener = onNetChangeListener;
		return this;
	}

	public SuperPlayer playInFullScreen(boolean fullScreenOnly) {
		this.fullScreenOnly = fullScreenOnly;
		if (fullScreenOnly) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		updateFullScreenButton();
		return this;
	}

	/**
	 * 注册网络监听器
	 */
	private void registerNetReceiver() {
		if (netChangeReceiver == null) {
			IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
			netChangeReceiver = new NetChangeReceiver();
			activity.registerReceiver(netChangeReceiver, filter);
		}
	}

	/**
	 * 销毁网络监听器
	 */
	private void unregisterNetReceiver() {
		if (netChangeReceiver != null) {
			activity.unregisterReceiver(netChangeReceiver);
			netChangeReceiver = null;
		}
	}


	/*************** 网络变化监听****************/
	public class NetChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (onNetChangeListener == null) {
				return;
			}
			if (NetUtils.getNetworkType(activity) == 3) {// 网络是WIFI
				view_tip_control.setVisibility(View.GONE);
				onNetChangeListener.onWifi();
				//onResume();
				//updatePausePlay();
			} else if (NetUtils.getNetworkType(activity) == 2
					|| NetUtils.getNetworkType(activity) == 4) {// 网络不是手机网络或者是以太网
				onPause();
				//updatePausePlay();
				view_tip_control.setVisibility(View.VISIBLE);
				mLoadingView.setVisibility(View.GONE);
				onNetChangeListener.onMobile();

			} else if (NetUtils.getNetworkType(activity) == 1) {// 网络链接断开
				onPause();
				onNetChangeListener.onDisConnect();
			} else {
				onNetChangeListener.onNoAvailable();
			}
		}
	}

	/*************************************** 对外调用的方法 ********************/

	/**
	 * 是否显示中心控制器
	 *
	 * @param isShow true ： 显示 false ： 不显示
	 */
	public SuperPlayer showCenterControl(boolean isShow) {
		this.isShowCenterControl = isShow;
		return this;
	}

	/**
	 * 设置播放视频是否有网络变化的监听
	 *
	 * @param isNetListener true ： 监听 false ： 不监听
	 * @return
	 */
	public SuperPlayer setNetChangeListener(boolean isNetListener) {
		this.isNetListener = isNetListener;
		return this;
	}

	/**
	 * 设置小屏幕是否支持手势操作（默认false）
	 *
	 * @param isSupportGesture true : 支持（小屏幕支持，大屏幕支持）
	 *                         false ：不支持（小屏幕不支持,大屏幕支持）
	 * @return
	 */
	public SuperPlayer setSupportGesture(boolean isSupportGesture) {
		this.isSupportGesture = isSupportGesture;
		return this;
	}

	/**
	 * 获得某个控件
	 *
	 * @param ViewId
	 * @return
	 */
	public View getView(int ViewId) {
		return activity.findViewById(ViewId);
	}

	/**
	 * 设置只能全屏
	 *
	 * @param fullScreenOnly true ： 只能全屏 false ： 小屏幕显示
	 */
	public void setFullScreenOnly(boolean fullScreenOnly) {
		this.fullScreenOnly = fullScreenOnly;
		if (fullScreenOnly) {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else {
			activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		updateFullScreenButton();
	}
}

