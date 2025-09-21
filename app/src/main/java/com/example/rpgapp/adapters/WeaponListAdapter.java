package com.example.rpgapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpgapp.R;
import com.example.rpgapp.model.UserWeapon;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

public class WeaponListAdapter extends RecyclerView.Adapter<WeaponListAdapter.WeaponViewHolder> {

    private final List<UserWeapon> weapons;
    private final OnWeaponClickListener listener;

    public interface OnWeaponClickListener {
        void onWeaponClick(UserWeapon weapon);
    }

    public WeaponListAdapter(List<UserWeapon> weapons, OnWeaponClickListener listener) {
        this.weapons = weapons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeaponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_weapon_battle, parent, false);
        return new WeaponViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeaponViewHolder holder, int position) {
        UserWeapon weapon = weapons.get(position);
        holder.bind(weapon, listener);
    }

    @Override
    public int getItemCount() {
        return weapons.size();
    }

    static class WeaponViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, boost;

        public WeaponViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.weaponImage);
            name = itemView.findViewById(R.id.weaponName);
            boost = itemView.findViewById(R.id.weaponBoost);
        }

//        public void bind(UserWeapon weapon, OnWeaponClickListener listener) {
//            name.setText(weapon.getName());
//            boost.setText("Boost: " + weapon.getCurrentBoost());
//
//            // Umesto if-else po tipu
//            image.setImageResource(weapon.getImageResourceId());
//
//            itemView.setOnClickListener(v -> listener.onWeaponClick(weapon));
//        }
    public void bind(UserWeapon weapon, OnWeaponClickListener listener) {
        name.setText(weapon.getName());
        boost.setText("Boost: " + weapon.getCurrentBoost());

        // Prikaži animirani GIF umesto statične slike
        Glide.with(image.getContext())
                .asGif()
                .load(weapon.getImageResourceId())
                .placeholder(R.drawable.ic_face)
                .into(image);

        itemView.setOnClickListener(v -> listener.onWeaponClick(weapon));
    }

    }
}
