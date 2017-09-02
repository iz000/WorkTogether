package jang.worktogether.Utils;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class KeyboardUtil {

    public static void setupHideKeyBoard(View view, final Activity activity) {
        //EditText 이외의 부분을 터치하면 키보드 내려감
        if(!(view instanceof EditText)) {

            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyboard(v, activity);
                    return false;
                }
            });
        }

        //view가 레이아웃이면 그 안에 있는 것들도 재귀를 통해 터치 리스너를 등록
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupHideKeyBoard(innerView, activity);
            }
        }
    }

    public static void setupHideKeyBoard(View view, final Activity activity,
                                         View.OnTouchListener onTouchListener) {
        //EditText 이외의 부분을 터치하면 키보드 내려감
        if(!(view instanceof EditText)) {
            view.setOnTouchListener(onTouchListener);
        }

        //view가 레이아웃이면 그 안에 있는 것들도 재귀를 통해 터치 리스너를 등록
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupHideKeyBoard(innerView, activity);
            }
        }
    }

    public static void hideKeyboard(View v, Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
}
