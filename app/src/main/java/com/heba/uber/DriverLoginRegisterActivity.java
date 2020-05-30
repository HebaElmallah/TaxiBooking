package com.heba.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

public class DriverLoginRegisterActivity extends AppCompatActivity {

    private Button driverLoginButton;
    private Button driverRegisterButton;
    private TextView driverRegisterLink;
    private TextView driverStatus;
    private EditText emailDriver;
    private EditText passwordDriver;
    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;
    private DatabaseReference driverDatabaseRef;
    private String onlineDriverID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_register);

        mAuth = FirebaseAuth.getInstance();

        driverLoginButton =(Button) findViewById(R.id.driver_login_btn);
        driverRegisterButton = (Button) findViewById(R.id.driver_register_btn);
        driverRegisterLink = (TextView) findViewById(R.id.driver_register_link);
        driverStatus = (TextView) findViewById(R.id.driver_status);
        emailDriver = (EditText) findViewById(R.id.email_driver);
        passwordDriver = (EditText) findViewById(R.id.password_driver);

        loadingBar = new ProgressDialog(this);


        driverRegisterButton.setVisibility(View.INVISIBLE);
        driverRegisterButton.setEnabled(false);

        driverRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driverLoginButton.setVisibility(View.INVISIBLE);
                driverRegisterLink.setVisibility(View.INVISIBLE);
                driverStatus.setText("Register Customer");

                driverRegisterButton.setVisibility(View.VISIBLE);
                driverRegisterButton.setEnabled(true);
            }
        });

        driverRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailDriver.getText().toString();
                String password = passwordDriver.getText().toString();

                registerDeriver(email,password);
            }
        });

        driverLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailDriver.getText().toString();
                String password = passwordDriver.getText().toString();

               signinDriver(email,password);

            }
        });

    }

    private void signinDriver(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(DriverLoginRegisterActivity.this, "Please Write Email..", Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(DriverLoginRegisterActivity.this, "Please Write Password..", Toast.LENGTH_SHORT).show();
        } else {

            loadingBar.setTitle("Driver Login");
            loadingBar.setMessage("Please wait,untile we check your credential..");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Intent driverIntent = new Intent(DriverLoginRegisterActivity.this,DriversMapActivity.class);
                                startActivity(driverIntent);
                                Toast.makeText(DriverLoginRegisterActivity.this, " Driver Logged Succefuly!", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();

                            } else {
                                Toast.makeText(DriverLoginRegisterActivity.this, " Login Un Succeful..Please try again!", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });

        }

    }
    private void registerDeriver(String email ,String password){

        if(TextUtils.isEmpty(email)){
            Toast.makeText(DriverLoginRegisterActivity.this,"Please Write Email..",Toast.LENGTH_SHORT).show();
        }

         if(TextUtils.isEmpty(password)){
             Toast.makeText(DriverLoginRegisterActivity.this,"Please Write Password..",Toast.LENGTH_SHORT).show();
         }
         else {

             loadingBar.setTitle("Driver Registration");
             loadingBar.setMessage("Please wait,untile you register your data..");
             loadingBar.show();

             mAuth.createUserWithEmailAndPassword(email, password)
                     .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                         @Override
                         public void onComplete(@NonNull Task<AuthResult> task) {
                           if(task.isSuccessful())
                           {
                               onlineDriverID = mAuth.getCurrentUser().getUid();
                               driverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                       .child("users").child("drivers").child(onlineDriverID);

                               driverDatabaseRef.setValue(true);
                               Intent driverIntent = new Intent(DriverLoginRegisterActivity.this,DriversMapActivity.class);
                               startActivity(driverIntent);

                               Toast.makeText(DriverLoginRegisterActivity.this," Driver Register Succefuly!", Toast.LENGTH_SHORT).show();
                               loadingBar.dismiss();


                           }
                           else {
                               Toast.makeText(DriverLoginRegisterActivity.this,"Driver not Register..Please try again!",Toast.LENGTH_SHORT).show();
                               loadingBar.dismiss();
                           }
                         }
                     });
         }

    }
}
