package jang.worktogether.basic;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import jang.worktogether.R;
import jang.worktogether.Utils.ErrorcodeUtil;
import jang.worktogether.Utils.HttpUtil;
import jang.worktogether.Utils.KeyboardUtil;

public class JoinActivity extends AppCompatActivity{

    private HttpUtil httpUtil;
    EditText email_et;
    EditText name_et;
    EditText password_et;
    EditText check_et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        httpUtil = new HttpUtil(JoinActivity.this);
        email_et = (EditText)findViewById(R.id.email_et);
        name_et = (EditText)findViewById(R.id.name_et);
        password_et = (EditText)findViewById(R.id.password_et);
        check_et = (EditText)findViewById(R.id.check_et);

        //EditText 이외의 부분 터치하면 키보드 사라지도록 설정
        KeyboardUtil.setupHideKeyBoard(findViewById(R.id.join_mainLayout), JoinActivity.this);

        httpUtil.setCallback(new HttpUtil.Callback() {
            @Override
            public void callback(String response) {
                if(response.length() == 3){
                    Toast.makeText(JoinActivity.this, ErrorcodeUtil.errorMessage(response),
                            Toast.LENGTH_LONG).show();
                }
                else if(response.equals("0")){
                    Toast.makeText(JoinActivity.this, "회원가입성공", Toast.LENGTH_LONG).show();
                    JoinActivity.this.finish();
                }
            }
        });

        email_et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_UP){
                    name_et.requestFocus();
                    return true;
                }
                return false;
            }
        });

        name_et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_UP){
                    password_et.requestFocus();
                    return true;
                }
                return false;
            }
        });

        password_et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_UP){
                   check_et.requestFocus();
                    return true;
                }
                return false;
            }
        });
    }

    private void join(){
        if(email_et.getText().toString().equals("") || name_et.getText().toString().equals("") ||
                password_et.getText().toString().equals("") || check_et.getText().toString().equals("")){
            Toast.makeText(this, "빈 칸을 다 채워주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!password_et.getText().toString().equals(check_et.getText().toString())){
            Toast.makeText(this, "비밀번호와 비밀번호 확인이 다릅니다", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!isEmailValid(email_et.getText().toString())){
            Toast.makeText(this, "이메일 형식에 맞춰 입력해 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        httpUtil.setUrl("join.php")
                .setData("email", email_et.getText().toString())
                .setData("name", name_et.getText().toString())
                .setData("password", password_et.getText().toString())
                .postData();
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.join_button : {
                join();
                break;
            }
        }
    }

    public static boolean isEmailValid(String email) { //이메일 형식에 맞는지 확인.
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
