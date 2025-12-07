package com.example.koscost.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CurrencyTextWatcher implements TextWatcher {
    private final EditText editText;

    public CurrencyTextWatcher(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        editText.removeTextChangedListener(this);

        try {
            String originalString = s.toString();
            // Hapus titik atau koma lama biar bersih
            if (originalString.contains(",")) {
                originalString = originalString.replaceAll(",", "");
            }
            if (originalString.contains(".")) {
                originalString = originalString.replaceAll("\\.", "");
            }

            long longval = Long.parseLong(originalString);

            // Format ulang pakai titik
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#,###,###", symbols);

            String formattedString = formatter.format(longval);
            editText.setText(formattedString);
            editText.setSelection(editText.getText().length()); // Kursor di ujung

        } catch (NumberFormatException nfe) {
            // Biarkan kosong kalau error
        }

        editText.addTextChangedListener(this);
    }

    // Fungsi statis untuk mengubah String "1.500.000" jadi Double murni 1500000
    public static double parseCurrency(String text) {
        try {
            return Double.parseDouble(text.replace(".", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}