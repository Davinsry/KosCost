package com.example.koscost.adapter; // Sesuaikan package

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.koscost.R;
import com.example.koscost.activities.InputSewaActivity; // Pastikan ini ada
import com.example.koscost.model.Kamar;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class KamarAdapter extends RecyclerView.Adapter<KamarAdapter.KamarViewHolder> {

    private Context context;
    private List<Kamar> listKamar;

    public KamarAdapter(Context context, List<Kamar> listKamar) {
        this.context = context;
        this.listKamar = listKamar;
    }

    @NonNull
    @Override
    public KamarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_kamar, parent, false);
        return new KamarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KamarViewHolder holder, int position) {
        Kamar kamar = listKamar.get(position);

        holder.tvNomor.setText("Kamar " + kamar.getNoKamar());
        holder.tvFasilitas.setText(kamar.getFasilitas());

        // Format Harga ke Rupiah
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        holder.tvHarga.setText(formatRupiah.format(kamar.getHargaBulanan()) + " / bln");

        // Logika Warna Status (0 = Kosong/Hijau, 1 = Isi/Merah)
        if (kamar.getStatus().equals("1")) {
            holder.tvStatus.setText("TERISI");
            holder.tvStatus.setTextColor(Color.RED);
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Merah muda
        } else {
            holder.tvStatus.setText("KOSONG");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.cardView.setCardBackgroundColor(Color.WHITE);
        }

        // Klik Item -> Pindah ke Form Sewa (InputSewaActivity)
        holder.itemView.setOnClickListener(v -> {
            // Hanya bisa diklik kalau KOSONG (0)
            if (kamar.getStatus().equals("0")) {
                Intent intent = new Intent(context, InputSewaActivity.class);
                intent.putExtra("NO_KAMAR", kamar.getNoKamar());
                intent.putExtra("HARGA_HARIAN", kamar.getHargaHarian());
                intent.putExtra("HARGA_MINGGUAN", kamar.getHargaMingguan());
                intent.putExtra("HARGA_BULANAN", kamar.getHargaBulanan());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listKamar.size();
    }

    public class KamarViewHolder extends RecyclerView.ViewHolder {
        TextView tvNomor, tvStatus, tvFasilitas, tvHarga;
        CardView cardView;

        public KamarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomor = itemView.findViewById(R.id.tv_nomor_kamar);
            tvStatus = itemView.findViewById(R.id.tv_status_kamar);
            tvFasilitas = itemView.findViewById(R.id.tv_fasilitas);
            tvHarga = itemView.findViewById(R.id.tv_harga);
            cardView = itemView.findViewById(R.id.card_kamar);
        }
    }
}