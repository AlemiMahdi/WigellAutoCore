package com.wac.autocore.standalone;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.security.AuthenticationService;
import com.wac.autocore.security.UserService;
import com.wac.autocore.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.core.AuthenticationException;
import java.util.Scanner;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Fristående program (WAC-106), "Main 2".
 * Kör två verksamhetsoperationer utan Spring Application Context, JavaFX eller databas.
 * Här skapas alla objekt med new (Composition Root), även säkerheten.
 * Samma användare, lösenordskontroll och roller som appen.
 */
public class StandaloneMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneMain.class);

    public static void main(String[] args) {

        // Composition Root: kopplar ihop allt för hand, i stället för Spring
        InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
        BookingService bookingService = new BookingService(
                bookingRepository,
                new InMemoryWorkOrderRepository(),
                new InMemoryServiceItemRepository()
        );

        // Composition Root: säkerheten, samma som SecurityConfig fast med new
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserService userService = new UserService(passwordEncoder);
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        AuthenticationService authenticationService = new AuthenticationService(new ProviderManager(provider));

        LOGGER.info("Standalone program started (no Spring context, no JavaFX, no database)");

        // Inloggning mot samma användare som appen
        Scanner scanner = new Scanner(System.in);
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        try {
            authenticationService.login(username, password);
        } catch (AuthenticationException exception) {
            // Logga aldrig lösenordet, bara användarnamnet
            return;
        }

        StandaloneOperations operations = new StandaloneOperations(bookingService, authenticationService);

        // Operation 1: arbetsorderns status
        WorkOrder workOrder = WorkOrder.createDraft(1, 1, WorkOrderTypeEnum.PLANNED);

        boolean confirmed = operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.CONFIRMED);
        LOGGER.info("Work order DRAFT -> CONFIRMED: {}", result(confirmed));

        boolean completed = operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.COMPLETED);
        LOGGER.info("Work order CONFIRMED -> COMPLETED: {}", result(completed));

        // Operation 2: bokningskontroll
        LocalDate date = LocalDate.of(2026, 11, 10);
        Booking existing = new Booking(1, date, LocalTime.of(10, 0), 60, "Oil change");
        bookingRepository.save(existing, 1);
        LOGGER.info("Existing booking: mechanic 1, {} 10:00-11:00", date);

        boolean freeTime = operations.canBook(1, date, LocalTime.of(13, 0), 60);
        LOGGER.info("New booking 13:00-14:00: {}", result(freeTime));

        boolean overlapping = operations.canBook(1, date, LocalTime.of(10, 30), 60);
        LOGGER.info("New booking 10:30-11:30: {}", result(overlapping));

        authenticationService.logout();
        LOGGER.info("Standalone program finished");
    }

    // Gör om true/false till läsbar text i loggen
    private static String result(boolean approved) {
        return approved ? "APPROVED" : "DENIED";
    }
}