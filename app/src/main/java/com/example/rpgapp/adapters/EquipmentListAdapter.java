package com.example.rpgapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rpgapp.R;
import com.example.rpgapp.model.EquipmentDisplay;

import java.util.List;

public class EquipmentListAdapter extends RecyclerView.Adapter<EquipmentListAdapter.ViewHolder> {

    private final List<EquipmentDisplay> equipment;

    public EquipmentListAdapter(List<EquipmentDisplay> equipment) {
        this.equipment = equipment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_weapon_battle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EquipmentDisplay eq = equipment.get(position);
        holder.bind(eq);
    }

    @Override
    public int getItemCount() {
        return equipment.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, boost;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.weaponImage);
            name = itemView.findViewById(R.id.weaponName);
            boost = itemView.findViewById(R.id.weaponBoost);
        }

        public void bind(EquipmentDisplay eq) {
            name.setText(eq.getName());
            boost.setText("Boost: " + eq.getBoost());
            Glide.with(image.getContext())
                    .asGif()
                    .load(eq.getImageResId())
                    .placeholder(R.drawable.ic_face)
                    .into(image);
        }
    }
}
