package com.wac.autocore.ui.views;

import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.*;

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


}

