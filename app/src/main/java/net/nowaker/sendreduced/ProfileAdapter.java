package net.nowaker.sendreduced;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {

    public interface OnProfileClick {
        void onProfileClick(Profile profile);
    }

    private final List<Profile> items;
    private final OnProfileClick callback;

    public ProfileAdapter(List<Profile> items, OnProfileClick callback) {
        this.items = items;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Profile p = items.get(position);
        Context ctx = holder.itemView.getContext();
        holder.name.setText(p.name);
        holder.summary.setText(summarize(ctx, p));
        holder.itemView.setOnClickListener(v -> callback.onProfileClick(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String summarize(Context ctx, Profile p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.isJpeg() ? ctx.getString(R.string.format_jpeg) : ctx.getString(R.string.format_png));
        sb.append(" · ");
        sb.append(p.keepsResolution()
                ? ctx.getString(R.string.summary_keep_res)
                : p.maxResolution + "px");
        if (p.isJpeg()) {
            sb.append(" · q").append(p.quality);
        }
        sb.append(" · ");
        sb.append(p.preserveMetadata
                ? ctx.getString(R.string.summary_keep_meta)
                : ctx.getString(R.string.summary_strip));
        return sb.toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView summary;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.profileName);
            summary = itemView.findViewById(R.id.profileSummary);
        }
    }
}
