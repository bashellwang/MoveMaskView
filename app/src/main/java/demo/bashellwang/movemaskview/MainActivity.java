package demo.bashellwang.movemaskview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button mComfirmBtn;
    private MoveMaskView mMaskView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mComfirmBtn = (Button) findViewById(R.id.complete_btn);
        mMaskView = (MoveMaskView) findViewById(R.id.move_mask_view);
        mMaskView.setText("abcdefghijklmn1234567890");
        mMaskView.setOnTouchFinishedListener(new MoveMaskView.OnTouchFinishedListener() {

            @Override
            public void onTouchFinished(List<Integer> mMaskIndex, String maskResult) {
                // TODO Auto-generated method stub
//				Toast.makeText(SetPswMaskActivity.this, maskResult, 0).show();
                Toast.makeText(MainActivity.this, maskResult, Toast.LENGTH_SHORT).show();
            }

        });
    }
}
