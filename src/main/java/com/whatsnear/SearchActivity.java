package com.whatsnear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.trafi.anchorbottomsheetbehavior.AnchorBottomSheetBehavior;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.GeoObjectCollection;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateSource;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.VisibleRegionUtils;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.photos.Image;
import com.yandex.mapkit.places.photos.PhotosEntry;
import com.yandex.mapkit.search.BusinessImagesObjectMetadata;
import com.yandex.mapkit.search.BusinessObjectMetadata;
import com.yandex.mapkit.search.BusinessPhotoObjectMetadata;
import com.yandex.mapkit.search.BusinessRating1xObjectMetadata;
import com.yandex.mapkit.search.Category;
import com.yandex.mapkit.search.CollectionObjectMetadata;
import com.yandex.mapkit.search.Feature;
import com.yandex.mapkit.search.Phone;
import com.yandex.mapkit.search.Properties;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.search.Snippet;
import com.yandex.runtime.Error;
import com.yandex.runtime.any.Collection;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.RemoteError;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This example shows how to add and interact with a layer that displays search results on the map.
 * Note: search API calls count towards MapKit daily usage limits. Learn more at
 * https://tech.yandex.ru/mapkit/doc/3.x/concepts/conditions-docpage/#conditions__limits
 */
public class SearchActivity extends Activity implements Session.SearchListener, CameraListener {
    /**
     * Replace "your_api_key" with a valid developer key.
     * You can get it at the https://developer.tech.yandex.ru/ website.
     */
    private final String MAPKIT_API_KEY = "17ee6c57-98f3-4007-aebf-27f98705b7a8";

    private MapView mapView;
    private EditText searchEdit;
    private SearchManager searchManager;
    private Session searchSession;

    private void submitQuery(String query) {
        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setSnippets(Snippet.BUSINESS_LIST.value + Snippet.BUSINESS_RATING1X.value + Snippet.PHOTOS.value + Snippet.BUSINESS_IMAGES.value);

        searchSession = searchManager.submit(
                query,
                VisibleRegionUtils.toPolygon(mapView.getMap().getVisibleRegion()),
                searchOptions,
                this);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
        SearchFactory.initialize(this);

        setContentView(R.layout.search);
        super.onCreate(savedInstanceState);

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.getMap().addCameraListener(this);


        mapView.getMap().move(
                new CameraPosition(new Point(59.945933, 30.320045), 14.0f, 0.0f, 0.0f));

        submitQuery("cafe");


        LinearLayout llBottomSheet = findViewById(R.id.bottom_sheet);


        mapView.getMap().addInputListener(new InputListener() {
            @Override
            public void onMapTap(@NonNull Map map, @NonNull Point point) {
                AnchorBottomSheetBehavior bottomSheetBehavior = AnchorBottomSheetBehavior.from(llBottomSheet);
                bottomSheetBehavior.setState(AnchorBottomSheetBehavior.STATE_COLLAPSED);
            }

            @Override
            public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
            }
        });


        llBottomSheet.setOnTouchListener((v, event) -> {
            mapView.setNoninteractive(true);
            final int motionEvent = event.getAction();
            if (motionEvent == MotionEvent.ACTION_UP || motionEvent == MotionEvent.ACTION_CANCEL) {
                mapView.setNoninteractive(false);
            }
            return true;
        });


        AnchorBottomSheetBehavior bottomSheetBehavior = AnchorBottomSheetBehavior.from(llBottomSheet);

        bottomSheetBehavior.addBottomSheetCallback(new AnchorBottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i, int i1) {
                if (i1 == AnchorBottomSheetBehavior.STATE_EXPANDED) {

                    view.setBackground(ContextCompat.getDrawable(view.getContext(), R.xml.shape2));
                } else {
                    view.setBackground(ContextCompat.getDrawable(view.getContext(), R.xml.shape));
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });

