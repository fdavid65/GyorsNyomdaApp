package com.example.gyorsnyomdaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.content.Context;                     // Rezgéshez
import android.os.Build;                            // Rezgéshez
import android.os.VibrationEffect;                  // Rezgéshez
import android.os.Vibrator;                         // Rezgéshez
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.Manifest;                            // Engedélyellenőrzéshez
import android.content.pm.PackageManager;           // Engedélyellenőrzéshez
import androidx.core.app.ActivityCompat;            // Engedélyellenőrzéshez
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditJobActivity extends AppCompatActivity {

    private static final String TAG = "EditJobActivity";

    private TextView textViewEditFileName;
    private EditText editTextEditCopies;
    private RadioGroup radioGroupEditColorMode;
    private Spinner spinnerEditPaperSize;
    private RadioGroup radioGroupEditPaperType;
    private EditText editTextEditNotes;
    private Button buttonSaveChanges;
    private ProgressBar progressBarEdit;

    private FirebaseFirestore db;
    private String documentId;
    private String currentFileName;
    private int currentCopies;
    private String currentColorMode;
    private String currentPaperSize;
    private String currentPaperType;
    private String currentNotes;

    public static final String EXTRA_DOCUMENT_ID = "JOB_DOCUMENT_ID";
    public static final String EXTRA_FILE_NAME = "JOB_FILE_NAME";
    public static final String EXTRA_COPIES = "JOB_COPIES";
    public static final String EXTRA_COLOR_MODE = "JOB_COLOR_MODE";
    public static final String EXTRA_PAPER_SIZE = "JOB_PAPER_SIZE";
    public static final String EXTRA_PAPER_TYPE = "JOB_PAPER_TYPE";
    public static final String EXTRA_NOTES = "JOB_NOTES";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_job);

        Toolbar toolbar = findViewById(R.id.toolbar_edit_job);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();

        textViewEditFileName = findViewById(R.id.textViewEditFileName);
        editTextEditCopies = findViewById(R.id.editTextEditCopies);
        radioGroupEditColorMode = findViewById(R.id.radioGroupEditColorMode);
        spinnerEditPaperSize = findViewById(R.id.spinnerEditPaperSize);
        radioGroupEditPaperType = findViewById(R.id.radioGroupEditPaperType);
        editTextEditNotes = findViewById(R.id.editTextEditNotes);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        progressBarEdit = findViewById(R.id.progressBarEdit);

        ArrayAdapter<String> paperSizeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, PrintJob.PAPER_SIZES);
        paperSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditPaperSize.setAdapter(paperSizeAdapter);

        if (getIntent().getExtras() != null) {
            documentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);
            currentFileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
            currentCopies = getIntent().getIntExtra(EXTRA_COPIES, 1);
            currentColorMode = getIntent().getStringExtra(EXTRA_COLOR_MODE);
            currentPaperSize = getIntent().getStringExtra(EXTRA_PAPER_SIZE);
            currentPaperType = getIntent().getStringExtra(EXTRA_PAPER_TYPE);
            currentNotes = getIntent().getStringExtra(EXTRA_NOTES);

            if (documentId == null || documentId.isEmpty()) {
                Toast.makeText(this, "Hiba: Munka azonosító hiányzik.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            populateUI();

        } else {
            Toast.makeText(this, "Hiba: Szerkesztendő adatok hiányoznak.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        createNotificationChannel();
        buttonSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void populateUI() {
        textViewEditFileName.setText(currentFileName);
        editTextEditCopies.setText(String.valueOf(currentCopies));

        // Színmód
        if (PrintJob.COLOR_MODE_COLOR.equals(currentColorMode)) {
            radioGroupEditColorMode.check(R.id.radioButtonEditColor);
        } else {
            radioGroupEditColorMode.check(R.id.radioButtonEditBw); // Alapértelmezett fekete-fehér
        }

        // Papírméret
        if (currentPaperSize != null) {
            int paperSizePosition = Arrays.asList(PrintJob.PAPER_SIZES).indexOf(currentPaperSize);
            if (paperSizePosition >= 0) {
                spinnerEditPaperSize.setSelection(paperSizePosition);
            } else {
                // Ha a méret nincs a listában, válaszd az elsőt vagy hagyd az alapértelmezettet
                spinnerEditPaperSize.setSelection(Arrays.asList(PrintJob.PAPER_SIZES).indexOf("A4")); // Pl. A4 alapértelmezett
            }
        } else {
            spinnerEditPaperSize.setSelection(Arrays.asList(PrintJob.PAPER_SIZES).indexOf("A4"));
        }


        // Papírtípus
        if (PrintJob.PAPER_TYPE_HARD.equals(currentPaperType)) {
            radioGroupEditPaperType.check(R.id.radioButtonEditHardPaper);
        } else {
            radioGroupEditPaperType.check(R.id.radioButtonEditSoftPaper);
        }

        editTextEditNotes.setText(currentNotes != null ? currentNotes : "");
    }

    private void saveChanges() {
        String copiesStr = editTextEditCopies.getText().toString().trim();
        if (TextUtils.isEmpty(copiesStr)) {
            editTextEditCopies.setError("Példányszám megadása kötelező");
            editTextEditCopies.requestFocus();
            return;
        }
        int newCopies;
        try {
            newCopies = Integer.parseInt(copiesStr);
            if (newCopies <= 0) {
                editTextEditCopies.setError("A példányszám legalább 1 kell legyen");
                editTextEditCopies.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            editTextEditCopies.setError("Érvénytelen számformátum");
            editTextEditCopies.requestFocus();
            return;
        }

        String newColorMode = (radioGroupEditColorMode.getCheckedRadioButtonId() == R.id.radioButtonEditColor) ? PrintJob.COLOR_MODE_COLOR : PrintJob.COLOR_MODE_BW;
        String newPaperType = (radioGroupEditPaperType.getCheckedRadioButtonId() == R.id.radioButtonEditHardPaper) ? PrintJob.PAPER_TYPE_HARD : PrintJob.PAPER_TYPE_SOFT;
        String newPaperSize = spinnerEditPaperSize.getSelectedItem().toString();
        String newNotes = editTextEditNotes.getText().toString().trim();

        setInProgressUI(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("copies", newCopies);
        updates.put("colorMode", newColorMode);
        updates.put("paperSize", newPaperSize);
        updates.put("paperType", newPaperType);
        updates.put("notes", newNotes);

        db.collection("printJobs").document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    setInProgressUI(false);
                    Toast.makeText(EditJobActivity.this, "Módosítások sikeresen mentve!", Toast.LENGTH_SHORT).show();
                    // REZGÉS HOZZÁADÁSA
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null && v.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            v.vibrate(200);
                        }
                    }
                    sendJobModifiedNotification(currentFileName != null ? currentFileName : "Egy munka");
                    // REZGÉS VÉGE
                    finish();
                })
                .addOnFailureListener(e -> {
                    setInProgressUI(false);
                    Toast.makeText(EditJobActivity.this, "Hiba a mentéskor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Error updating document", e);
                });
    }

    private void sendJobModifiedNotification(String jobName) {
        int notificationIcon = android.R.drawable.ic_dialog_info;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(notificationIcon)
                .setContentTitle("Munka Módosítva")
                .setContentText("A(z) '" + jobName + "' munka sikeresen frissült.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Engedély ellenőrzése újabb Androidokon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("EditJobActivity", "Nincs értesítési engedély, nem küldünk.");
                return;
            }
        }
        notificationManager.notify(101, builder.build());
    }

    private void setInProgressUI(boolean inProgress) {
        progressBarEdit.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        if(inProgress) progressBarEdit.setIndeterminate(true);

        buttonSaveChanges.setEnabled(!inProgress);
        editTextEditCopies.setEnabled(!inProgress);
        spinnerEditPaperSize.setEnabled(!inProgress);
        editTextEditNotes.setEnabled(!inProgress);
        for (int i = 0; i < radioGroupEditColorMode.getChildCount(); i++) {
            radioGroupEditColorMode.getChildAt(i).setEnabled(!inProgress);
        }
        for (int i = 0; i < radioGroupEditPaperType.getChildCount(); i++) {
            radioGroupEditPaperType.getChildAt(i).setEnabled(!inProgress);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private static final String CHANNEL_ID = "munka_modositas_csatorna"; // Egyedi név a fióknak

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Csak újabb Androidon kell
            CharSequence name = "Munka Módosítások"; // A fiók neve, amit a felhasználó láthat
            String description = "Értesítések a munkák módosításáról";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}