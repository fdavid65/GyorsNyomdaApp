package com.example.gyorsnyomdaapp;

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

interface OnPrintJobInteractionListener {
    void onEditJobClicked(PrintJob job);
    void onDeleteJobClicked(PrintJob job);
}

public class PrintJobAdapter extends RecyclerView.Adapter<PrintJobAdapter.PrintJobViewHolder> {

    private List<PrintJob> printJobs;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private OnPrintJobInteractionListener listener;
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
        PrintJob currentJob = printJobs.get(position);

        holder.fileNameTextView.setText(currentJob.getFileName());
        holder.copiesTextView.setText("Példányszám: " + currentJob.getCopies());

        // Színmód
        if (currentJob.getColorMode() != null && !currentJob.getColorMode().isEmpty()) {
            holder.colorModeValueTextView.setText(currentJob.getColorMode());
            holder.colorModeValueTextView.setVisibility(View.VISIBLE);
            if (holder.colorModeLabelTextView != null) holder.colorModeLabelTextView.setVisibility(View.VISIBLE);
        } else {
            holder.colorModeValueTextView.setText("-");
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
            holder.notesTextView.setText("");
            holder.notesTextView.setVisibility(View.GONE);
        }

        holder.statusTextView.setText("Státusz: " + currentJob.getStatus());

        if (currentJob.getTimestamp() != null) {
            holder.timestampTextView.setText(dateFormat.format(currentJob.getTimestamp()));
        } else {
            holder.timestampTextView.setText("Ismeretlen időpont");
        }

        // Szerkesztés gomb láthatósága
        if (PrintJob.STATUS_UPLOADED.equals(currentJob.getStatus())) {
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

    static class PrintJobViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        TextView copiesTextView;
        TextView colorModeValueTextView;
        TextView paperSizeValueTextView;
        TextView paperTypeValueTextView;
        TextView notesTextView;
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
            colorModeValueTextView = itemView.findViewById(R.id.textViewColorModeValue);
            paperSizeValueTextView = itemView.findViewById(R.id.textViewPaperSizeValue);
            paperTypeValueTextView = itemView.findViewById(R.id.textViewPaperTypeValue);
            notesTextView = itemView.findViewById(R.id.textViewNotes);
            View labelView;
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