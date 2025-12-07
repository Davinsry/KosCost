package com.example.koscost.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;
import com.example.koscost.model.Kamar;

public interface ApiService {

    // --- 1. FITUR LOGIN (Request OTP) ---
    @FormUrlEncoded
    @POST("login.php")
    Call<ResponseBody> requestLoginOtp(
            @Field("email") String email
    );

    // --- 2. FITUR DASHBOARD (Ambil Data Kamar) ---
    // (INI YANG TADI HILANG)
    @GET("get_kamar.php")
    Call<List<Kamar>> getDaftarKamar(@Query("email") String email);

    // --- 3. TAMBAH KAMAR BARU (Kirim Email) ---
    @FormUrlEncoded
    @POST("tambah_kamar.php")
    Call<ResponseBody> tambahKamar(
            @Field("email") String email, // Email pemilik wajib dikirim
            @Field("no_kamar") String noKamar,
            @Field("fasilitas") String fasilitas,
            @Field("harga_harian") double harian,
            @Field("harga_mingguan") double mingguan,
            @Field("harga_bulanan") double bulanan
    );

    // --- 4. FITUR CHECK-IN (Simpan Data Sewa) ---
    @FormUrlEncoded
    @POST("simpan_sewa.php")
    Call<ResponseBody> simpanSewa(
            @Field("no_kamar") String noKamar,
            @Field("nama_penghuni") String nama,
            @Field("no_wa") String noWa,
            @Field("pekerjaan") String pekerjaan,
            @Field("durasi_sewa") String durasi,
            @Field("total_harga") double totalHarga,
            @Field("status_bayar") String statusBayar
    );

    // --- 5. FITUR VERIFIKASI OTP ---
    @FormUrlEncoded
    @POST("verify_otp.php")
    Call<ResponseBody> verifikasiOtp(
            @Field("email") String email,
            @Field("otp_code") String otpCode
    );

    // --- 6. AMBIL DETAIL PENGHUNI ---
    @GET("get_detail_sewa.php")
    Call<ResponseBody> getDetailSewa(@Query("no_kamar") String noKamar);

    // --- 7. PROSES CHECK-OUT ---
    @FormUrlEncoded
    @POST("checkout.php")
    Call<ResponseBody> prosesCheckout(@Field("no_kamar") String noKamar);

    // --- 8. EDIT KAMAR ---
    @FormUrlEncoded
    @POST("edit_kamar.php")
    Call<ResponseBody> editKamar(
            @Field("id_kamar") String idKamar,
            @Field("no_kamar") String noKamar,
            @Field("fasilitas") String fasilitas,
            @Field("harga_harian") double harian,
            @Field("harga_mingguan") double mingguan,
            @Field("harga_bulanan") double bulanan
    );

    // --- 9. HAPUS KAMAR ---
    @FormUrlEncoded
    @POST("hapus_kamar.php")
    Call<ResponseBody> hapusKamar(@Field("id_kamar") String idKamar);

    // --- 10. FITUR LAPORAN ---
    // (Perlu kirim email juga agar laporannya sesuai user)
    @GET("get_laporan.php")
    Call<ResponseBody> getLaporanKeuangan(@Query("email") String email);

    // --- 11. REGISTER ---
    @FormUrlEncoded
    @POST("register.php")
    Call<ResponseBody> registerUser(
            @Field("email") String email,
            @Field("nama_kos") String namaKos
    );
}