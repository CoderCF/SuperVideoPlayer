package com.cf.supervideolibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

public class VV extends VideoView {

	public VV(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//两种都可以
//		setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(),widthMeasureSpec),                    
//		getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec));
		int width = getDefaultSize(0, widthMeasureSpec);  
        int height = getDefaultSize(0, heightMeasureSpec);  
        setMeasuredDimension(width, height); 
	}
}
