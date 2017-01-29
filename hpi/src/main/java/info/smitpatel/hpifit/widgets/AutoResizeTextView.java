package info.smitpatel.hpifit.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Build;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;

public class AutoResizeTextView extends AppCompatTextView {

    private interface SizeTester {
        /**
         * @param suggestedSize  Size of text to be tested
         * @param availableSpace available space in which text must fit
         * @return an integer < 0 if after applying {@code suggestedSize} to
         * text, it takes less space than {@code availableSpace}, > 0
         * otherwise
         */
        public int onTestSize(int suggestedSize, RectF availableSpace);
    }

    private RectF textRect = new RectF();
    private RectF availableSpaceRect;

    private SparseIntArray textCachedSizes;
    private TextPaint paint;

    private float maxTextSize;
    private float spacingMult = 1.0f;
    private float spacingAdd = 0.0f;
    private float minTextSize = 20;
    private int widthLimit;

    private static final int NO_LINE_LIMIT = -1;
    private int maxLines;

    private boolean enableSizeCache = true;
    private boolean initializedDimens;

    public AutoResizeTextView(Context context) {
        super(context);
        initialize();
    }

    public AutoResizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        paint = new TextPaint(getPaint());
        maxTextSize = getTextSize();
        availableSpaceRect = new RectF();
        textCachedSizes = new SparseIntArray();
        if (maxLines == 0) {
            maxLines = NO_LINE_LIMIT;
        }
    }

    @Override
    public void setTextSize(float size) {
        maxTextSize = size;
        textCachedSizes.clear();
        adjustTextSize();
    }

    @Override
    public void setMaxLines(int maxlines) {
        super.setMaxLines(maxlines);
        maxLines = maxlines;
        adjustTextSize();
    }

    public int getMaxLines() {
        return maxLines;
    }

    @Override
    public void setSingleLine() {
        super.setSingleLine();
        maxLines = 1;
        adjustTextSize();
    }

    @Override
    public void setSingleLine(boolean singleLine) {
        super.setSingleLine(singleLine);
        if (singleLine) {
            maxLines = 1;
        } else {
            maxLines = NO_LINE_LIMIT;
        }
        adjustTextSize();
    }

    @Override
    public void setLines(int lines) {
        super.setLines(lines);
        maxLines = lines;
        adjustTextSize();
    }

    @Override
    public void setTextSize(int unit, float size) {
        Context context = getContext();
        Resources r;

        if (context == null) {
            r = Resources.getSystem();
        } else {
            r = context.getResources();
        }

        maxTextSize = TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
        textCachedSizes.clear();
        adjustTextSize();
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        super.setLineSpacing(add, mult);
        spacingMult = mult;
        spacingAdd = add;
    }

    /**
     * Set the lower text size limit and invalidate the view
     * @param minTextSize
     */
    public void setMinTextSize(float minTextSize) {
        this.minTextSize = minTextSize;
        adjustTextSize(getText().toString());
    }

    private void adjustTextSize() {
        if (!initializedDimens) {
            return;
        }

        int startSize = (int) minTextSize;
        int heightLimit = getMeasuredHeight() - getCompoundPaddingBottom() - getCompoundPaddingTop();
        widthLimit = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        availableSpaceRect.right = widthLimit;
        availableSpaceRect.bottom = heightLimit;

        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, efficientTextSizeSearch(startSize,
                (int) maxTextSize, mSizeTester, availableSpaceRect));
    }

    private void adjustTextSize(final String text) {
        if (!initializedDimens) {
            return;
        }

        int heightLimit = getMeasuredHeight() - getCompoundPaddingBottom() - getCompoundPaddingTop();
        widthLimit = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();

        availableSpaceRect.right = widthLimit;
        availableSpaceRect.bottom = heightLimit;

        int maxTextSplits = text.split(" ").length;
        AutoResizeTextView.super.setMaxLines(Math.min(maxTextSplits, maxLines));

        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, binarySearch((int) minTextSize,
                (int) maxTextSize, mSizeTester, availableSpaceRect));
    }

    private final SizeTester mSizeTester = new SizeTester() {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public int onTestSize(int suggestedSize, RectF availableSPace) {
            paint.setTextSize(suggestedSize);
            String text = getText().toString();
            boolean singleLine = getMaxLines() == 1;

            if (singleLine) {
                textRect.bottom = paint.getFontSpacing();
                textRect.right = paint.measureText(text);
            } else {
                StaticLayout layout = new StaticLayout(text, paint, widthLimit,
                        Alignment.ALIGN_NORMAL, spacingMult, spacingAdd, true);

                // Return early if we have more lines
                if (getMaxLines() != NO_LINE_LIMIT && layout.getLineCount() > getMaxLines()) {
                    return 1;
                }

                textRect.bottom = layout.getHeight();
                int maxWidth = -1;
                for (int i = 0; i < layout.getLineCount(); i++) {
                    if (maxWidth < layout.getLineWidth(i)) {
                        maxWidth = (int) layout.getLineWidth(i);
                    }
                }

                textRect.right = maxWidth;
            }

            textRect.offsetTo(0, 0);
            if (availableSPace.contains(textRect)) {
                // May be too small?
                return -1;
            } else {
                // too big
                return 1;
            }
        }
    };

    /**
     * Enables or disables size caching, enabling it will improve performance
     * where you are animating a value inside TextView. This stores the font
     * size against getText().length() Be careful though while enabling it as 0
     * takes more space than 1 on some fonts and so on.
     *
     * @param enable Enable font size caching
     */
    public void enableSizeCache(boolean enable) {
        enableSizeCache = enable;
        textCachedSizes.clear();
        adjustTextSize();
    }

    private int efficientTextSizeSearch(int start, int end, SizeTester sizeTester, RectF availableSpace) {
        if (!enableSizeCache) {
            return binarySearch(start, end, sizeTester, availableSpace);
        }

        int key = getText().toString().length();
        int size = textCachedSizes.get(key);

        if (size != 0) {
            return size;
        }

        size = binarySearch(start, end, sizeTester, availableSpace);
        textCachedSizes.put(key, size);
        return size;
    }

    private static int binarySearch(int start, int end, SizeTester sizeTester, RectF availableSpace) {
        int lastBest = start;
        int lo = start;
        int hi = end - 1;
        int mid = 0;

        while (lo <= hi) {
            mid = (lo + hi) >>> 1;
            int midValCmp = sizeTester.onTestSize(mid, availableSpace);
            if (midValCmp < 0) {
                lastBest = lo;
                lo = mid + 1;
            } else if (midValCmp > 0) {
                hi = mid - 1;
                lastBest = hi;
            } else {
                return mid;
            }
        }

        return lastBest;

    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        super.onTextChanged(text, start, before, after);
        adjustTextSize();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        initializedDimens = true;
        textCachedSizes.clear();

        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if (width != oldWidth || height != oldHeight) {
            adjustTextSize();
        }
    }
}
