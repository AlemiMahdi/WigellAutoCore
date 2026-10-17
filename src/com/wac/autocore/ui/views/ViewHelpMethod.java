package com.wac.autocore.ui.views;

import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.*;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class ViewHelpMethod {

    /**
     * Används i ProcessPaymentView och ShowPaymentsView så att betalsätt ser likadant ut.
     */
    public static String formatPaymentType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "—";
        }
        return LanguageManager.getInstance()
                .text("payment.type." + type.trim().toUpperCase()).get();
    }

    // 1450.0 -> "1 450 SEK" (mellanslag som tusentalsavgränsare, som i designen)
    public static String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(price) + " SEK";
    }


}

