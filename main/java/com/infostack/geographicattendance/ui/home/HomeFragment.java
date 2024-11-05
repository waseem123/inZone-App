package com.infostack.geographicattendance.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.infostack.geographicattendance.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient locationClient;
    private ImageView selfiePreview;
    private TextView locationText, timestampText;
    private boolean isPunchIn = true;

    // ActivityResultLauncher for requesting camera permission
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    captureSelfie();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required to take a selfie", Toast.LENGTH_SHORT).show();
                }
            });

    // ActivityResultLauncher for requesting location permission
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(requireContext(), "Location permission is required to fetch location", Toast.LENGTH_SHORT).show();
                }
            });

    // ActivityResultLauncher for camera intent
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    selfiePreview.setImageBitmap(photo);

                    // Fetch location and update timestamp after selfie capture
                    fetchLocation();
                    updateTimestamp();

                    String punchType = isPunchIn ? "Punch In" : "Punch Out";
                    Toast.makeText(requireContext(), punchType + " successful!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize UI components
        selfiePreview = binding.selfiePreview;
        locationText = binding.locationText;
        timestampText = binding.timestampText;
        Button punchInButton = binding.punchInButton;
        Button punchOutButton = binding.punchOutButton;

        // Initialize location client
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Set up Punch In button
        punchInButton.setOnClickListener(v -> {
            isPunchIn = true;
            checkCameraPermission();
        });

        // Set up Punch Out button
        punchOutButton.setOnClickListener(v -> {
            isPunchIn = false;
            checkCameraPermission();
        });

        // Observe ViewModel text updates
        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        return root;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            captureSelfie();
        }
    }

    private void captureSelfie() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        String locText = "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude();
                        locationText.setText(locText);
                    } else {
                        locationText.setText("Location not available");
                    }
                }
            });
        }
    }

    private void updateTimestamp() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String punchText = isPunchIn ? "Punch In at: " : "Punch Out at: ";
        timestampText.setText(punchText + timestamp);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
