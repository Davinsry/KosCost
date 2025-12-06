package com.example.koscost.api; // Pastikan package sesuai

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import java.util.List;
import com.example.koscost.model.Kamar;

public interface ApiService {

    // --- 1. FITUR LOGIN (Request OTP) ---
    // Mengirim email ke server, server lanjut ke n8n
    @FormUrlEncoded
    @POST("login.php") // Nama file di VPS nanti
    Call<ResponseBody> requestLoginOtp(
            @Field("email") String email
    );

    // --- 2. FITUR DASHBOARD (Ambil Data Kamar) ---
    // Mengambil semua data kamar buat ditampilkan di Grid
    @GET("get_kamar.php")
    Call<List<Kamar>> getDaftarKamar();

    // --- 3. FITUR CHECK-IN (Simpan Data Sewa) ---
    // Mengirim data penghuni baru ke server
    @FormUrlEncoded
    @POST("simpan_sewa.php")
    Call<ResponseBody> simpanSewa(
            @Field("no_kamar") String noKamar,
            @Field("nama_penghuni") String nama,
            @Field("no_wa") String noWa,
            @Field("pekerjaan") String pekerjaan,
            @Field("durasi_sewa") String durasi, // Harian/Mingguan
            @Field("total_harga") double totalHarga,
            @Field("status_bayar") String statusBayar
    );

    // --- 4. FITUR VERIFIKASI OTP ---
    @FormUrlEncoded
    @POST("verify_otp.php")
    Call<ResponseBody> verifikasiOtp(
            @Field("email") String email,
            @Field("otp_code") String otpCode
    );
}