package com.cf.supervideolibrary.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class Tools {
	public static int getScreenWidth(Context context) {    
		WindowManager manager = (WindowManager) context    
				.getSystemService(Context.WINDOW_SERVICE);    
		Display display = manager.getDefaultDisplay();    
		return display.getWidth();    
	}    
	//获取屏幕的高度    
	public static int getScreenHeight(Context context) {    
		WindowManager manager = (WindowManager) context    
				.getSystemService(Context.WINDOW_SERVICE);    
		Display display = manager.getDefaultDisplay();    
		return display.getHeight();    
	} 
	/**
	 * 隐藏虚拟键
	 * @param context
	 */
	 public static void toggleHideyBar(Activity context) {

	        // BEGIN_INCLUDE (get_current_ui_flags)
	        // BEGIN_INCLUDE (get_current_ui_flags)
	        // getSystemUiVisibility() gives us that bitfield.
	        int uiOptions = context.getWindow().getDecorView().getSystemUiVisibility();
	        int newUiOptions = uiOptions;
	        // END_INCLUDE (get_current_ui_flags)
	        // BEGIN_INCLUDE (toggle_ui_flags)
	        boolean isImmersiveModeEnabled =
	                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
	        if (isImmersiveModeEnabled) {
	            Log.i("TAG", "Turning immersive mode mode off. ");
	        } else {
	            Log.i("TAG", "Turning immersive mode mode on.");
	        }

	        // Navigation bar hiding:  Backwards compatible to ICS.
	        if (Build.VERSION.SDK_INT >= 14) {
	            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
	        }

	        // Status bar hiding: Backwards compatible to Jellybean
	        if (Build.VERSION.SDK_INT >= 16) {
	            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
	        }

	        // Immersive mode: Backward compatible to KitKat.
	        // Note that this flag doesn't do anything by itself, it only augments the behavior
	        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
	        // all three flags are being toggled together.
	        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
	        // Sticky immersive mode differs in that it makes the navigation and status bars
	        // semi-transparent, and the UI flag does not get cleared when the user interacts with
	        // the screen.
	        if (Build.VERSION.SDK_INT >= 18) {
	            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
	        }

	        context.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
	        //END_INCLUDE (set_ui_flags)
	    }
	 
	 /**
	  * 通过反射，获取包含虚拟键的整体屏幕宽度
	  *
	  * @return
	  */
	 public static int getHasVirtualKeyWidth(Context context) {
	     int width = 0;
	     Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
	     DisplayMetrics dm = new DisplayMetrics();
	     @SuppressWarnings("rawtypes")
	     Class c;
	     try {
	         c = Class.forName("android.view.Display");
	         @SuppressWarnings("unchecked")
	         Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
	         method.invoke(display, dm);
	         width = dm.widthPixels;
	     } catch (Exception e) {
	         e.printStackTrace();
	     }
	     return width;
	 }
	 /**
	  * 通过反射，获取包含虚拟键的整体屏幕高度
	  *
	  * @return
	  */
	 public static int getHasVirtualKeyHeight(Context context) {
		 int height = 0;
		 Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
		 DisplayMetrics dm = new DisplayMetrics();
		 @SuppressWarnings("rawtypes")
		 Class c;
		 try {
			 c = Class.forName("android.view.Display");
			 @SuppressWarnings("unchecked")
			 Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
			 method.invoke(display, dm);
			 height = dm.heightPixels;
		 } catch (Exception e) {
			 e.printStackTrace();
		 }
		 return height;
	 }
}
