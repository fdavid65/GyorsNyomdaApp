package com.example.gyorsnyomdaapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter; // Spinnerhez
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;    // RadioButton-hoz
import android.widget.RadioGroup;     // RadioGroup-hoz
import android.widget.Spinner;        // Spinnerhez
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";

    private Button buttonSelectFile, buttonUploadFile;
    private TextView textViewSelectedFileName;
    private ImageView imageViewUploadPreview;
    private EditText editTextCopies;
    private RadioGroup radioGroupColorMode;
    private Spinner spinnerPaperSize;
    private RadioGroup radioGroupPaperType;
    private EditText editTextNotes;
    private ProgressBar progressBarUpload;

    private Uri selectedFileUri;
    private String selectedFileName;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Toolbar toolbar = findViewById(R.id.toolbar_upload);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        buttonSelectFile = findViewById(R.id.buttonSelectFile);
        buttonUploadFile = findViewById(R.id.buttonUploadFile);
        textViewSelectedFileName = findViewById(R.id.textViewSelectedFileName);
        imageViewUploadPreview = findViewById(R.id.imageViewUploadPreview);
        editTextCopies = findViewById(R.id.editTextCopies);
        radioGroupColorMode = findViewById(R.id.radioGroupColorMode);
        spinnerPaperSize = findViewById(R.id.spinnerPaperSize);
        radioGroupPaperType = findViewById(R.id.radioGroupPaperType);
        editTextNotes = findViewById(R.id.editTextNotes);
        progressBarUpload = findViewById(R.id.progressBarUpload);

        // Spinner feltöltése papírméretekkel
        ArrayAdapter<String> paperSizeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, PrintJob.PAPER_SIZES);
        paperSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaperSize.setAdapter(paperSizeAdapter);
        // Alapértelmezett kiválasztás
        int defaultA4Position = Arrays.asList(PrintJob.PAPER_SIZES).indexOf("A4");
        if (defaultA4Position >= 0) {
            spinnerPaperSize.setSelection(defaultA4Position);
        }

        getSupportActionBar().setTitle("Fájl Feltöltése");
        buttonUploadFile.setText("Feltöltés és megrendelés");
        buttonUploadFile.setEnabled(false);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            selectedFileName = getFileName(selectedFileUri);
                            textViewSelectedFileName.setText(selectedFileName);
                            buttonUploadFile.setEnabled(true);

                            if (isImageFile(selectedFileName)) {
                                imageViewUploadPreview.setVisibility(View.VISIBLE);
                                Picasso.get()
                                        .load(selectedFileUri)
                                        .fit()
                                        .placeholder(R.drawable.ic_placeholder_image)
                                        .error(R.drawable.ic_error_image)
                                        .into(imageViewUploadPreview);
                            } else {
                                imageViewUploadPreview.setVisibility(View.GONE);
                                imageViewUploadPreview.setImageDrawable(null);
                            }
                        } else {
                            Toast.makeText(this, "Nem sikerült a fájl kiválasztása", Toast.LENGTH_SHORT).show();
                            resetFileSelectionUI();
                        }
                    }
                });

        buttonSelectFile.setOnClickListener(v -> openFilePicker());
        buttonUploadFile.setOnClickListener(v -> uploadFileAndCreateJob());
    }

    private void resetFileSelectionUI() {
        selectedFileUri = null;
        selectedFileName = null;
        textViewSelectedFileName.setText("Nincs fájl kiválasztva");
        imageViewUploadPreview.setVisibility(View.GONE);
        Picasso.get().cancelRequest(imageViewUploadPreview);
        imageViewUploadPreview.setImageDrawable(null);
        buttonUploadFile.setEnabled(false);
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        String lowerCaseFileName = fileName.toLowerCase();
        int lastDot = lowerCaseFileName.lastIndexOf(".");
        if (lastDot > 0 && lastDot < lowerCaseFileName.length() - 1) {
            String extension = lowerCaseFileName.substring(lastDot + 1);
            return IMAGE_EXTENSIONS.contains(extension);
        }
        return false;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri == null) return "ismeretlen_fajl";
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            } catch (Exception e) { Log.e(TAG, "Error getting file name from content URI", e); }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result == null ? "ismeretlen_fajl" : result;
    }

    private void uploadFileAndCreateJob() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show(); return; }
        if (selectedFileUri == null || selectedFileName == null || selectedFileName.isEmpty()) { Toast.makeText(this, "Nincs fájl kiválasztva!", Toast.LENGTH_SHORT).show(); return; }

        String copiesStr = editTextCopies.getText().toString().trim();
        if (TextUtils.isEmpty(copiesStr)) { editTextCopies.setError("Példányszám megadása kötelező"); editTextCopies.requestFocus(); return; }
        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
            if (copies <= 0) { editTextCopies.setError("A példányszám legalább 1 kell legyen"); editTextCopies.requestFocus(); return; }
        } catch (NumberFormatException e) { editTextCopies.setError("Érvénytelen számformátum"); editTextCopies.requestFocus(); return; }

        String colorMode = (radioGroupColorMode.getCheckedRadioButtonId() == R.id.radioButtonColor) ? PrintJob.COLOR_MODE_COLOR : PrintJob.COLOR_MODE_BW;
        String paperType = (radioGroupPaperType.getCheckedRadioButtonId() == R.id.radioButtonHardPaper) ? PrintJob.PAPER_TYPE_HARD : PrintJob.PAPER_TYPE_SOFT;
        String paperSize = spinnerPaperSize.getSelectedItem().toString();
        String notes = editTextNotes.getText().toString().trim();

        setUploadInProgressUI(true);

        String storageFileName = currentUser.getUid() + "/" + UUID.randomUUID().toString() + "_" + selectedFileName;
        StorageReference fileRef = storage.getReference().child("uploads/" + storageFileName);
        UploadTask uploadTask = fileRef.putFile(selectedFileUri);

        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressBarUpload.setIndeterminate(false);
            progressBarUpload.setProgress((int) progress);
        }).addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String downloadUrl = uri.toString();
            saveJobToFirestore(currentUser.getUid(), selectedFileName, downloadUrl, copies, colorMode, paperSize, paperType, notes);
        }).addOnFailureListener(e -> { Log.e(TAG, "Error getting download URL", e); Toast.makeText(UploadActivity.this, "Hiba: " + e.getMessage(), Toast.LENGTH_LONG).show(); setUploadInProgressUI(false);
        })).addOnFailureListener(e -> { Log.e(TAG, "File upload failed", e); Toast.makeText(UploadActivity.this, "Hiba: " + e.getMessage(), Toast.LENGTH_LONG).show(); setUploadInProgressUI(false);
        });
    }

    private void saveJobToFirestore(String userId, String originalFileName, String fileUrl, int copies,
                                    String colorMode, String paperSize, String paperType, String notes) {
        PrintJob printJob = new PrintJob(userId, originalFileName, fileUrl, PrintJob.STATUS_UPLOADED, copies, colorMode, paperSize, paperType, notes);
        db.collection("printJobs").add(printJob)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(UploadActivity.this, "Nyomtatási igény rögzítve!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving job to Firestore", e);
                    Toast.makeText(UploadActivity.this, "Hiba az igény rögzítésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setUploadInProgressUI(false);
                });
    }

    private void setUploadInProgressUI(boolean inProgress) {
        progressBarUpload.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        if(inProgress) progressBarUpload.setIndeterminate(true);

        buttonUploadFile.setEnabled(!inProgress && selectedFileUri != null);
        buttonSelectFile.setEnabled(!inProgress);
        editTextCopies.setEnabled(!inProgress);
        imageViewUploadPreview.setEnabled(!inProgress);
        spinnerPaperSize.setEnabled(!inProgress);
        editTextNotes.setEnabled(!inProgress);
        for (int i = 0; i < radioGroupColorMode.getChildCount(); i++) {
            radioGroupColorMode.getChildAt(i).setEnabled(!inProgress);
        }
        for (int i = 0; i < radioGroupPaperType.getChildCount(); i++) {
            radioGroupPaperType.getChildAt(i).setEnabled(!inProgress);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}