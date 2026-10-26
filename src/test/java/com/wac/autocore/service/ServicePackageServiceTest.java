package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.IBookingRepository;
import com.wac.autocore.repository.IServicePackageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Körs utan databas, Hibernate och Spring: repositories ersätts med enkla fakes.
public class ServicePackageServiceTest {

    // Fake: kommer ihåg sparade paket, ingen databas
    private final IServicePackageRepository fakePackageRepository = new IServicePackageRepository() {
        @Override
        public ServicePackage save(ServicePackage servicePackage) {
            return servicePackage;
        }

        @Override
        public List<ServicePackage> findAll() {
            return new ArrayList<>();
        }
    };

    // Fake: tom bokningslista, så att ingen överlappning hittas
    private final IBookingRepository fakeBookingRepository = new IBookingRepository() {
        @Override
        public Booking save(Booking booking) {
            return booking;
        }

        @Override
        public Booking save(Booking booking, int mechanicId) {
            return booking;
        }

        @Override
        public List<Booking> findAll() {
            return new ArrayList<>();
        }

        @Override
        public Booking findById(int id) {
            return null;
        }
    };

    private final ServicePackageService servicePackageService =
            new ServicePackageService(fakePackageRepository);
    private final BookingService bookingService =
            new BookingService(fakeBookingRepository);

    // Objekt som testet lägger i de delade listorna och måste städa bort
    private ServicePackage createdPackage;
    private Booking createdBooking;

    @AfterEach
    void cleanUp() {
        Database.getServicePackages().remove(createdPackage);
        Database.getBookings().remove(createdBooking);
    }

    @Test
    void changingPackageAfterBookingDoesNotChangeExistingBooking() {
        ServiceItem battery = new ServiceItem("Batterikontroll", "", 200, 15);
        ServiceItem tires = new ServiceItem("Däckkontroll", "", 150, 15);
        ServiceItem coolant = new ServiceItem("Kylarvätska", "", 100, 10);
        ServiceItem brakeFluid = new ServiceItem("Bromsvätska", "", 250, 20);

        // Vinterpaket med tre tjänster vid bokningstillfället
        createdPackage = servicePackageService.createServicePackage("Vinterkontroll");
        servicePackageService.updateServices(createdPackage,
                new ArrayList<>(Arrays.asList(battery, tires, coolant)));

        int vehicleId = Database.getVehicles().get(0).getId();
        int mechanicId = Database.getMechanics().get(0).getId();

        createdBooking = bookingService.createBooking(
                vehicleId,
                LocalDate.now().plusDays(7),
                LocalTime.of(9, 0),
                40,
                "Vinterkontroll",
                createdPackage.getServices(),
                mechanicId);

        // En månad senare lägger verkstaden till bromsvätska i paketet
        servicePackageService.updateServices(createdPackage,
                new ArrayList<>(Arrays.asList(battery, tires, coolant, brakeFluid)));

        assertEquals(4, createdPackage.getServices().size());
        assertEquals(3, createdBooking.getServices().size());
        assertFalse(createdBooking.getServices().contains(brakeFluid));
        assertEquals(3, createdBooking.getFrozenPrice().size());
    }
}
