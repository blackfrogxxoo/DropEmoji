package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.myapplication.dropemoji.DropEmojiView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private DropEmojiView dropEmojiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dropEmojiView = findViewById(R.id.dropEmojiView);
        final Button btnAdd = findViewById(R.id.btnAddEmoji);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dropEmojiView.addEmoji(R.drawable.ic_launcher_round);
            }
        });
        dropEmojiView.postDelayed(new Runnable() {
            @Override
            public void run() {
                dropEmojiView.setCommentBottom(btnAdd.getTop());
            }
        }, 100);
        dropEmojiView.setCallback(new DropEmojiView.Callback(){
            @Override
            public void onSend(int[] bitmapResIds) {
                Log.i(TAG, "onSend: " + Arrays.toString(bitmapResIds));
            }
        });
        dropEmojiView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "onClick", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
