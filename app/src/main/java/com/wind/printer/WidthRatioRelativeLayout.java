package com.wind.printer;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;


public class WidthRatioRelativeLayout extends RelativeLayout {
	private float mRatio = 1f;

	public WidthRatioRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public WidthRatioRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public WidthRatioRelativeLayout(Context context) {
		super(context);
	}

	private void init(AttributeSet attrs) {
		TypedArray typeArray = getContext().obtainStyledAttributes(attrs,
				R.styleable.RatioLayout);
		mRatio = typeArray.getFloat(R.styleable.RatioLayout_ratio, 1f);
		typeArray.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
				getDefaultSize(0, heightMeasureSpec));

		// Children are just made to fill our space.
		int childWidthSize = getMeasuredWidth();
		// 高度和宽度一样
		widthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize,
				MeasureSpec.EXACTLY);
		heightMeasureSpec = MeasureSpec.makeMeasureSpec(
				Math.round(childWidthSize * mRatio), MeasureSpec.EXACTLY);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

}
