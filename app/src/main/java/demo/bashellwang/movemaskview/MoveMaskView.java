package demo.bashellwang.movemaskview;

import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 支持滑动设置掩码的组件
 * 
 * @author bashellwang
 *
 */
public class MoveMaskView extends View {
	private Context mContext;
	private Paint mTextPaint, mLinePaint;
	private float cellWidth;
	/**
	 * 总共多少行显示
	 */
	private int totalLines = 1;
	/**
	 * 每一个小方格的宽度
	 */
	private float squareWidth = 120;// 105

	/**
	 * 每一个字母所在区域高度
	 */
	private float cellHeight;
	/**
	 * “*”所在区域 高度
	 */
	private float starCellHeight;
	/**
	 * 文本大小
	 */
	private float fontSize = 60;
	/**
	 * 描边线宽
	 */
	private float lineStrokeSize = 20;

	private char[] LETTERS = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'B', 'C', 'D', 'E', 'G', 'H' };
	private static final String TAG = "MoveMaskView";
	private List<Integer> mMaskIndex = new ArrayList<Integer>();
	private int firstIndex = -1;// 标记掩码的第一个位置
	/**
	 * 需要打掩码的个数
	 */
	private int maskNum = 4;
	/**
	 * 每一行显示的个数
	 */
	private int lettersInLine = 8;
	/**
	 * 标记当前掩码的第一个位置
	 */
	private int currentFirstIndex = -100;
	private int deltHeight = 10;

	private int textPressedColor;// 掩码内文字颜色
	private int textUnpressedColor;// 掩码外文字颜色
	private int unpressedBgColor;// 掩码外背景色
	private int pressedBgColor;// 掩码内背景色
	private int lineColor;// 掩码外框线颜色
	private int starTextColor;// 底部 * 颜色
	int touchIndex = -1;// 记录上一次的触摸点位置

	private String starText;// 掩码下显示的符号
	private int starX;// 记录掩码下符号位置
	private int starY;// 记录掩码下符号位置
	private boolean isTouchFinished = false;// 记录用户是否结束触摸动作

	/**
	 * 暴露一个字母的监听
	 */
	public interface OnTouchFinishedListener {
		void onTouchFinished(List<Integer> mMaskIndex, String maskResult);
	}

	private OnTouchFinishedListener listener;

	public OnTouchFinishedListener getListener() {
		return listener;
	}

	/**
	 * 设置字母更新监听
	 * 
	 * @param listener
	 */
	public void setOnTouchFinishedListener(OnTouchFinishedListener listener) {
		this.listener = listener;
	}

	/**
	 * 设置被打掩码的字符串
	 * 
	 * @param text
	 */
	public void setText(String text) {
		if (text == null) {
			return;
		}
		this.LETTERS = text.toCharArray();
		// 需要重新计算测量
		requestLayout();
	}

	public void setMaskNum(int num) {
		this.maskNum = num;
	}

	public MoveMaskView(Context context) {
		this(context, null);
	}

	public MoveMaskView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MoveMaskView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;

		// 获取自定义属性值
		TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.moveMaskView, 0, 0);
		try {

			textPressedColor = typeArray.getColor(R.styleable.moveMaskView_textPressedColor, 0xa0a0a0);
			textUnpressedColor = typeArray.getColor(R.styleable.moveMaskView_textUnpressedColor, 0x7a7a7a);
			unpressedBgColor = typeArray.getColor(R.styleable.moveMaskView_unpressedBgColor, 0xffffffff);
			pressedBgColor = typeArray.getColor(R.styleable.moveMaskView_pressedBgColor, 0x616060);
			lineColor = typeArray.getColor(R.styleable.moveMaskView_lineColor, 0xd8d8d8);
			starTextColor = typeArray.getColor(R.styleable.moveMaskView_starTextColor, 0x000000);

		} finally {
			typeArray.recycle();
		}

		// 绘制文字
		mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setColor(textUnpressedColor);
		mTextPaint.setStyle(Paint.Style.FILL);
		mTextPaint.setTextSize(fontSize);

		// 描边
		mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mLinePaint.setColor(lineColor);
		mLinePaint.setStyle(Paint.Style.STROKE);
		mLinePaint.setStrokeWidth(1);

	}

	/**
	 * 判断当前触摸点是否在掩码内
	 * 
	 * @param index
	 *            当前触摸点
	 * @param maskIndexs
	 *            掩码id数组
	 * @return 在掩码内，返回在掩码内的索引，否则返回-1
	 */
	private int isInMask(int index, List<Integer> maskIndexs) {
		if (maskIndexs == null || maskIndexs.size() == 0) {
			return -1;
		}
		for (int i = 0; i < maskIndexs.size(); i++) {
			if (index == maskIndexs.get(i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 获取打了掩码之后的字符串
	 * @param firstIndex 第一个掩码位置
	 * @param str 需要被打码的字符串
	 * @param maskNum 掩码个数
	 * @return
	 */
	private String getMaskResultString(int firstIndex, String str, int maskNum) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			if (i >= firstIndex && i < firstIndex + maskNum) {
				sb.append("*");
			} else {
				sb.append(str.charAt(i));
			}
		}
		return sb.toString();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		/**
		 * 优先向后移动，不足则向前移动，获取需要打码的四位
		 */
		mMaskIndex = getMaskIndexsWithFirstIndex(firstIndex, LETTERS, maskNum);
		if (isTouchFinished && listener != null) {
			listener.onTouchFinished(mMaskIndex,getMaskResultString(firstIndex, String.valueOf(LETTERS), maskNum));
			isTouchFinished = false;
		}
		for (int i = 0; i < LETTERS.length; i++) {
			String text = LETTERS[i] + "";
			// 计算文本坐标,考虑多行的情况
			float x = cellWidth / 2.0f - mTextPaint.measureText(text) / 2.0f + (i % lettersInLine) * cellWidth;
			int textHeight = getFontHeight(text, mTextPaint);
			float y = cellHeight / 2.0f + textHeight / 2.0f + (i / lettersInLine) * (cellHeight + starCellHeight);

			float left, top, right, bottom;
			// 需要考虑到线宽,描边时边线有宽度，从线的中间开始绘制线，所以这里要除去线宽的干扰
			if (i % lettersInLine == 0) {
				left = (i % lettersInLine) * cellWidth + lineStrokeSize / 2;
			} else {
				left = (i % lettersInLine) * cellWidth;
			}
			top = (i / lettersInLine) * (cellHeight + starCellHeight) + deltHeight;
			right = ((i % lettersInLine) + 1) * cellWidth;
			bottom = (i / lettersInLine) * (cellHeight + starCellHeight) + cellHeight;

			// 绘制背景,考虑多行
			mTextPaint.setColor(unpressedBgColor);
			canvas.drawRect(left, top, right, bottom, mTextPaint);

			/**
			 * 根据当前被触摸的点绘制背景
			 */
			// 掩码内
			if (currentFirstIndex <= i && i < currentFirstIndex + maskNum) {
				mTextPaint.setColor(pressedBgColor);
				canvas.drawRect(left, top, right, bottom, mTextPaint);
				mTextPaint.setColor(starTextColor);
				starText = "*";
				starX = (int) (cellWidth / 2.0f - mTextPaint.measureText(starText) / 2.0f
						+ (i % lettersInLine) * cellWidth);
				// 获取文本的高度
				int mtextHeight = getFontHeight(starText, mTextPaint);
				// 这里* 号，偏上，所以要微调下
				starY = (int) (bottom + starCellHeight / 2.0f
						+ mtextHeight/* / 2.0f */ );
				canvas.drawText(starText, starX, starY, mTextPaint);
				// 第一个掩码，画左边粗线
				if (i == currentFirstIndex) {
					// 画左边一根线
					mLinePaint.setColor(pressedBgColor);
					mLinePaint.setStrokeWidth(lineStrokeSize);
					canvas.drawLine(left, top - deltHeight, left, bottom + deltHeight, mLinePaint);

					mLinePaint.setColor(lineColor);
					mLinePaint.setStrokeWidth(1);
				}

				// 最后一个掩码，且在每行末，画右边粗线
				if (currentFirstIndex + maskNum - 1 == i) {
					// 因为一个方格一个方格的画，下一个会覆盖上一个，使得竖线颜色变得不太明显
					if (i == LETTERS.length - 1 || (i + 1) % lettersInLine == 0) {
						// 如果是每排最后一个，则画右边线
						mLinePaint.setColor(pressedBgColor);
						mLinePaint.setStrokeWidth(lineStrokeSize);
						canvas.drawLine(right, top - deltHeight, right, bottom + deltHeight, mLinePaint);
						mLinePaint.setColor(lineColor);
						mLinePaint.setStrokeWidth(1);
					}
				}
				// 绘制文本A-Z
				mTextPaint.setColor(textPressedColor);
				mTextPaint.setStyle(Paint.Style.FILL);
				canvas.drawText(text, x, y, mTextPaint);
			} else {
				// 不在掩码范围内,画上面一根线
				canvas.drawLine(left, top, right, top, mLinePaint);
				// 画下面一根线,
				canvas.drawLine(left, bottom, right, bottom, mLinePaint);
				// 每行第一个方块，画左边线条
				if (i % lettersInLine == 0) {
					canvas.drawLine(left, top, left, bottom, mLinePaint);
				} else if (currentFirstIndex + maskNum == i && i % lettersInLine != 0) {
					// 画掩码两端粗线,因为一个方格一个方格的画，下一个会覆盖上一个，使得竖线颜色变得不太明显，因此在最后一个掩码的下一个方格画竖线，且不在每行第一个掩码处，画左边竖线
					// 如果是每排最后一个，则画右边线
					mLinePaint.setColor(pressedBgColor);
					mLinePaint.setStrokeWidth(lineStrokeSize);
					canvas.drawLine(left, top - deltHeight, left, bottom + deltHeight, mLinePaint);
					mLinePaint.setColor(lineColor);
					mLinePaint.setStrokeWidth(1);
				} else {
					canvas.drawLine(left, top, left, bottom, mLinePaint);
				}
				if (i == LETTERS.length - 1 || (i + 1) % lettersInLine == 0) {
					// 每行末画右边竖线
					canvas.drawLine(right, top, right, bottom, mLinePaint);
				}
				// 绘制文本A-Z
				mTextPaint.setColor(textUnpressedColor);
				mTextPaint.setStyle(Paint.Style.FILL);
				canvas.drawText(text, x, y, mTextPaint);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int index = -1;
		switch (MotionEventCompat.getActionMasked(event)) {
		case MotionEvent.ACTION_DOWN:
			isTouchFinished = false;
			// 获取当前触摸到的字母索引
			index = getCurrentIndex(event);
			if (index >= 0 && index < LETTERS.length) {
				// 判断是否第一次点击
				if (mMaskIndex == null || mMaskIndex.size() == 0) {
					firstIndex = index;
					touchIndex = index;
				} else {
					// 判断是否跟上一次触摸到的一样,并且是否在掩码内
					if (index != touchIndex && isInMask(index, mMaskIndex) == -1) {
						// 点击不一样，且不再掩码范围内，则需要处理
						firstIndex = index;
					}
					touchIndex = index;
				}
				Log.d(TAG, "onTouchEvent: " + LETTERS[index]);
			}

			break;
		case MotionEvent.ACTION_MOVE:
			// 获取当前触摸到的字母索引
			index = getCurrentIndex(event);
			if (index >= 0 && index < LETTERS.length) {
				// 判断是否第一次点击
				if (mMaskIndex == null || mMaskIndex.size() == 0) {
					firstIndex = index;
					touchIndex = index;
				} else {
					// 判断是否跟上一次触摸到的一样,并且在掩码外
					if (index != touchIndex && isInMask(index, mMaskIndex) == -1) {
						// 点击不一样，且不再掩码范围内，则需要处理
						if (isInMask(touchIndex, mMaskIndex) != -1) {
							// 上次在掩码内
							firstIndex += index - touchIndex;
						} else {
							firstIndex = index;
						}
					} else {
						// 掩码内

						int del = index - touchIndex;
						firstIndex += del;
						if (firstIndex < 0) {
							firstIndex = 0;
						} else if (firstIndex >= LETTERS.length - maskNum) {
							firstIndex = LETTERS.length - maskNum;
						}
					}
					touchIndex = index;
				}

			}
			break;
		case MotionEvent.ACTION_UP:
			// touchIndex = -1;
			isTouchFinished = true;
			break;

		default:
			break;
		}
		postInvalidate();
		// invalidate();
		return true;
	}

	/**
	 * 根据当前触摸点来判断点击的letter index
	 * 
	 * @param event
	 * @return
	 */
	private int getCurrentIndex(MotionEvent event) {
		int xIndex, yIndex = 0;
		xIndex = (int) (event.getX() / cellWidth);
		yIndex = (int) (event.getY() / (cellHeight + starCellHeight));
		return yIndex * lettersInLine + xIndex;
	}

	private List<Integer> getMaskIndexsWithFirstIndex(int firstIndex, char[] letters, int maskNum) {
		if (letters == null || letters.length == 0) {
			return null;
		}

		if (firstIndex < 0 || firstIndex >= letters.length) {
			return null;
		}
		List<Integer> maskIndexs = new ArrayList<Integer>();
		// 清空原来信息
		// maskIndexs.clear();
		int temp = -1;
		Log.e(TAG, "--------------------------:");
		for (int i = 0; i < maskIndexs.size(); i++) {
			Log.e(TAG, "maskIndexs:" + i + "--" + maskIndexs.get(i));
		}
		Log.e(TAG, "--------------------------:");
		// 数目足够，向后顺序获取相应位数
		if (firstIndex >= 0 && firstIndex < letters.length - (maskNum - 1)) {
			temp = firstIndex;
			for (int i = 0; i < maskNum; i++) {
				maskIndexs.add(i, temp++);
			}

		} else if (firstIndex >= letters.length - (maskNum - 1) && firstIndex < letters.length) {
			// 逆序获取相应位数
			temp = letters.length - maskNum;
			for (int i = letters.length - 1; i >= letters.length - maskNum; i--) {
				maskIndexs.add(letters.length - 1 - i, temp++);
			}
		}
		// 获取当前掩码第一个位置
		currentFirstIndex = maskIndexs.get(0);
		return maskIndexs;

	}

	// 计算测量 view需要占用多大位置
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
		// 获取总共要显示的行数,当调用了 setText() 方法时，view会重新测量及布局，因此这里 totalLines 也需要重新计算
		if (LETTERS.length % lettersInLine == 0) {
			totalLines = LETTERS.length / lettersInLine;
		} else {
			totalLines = LETTERS.length / lettersInLine + 1;
		}
	}

	/**
	 * 测量宽度
	 * 
	 * @param measureSpec
	 * @return
	 */
	private int measureWidth(int measureSpec) {
		float result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			// 考虑到线宽问题，所以宽度变大了
			if (LETTERS.length < lettersInLine) {
				result = squareWidth * LETTERS.length + lineStrokeSize * 2;
			} else {
				result = squareWidth * lettersInLine + lineStrokeSize * 2;
			}
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}
		return (int) result;

	}

	/**
	 * 测量高度
	 * 
	 * @param measureSpec
	 * @return
	 */
	private int measureHeight(int measureSpec) {
		float result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.EXACTLY) {
			result = specSize;
		} else {
			result = squareWidth * 1.5f * totalLines/* +deltHeight */;
			if (specMode == MeasureSpec.AT_MOST) {
				result = Math.min(result, specSize);
			}
		}
		return (int) result;

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// 获取单元格的宽和高
		cellHeight = squareWidth;
		cellWidth = cellHeight;

		starCellHeight = getMeasuredHeight() / totalLines - cellHeight;
	}

	/**
	 * 获取文本高度
	 * 
	 * @param text
	 * @return
	 */
	public int getFontHeight(String text, Paint paint) {
		Rect bounds = new Rect();// 矩形
		paint.getTextBounds(text, 0, text.length(), bounds);
		return bounds.height();
	}
}
