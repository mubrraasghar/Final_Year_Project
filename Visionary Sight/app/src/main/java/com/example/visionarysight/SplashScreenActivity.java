package com.example.visionarysight;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen); 

        ImageView splashLogo = findViewById(R.id.splash_logo);

        Animation easeInAnimation = AnimationUtils.loadAnimation(this, R.anim.ease_in);

        splashLogo.startAnimation(easeInAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreenActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }
        }, 4000);
    }
}
