

# Android拖拽辅助类ViewDragHelper(一) -- 滑动解锁的实现

滑动解锁作为一种较新的验证方式,以其方便快捷简单的特点,迅速成为目前较为流行的验证方式;而图片滑动解锁比滑动解锁更加高效安全,成为现代最为流行的用户验证方式。

### 效果展示

![](https://tempimg-1302248544.cos.ap-chengdu.myqcloud.com/Img/SlideLock/hdjs3.gif)

先来分析一下页面的元素

- 背景图
- 圆角滑道
- 圆形滑块
- 闪动提示文字

其他一些细节：

- 滑道和圆形滑块之间有些边距，我们使用padding来处理。
- 我们需要自定义的就是第2点，这个滑道包含一个滑块的图片和提示文字，滑块使用原生ImageView即可，而提示文字则是一个支持渐变着色的TextView(不是重点)。

### 实现步骤

#### 渐变着色的TextView

先秒掉简单的，渐变着色的TextView，不是重点，代码量不多。

```java
public class ShineTextView extends TextView {
    // Android 支持三种颜色渐变， LinearGradient（线性渐变）、 RadialGradient （放射渐变）、 		// SweepGradient（扫描渐变）。这三种渐变均继承自android.graphics.Shader， 
    // Paint 类通过setShader()方法来支持渐变。
    private LinearGradient mLinearGradient;
    private Matrix mGradientMatrix;
    private int mViewWidth = 0;
    private int mTranslate = 0;
	// 构造方法
    public ShineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mViewWidth == 0) {
            mViewWidth = getMeasuredWidth();
            if (mViewWidth > 0) {
                Paint paint = getPaint();
                mLinearGradient = new LinearGradient(0,
                        0,
                        mViewWidth,
                        0,
                        new int[]{getCurrentTextColor(), 0xffffffff, getCurrentTextColor()},
                        null,
                        Shader.TileMode.CLAMP);
                paint.setShader(mLinearGradient);
                mGradientMatrix = new Matrix();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mGradientMatrix != null) {
            mTranslate += mViewWidth / 5;
            if (mTranslate > 2 * mViewWidth) {
                mTranslate = -mViewWidth;
            }
            mGradientMatrix.setTranslate(mTranslate, 0);
            mLinearGradient.setLocalMatrix(mGradientMatrix);
            // 每80毫秒执行onDraw()
            postInvalidateDelayed(80);
        }
    }
}
```

下面重点介绍我们使用**ViewDragHelper**实现拽托、滑动的滑道**View:SlideLockView**

#### 让滑块滑动起来

滑道实际就是一个FrameLayout，我们使用**ViewDragHelper**将滑块ImageView进行拽托，主要我们要做以下几件事：

- 限制拽托的左侧起点、右侧终点（否则滑块就出去啦！）
- 松手时判断滑块的x坐标是偏向滑道的左侧还是右侧，来决定滑动到起点还是终点。
- 滑动结束，判断是否到达了右侧的终点。
- 判断拽托速度，如果超过指定速度，则自动滚动滑块到右侧终点。

看到这4点，如果让我们用事件分发来处理，代码量和判断会非常多，并且需要做速度检测，而使用ViewDragHelper，上面4点都封装好啦，我们添加一个回调，再将事件委托给它，在回调中做事情上面4点的处理，一切都简单起来了。

- 1.创建SlideLockView，继承FrameLayout

```java
public class SlideLockView extends FrameLayout {
    /**
     * 拽托帮助类
     */
    private ViewDragHelper mViewDragHelper;
    
    public SlideLockView(@NonNull Context context) {
        this(context, null);
    }

    public SlideLockView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideLockView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        // 进行初始化...
    }
}
```

- 2.创建ViewDragHelper，使用create静态方法创建，有3个参数，第一个拽托控件的父控件（就是当前View），第二个参数是拽托灵敏度，数值越大，越灵敏，默认为1.0，第三个参数为回调对象。

```java
private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    final SlideLockView slideRail = this;
    mViewDragHelper = ViewDragHelper.create(this, 0.3f, new ViewDragHelper.Callback() {
        ...
    });
}
```

- 委托onInterceptTouchEvent、onTouchEvent事件给ViewDragHelper

```java
@Override
public boolean onInterceptTouchEvent(MotionEvent ev) {
    // 将onInterceptTouchEvent委托给ViewDragHelper
    return mViewDragHelper.shouldInterceptTouchEvent(ev);
}
    
@Override
public boolean onTouchEvent(MotionEvent event) {
    // 将onTouchEvent委托给ViewDragHelper
    mViewDragHelper.processTouchEvent(event);
    return true;
}
```

- 找到布局中的滑块，我们要求滑块的id为lock_btn，所以需要在ids.xml中预先定义这个id，如果没有查找到，则抛出异常。

```xml
// 文件名：ids.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <item name="lock_btn" type="id" />
</resources>
```

```java
@Override
protected void onFinishInflate() {
    super.onFinishInflate();
    //找到需要拽托的滑块
    mLockBtn = findViewById(R.id.lock_btn);
    if (mLockBtn == null) {
        throw new NullPointerException("必须要有一个滑动滑块");
    }
}
```

- 剩下的事情就在ViewDragHelper的回调中设置。

复写tryCaptureView()、clampViewPositionHorizontal()、clampViewPositionVertical()。

- tryCaptureView为判断子View是否可以拽托

- clampViewPositionHorizontal()则是横向拽托子View时回调，返回可以拽托到的位置。

- clampViewPositionVertical则是纵向拽托。

```java
private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    final SlideLockView slideRail = this;
    mViewDragHelper = ViewDragHelper.create(this, 0.3f, new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            // 判断能拽托的View，这里会遍历内部子控件来决定是否可以拽托，我们只需要滑块可以滑动
            return child == mLockBtn;
        }
        
        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            // 拽托子View横向滑动时回调，回调的left，则是可以滑动的左上角x坐标
            return left;
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            // 拽托子View纵向滑动时回调，锁定顶部padding距离即可，不能不复写，否则少了顶部的padding，位置就偏去上面了
            return getPaddingTop();
        }
    });
}
```

#### 限制滑块滑动范围

- 经过上面3个方法重写，滑块已经可以左右滑动了，但是可以滑动出滑道（父控件），我们需要限制横向滑动的范围，不能超过左侧起点和右侧终点。我们需要修改clampViewPositionHorizontal这个方法。
- 左侧起点的x坐标，就是paddingStart。
- 右侧终点，为滑道总长度 - 右边边距 - 滑块宽度。
- 判断回调的left值，如果小于起点，则强制为起点，如果大于右侧终点值，则强制为终点。

这样处理，滑块则不会滑出滑道了！代码量不对，也很清晰。

```java
private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    final SlideLockView slideRail = this;
    mViewDragHelper = ViewDragHelper.create(this, 0.3f, new ViewDragHelper.Callback() {
        //...
        
        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            //拽托子View横向滑动时回调，回调的left，则是可以滑动的左上角x坐标
            int lockBtnWidth = mLockBtn.getWidth();
            //限制左右临界点
            int fullWidth = slideRail.getWidth();
            //最少的左边
            int leftMinDistance = getPaddingStart();
            //最多的右边
            int leftMaxDistance = fullWidth - getPaddingEnd() - lockBtnWidth;
            //修复两端的临界值
            if (left < leftMinDistance) {
                return leftMinDistance;
            } else if (left > leftMaxDistance) {
                return leftMaxDistance;
            }
            return left;
        }

        //...
    });
}
```

#### 松手回弹和速度检测

有了滑动和限制滑动范围，我们还有一个松手回弹和速度检测，ViewDragHelper同样给我们封装了，提供了一个onViewReleased()回调，并且做了速度检测，将速度也回传给了我们。

- 复写onViewCaptured()，主要是为了获取一开始捕获到滑块时，他的top值。

- 复写onViewReleased()，主要是计算松手时,滑块比较近起点还比较近是终点，使用ViewDragHelper的settleCapturedViewAt()方法，开始弹性滚动滑块去到起点或终点。

- 判断中，我们添加判断速度是否超过1000，如果超过，即使拽托距离比较小，就当为fling操作，让滑块滚动到终点。

settleCapturedViewAt()这个方法，内部是使用Scroller进行弹性滚动的，所以我们需要复写父View的computeScroll()方法，进行内容滚动处理。

如果不知道为什么这么做，搜索一下Scroller的资料了解一下~

```java
private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    final SlideLockView slideRail = this;
    mViewDragHelper = ViewDragHelper.create(this, 0.3f, new ViewDragHelper.Callback() {
        private int mTop;
        
        //...
        
        @Override
        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
            //捕获到拽托的View时回调，获取顶部距离
            mTop = capturedChild.getTop();
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            //获取滑块当前的位置
            int currentLeft = releasedChild.getLeft();
            //获取滑块的宽度
            int lockBtnWidth = mLockBtn.getWidth();
            //获取滑道宽度
            int fullWidth = slideRail.getWidth();
            //一般滑道的宽度，用来判断滑块距离起点近还是终点近
            int halfWidth = fullWidth / 2;
            //松手位置在小于一半，并且滑动速度小于1000，则回到左边
            if (currentLeft <= halfWidth && xvel < 1000) {
                mViewDragHelper.settleCapturedViewAt(getPaddingStart(), mTop);
            } else {
                //否则去到右边（宽度，减去padding和滑块宽度）
                mViewDragHelper.settleCapturedViewAt(fullWidth - getPaddingEnd() - lockBtnWidth, mTop);
            }
            invalidate();
        }
    });
}

@Override
public void computeScroll() {
    super.computeScroll();
    //判断是否移动到头了，未到头则继续
    if (mViewDragHelper != null) {
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }
}
```

#### 解锁回调

经过上面的编码，滑动解锁就完成了，但还差一个解锁回调，进行解锁操作，并且我们需要一个时机知道滚动结束了（ViewDragHelper状态回调，滚动闲置了，并且滑块位于终点，则为解锁完成）。

- 复写onViewDragStateChanged()方法，处理ViewDragHelper状态改变，状态主要有以下3个：
  1. STATE_IDLE = 0，滚动闲置，可以认为滚动停止了。
  2. STATE_DRAGGING = 1，正在拽托。
  3. STATE_SETTLING = 2，fling操作时。
- 提供Callback接口回调和设置方法。

我们在onViewDragStateChanged()回调中判断，状态为STATE_IDLE，并且滑块位置为终点值时，就为解锁，并且回调Callback对象。

```java
public class SlideLockView extends FrameLayout {
    /**
     * 回调
     */
    private Callback mCallback;
    /**
     * 是否解锁
     */
    private boolean isUnlock = false;
    
    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        final SlideLockView slideRail = this;
        mViewDragHelper = ViewDragHelper.create(this, 0.3f, new ViewDragHelper.Callback() {
            private int mTop;
            
            //...
            
            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
                int lockBtnWidth = mLockBtn.getWidth();
                //限制左右临界点
                int fullWidth = slideRail.getWidth();
                //最多的右边
                int leftMaxDistance = fullWidth - getPaddingEnd() - lockBtnWidth;
                int left = mLockBtn.getLeft();
                if (state == ViewDragHelper.STATE_IDLE) {
                    //移动到最右边，解锁完成
                    if (left == leftMaxDistance) {
                        //未解锁才进行解锁回调，由于这个判断会进两次，所以做了标志位限制
                        if (!isUnlock) {
                            isUnlock = true;
                            if (mCallback != null) {
                                mCallback.onUnlock();
                            }
                        }
                    }
                }
            }
        });
    }

    public interface Callback {
        /**
         * 当解锁时回调
         */
        void onUnlock();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }
}
```

### 完整代码

```java
public class SlideLockView extends FrameLayout {
    /**
     * 滑动滑块
     */
    private View mLockBtn;
    /**
     * 拽托帮助类
     */
    private ViewDragHelper mViewDragHelper;
    /**
     * 回调
     */
    private Callback mCallback;
    /**
     * 是否解锁
     */
    private boolean isUnlock = false;

    public SlideLockView(@NonNull Context context) {
        this(context, null);
    }

    public SlideLockView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideLockView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        final SlideLockView slideRail = this;
        mViewDragHelper = ViewDragHelper.create(this, 0.3f, new ViewDragHelper.Callback() {
            private int mTop;

            @Override
            public boolean tryCaptureView(@NonNull View child, int pointerId) {
                //判断能拽托的View，这里会遍历内部子控件来决定是否可以拽托，我们只需要滑块可以滑动
                return child == mLockBtn;
            }

            @Override
            public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
                //拽托子View横向滑动时回调，回调的left，则是可以滑动的左上角x坐标
                int lockBtnWidth = mLockBtn.getWidth();
                //限制左右临界点
                int fullWidth = slideRail.getWidth();
                //最少的左边
                int leftMinDistance = getPaddingStart();
                //最多的右边
                int leftMaxDistance = fullWidth - getPaddingEnd() - lockBtnWidth;
                //修复两端的临界值
                if (left < leftMinDistance) {
                    return leftMinDistance;
                } else if (left > leftMaxDistance) {
                    return leftMaxDistance;
                }
                return left;
            }

            @Override
            public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
                //拽托子View纵向滑动时回调，锁定顶部padding距离即可，不能不复写，否则少了顶部的padding，位置就偏去上面了
                return getPaddingTop();
            }

            @Override
            public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
                super.onViewCaptured(capturedChild, activePointerId);
                //捕获到拽托的View时回调，获取顶部距离
                mTop = capturedChild.getTop();
            }

            @Override
            public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                //获取滑块当前的位置
                int currentLeft = releasedChild.getLeft();
                //获取滑块的宽度
                int lockBtnWidth = mLockBtn.getWidth();
                //获取滑道宽度
                int fullWidth = slideRail.getWidth();
                //一般滑道的宽度，用来判断滑块距离起点近还是终点近
                int halfWidth = fullWidth / 2;
                //松手位置在小于一半，并且滑动速度小于1000，则回到左边
                if (currentLeft <= halfWidth && xvel < 1000) {
                    mViewDragHelper.settleCapturedViewAt(getPaddingStart(), mTop);
                } else {
                    //否则去到右边（宽度，减去padding和滑块宽度）
                    mViewDragHelper.settleCapturedViewAt(fullWidth - getPaddingEnd() - lockBtnWidth, mTop);
                }
                invalidate();
            }

            @Override
            public void onViewDragStateChanged(int state) {
                super.onViewDragStateChanged(state);
                int lockBtnWidth = mLockBtn.getWidth();
                //限制左右临界点
                int fullWidth = slideRail.getWidth();
                //最多的右边
                int leftMaxDistance = fullWidth - getPaddingEnd() - lockBtnWidth;
                int left = mLockBtn.getLeft();
                if (state == ViewDragHelper.STATE_IDLE) {
                    //移动到最右边，解锁完成
                    if (left == leftMaxDistance) {
                        //未解锁才进行解锁回调，由于这个判断会进两次，所以做了标志位限制
                        if (!isUnlock) {
                            isUnlock = true;
                            if (mCallback != null) {
                                mCallback.onUnlock();
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //找到需要拽托的滑块
        mLockBtn = findViewById(R.id.lock_btn);
        if (mLockBtn == null) {
            throw new NullPointerException("必须要有一个滑动滑块");
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //将onInterceptTouchEvent委托给ViewDragHelper
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //将onTouchEvent委托给ViewDragHelper
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        //判断是否移动到头了，未到头则继续
        if (mViewDragHelper != null) {
            if (mViewDragHelper.continueSettling(true)) {
                invalidate();
            }
        }
    }

    public interface Callback {
        /**
         * 当解锁时回调
         */
        void onUnlock();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }
}
```

### 基本使用

#### Xml控件布局

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/app_lock_screen_bg"
    tools:context=".ScreenLockActivity">

    <com.zh.android.slidelockscreen.widget.SlideLockView
        android:id="@+id/slide_rail"
        android:layout_width="300dp"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|center_horizontal"
        android:layout_marginBottom="20dp"
        android:background="@drawable/app_slide_rail_bg"
        android:paddingStart="6dp"
        android:paddingTop="8dp"
        android:paddingEnd="6dp"
        android:paddingBottom="8dp">

        <com.zh.android.slidelockscreen.widget.ShineTextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:layout_marginStart="20dp"
            android:gravity="center"
            android:text="右滑解锁打开应用"
            android:textColor="@color/app_tip_text"
            android:textSize="18sp" />

        <ImageView
            android:id="@id/lock_btn"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/app_lock_btn" />
    </com.zh.android.slidelockscreen.widget.SlideLockView>
</FrameLayout>
```

#### Java代码

```java
public class ScreenLockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_lock);
        SlideLockView slideRail = findViewById(R.id.slide_rail);
        slideRail.setCallback(new SlideLockView.Callback() {
            @Override
            public void onUnlock() {
                //解锁，跳转到首页
                Intent intent = new Intent(ScreenLockActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
```

### Github项目地址

