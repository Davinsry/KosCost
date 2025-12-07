package com.example.koscost.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.koscost.R;
import com.example.koscost.model.Transaksi;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class RiwayatAdapter extends RecyclerView.Adapter<RiwayatAdapter.Holder> {

    List<Transaksi> list;
    public RiwayatAdapter(List<Transaksi> list) { this.list = list; }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Kita pakai layout simple bawaan android biar cepat, atau bisa buat custom
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Transaksi t = list.get(position);
        holder.text1.setText(t.getNama() + " (Kamar " + t.getKamar() + ")");

        NumberFormat rp = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        holder.text2.setText(rp.format(t.getHarga()) + " • " + t.getTanggal());
    }

    @Override public int getItemCount() { return list.size(); }

    class Holder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        public Holder(View v) {
            super(v);
            text1 = v.findViewById(android.R.id.text1);
            text2 = v.findViewById(android.R.id.text2);
        }
    }
}