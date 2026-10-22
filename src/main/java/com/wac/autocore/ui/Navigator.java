package com.wac.autocore.ui;

import java.util.function.Consumer;

/**
 * En liten "krok" så att en vy kan be om att få byta sida,
 * t.ex. när man klickar på "+ New customer" i kundlistan.
 *
 * Varför behövs den? Vyerna känner inte till AutoCoreApp (och ska inte
 * göra det – då skulle varje vy bero på menyn). I stället registrerar
 * AutoCoreApp EN handler vid start, och vyerna anropar bara
 * Navigator.goTo("create-customer"). Då byter menyn sida precis som om
 * man klickat på menyknappen, inklusive markeringen av aktivt menyval.
 *
 * Giltiga sidnycklar (pageKey):
 * dashboard, show-customers, create-customer, show-vehicles, create-vehicle,
 * show-bookings, create-booking, show-work-orders, create-work-order,
 * start-work-order, complete-work-order, show-services, show-mechanics,
 * show-invoices, create-invoice, show-payments, process-payment
 */
public final class Navigator {

    // Koden som faktiskt byter sida. Sätts av AutoCoreApp vid start.
    private static Consumer<String> handler;

    // Ingen ska skapa objekt av klassen – den har bara static-metoder
    private Navigator() {
    }

    /** Registrerar koden som byter sida (anropas en gång av AutoCoreApp). */
    public static void setHandler(Consumer<String> newHandler) {
        handler = newHandler;
    }

    /** Byter till sidan med given nyckel. Gör ingenting om ingen handler finns. */
    public static void goTo(String pageKey) {
        if (handler != null) {
            handler.accept(pageKey);
        }
    }
}