        bottomSheetBehavior.setState(AnchorBottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    public void onSearchResponse(@NonNull Response response) {
        MapObjectCollection mapObjects = mapView.getMap().getMapObjects();
        mapObjects.clear();

        for (GeoObjectCollection.Item searchResult : response.getCollection().getChildren()) {
            GeoObject geoObject = searchResult.getObj();
            Collection metadataContainer = geoObject.getMetadataContainer();


            Point resultLocation = searchResult.getObj().getGeometry().get(0).getPoint();
            if (resultLocation != null) {
                mapObjects.addPlacemark(
                        resultLocation,
                        ImageProvider.fromResource(this, R.drawable.search_result))
                        .addTapListener(((mapObject, point) -> {
                            showInfo(metadataContainer);
                            return true;
                        }));
            }
        }
    }

    private void showInfo(Collection metadataContainer) {
        BusinessObjectMetadata metadata = metadataContainer.getItem(BusinessObjectMetadata.class);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.bottom_sheet);
        AnchorBottomSheetBehavior bottomSheetBehavior = AnchorBottomSheetBehavior.from(linearLayout);
        bottomSheetBehavior.setState(AnchorBottomSheetBehavior.STATE_ANCHORED);
        TextView textView = (TextView) findViewById(R.id.info_name);
        textView.setText(metadata.getName());
        TextView textViewType = (TextView) findViewById(R.id.info_type);
        StringBuilder categories = new StringBuilder();
        Iterator<Category> iterator = metadata.getCategories().iterator();
        categories.append(iterator.next().getName());

        while (iterator.hasNext()) {
            categories.append(", ");
            categories.append(iterator.next().getName());
        }




        textViewType.setText(categories.toString());
        BusinessRating1xObjectMetadata ratingItem = metadataContainer.getItem(BusinessRating1xObjectMetadata.class);
        CollectionObjectMetadata collectionObjectMetadata = metadataContainer.getItem(CollectionObjectMetadata.class);

        Properties propertiesList = metadata.getProperties();

        StringBuilder stringBuilder = new StringBuilder(64);



        stringBuilder.append("\n\n\n\n");
        stringBuilder.append(metadata.getName());
        stringBuilder.append("\n");

        List<Phone> phones = metadata.getPhones();

        stringBuilder.append("Phones\n\t");
        for (Phone phone : phones) {
            stringBuilder.append("(");
            stringBuilder.append(phone.getFormattedNumber());
            stringBuilder.append(", ");
            stringBuilder.append(phone.getInfo());
            stringBuilder.append(", ");
            stringBuilder.append(phone.getType());
            stringBuilder.append(") ");
        }

        stringBuilder.append("\n");



        stringBuilder.append("metadata.getProperties(): \n");
//        if (propertiesList != null) {
//            for (Properties.Item item : propertiesList.getItems()) {
//                 stringBuilder.append(item.getKey());
//                 stringBuilder.append(" : ");
//                 stringBuilder.append(item.getValue());
//                 stringBuilder.append("\n");
//            }
//        }
        stringBuilder.append("metadata.getFeatures \n");
        for (Feature feature : metadata.getFeatures()) {
            String name = feature.getName();
            Feature.VariantValue value = feature.getValue();
            List<String> textValue = value.getTextValue();

            List<Feature.FeatureEnumValue> enumValue = value.getEnumValue();
            String aref = feature.getAref();
            String id = feature.getId();
            stringBuilder.append(name);
            stringBuilder.append("\n\ttextValue: ");
            stringBuilder.append(textValue);
            stringBuilder.append("\n\taref: ");
            stringBuilder.append(aref);
            stringBuilder.append("\n\tid: ");
            stringBuilder.append(id);
            stringBuilder.append("\n\tenumValue: ");

            if (enumValue != null) {
                for (Feature.FeatureEnumValue enumValue1 : enumValue) {
                    stringBuilder.append("(");
                    stringBuilder.append(enumValue1.getId());
                    stringBuilder.append(", ");
                    stringBuilder.append(enumValue1.getName());
                    stringBuilder.append(", ");
                    stringBuilder.append(enumValue1.getImageUrlTemplate());
                    stringBuilder.append(") ");
                }
            }
            stringBuilder.append("\n");
        }



        BusinessPhotoObjectMetadata item = metadataContainer.getItem(BusinessPhotoObjectMetadata.class);

        stringBuilder.append("Images:\n");
        for (BusinessPhotoObjectMetadata.Photo photo : item.getPhotos()) {
            String imageId = photo.getId();
            stringBuilder.append("\t");
            stringBuilder.append(imageId);
            stringBuilder.append("\n");
        }



        Logger.getLogger("META_LOGGER").log(Level.WARNING, stringBuilder.toString());
        ((TextView) findViewById(R.id.info_rating)).setText(String.valueOf(ratingItem.getScore()));
        ((TextView) findViewById(R.id.info_num)).setText(String.format(new Locale("ru"), "%d оценок", ratingItem.getRatings()));
    }

    @Override
    public void onSearchError(Error error) {
        String errorMessage = getString(R.string.unknown_error_message);
        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraPositionChanged(
            Map map,
            CameraPosition cameraPosition,
            CameraUpdateSource cameraUpdateSource,
            boolean finished) {
        if (finished) {
            submitQuery("cafe");
        }
    }
}
