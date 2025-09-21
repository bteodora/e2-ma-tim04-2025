package com.example.rpgapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.rpgapp.R;
import com.example.rpgapp.model.UserWeapon;

import java.util.List;

public class WeaponListAdapter extends ArrayAdapter<UserWeapon> {
    private final Context context;
    private final List<UserWeapon> weapons;

    public WeaponListAdapter(Context context, List<UserWeapon> weapons) {
        super(context, 0, weapons);
        this.context = context;
        this.weapons = weapons;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_weapon_battle, parent, false);
        }

        UserWeapon weapon = weapons.get(position);
//        ImageView icon = convertView.findViewById(R.id.weaponIcon);
//        TextView name = convertView.findViewById(R.id.weaponName);

//        icon.setImageResource(weapon.getImageResourceId());
//        name.setText(weapon.getName());

        return convertView;
    }
}
