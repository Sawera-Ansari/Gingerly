package com.asws.gingerly;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.EmojiTextView;

import com.asws.gingerly.R;

import static com.google.firebase.database.FirebaseDatabase.getInstance;
import static com.asws.gingerly.R.layout.list_item;

public class MainActivity extends AppCompatActivity {

    private FirebaseListAdapter<ChatMessage> adapter;

    EditText etEmoji;

    RelativeLayout activity_main;
    private int SIGN_IN_REQUEST_CODE;
    private ListView listOfMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmoji=findViewById(R.id.input);

        listOfMessages = (ListView)findViewById(R.id.list_of_messages);

        final EmojiPopup popup = EmojiPopup.Builder
                .fromRootView(findViewById(R.id.activity_main)).build(etEmoji);
        FloatingActionButton btnEmojis = (FloatingActionButton) findViewById(R.id.btnEmojis);
        btnEmojis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setActivated(!view.isActivated());
                popup.toggle();
            }
        });

        Query query = getInstance().getReference().child("Messages");
        FirebaseListOptions<ChatMessage> options =
                new FirebaseListOptions.Builder<ChatMessage>()
                        .setQuery(query, ChatMessage.class)
                        .setLayout(list_item)
                        .build();

        adapter = new FirebaseListAdapter<ChatMessage>(options) {
            @Override
            protected void populateView(@NonNull View v, @NonNull ChatMessage model, int position) {

                TextView messageText = (TextView)v.findViewById(R.id.message_text);
                TextView messageUser = (TextView)v.findViewById(R.id.message_user);
                TextView messageTime = (TextView)v.findViewById(R.id.message_time);

                messageText.setText(model.getMessageText());
                messageUser.setText(model.getMessageUser());

                messageTime.setText(DateFormat.format("dd-MM-yyyy (HH:mm:ss)",
                        model.getMessageTime()));
            }
        };

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .build(),
                    SIGN_IN_REQUEST_CODE
            );
        } else {
            Toast.makeText(this, "Welcome " + FirebaseAuth.getInstance().
                            getCurrentUser()
                            .getDisplayName(),
                    Toast.LENGTH_LONG).
                    show();


            displayChatMessage();

            //String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            //String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            //FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID).setValue(currentUserName);

        }

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = (EditText)findViewById(R.id.input);
                ChatMessage chatMessage = new ChatMessage(input.getText().toString(),FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                getInstance().getReference().child("Messages").push().setValue(chatMessage);
                //FirebaseDatabase.getInstance().getReference().child("Users").push().setValue(chatMessage.getMessageText());
                input.setText("");
            }
        });
    }

    private EmojiTextView getEmojiTextView() {
        EmojiTextView tvEmoji = (EmojiTextView) LayoutInflater
                .from(getApplicationContext())
                .inflate(R.layout.text_view_emoji, listOfMessages,false);
        tvEmoji.setText(etEmoji.getText().toString());
        return tvEmoji;
    }

    private void displayChatMessage() {

        listOfMessages.setAdapter(adapter);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==SIGN_IN_REQUEST_CODE){
            if (resultCode==RESULT_OK){
                Toast.makeText(this,
                        "Successfully signed in. Welcome!",
                        Toast.LENGTH_LONG)
                        .show();

                String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID).setValue(currentUserName);

                displayChatMessage();
            }else{
                Toast.makeText(this,
                        "We couldn't sign you in. Please try again later.",
                        Toast.LENGTH_LONG)
                        .show();

                finish();
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.menu_sign_out){
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this,
                                    "You have been signed out.",
                                    Toast.LENGTH_LONG)
                                    .show();

                            finish();
                        }
                    });
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}

