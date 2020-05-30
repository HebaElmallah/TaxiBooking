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

public class CustomerLoginRegisterActivity extends AppCompatActivity {

    private Button customerLoginButton;
    private Button customerRegisterButton;
    private TextView customerRegisterLink;
    private TextView customerStatus;
    private EditText emailCustomer;
    private EditText passwordCustomer;

    private ProgressDialog loadingBar;

    private FirebaseAuth mAuth;
    private DatabaseReference customerDatabaseRef;
    private String onlineCustomerID;

    private DatabaseReference driversRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login_register);
        mAuth = FirebaseAuth.getInstance();

        customerLoginButton = (Button) findViewById(R.id.customer_login_btn);
        customerRegisterButton = (Button) findViewById(R.id.customer_register_btn);
        customerRegisterLink = (TextView) findViewById(R.id.register_customer_link);
        customerStatus = (TextView) findViewById(R.id.customer_status);
        emailCustomer = (EditText) findViewById(R.id.email_customer);
        passwordCustomer = (EditText) findViewById(R.id.password_customer);
        loadingBar = new ProgressDialog(this);

        customerRegisterButton.setVisibility(View.INVISIBLE);
        customerRegisterButton.setEnabled(false);

        customerRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customerLoginButton.setVisibility(View.INVISIBLE);
                customerRegisterLink.setVisibility(View.INVISIBLE);
                customerStatus.setText("Register Customer");

                customerRegisterButton.setVisibility(View.VISIBLE);
                customerRegisterButton.setEnabled(true);
            }
        });
        customerRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailCustomer.getText().toString();
                String password = passwordCustomer.getText().toString();
                registerCustomer(email, password);
            }
        });
        
        customerLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailCustomer.getText().toString();
                String password = passwordCustomer.getText().toString();
                sigininCustomer(email, password);
            }
        });



    }

    private void sigininCustomer(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please Write Email..", Toast.LENGTH_SHORT).show();
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please Write Password..", Toast.LENGTH_SHORT).show();
        } else {

            loadingBar.setTitle("Customer Login");
            loadingBar.setMessage("Please wait,untile we check your credential..");
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Intent customerIntent = new Intent(CustomerLoginRegisterActivity.this,DriversMapActivity.class);
                                startActivity(customerIntent);

                                Toast.makeText(CustomerLoginRegisterActivity.this, " Customer Logged Succefuly!", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            } else {
                                Toast.makeText(CustomerLoginRegisterActivity.this, " Login unsucceful..Please try again!", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });

        }

    }

    private void registerCustomer(String email, String password) {
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(CustomerLoginRegisterActivity.this, "Please Write Email..", Toast.LENGTH_SHORT).show();
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(CustomerLoginRegisterActivity.this, "Please Write Password..", Toast.LENGTH_SHORT).show();
                } else {

                    loadingBar.setTitle("Customer Registration");
                    loadingBar.setMessage("Please wait,untile you register your data..");
                    loadingBar.show();

                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        onlineCustomerID = mAuth.getCurrentUser().getUid();
                                        customerDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                                .child("users").child("customers").child(onlineCustomerID);
                                        customerDatabaseRef.setValue(true);
                                        Intent driverIntent = new Intent(CustomerLoginRegisterActivity.this,CustomersMapActivity.class);
                                        startActivity(driverIntent);
                                        Toast.makeText(CustomerLoginRegisterActivity.this, " Customer Register Succefuly!", Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    } else {
                                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer not Register..Please try again!", Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                }
                            });
                }
            }
    }
