package org.garageapps.android.petsciiviewsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.Toast;

import org.garageapps.android.petsciiview.PETSCIIView;

/**
 * SampleActivity
 *
 * @author Vladimir Ignjatijevic
 */
public class SampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        final PETSCIIView petsciiView = (PETSCIIView) findViewById(R.id.petsciiView);
        petsciiView.setListener(new PETSCIIView.PETSCIIListener() {
            @Override
            public void onClick(int action, int x, int y) {
                if (action == MotionEvent.ACTION_UP) {
                    Toast.makeText(SampleActivity.this, "Click at: " + x + "," + y + "", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
