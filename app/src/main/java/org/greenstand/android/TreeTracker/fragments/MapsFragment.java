package org.greenstand.android.TreeTracker.fragments;


import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.util.IOUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.greenstand.android.TreeTracker.activities.MainActivity;
import org.greenstand.android.TreeTracker.application.Permissions;
import org.greenstand.android.TreeTracker.R;
import org.greenstand.android.TreeTracker.utilities.TreeImage;
import org.greenstand.android.TreeTracker.utilities.ValueHelper;
import org.greenstand.android.TreeTracker.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import timber.log.Timber;

public class MapsFragment extends Fragment
        implements OnClickListener, OnMarkerClickListener, OnMapReadyCallback, View.OnLongClickListener {
    private static final String TAG = "MapsFragment";

    public interface LocationDialogListener {
		void refreshMap();
	}

	LocationDialogListener mSettingCallback;

	private ArrayList<Marker> redPulsatingMarkers = new ArrayList<Marker>();
	private ArrayList<Marker> redToGreenPulsatingMarkers = new ArrayList<Marker>();

	private SharedPreferences mSharedPreferences;
	private int mCurrentRedToGreenMarkerColor = -1;
	private boolean paused = false;
	protected int mCurrentMarkerColor;
	private static View view;


	public MapsFragment() {
		//some overrides and settings go here
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mSettingCallback = (LocationDialogListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString()
					+ " must implement LocationDialogListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}

	@Override
	public void onPause() {
		super.onPause();

		paused = true;
	}

	@Override
	public void onDestroy() {
        super.onDestroy();
    }

	@Override
	public void onResume() {
		super.onResume();
		if (paused) {
			((SupportMapFragment) getChildFragmentManager()
					.findFragmentById(R.id.map)).getMapAsync(this);
		}
		paused = false;

		mCurrentRedToGreenMarkerColor = R.drawable.green_pin;
		mCurrentMarkerColor = R.drawable.red_pin_pulsating_4;

		handler.post(new Runnable() {
			public void run() {
				if (mCurrentRedToGreenMarkerColor == R.drawable.red_pin) {
					mCurrentRedToGreenMarkerColor = R.drawable.green_pin;
				} else {
					mCurrentRedToGreenMarkerColor = R.drawable.red_pin;
				}
				for (Marker marker : redToGreenPulsatingMarkers) {
					marker.setIcon(BitmapDescriptorFactory.fromResource(mCurrentRedToGreenMarkerColor));
				}

				if (!paused)
					handler.postDelayed(this, 500);
			}
		});

		handler.post(new Runnable() {
			public void run() {
				if (mCurrentMarkerColor == R.drawable.red_pin) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_1;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_1) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_2;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_2) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_3;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_3) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_4;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_4) {
					mCurrentMarkerColor = R.drawable.red_pin;
				}

				for (Marker marker : redPulsatingMarkers) {
					marker.setIcon(BitmapDescriptorFactory.fromResource(mCurrentMarkerColor));
				}

				if (!paused)
					handler.postDelayed(this, 200);
			}
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		try {
			SupportMapFragment fragment = (SupportMapFragment) getActivity()
					.getSupportFragmentManager().findFragmentById(
							R.id.map);
			if (fragment != null)
				getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();

		} catch (IllegalStateException e) {
			//handle this situation because you are necessary will get
			//an exception here :-(
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		if (view != null) {
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}
		try {
			view = inflater.inflate(R.layout.fragment_map, container, false);
		} catch (InflateException e) {
	        /* map is already there, just return view as it is */
		}

		View v = view;

		mSharedPreferences = getActivity().getSharedPreferences(
				"org.greenstand.android", Context.MODE_PRIVATE);

        if (!((AppCompatActivity) getActivity()).getSupportActionBar().isShowing()) {
            Timber.d("MainActivity", "toolbar hide");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }
		((TextView) getActivity().findViewById(R.id.toolbar_title)).setText(R.string.map);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        FloatingActionButton fab = (FloatingActionButton) v.findViewById(R.id.fab);
        fab.setOnClickListener(this);
        if(BuildConfig.BUILD_TYPE == "dev"){
            fab.setOnLongClickListener(this);
        }

		((SupportMapFragment) getChildFragmentManager()
				.findFragmentById(R.id.map)).getMapAsync(this);


		TextView mapGpsAccuracy = ((TextView) v.findViewById(R.id.fragment_map_gps_accuracy));
		TextView mapGpsAccuracyValue = ((TextView) v.findViewById(R.id.fragment_map_gps_accuracy_value));

		int minAccuracy = mSharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING);

		if (mapGpsAccuracy != null) {
			if (MainActivity.Companion.getMCurrentLocation() != null) {
				if (MainActivity.Companion.getMCurrentLocation().hasAccuracy() && (MainActivity.Companion.getMCurrentLocation().getAccuracy() < minAccuracy)) {
					mapGpsAccuracy.setTextColor(Color.GREEN);
					mapGpsAccuracyValue.setTextColor(Color.GREEN);
					mapGpsAccuracyValue.setText(Integer.toString(Math.round(MainActivity.Companion.getMCurrentLocation().getAccuracy())) + " " + getResources().getString(R.string.meters));
					MainActivity.Companion.setMAllowNewTreeOrUpdate(true);
				} else {
					mapGpsAccuracy.setTextColor(Color.RED);
					MainActivity.Companion.setMAllowNewTreeOrUpdate(false);

					if (MainActivity.Companion.getMCurrentLocation().hasAccuracy()) {
						mapGpsAccuracyValue.setTextColor(Color.RED);
						mapGpsAccuracyValue.setText(Integer.toString(Math.round(MainActivity.Companion.getMCurrentLocation().getAccuracy())) + " " + getResources().getString(R.string.meters));
					} else {
						mapGpsAccuracyValue.setTextColor(Color.RED);
						mapGpsAccuracyValue.setText("N/A");
					}
				}
			} else {
				if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
						ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					requestPermissions(
							new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
							Permissions.INSTANCE.getMY_PERMISSION_ACCESS_COURSE_LOCATION());
				}
				mapGpsAccuracy.setTextColor(Color.RED);
				mapGpsAccuracyValue.setTextColor(Color.RED);
				mapGpsAccuracyValue.setText("N/A");
				MainActivity.Companion.setMAllowNewTreeOrUpdate(false);
			}

		}

		return v;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == Permissions.INSTANCE.getMY_PERMISSION_ACCESS_COURSE_LOCATION()) {
			mSettingCallback.refreshMap();
		}
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

		}
	};

	private Fragment fragment;

	private Bundle bundle;

	private FragmentTransaction fragmentTransaction;


	public void onClick(View v) {


		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

		Cursor photoCursor;
		switch (v.getId()) {
            case R.id.fab:
            	Timber.d(TAG, "fab click");
                if (MainActivity.Companion.getMAllowNewTreeOrUpdate() || BuildConfig.GPS_ACCURACY.equals("off")) {
					fragment = new NewTreeFragment();
					bundle = getActivity().getIntent().getExtras();
					fragment.setArguments(bundle);

					fragmentTransaction = getActivity().getSupportFragmentManager()
							.beginTransaction();
					fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.NEW_TREE_FRAGMENT).commit();
				} else {
					Toast.makeText(getActivity(), "Insufficient GPS accuracy.", Toast.LENGTH_SHORT).show();
				}
				break;
//			case R.id.fragment_map_update_tree:
//
//				if (MainActivity.mAllowNewTreeOrUpdate) {
//					SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
//
////					String query = "select * from tree_photo " +
////							"left outer join tree on tree._id = tree_id " +
////							"left outer join photo on photo._id = photo_id " +
////							"left outer join location on location._id = photo.location_id " +
////							"where is_outdated = 'N'";
//
//					String query = "select * from tree " +
//							"left outer join location on location._id = tree.location_id " +
//							"left outer join tree_photo on tree._id = tree_id " +
//							"left outer join photo on photo._id = photo_id ";
//
//					Log.e("query", query);
//
//					photoCursor = db.rawQuery(query, null);
//
//					if (photoCursor.getCount() <= 0) {
//						Toast.makeText(getActivity(), "No trees to update", Toast.LENGTH_SHORT).show();
//						db.close();
//						return;
//					}
//
//					db.close();
//
//					fragment = new UpdateTreeFragment();
//					bundle = getActivity().getIntent().getExtras();
//					fragment.setArguments(bundle);
//
//					fragmentTransaction = getActivity().getSupportFragmentManager()
//							.beginTransaction();
//					fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.UPDATE_TREE_FRAGMENT).commit();
//				} else {
//					Toast.makeText(getActivity(), "Insufficient GPS accuracy.", Toast.LENGTH_SHORT).show();
//				}
//
//				break;
		}


	}

	// For debug analysis purposes only
    @Override
    public boolean onLongClick(View view) {

        Toast.makeText(getActivity(), "Adding lot of trees", Toast.LENGTH_LONG).show();


        // programmatically add 500 trees, for analysis only
        // this is on the main thread for ease, in Kotlin just make a Coroutine
        SQLiteDatabase dbw = MainActivity.Companion.getDbHelper().getWritableDatabase();

        int userId = -1;

        for(int i=0; i<500; i++) {

            ContentValues locationContentValues = new ContentValues();
            locationContentValues.put("accuracy",
                    Float.toString(MainActivity.Companion.getMCurrentLocation().getAccuracy()));
            locationContentValues.put("lat",
                    Double.toString(MainActivity.Companion.getMCurrentLocation().getLatitude() + (Math.random() - .5) / 1000));
            locationContentValues.put("long",
                    Double.toString(MainActivity.Companion.getMCurrentLocation().getLongitude() + (Math.random() - .5) / 1000));
            locationContentValues.put("user_id", userId);

            long locationId = dbw.insert("location", null, locationContentValues);

            long photoId = -1;
            try {
                InputStream myInput = getActivity().getAssets().open("testtreeimage.jpg");
                File f = TreeImage.INSTANCE.createImageFile(getActivity());
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(IOUtils.toByteArray(myInput));
                fos.close();

                ContentValues photoContentValues = new ContentValues();
                photoContentValues.put("user_id", userId);
                photoContentValues.put("location_id", locationId);
                photoContentValues.put("name", f.getAbsolutePath());

                photoId = dbw.insert("photo", null, photoContentValues);
                //Timber.d("photoId " + Long.toString(photoId));

            } catch (IOException e) {
                e.printStackTrace();
            }


            ContentValues treeContentValues = new ContentValues();
            treeContentValues.put("user_id", userId);
            treeContentValues.put("location_id", locationId);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            treeContentValues.put("time_created", dateFormat.format(new Date()));
            treeContentValues.put("time_updated", dateFormat.format(new Date()));

            long treeId = dbw.insert("tree", null, treeContentValues);


            ContentValues treePhotoContentValues = new ContentValues();
            treePhotoContentValues.put("tree_id", treeId);
            treePhotoContentValues.put("photo_id", photoId);
            long treePhotoId = dbw.insert("tree_photo", null, treePhotoContentValues);
            //Timber.d("treePhotoId " + Long.toString(treePhotoId));
        }

        Toast.makeText(getActivity(), "Lots of trees added", Toast.LENGTH_LONG).show();

        return true;
    }

    public boolean onMarkerClick(Marker marker) {
		fragment = new TreePreviewFragment();
		bundle = getActivity().getIntent().getExtras();

		if (bundle == null)
			bundle = new Bundle();

		bundle.putString(ValueHelper.TREE_ID, marker.getTitle());
		fragment.setArguments(bundle);

		fragmentTransaction = getActivity().getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.TREE_PREVIEW_FRAGMENT).commit();
		return true;
	}


	@Override
	public void onMapReady(GoogleMap map) {


		if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		map.setMyLocationEnabled(true);

		SQLiteDatabase db = MainActivity.Companion.getDbHelper().getReadableDatabase();

		Cursor treeCursor = db.rawQuery("select *, tree._id as tree_id from tree left outer join location on location_id = location._id where is_missing = 'N'", null);
		treeCursor.moveToFirst();

		redToGreenPulsatingMarkers.clear();
		redPulsatingMarkers.clear();

		if (treeCursor.getCount() > 0) {

			LatLng latLng = null;

			do {

				//Timber.d("tree id " + String.valueOf(treeCursor.getLong(treeCursor.getColumnIndex("_id"))));

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

/*
				Boolean isSynced = Boolean.parseBoolean(treeCursor.getString(treeCursor.getColumnIndex("is_synced")));

				//Timber.d("issynced " + Boolean.toString(isSynced));

				Date dateForUpdate = new Date();
				try {
					dateForUpdate = dateFormat.parse(treeCursor.getString(treeCursor.getColumnIndex("time_for_update")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Date updated = new Date();
				try {
					updated = dateFormat.parse(treeCursor.getString(treeCursor.getColumnIndex("time_updated")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Date created = new Date();
				try {
					created = dateFormat.parse(treeCursor.getString(treeCursor.getColumnIndex("time_created")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
*/
				boolean priority = treeCursor.getString(treeCursor.getColumnIndex("is_priority")).equals("Y");
				latLng = new LatLng(Double.parseDouble(treeCursor.getString(treeCursor.getColumnIndex("lat"))),
						Double.parseDouble(treeCursor.getString(treeCursor.getColumnIndex("long"))));


				MarkerOptions markerOptions = new MarkerOptions()
						.title(Long.toString(treeCursor.getLong(treeCursor.getColumnIndex("tree_id"))))// set Id instead of title
						.icon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin))
						.position(latLng);
				Marker marker = map.addMarker(markerOptions);



				if (priority) {
					redPulsatingMarkers.add(marker);
					continue;
				}


				// This 'update' logic is not currently in use
				/*if (dateForUpdate.before(new Date())) {


					Log.e("updated", "should be red");

					Calendar calendar = Calendar.getInstance();
					calendar.setTime(dateForUpdate);

					Calendar currCalendar = Calendar.getInstance();
					currCalendar.setTime(new Date());

					marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_pin));
				}
				*/

//		        Log.i("updated", "*************");
//		        if (created.before(updated) && !isSynced) {
//		        	redToGreenPulsatingMarkers.add(marker);
//		        }
			} while (treeCursor.moveToNext());


			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

		} else {
			if (MainActivity.Companion.getMCurrentLocation() != null) {
				LatLng myLatLng = new LatLng(MainActivity.Companion.getMCurrentLocation().getLatitude(), MainActivity.Companion.getMCurrentLocation().getLongitude());
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10));
			}
		}

		map.setOnMarkerClickListener(MapsFragment.this);

		// Other supported types include: MAP_TYPE_NORMAL,
		// MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID and MAP_TYPE_NONE
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	}
}
