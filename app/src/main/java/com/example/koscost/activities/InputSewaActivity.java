package com.example.koscost.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.utils.CurrencyTextWatcher; // Pastikan class ini sudah dibuat

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Calendar;
import java.util.Locale;

public class InputSewaActivity extends AppCompatActivity {

    // 1. Deklarasi Variabel (Sudah dirapikan)
    EditText etNama, etWa, etPekerjaan, etTglIn, etTglOut, etTotal, etBayar, etMetode;
    TextView tvNoKamar; // Pakai TextView karena tidak bisa diedit
    Button btnSimpan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_sewa);

        // 2. Binding (Sambungkan variabel dengan ID di XML)
        etNama = findViewById(R.id.et_nama_penghuni);
        etWa = findViewById(R.id.et_nomor_wa);
        etPekerjaan = findViewById(R.id.et_pekerjaan);

        // Pastikan di XML ID-nya benar. Walaupun TextView, ID-nya bisa tetap et_nomor_kamar
        tvNoKamar = findViewById(R.id.et_nomor_kamar);

        etTglIn = findViewById(R.id.et_tgl_checkin);
        etTglOut = findViewById(R.id.et_tgl_checkout);
        etTotal = findViewById(R.id.et_total_harga);
        etBayar = findViewById(R.id.et_jumlah_bayar);
        etMetode = findViewById(R.id.et_metode_bayar);
        btnSimpan = findViewById(R.id.btn_simpan);

        // 3. Ambil Nomor Kamar dari Intent (Dashboard)
        String noKamar = getIntent().getStringExtra("NO_KAMAR");
        if (noKamar != null) {
            tvNoKamar.setText(noKamar);
        }

        // 4. Pasang Format Uang (Agar ada titiknya, misal 1.500.000)
        etTotal.addTextChangedListener(new CurrencyTextWatcher(etTotal));
        etBayar.addTextChangedListener(new CurrencyTextWatcher(etBayar));

        // 5. Pasang Kalendar (Date Picker)
        setupDatePicker(etTglIn);
        setupDatePicker(etTglOut);

        // 6. Aksi Tombol Simpan
        btnSimpan.setOnClickListener(v -> simpanData());
    }

    // Fungsi untuk menampilkan Kalendar saat diklik
    private void setupDatePicker(EditText editText) {
        editText.setFocusable(false); // Biar keyboard tidak muncul
        editText.setClickable(true);
        editText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                // Format tanggal untuk Database (YYYY-MM-DD)
                // String formatDB = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
                // Kita tampilkan langsung format DB di EditText biar gampang
                String selectedDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
                editText.setText(selectedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void simpanData() {
        // Ambil data dari inputan
        String noKamar = tvNoKamar.getText().toString(); // Ambil dari TextView
        String nama = etNama.getText().toString();
        String wa = etWa.getText().toString();
        String kerja = etPekerjaan.getText().toString();
        String tglIn = etTglIn.getText().toString();
        String tglOut = etTglOut.getText().toString();
        String metode = etMetode.getText().toString();

        // Ambil String Harga (yang ada titiknya)
        String strTotal = etTotal.getText().toString();
        String strBayar = etBayar.getText().toString();

        // Validasi
        if (nama.isEmpty() || noKamar.isEmpty() || strTotal.isEmpty() || tglIn.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi data!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Konversi Harga dari Format Titik ke Angka Murni (Double)
        // Menggunakan helper parseCurrency dari CurrencyTextWatcher
        double totalHarga = CurrencyTextWatcher.parseCurrency(strTotal);
        double uangBayar = CurrencyTextWatcher.parseCurrency(strBayar);

        // Tentukan Status Lunas
        String statusLunas = (uangBayar >= totalHarga) ? "Lunas" : "Belum Lunas";

        // Kirim ke Server via Retrofit
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.simpanSewa(
                noKamar, nama, wa, kerja, "Bulanan", totalHarga, statusLunas
        );

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InputSewaActivity.this, "Check-In Berhasil!", Toast.LENGTH_SHORT).show();

                    // Pindah ke Halaman Kuitansi
                    Intent intent = new Intent(InputSewaActivity.this, CetakKuitansiActivity.class);
                    intent.putExtra("NAMA", nama);
                    intent.putExtra("KAMAR", noKamar);
                    intent.putExtra("PERIODE", tglIn + " s/d " + tglOut);
                    intent.putExtra("HARGA", totalHarga); // Kirim angka murni (double)
                    intent.putExtra("METODE", metode);
                    intent.putExtra("STATUS", statusLunas);
                    startActivity(intent);
                    finish(); // Tutup halaman input
                } else {
                    Toast.makeText(InputSewaActivity.this, "Gagal simpan di server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(InputSewaActivity.this, "Error Koneksi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}