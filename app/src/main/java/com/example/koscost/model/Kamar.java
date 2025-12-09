package com.example.koscost.model;

import com.google.gson.annotations.SerializedName;

public class Kamar {

    @SerializedName("id_kamar")
    private String idKamar;

    @SerializedName("no_kamar")
    private String noKamar;

    @SerializedName("status")
    private String status; // "0" = Kosong, "1" = Terisi

    @SerializedName("fasilitas")
    private String fasilitas;

    // Variabel Harga
    @SerializedName("harga_harian")
    private double hargaHarian;

    @SerializedName("harga_mingguan")
    private double hargaMingguan;

    @SerializedName("harga_bulanan")
    private double hargaBulanan;

    // --- Constructor ---
    public Kamar() {}

    // --- Getter (Untuk Ambil Data) ---
    public String getIdKamar() { return idKamar; }
    public String getNoKamar() { return noKamar; }
    public String getStatus() { return status; }
    public String getFasilitas() { return fasilitas; }
    public double getHargaHarian() { return hargaHarian; }
    public double getHargaMingguan() { return hargaMingguan; }
    public double getHargaBulanan() { return hargaBulanan; }

    // --- Setter (INI YANG TADI KURANG) ---
    // Tambahkan kode di bawah ini:

    public void setIdKamar(String idKamar) { this.idKamar = idKamar; }

    public void setNoKamar(String noKamar) { this.noKamar = noKamar; }

    public void setStatus(String status) { this.status = status; }

    public void setFasilitas(String fasilitas) { this.fasilitas = fasilitas; } // <--- Ini yang bikin error tadi

    public void setHargaHarian(double hargaHarian) { this.hargaHarian = hargaHarian; }

    public void setHargaMingguan(double hargaMingguan) { this.hargaMingguan = hargaMingguan; }

    public void setHargaBulanan(double hargaBulanan) { this.hargaBulanan = hargaBulanan; } // <--- Ini juga dibutuhkan
}