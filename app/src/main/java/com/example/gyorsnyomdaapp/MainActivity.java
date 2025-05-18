package com.example.gyorsnyomdaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnPrintJobInteractionListener {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RecyclerView recyclerViewUploadedJobs, recyclerViewPrintOrderedJobs, recyclerViewRecentJobs;
    private PrintJobAdapter uploadedJobsAdapter, printOrderedJobsAdapter, recentJobsAdapter;
    private List<PrintJob> uploadedJobList, printOrderedJobList, recentJobsList;
    private TextView textViewEmptyUploadedList, textViewEmptyPrintOrderedList, textViewEmptyRecentJobsList;
    private MaterialButton buttonOrderAllUploaded;
    private FloatingActionButton fabUpload;

    public static final String STATUS_UPLOADED = "Feltöltve";
    public static final String STATUS_PRINT_ORDERED = "Megrendelve";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
            return;
        }

        // UI elemek inicializálása
        recyclerViewUploadedJobs = findViewById(R.id.recyclerViewUploadedJobs);
        recyclerViewPrintOrderedJobs = findViewById(R.id.recyclerViewPrintOrderedJobs);
        recyclerViewRecentJobs = findViewById(R.id.recyclerViewRecentJobs);
        textViewEmptyUploadedList = findViewById(R.id.textViewEmptyUploadedList);
        textViewEmptyPrintOrderedList = findViewById(R.id.textViewEmptyPrintOrderedList);
        textViewEmptyRecentJobsList = findViewById(R.id.textViewEmptyRecentJobsList);
        buttonOrderAllUploaded = findViewById(R.id.buttonOrderAllUploaded);
        fabUpload = findViewById(R.id.fabUpload);

        // Listák inicializálása
        uploadedJobList = new ArrayList<>();
        printOrderedJobList = new ArrayList<>();
        recentJobsList = new ArrayList<>();

        uploadedJobsAdapter = new PrintJobAdapter(uploadedJobList, this);
        printOrderedJobsAdapter = new PrintJobAdapter(printOrderedJobList, this);
        recentJobsAdapter = new PrintJobAdapter(recentJobsList, this);

        // RecyclerView-k beállítása
        setupRecyclerView(recyclerViewUploadedJobs, uploadedJobsAdapter);
        setupRecyclerView(recyclerViewPrintOrderedJobs, printOrderedJobsAdapter);
        setupRecyclerView(recyclerViewRecentJobs, recentJobsAdapter);

        fabUpload.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UploadActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        buttonOrderAllUploaded.setOnClickListener(v -> confirmOrderAllUploadedJobs());
    }

    //  RecyclerView-k beállítása
    private void setupRecyclerView(RecyclerView recyclerView, PrintJobAdapter adapter) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() { return false; }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUploadedJobs();
            loadPrintOrderedJobs();
            loadLastFiveJobs();
        } else {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        }
    }

    // 1. KOMPLEX LEKÉRDEZÉS: Feltöltött munkák, legfrissebb elöl
    private void loadUploadedJobs() {
        if (currentUser == null) { Log.d(TAG, "loadUploadedJobs: currentUser is null"); return; }
        Log.d(TAG, "Loading UPLOADED jobs...");
        db.collection("printJobs")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", STATUS_UPLOADED)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uploadedJobList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            PrintJob job = document.toObject(PrintJob.class);
                            job.setDocumentId(document.getId());
                            uploadedJobList.add(job);
                        }
                        uploadedJobsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Loaded " + uploadedJobList.size() + " UPLOADED jobs.");
                    } else {
                        Log.w(TAG, "Error UPLOADED: ", task.getException());
                        Toast.makeText(MainActivity.this, "Hiba: " + safeGetErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        uploadedJobList.clear();
                        uploadedJobsAdapter.notifyDataSetChanged();
                    }
                    checkIfListsAreEmpty();
                });
    }

    // 2. KOMPLEX LEKÉRDEZÉS: Megrendelt munkák, legfrissebb elöl
    private void loadPrintOrderedJobs() {
        if (currentUser == null) { Log.d(TAG, "loadPrintOrderedJobs: currentUser is null"); return; }
        Log.d(TAG, "Loading PRINT_ORDERED jobs...");
        db.collection("printJobs")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", STATUS_PRINT_ORDERED)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        printOrderedJobList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            PrintJob job = document.toObject(PrintJob.class);
                            job.setDocumentId(document.getId());
                            printOrderedJobList.add(job);
                        }
                        printOrderedJobsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Loaded " + printOrderedJobList.size() + " PRINT_ORDERED jobs.");
                    } else {
                        Log.w(TAG, "Error PRINT_ORDERED: ", task.getException());
                        Toast.makeText(MainActivity.this, "Hiba: " + safeGetErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        printOrderedJobList.clear();
                        printOrderedJobsAdapter.notifyDataSetChanged();
                    }
                    checkIfListsAreEmpty();
                });
    }

    // 3. KOMPLEX LEKÉRDEZÉS: Utolsó 5 munka, státusztól függetlenül, legfrissebb elöl
    private void loadLastFiveJobs() {
        if (currentUser == null) { Log.d(TAG, "loadLastFiveJobs: currentUser is null"); return; }
        Log.d(TAG, "Loading last 5 jobs...");
        db.collection("printJobs")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recentJobsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            PrintJob job = document.toObject(PrintJob.class);
                            job.setDocumentId(document.getId());
                            recentJobsList.add(job);
                        }
                        recentJobsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Loaded " + recentJobsList.size() + " RECENT jobs.");
                    } else {
                        Log.w(TAG, "Error getting RECENT documents: ", task.getException());
                        Toast.makeText(MainActivity.this, "Hiba az utolsó munkák betöltésekor: " + safeGetErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        recentJobsList.clear();
                        recentJobsAdapter.notifyDataSetChanged();
                    }
                    checkIfListsAreEmpty();
                });
    }

    private String safeGetErrorMessage(Exception e) {
        if (e == null) return "Ismeretlen hiba.";
        return e.getMessage() != null ? e.getMessage() : "Ismeretlen hiba (nincs üzenet).";
    }

    private void checkIfListsAreEmpty() {
        // Feltöltött lista
        if (uploadedJobList.isEmpty()) {
            recyclerViewUploadedJobs.setVisibility(View.GONE);
            textViewEmptyUploadedList.setVisibility(View.VISIBLE);
            buttonOrderAllUploaded.setVisibility(View.GONE);
        } else {
            recyclerViewUploadedJobs.setVisibility(View.VISIBLE);
            textViewEmptyUploadedList.setVisibility(View.GONE);
            buttonOrderAllUploaded.setVisibility(View.VISIBLE);
        }

        // Megrendelt lista
        if (printOrderedJobList.isEmpty()) {
            recyclerViewPrintOrderedJobs.setVisibility(View.GONE);
            textViewEmptyPrintOrderedList.setVisibility(View.VISIBLE);
        } else {
            recyclerViewPrintOrderedJobs.setVisibility(View.VISIBLE);
            textViewEmptyPrintOrderedList.setVisibility(View.GONE);
        }

        // Legutóbbi munkák lista - ÚJ
        if (recentJobsList.isEmpty()) {
            recyclerViewRecentJobs.setVisibility(View.GONE);
            textViewEmptyRecentJobsList.setVisibility(View.VISIBLE);
        } else {
            recyclerViewRecentJobs.setVisibility(View.VISIBLE);
            textViewEmptyRecentJobsList.setVisibility(View.GONE);
        }
    }

    private void confirmOrderAllUploadedJobs() {
        if (uploadedJobList.isEmpty()) {
            Toast.makeText(this, "Nincsenek nyomtatásra váró fájlok.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Megrendelés")
                .setMessage("Biztosan megrendeled az összes feltöltött fájlt (" + uploadedJobList.size() + " db)?")
                .setPositiveButton("Igen, megrendelem", (dialog, which) -> {
                    orderAllUploadedJobs();
                })
                .setNegativeButton("Mégsem", null)
                .show();
    }

    private void orderAllUploadedJobs() {
        if (uploadedJobList.isEmpty()) { return; }
        WriteBatch batch = db.batch();
        for (PrintJob job : uploadedJobList) {
            if (job.getDocumentId() != null) {
                batch.update(db.collection("printJobs").document(job.getDocumentId()), "status", STATUS_PRINT_ORDERED);
            }
        }
        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(MainActivity.this, "Munkák sikeresen nyomtatásra leadva!", Toast.LENGTH_SHORT).show();
            refreshAllLists();
        }).addOnFailureListener(e -> {
            Toast.makeText(MainActivity.this, "Hiba a munkák státuszának frissítésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.w(TAG, "Error updating job statuses", e);
        });
    }

    @Override
    public void onEditJobClicked(PrintJob job) {
        if (!PrintJob.STATUS_UPLOADED.equals(job.getStatus())) {
            Toast.makeText(this, "Ez a munka már nyomtatásra lett leadva, nem szerkeszthető.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(MainActivity.this, EditJobActivity.class);
        intent.putExtra(EditJobActivity.EXTRA_DOCUMENT_ID, job.getDocumentId());
        intent.putExtra(EditJobActivity.EXTRA_FILE_NAME, job.getFileName());
        intent.putExtra(EditJobActivity.EXTRA_COPIES, job.getCopies());
        intent.putExtra(EditJobActivity.EXTRA_COLOR_MODE, job.getColorMode());
        intent.putExtra(EditJobActivity.EXTRA_PAPER_SIZE, job.getPaperSize());
        intent.putExtra(EditJobActivity.EXTRA_PAPER_TYPE, job.getPaperType());
        intent.putExtra(EditJobActivity.EXTRA_NOTES, job.getNotes());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onDeleteJobClicked(PrintJob job) {
        new AlertDialog.Builder(this)
                .setTitle("Munka törlése")
                .setMessage("Biztosan törölni szeretnéd a '" + job.getFileName() + "' nevű munkát?")
                .setPositiveButton("Törlés", (dialog, which) -> {
                    deletePrintJob(job);
                })
                .setNegativeButton("Mégsem", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePrintJob(PrintJob job) {
        if (job.getDocumentId() == null || job.getDocumentId().isEmpty()) { return; }

        db.collection("printJobs").document(job.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore document deleted: " + job.getDocumentId());
                    if (job.getFileUrl() != null && !job.getFileUrl().isEmpty()) {
                        try {
                            StorageReference fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(job.getFileUrl());
                            fileRef.delete().addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Storage file deleted: " + job.getFileUrl());
                                Toast.makeText(MainActivity.this, "'" + job.getFileName() + "' sikeresen törölve.", Toast.LENGTH_SHORT).show();
                                refreshAllLists();
                            }).addOnFailureListener(e -> {
                                Log.w(TAG, "Error deleting storage file: " + job.getFileUrl(), e);
                                Toast.makeText(MainActivity.this, "Hiba a fájl törlésekor, de a bejegyzés törölve.", Toast.LENGTH_LONG).show();
                                refreshAllLists();
                            });
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "Invalid storage URL: " + job.getFileUrl(), e);
                            Toast.makeText(MainActivity.this, "Érvénytelen fájl URL, de a bejegyzés törölve.", Toast.LENGTH_LONG).show();
                            refreshAllLists();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "'" + job.getFileName() + "' sikeresen törölve (nem volt társított fájl).", Toast.LENGTH_SHORT).show();
                        refreshAllLists();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error deleting firestore document: " + job.getDocumentId(), e);
                    Toast.makeText(MainActivity.this, "Hiba a munka törlésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void refreshAllLists() {
        loadUploadedJobs();
        loadPrintOrderedJobs();
        loadLastFiveJobs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Kijelentkezés")
                .setMessage("Biztosan kijelentkezel?")
                .setPositiveButton("Igen", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                })
                .setNegativeButton("Mégsem", null)
                .show();
    }
}