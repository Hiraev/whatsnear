package com.whatsnear;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.whatsnear.R;
import com.yandex.runtime.image.ImageProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a basic example that displays a map and sets camera focus on the target location.
 * Note: When working on your projects, remember to request the required permissions.
 */
public class MapActivity extends Activity implements UserLocationObjectListener{
    /**
     * Replace "your_api_key" with a valid developer key.
     * You can get it at the https://developer.tech.yandex.ru/ website.
     */
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private final String MAPKIT_API_KEY = "17ee6c57-98f3-4007-aebf-27f98705b7a8";
    private CircleMapObject userPosition;
    private MapView mapView;
    private UserLocationLayer userLocationLayer;
    private float zoom = 14.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * Set the api key before calling initialize on MapKitFactory.
         * It is recommended to set api key in the Application.onCreate method,
         * but here we do it in each activity to make examples isolated.
         */
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        /**
         * Initialize the library to load required native libraries.
         * It is recommended to initialize the MapKit library in the Activity.onCreate method
         * Initializing in the Application.onCreate method may lead to extra calls and increased battery use.
         */
        MapKitFactory.initialize(this);
        // Now MapView can be created.
        setContentView(R.layout.map);
        super.onCreate(savedInstanceState);
        mapView = (MapView) findViewById(R.id.mapview);


//        Ставив темную тему
        mapView.getMap().setNightModeEnabled(true);

        int checked1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int checked2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (checked1 == PackageManager.PERMISSION_GRANTED
                && checked2 == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            Task<Location> task = mFusedLocationClient.getLastLocation();
            task.addOnCompleteListener((locationTask) -> {
                float longitude = (float) locationTask.getResult().getLongitude();
                float latitude = (float) locationTask.getResult().getLatitude();
                Point pos = new Point(latitude, longitude);
                mapView.getMap().move(
                        new CameraPosition(pos, zoom, 1, 1));



                userPosition = mapView.getMap().getMapObjects().addCircle(new Circle(new Point(latitude, longitude), 100), Color.BLACK, 2, Color.RED);
                userPosition.setZIndex(100);

                userPosition.addTapListener((mapObject, point) -> {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "ВАВ, РАБОТАЕТ", Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                });
            });

        } else {
            Logger logger = Logger.getLogger("MY_LOGGER");
            logger.log(Level.WARNING, "SOMETHING IS WRONG");
        }


        userLocationLayer = mapView.getMap().getUserLocationLayer();

        userLocationLayer.setEnabled(true);
        userLocationLayer.setHeadingEnabled(true);
        userLocationLayer.setObjectListener(this);
    }

    @Override
    protected void onStop() {
        // Activity onStop call must be passed to both MapView and MapKit instance.
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        // Activity onStart call must be passed to both MapView and MapKit instance.
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    public void onObjectAdded(@NonNull UserLocationView userLocationView) {
        float direction = userLocationView.getArrow().getDirection();
        final Point geometry = userLocationView.getArrow().getGeometry();

//        mapView.getMap().move(new CameraPosition(geometry, zoom, 1, 1));
        userPosition.setGeometry(new Circle(geometry, 100));

        userLocationView.getPin().useCompositeIcon().setIcon(
                "pin",
                ImageProvider.fromResource(this, R.drawable.user_arrow),
                new IconStyle().setAnchor(new PointF(0.5f, 0.5f))
                        .setRotationType(RotationType.NO_ROTATION)
                        .setZIndex(1f)
                        .setScale(0.5f)
        );
        userLocationView.getPin().setDirection(direction);
        Logger.getLogger("MYLOGGER").log(Level.WARNING, String.valueOf(direction));
    }

    @Override
    public void onObjectRemoved(@NonNull UserLocationView userLocationView) {
    }

    @Override
    public void onObjectUpdated(@NonNull UserLocationView userLocationView, @NonNull ObjectEvent objectEvent) {
    }
}
