package com.empti.uberdemo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleapiclient;
    Location mlastlocation;
    LocationRequest mlocationrequest;
    private boolean requestbol = false;

    private LatLng pickuplocation;
    private Button mlogout, mrequest , msetting;
    private Marker pickupMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mlogout = (Button)findViewById(R.id.logout);
        mlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(CustomerMapActivity.this,MainActivity.class));
                finish();
                return;
            }
        });

        mrequest = (Button)findViewById(R.id.request);
        mrequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if (requestbol){
                   requestbol = false;
                   geoQuery.removeAllListeners();
                   driverlocationref.removeEventListener(driverlocationreflistener);

                   if (driverFoundId != null){
                       DatabaseReference driverref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                       driverref.setValue(true);
                       driverFoundId = null;
                   }
                   driverfound = false;
                   radius = 1;
                   String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                   DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                   GeoFire geoFire = new GeoFire(ref);
                   geoFire.removeLocation(userid);

                   if (pickupMarker != null){
                       pickupMarker.remove();
                   }
                   mrequest.setText("Call Driver..");

               }else{
                   requestbol = true;
                   String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                   DatabaseReference ref = FirebaseDatabase.getInstance().getReference("CustomerRequest");
                   GeoFire geoFire = new GeoFire(ref);
                   geoFire.setLocation(userid, new GeoLocation(mlastlocation.getLatitude(),mlastlocation.getLongitude()));

                   pickuplocation = new LatLng(mlastlocation.getLatitude(),mlastlocation.getLongitude());
                   pickupMarker = mMap.addMarker(new MarkerOptions().position(pickuplocation).title("PickUp Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));

                   mrequest.setText("Getting Your Driver..");

                   getCloserDriver();
               }

            }
        });
        msetting = (Button)findViewById(R.id.setting);
        msetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerMapActivity.this,CustomerSettingsActivity.class) );
                return;

            }
        });
    }
    private int radius = 1;
    private boolean driverfound = false;
    private String driverFoundId;

    GeoQuery geoQuery;
    private void getCloserDriver(){
        DatabaseReference driverlocation =FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        GeoFire geoFire = new GeoFire(driverlocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickuplocation.latitude,pickuplocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverfound && requestbol) {
                    driverfound = true;
                    driverFoundId = key;

                    DatabaseReference driverref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                   String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideId",customerId);
                    driverref.updateChildren(map);

                    getDriverlocation();
                    mrequest.setText("Looking for Driver Location");
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
                if (!driverfound){
                    radius++;
                    getCloserDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
    private Marker mDriverMarker;
    private DatabaseReference driverlocationref;
    private ValueEventListener driverlocationreflistener;
    private void getDriverlocation(){
        driverlocationref = FirebaseDatabase.getInstance().getReference().child("DriversWorking").child(driverFoundId).child("l");
        driverlocationreflistener = driverlocationref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestbol){
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationlat= 0;
                    double  locationlng =0;
                    mrequest.setText("Driver Found..");
                    if (map.get(0) != null){
                        locationlat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null){
                        locationlng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverlatlng = new LatLng(locationlat,locationlng);
                    if (mDriverMarker != null){
                        mDriverMarker.remove();
                    }
                    Location loc1 = new Location("");
                    loc1.setLatitude(pickuplocation.latitude);
                    loc1.setLongitude(pickuplocation.longitude);

                    Location loc2 = new Location("");
                    loc2.setLatitude(driverlatlng.latitude);
                    loc2.setLongitude(driverlatlng.longitude);

                    float distance = loc1.distanceTo(loc2);

                    if(distance<100){
                        mrequest.setText("Driver's Here");
                    }else {
                        mrequest.setText("Driver Found" + String.valueOf(distance));
                    }


                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverlatlng).title("Driver location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.car)));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleapiclient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleapiclient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        mlastlocation = location;

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mlocationrequest = new LocationRequest();
        mlocationrequest.setInterval(1000);
        mlocationrequest.setFastestInterval(1000);
        mlocationrequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleapiclient, mlocationrequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();


    }
}

