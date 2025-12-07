package com.example.koscost.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast; // Tambahan Import
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog; // Tambahan Import

import com.example.koscost.R;
import com.example.koscost.activities.DetailKamarActivity;
import com.example.koscost.activities.InputSewaActivity;
import com.example.koscost.activities.EditKamarActivity; // Tambahan Import Activity Edit
import com.example.koscost.model.Kamar;
import com.example.koscost.api.ApiService; // Tambahan Import API
import com.example.koscost.api.RetrofitClient; // Tambahan Import Client

import org.json.JSONObject; // Tambahan JSON
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody; // Tambahan ResponseBody
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

        // Set Data ke View
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

        // --- 1. KLIK BIASA (Tap) -> Checkin/Checkout ---
        holder.itemView.setOnClickListener(v -> {
            if (kamar.getStatus().equals("0")) {
                // STATUS 0 = KOSONG -> KE HALAMAN INPUT SEWA
                Intent intent = new Intent(context, InputSewaActivity.class);
                intent.putExtra("NO_KAMAR", kamar.getNoKamar());
                intent.putExtra("HARGA_HARIAN", kamar.getHargaHarian());
                intent.putExtra("HARGA_MINGGUAN", kamar.getHargaMingguan());
                intent.putExtra("HARGA_BULANAN", kamar.getHargaBulanan());
                context.startActivity(intent);
            } else {
                // STATUS 1 = TERISI -> KE HALAMAN DETAIL & CHECKOUT
                Intent intent = new Intent(context, DetailKamarActivity.class);
                intent.putExtra("NO_KAMAR", kamar.getNoKamar());
                context.startActivity(intent);
            }
        });

        // --- 2. KLIK TAHAN (Long Press) -> Muncul Menu Edit/Hapus ---
        holder.itemView.setOnLongClickListener(v -> {
            // Buat Dialog Pilihan
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Opsi Kamar " + kamar.getNoKamar());

            String[] options = {"Edit Data Kamar", "Hapus Kamar"};
            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // PILIH EDIT
                    Intent intent = new Intent(context, EditKamarActivity.class);
                    intent.putExtra("ID", kamar.getIdKamar());
                    intent.putExtra("NO", kamar.getNoKamar());
                    intent.putExtra("FAS", kamar.getFasilitas());
                    intent.putExtra("H_HARIAN", kamar.getHargaHarian());
                    intent.putExtra("H_MINGGUAN", kamar.getHargaMingguan());
                    intent.putExtra("H_BULANAN", kamar.getHargaBulanan());
                    context.startActivity(intent);
                } else {
                    // PILIH HAPUS
                    new AlertDialog.Builder(context)
                            .setTitle("Hapus Kamar?")
                            .setMessage("Yakin ingin menghapus Kamar " + kamar.getNoKamar() + " selamanya?")
                            .setPositiveButton("Ya, Hapus", (d, w) -> {
                                hapusKamar(kamar.getIdKamar());
                            })
                            .setNegativeButton("Batal", null)
                            .show();
                }
            });
            builder.show();
            return true; // True artinya event long click "selesai" di sini
        });
    }

    @Override
    public int getItemCount() {
        return listKamar.size();
    }

    // --- METHOD HAPUS KAMAR ---
    private void hapusKamar(String idKamar) {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.hapusKamar(idKamar).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    Toast.makeText(context, json.getString("message"), Toast.LENGTH_SHORT).show();

                    if (json.getString("status").equals("sukses")) {
                        // Refresh Dashboard (Restart MainActivity)
                        if (context instanceof com.example.koscost.MainActivity) {
                            ((com.example.koscost.MainActivity) context).recreate();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Gagal Hapus: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- ViewHolder Class ---
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