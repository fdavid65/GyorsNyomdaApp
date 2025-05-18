package com.example.gyorsnyomdaapp; // Győződj meg róla, hogy ez a te csomagneved!

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// Listener interfész az Adapter és az Activity közötti kommunikációhoz
// Ezt az interfészt a MainActivity fogja implementálni.
interface OnPrintJobInteractionListener {
    void onEditJobClicked(PrintJob job);
    void onDeleteJobClicked(PrintJob job);
}

public class PrintJobAdapter extends RecyclerView.Adapter<PrintJobAdapter.PrintJobViewHolder> {

    private List<PrintJob> printJobs;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private OnPrintJobInteractionListener listener; // Tagváltozó a listenerhez

    // Konstruktor, ami fogadja a munkák listáját és a listener implementációt
    public PrintJobAdapter(List<PrintJob> printJobs, OnPrintJobInteractionListener listener) {
        this.printJobs = printJobs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PrintJobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_print_job, parent, false);
        return new PrintJobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PrintJobViewHolder holder, int position) {
        PrintJob currentJob = printJobs.get(position); // Aktuális munka objektum

        // Meglévő adatok beállítása
        holder.fileNameTextView.setText(currentJob.getFileName());
        holder.copiesTextView.setText("Példányszám: " + currentJob.getCopies()); // String.valueOf itt nem feltétlen kell, de nem árt

        // ÚJ ADATOK MEGJELENÍTÉSE
        // Színmód
        if (currentJob.getColorMode() != null && !currentJob.getColorMode().isEmpty()) {
            holder.colorModeValueTextView.setText(currentJob.getColorMode());
            holder.colorModeValueTextView.setVisibility(View.VISIBLE);
            // Ha van címke TextView (textViewColorModeLabel), azt is láthatóvá kell tenni
            if (holder.colorModeLabelTextView != null) holder.colorModeLabelTextView.setVisibility(View.VISIBLE);
        } else {
            holder.colorModeValueTextView.setText("-"); // Vagy hagyd üresen és GONE
            // Ha van címke TextView, azt is elrejtheted, vagy csak az értéket
            if (holder.colorModeLabelTextView != null) holder.colorModeLabelTextView.setVisibility(View.GONE); // Vagy csak az érték GONE
            holder.colorModeValueTextView.setVisibility(View.GONE);

        }

        // Papírméret
        if (currentJob.getPaperSize() != null && !currentJob.getPaperSize().isEmpty()) {
            holder.paperSizeValueTextView.setText(currentJob.getPaperSize());
            holder.paperSizeValueTextView.setVisibility(View.VISIBLE);
            if (holder.paperSizeLabelTextView != null) holder.paperSizeLabelTextView.setVisibility(View.VISIBLE);
        } else {
            holder.paperSizeValueTextView.setText("-");
            if (holder.paperSizeLabelTextView != null) holder.paperSizeLabelTextView.setVisibility(View.GONE);
            holder.paperSizeValueTextView.setVisibility(View.GONE);
        }

        // Papírtípus
        if (currentJob.getPaperType() != null && !currentJob.getPaperType().isEmpty()) {
            holder.paperTypeValueTextView.setText(currentJob.getPaperType());
            holder.paperTypeValueTextView.setVisibility(View.VISIBLE);
            if (holder.paperTypeLabelTextView != null) holder.paperTypeLabelTextView.setVisibility(View.VISIBLE);
        } else {
            holder.paperTypeValueTextView.setText("-");
            if (holder.paperTypeLabelTextView != null) holder.paperTypeLabelTextView.setVisibility(View.GONE);
            holder.paperTypeValueTextView.setVisibility(View.GONE);
        }

        // Megjegyzés
        if (currentJob.getNotes() != null && !currentJob.getNotes().trim().isEmpty()) {
            holder.notesTextView.setText("Megjegyzés: " + currentJob.getNotes());
            holder.notesTextView.setVisibility(View.VISIBLE);
        } else {
            holder.notesTextView.setText(""); // Ürítjük, ha nincs megjegyzés
            holder.notesTextView.setVisibility(View.GONE);
        }
        // --- ÚJ ADATOK VÉGE ---

        holder.statusTextView.setText("Státusz: " + currentJob.getStatus());

        if (currentJob.getTimestamp() != null) {
            holder.timestampTextView.setText(dateFormat.format(currentJob.getTimestamp()));
        } else {
            holder.timestampTextView.setText("Ismeretlen időpont");
        }

        // Szerkesztés gomb láthatósága
        if (PrintJob.STATUS_UPLOADED.equals(currentJob.getStatus())) { // Vagy MainActivity.STATUS_UPLOADED
            holder.buttonEditJob.setVisibility(View.VISIBLE);
        } else {
            holder.buttonEditJob.setVisibility(View.GONE);
        }

        // Kattintásfigyelők
        holder.buttonEditJob.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditJobClicked(currentJob);
            }
        });

        holder.buttonDeleteJob.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteJobClicked(currentJob);
            }
        });
    }

    @Override
    public int getItemCount() {
        return printJobs.size();
    }

    public void updateJobs(List<PrintJob> newJobs) {
        this.printJobs.clear();
        this.printJobs.addAll(newJobs);
        notifyDataSetChanged();
    }

    // ViewHolder osztály kiegészítve az új TextView-kkal
    static class PrintJobViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        TextView copiesTextView;
        // ÚJ TextView-k az értékekhez
        TextView colorModeValueTextView;
        TextView paperSizeValueTextView;
        TextView paperTypeValueTextView;
        TextView notesTextView;
        // Opcionális: TextView-k a címkékhez, ha az XML-ben külön vannak
        TextView colorModeLabelTextView;
        TextView paperSizeLabelTextView;
        TextView paperTypeLabelTextView;
        // ---
        TextView statusTextView;
        TextView timestampTextView;
        ImageButton buttonEditJob;
        ImageButton buttonDeleteJob;

        public PrintJobViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.textViewFileName);
            copiesTextView = itemView.findViewById(R.id.textViewCopies);
            // ÚJ TextView-k inicializálása az értékekhez
            colorModeValueTextView = itemView.findViewById(R.id.textViewColorModeValue);
            paperSizeValueTextView = itemView.findViewById(R.id.textViewPaperSizeValue);
            paperTypeValueTextView = itemView.findViewById(R.id.textViewPaperTypeValue);
            notesTextView = itemView.findViewById(R.id.textViewNotes);
            // Opcionális: Címke TextView-k inicializálása, ha vannak az XML-ben
            // Ezeket csak akkor kell inicializálni, ha az item_print_job.xml-ben
            // külön ID-val rendelkező TextView-k vannak a "Szín:", "Méret:", "Papír:" címkéknek.
            // Ha nincsenek, akkor ezek a sorok hibát okoznak, vagy null referenciát adnak.
            // Az előző item_print_job.xml javaslatomban voltak ilyen címkék, de lehet, hogy a tiedben nincsenek külön ID-val.
            // Ha a címkék ID-ja textViewColorModeLabel, textViewPaperSizeLabel, textViewPaperTypeLabel:
            View labelView; // Segédváltozó a null ellenőrzéshez
            labelView = itemView.findViewById(R.id.textViewColorModeLabel);
            if (labelView instanceof TextView) colorModeLabelTextView = (TextView) labelView;

            labelView = itemView.findViewById(R.id.textViewPaperSizeLabel);
            if (labelView instanceof TextView) paperSizeLabelTextView = (TextView) labelView;

            labelView = itemView.findViewById(R.id.textViewPaperTypeLabel);
            if (labelView instanceof TextView) paperTypeLabelTextView = (TextView) labelView;
            // ---
            statusTextView = itemView.findViewById(R.id.textViewStatus);
            timestampTextView = itemView.findViewById(R.id.textViewTimestamp);
            buttonEditJob = itemView.findViewById(R.id.buttonEditJob);
            buttonDeleteJob = itemView.findViewById(R.id.buttonDeleteJob);
        }
    }
}