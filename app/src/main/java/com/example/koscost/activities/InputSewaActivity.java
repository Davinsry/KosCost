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
import com.example.koscost.utils.CurrencyTextWatcher; // Pastikan ini ada
import com.example.koscost.utils.SessionManager; // Tambah SessionManager

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Calendar;
import java.util.Locale;

public class InputSewaActivity extends AppCompatActivity {

    EditText etNama, etWa, etPekerjaan, etTglIn, etTglOut, etTotal, etBayar, etMetode;
    TextView tvNoKamar;
    Button btnSimpan;
    SessionManager sessionManager; // Variabel Session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_sewa);

        // Inisialisasi Session
        sessionManager = new SessionManager(this);

        // Binding
        etNama = findViewById(R.id.et_nama_penghuni);
        etWa = findViewById(R.id.et_nomor_wa);
        etPekerjaan = findViewById(R.id.et_pekerjaan);
        tvNoKamar = findViewById(R.id.et_nomor_kamar); // Pastikan di XML pakai TextView
        etTglIn = findViewById(R.id.et_tgl_checkin);
        etTglOut = findViewById(R.id.et_tgl_checkout);
        etTotal = findViewById(R.id.et_total_harga);
        etBayar = findViewById(R.id.et_jumlah_bayar);
        etMetode = findViewById(R.id.et_metode_bayar);
        btnSimpan = findViewById(R.id.btn_simpan);

        // Ambil Nomor Kamar
        String noKamar = getIntent().getStringExtra("NO_KAMAR");
        if (noKamar != null) {
            tvNoKamar.setText(noKamar);
        }

        // Pasang Format Uang
        etTotal.addTextChangedListener(new CurrencyTextWatcher(etTotal));
        etBayar.addTextChangedListener(new CurrencyTextWatcher(etBayar));

        // Pasang Kalendar
        setupDatePicker(etTglIn);
        setupDatePicker(etTglOut);

        btnSimpan.setOnClickListener(v -> simpanData());
    }

    private void setupDatePicker(EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String selectedDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
                editText.setText(selectedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void simpanData() {
        String noKamar = tvNoKamar.getText().toString();
        String nama = etNama.getText().toString();
        String wa = etWa.getText().toString();
        String kerja = etPekerjaan.getText().toString();
        String tglIn = etTglIn.getText().toString();
        String tglOut = etTglOut.getText().toString();
        String strTotal = etTotal.getText().toString();
        String strBayar = etBayar.getText().toString();
        String metode = etMetode.getText().toString();

        if (nama.isEmpty() || strTotal.isEmpty() || tglIn.isEmpty()) {
            Toast.makeText(this, "Data wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ambil Email
        String emailUser = sessionManager.getEmail();
        if (emailUser == null) {
            Toast.makeText(this, "Sesi habis, login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bersihkan Format Uang
        double totalHarga = CurrencyTextWatcher.parseCurrency(strTotal);
        double uangBayar = CurrencyTextWatcher.parseCurrency(strBayar);
        String statusLunas = (uangBayar >= totalHarga) ? "Lunas" : "Belum Lunas";

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        // KIRIM DATA TERMASUK EMAIL
        api.simpanSewa(
                emailUser, // <--- Email dikirim di sini
                noKamar, nama, wa, kerja, "Bulanan", totalHarga, statusLunas
        ).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(InputSewaActivity.this, "Check-In Berhasil!", Toast.LENGTH_SHORT).show();

                    // Pindah ke Kuitansi
                    Intent intent = new Intent(InputSewaActivity.this, CetakKuitansiActivity.class);
                    intent.putExtra("NAMA", nama);
                    intent.putExtra("KAMAR", noKamar);
                    intent.putExtra("PERIODE", tglIn + " s.d " + tglOut);
                    intent.putExtra("HARGA", totalHarga);
                    intent.putExtra("STATUS", statusLunas);
                    intent.putExtra("METODE", metode);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(InputSewaActivity.this, "Gagal: Server Error " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(InputSewaActivity.this, "Koneksi Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}