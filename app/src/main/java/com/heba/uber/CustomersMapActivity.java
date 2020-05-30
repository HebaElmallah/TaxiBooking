package com.heba.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PathPermission;
import android.graphics.Path;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback ,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    Marker driverMarker , pickUpMarker;
    private DatabaseReference driversRef;
    private Button customerLogOutButton;
    private Button callCabCarButton;
    String customerID;
    private DatabaseReference customerDatabaseRef;
    private LatLng customerPickUpLocation;
    private DatabaseReference driverAvailableRef;
    private int radius = 1;
    private Boolean driverFound = false , requestType = false;
    private String driverFoundID;
    private DatabaseReference driverLocationRef;
    private  ValueEventListener driverLocationRefListener;
    GeoQuery geoQuery;
    private Button settingsButton;

    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView profilePic;
    private RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        settingsButton = (Button) findViewById(R.id.customer_settings_btn);
        customerLogOutButton = (Button) findViewById(R.id.customer_logout_btn);
        callCabCarButton = (Button) findViewById(R.id.call_a_car_btn);

        txtCarName =  findViewById(R.id.name_driver);
        txtName =  findViewById(R.id.car_name_driver);
        txtPhone =  findViewById(R.id.phone_driver);
        profilePic =  findViewById(R.id.profile_image_driver);
        relativeLayout =  findViewById(R.id.rell);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child(" customer request");
        driverAvailableRef = FirebaseDatabase.getInstance().getReference().child("driver availble");
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driver working");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        customerLogOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mAuth.signOut();
                logOutCustomer();

            }
        });

        callCabCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestType)
                {
                    requestType = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverFound != null)
                    {
                        driversRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverFoundID).child("customerRideID");
                        driversRef.removeValue();
                        driverFoundID = null;
                    }
                    driverFound =false;
                    radius = 1;
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.removeLocation(customerID);
                    if(pickUpMarker != null)
                    {
                        pickUpMarker.remove();
                    }

                    if(driverMarker != null)
                    {
                        driverMarker.remove();
                    }

                    callCabCarButton.setText("call a cab");
                    relativeLayout.setVisibility(View.GONE);
                }
                else
                {

                    requestType = true;
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.setLocation(customerID , new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));
                    customerPickUpLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(customerPickUpLocation).title("My Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                    callCabCarButton.setText("Getting your Driver");
                    getClosestDriverCap();
                }

            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomersMapActivity.this, SettingsActivity.class);
                intent.putExtra("Type", "Customers");
                startActivity(intent);
            }
        });
    }

    private void getClosestDriverCap() {
        GeoFire geoFire = new GeoFire(driverAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPickUpLocation.latitude,customerPickUpLocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestType)
                {
                    driverFound = true;
                    driverFoundID = key;

                    driversRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("customerRideID" , customerID);
                    driversRef.updateChildren(driverMap);

                    gettingDriverLocation();

                    callCabCarButton.setText("looking for driver location");

                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound)
                {
                    radius = radius + 1;
                    getClosestDriverCap();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void gettingDriverLocation() {
         driverLocationRefListener = driverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists() && requestType)
                        {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double locationLat = 0;
                            double locationLng = 0;
                            callCabCarButton.setText("driver found");

                            relativeLayout.setVisibility(View.VISIBLE);
                            getAssignedDriverInformation();

                            if(driverLocationMap.get(0) != null)
                            {
                                locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if(driverLocationMap.get(1) != null)
                            {
                                locationLng = Double.parseDouble(driverLocationMap.get(0).toString());
                            }

                            LatLng driverLatLog = new LatLng(locationLat, locationLng);
                            if(driverMarker != null)
                            {
                                driverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(customerPickUpLocation.latitude);
                            location1.setLongitude(customerPickUpLocation.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(driverLatLog.latitude);
                            location2.setLongitude(driverLatLog.longitude);

                            float distance = location1.distanceTo(location2);

                            if(distance <90)
                            {
                                callCabCarButton.setText("drivers reached");
                            }
                            else
                            {
                                callCabCarButton.setText("driverfound"+ String.valueOf(distance));
                            }


                            driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLog).title("your driver is here"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    private void logOutCustomer() {
        Intent welcomeIntent = new Intent(CustomersMapActivity.this,WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.setMyLocationEnabled(true);
//        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION  ) != ){
//
//            return;
//        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {


        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
//        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PathPermission){
//
//            return;
//        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private void getAssignedDriverInformation()
    {
      DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users")
                                      .child("Drivers").child(driverFoundID);

      reference.addValueEventListener(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

              if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() >0)
              {

                  String name = dataSnapshot.child("name").getValue().toString();
                  String phone = dataSnapshot.child("phone").getValue().toString();
                  String car = dataSnapshot.child("car").getValue().toString();

                  txtName.setText(name);
                  txtPhone.setText(phone);
                  txtCarName.setText(car);

                  if(dataSnapshot.hasChild("image"))
                  {
                      String image = dataSnapshot.child("image").getValue().toString();
                      Picasso.get().load(image).into(profilePic);
                  }

              }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError databaseError) {

          }
      });
    }

}
