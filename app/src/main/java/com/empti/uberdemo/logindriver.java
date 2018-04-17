package com.empti.uberdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class logindriver extends AppCompatActivity {
    private Button mlogin, mregister;
    private EditText memail, mpassword;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthlistener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logindriver);

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthlistener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    startActivity(new Intent(logindriver.this,DriverMapActivity.class));
                    finish();
                    return;
                }
            }
        };

        mlogin = (Button)findViewById(R.id.login);
        mregister =(Button)findViewById(R.id.register);
        memail = (EditText)findViewById(R.id.email);
        mpassword = (EditText)findViewById(R.id.password);

        mregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = memail.getText().toString();
                final String password = mpassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(logindriver.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()){
                            Toast.makeText(logindriver.this, "Sign up Error", Toast.LENGTH_SHORT).show();
                        }else {
                            String userid = mAuth.getCurrentUser().getUid();
                            DatabaseReference currentuserdb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userid);
                            currentuserdb.setValue(true);
                        }
                    }
                });
            }
        });

        mlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = memail.getText().toString();
                final String password = mpassword.getText().toString();
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(logindriver.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()){
                            Toast.makeText(logindriver.this, "Login In Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthlistener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.addAuthStateListener(firebaseAuthlistener);
    }
}
