package com.example.brickbreaker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainMenuActivity extends AppCompatActivity {
    private Button StartButoon, highscore, Exitbutten;
private TextView highscoretext ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);
        highscoretext = findViewById(R.id.highscoreText);
        StartButoon = findViewById(R.id.startButton);
        highscore = findViewById(R.id.highscoreButton);
        Exitbutten = findViewById(R.id.exitButton);

        StartButoon.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
        highscore.setOnClickListener(v -> {
            int highscore = getSharedPreferences("Brick breaker", MODE_PRIVATE).getInt("high score", 0);
            highscoretext.setText(""+highscore);
        });
        Exitbutten.setOnClickListener(v -> finish());
    }
}




