package jang.worktogether.group;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

import jang.worktogether.R;

public class CheckableLinearLayout extends LinearLayout implements Checkable{

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setChecked(boolean checked) {
        CheckBox checkBox = (CheckBox) findViewById(R.id.select_checkbox);
        if(checkBox.isChecked() != checked){
            checkBox.setChecked(checked);
        }
    }

    @Override
    public boolean isChecked() {
        CheckBox checkBox = (CheckBox) findViewById(R.id.select_checkbox);
        return checkBox.isChecked();
    }

    @Override
    public void toggle() {
        CheckBox checkBox = (CheckBox) findViewById(R.id.select_checkbox);
        setChecked(!checkBox.isChecked());
    }
}
