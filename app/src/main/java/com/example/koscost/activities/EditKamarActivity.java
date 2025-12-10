package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.database.DatabaseHelper;
import com.example.koscost.utils.CurrencyTextWatcher;

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditKamarActivity extends AppCompatActivity {

    EditText etNo, etFasilitas, etHarian, etMingguan, etBulanan;
    Button btnUpdate;
    ImageButton btnBack;
    TextView tvJudul;
    String idKamar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_kamar);

        // Binding View
        etNo = findViewById(R.id.et_no_kamar_baru);
        etFasilitas = findViewById(R.id.et_fasilitas_baru);
        etHarian = findViewById(R.id.et_harga_harian);
        etMingguan = findViewById(R.id.et_harga_mingguan);
        etBulanan = findViewById(R.id.et_harga_bulanan);
        btnUpdate = findViewById(R.id.btn_update_kamar);
        btnBack = findViewById(R.id.btn_back);
        tvJudul = findViewById(R.id.tv_judul_tambah); // Ini sekarang AMAN karena XML sudah diupdate

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (tvJudul != null) tvJudul.setText("Edit Data Kamar");

        etHarian.addTextChangedListener(new CurrencyTextWatcher(etHarian));
        etMingguan.addTextChangedListener(new CurrencyTextWatcher(etMingguan));
        etBulanan.addTextChangedListener(new CurrencyTextWatcher(etBulanan));

        if (getIntent() != null) {
            idKamar = getIntent().getStringExtra("ID");
            etNo.setText(getIntent().getStringExtra("NO"));
            etFasilitas.setText(getIntent().getStringExtra("FAS"));
            etHarian.setText(String.format("%.0f", getIntent().getDoubleExtra("H_HARIAN", 0)));
            etMingguan.setText(String.format("%.0f", getIntent().getDoubleExtra("H_MINGGUAN", 0)));
            etBulanan.setText(String.format("%.0f", getIntent().getDoubleExtra("H_BULANAN", 0)));
        }

        btnUpdate.setOnClickListener(v -> updateKamar());
    }

    private void updateKamar() {
        if (etNo.getText().toString().isEmpty() || etBulanan.getText().toString().isEmpty()) {
            Toast.makeText(this, "Nomor & Harga Wajib Diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        String no = etNo.getText().toString();
        String fas = etFasilitas.getText().toString();
        double harian = CurrencyTextWatcher.parseCurrency(etHarian.getText().toString());
        double mingguan = CurrencyTextWatcher.parseCurrency(etMingguan.getText().toString());
        double bulanan = CurrencyTextWatcher.parseCurrency(etBulanan.getText().toString());

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.editKamar(idKamar, no, fas, harian, mingguan, bulanan).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditKamarActivity.this, "Kamar Berhasil Diedit!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    editOffline(idKamar, no, fas, harian, mingguan, bulanan);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                editOffline(idKamar, no, fas, harian, mingguan, bulanan);
            }
        });
    }

    private void editOffline(String id, String no, String fas, double h, double m, double b) {
        DatabaseHelper db = new DatabaseHelper(this);
        db.addPendingEditKamar(id, no, fas, h, m, b);
        Toast.makeText(this, "Offline: Perubahan Disimpan.", Toast.LENGTH_SHORT).show();
        finish();
    }
}