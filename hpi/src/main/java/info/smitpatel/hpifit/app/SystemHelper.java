package info.smitpatel.hpifit.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

public class SystemHelper {
    private static final String PREFIX = SystemHelper.class.getSimpleName() + ": ";

    @SuppressWarnings("deprecation")
    public static Point getScreenSize(Context context) {
        WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        return new Point(width, height);
    }

    /**
     * convert dp (density pixels) to pixels
     * @param dp dp (density pixels)
     * @return int that represents pixels
     */
    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * convert pixels to dp (density pixels)
     * @param px pixels
     * @return int that represents dp (density pixels)
     */
    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * show soft keyboard by force. After this method, hideSoftKeyboard() might be required
     * due to it will not close the keyboard even if text field loses focus
     * @param context
     */
    public static void showSoftKeyboard(Context context) {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    /**
     * hide soft keyboard
     * @param context context
     * @param view any visible view on the current layout
     */
    public static void hideSoftKeyboard(Context context, View view) {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
