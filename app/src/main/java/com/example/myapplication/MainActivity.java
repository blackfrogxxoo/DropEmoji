package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.myapplication.dropemoji.DropEmojiView;

public class MainActivity extends AppCompatActivity {

    private DropEmojiView dropEmojiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dropEmojiView = findViewById(R.id.dropEmojiView);
        Button btnAdd = findViewById(R.id.btnAddEmoji);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dropEmojiView.addEmoji(R.drawable.ic_launcher_round);
            }
        });
    }
}
