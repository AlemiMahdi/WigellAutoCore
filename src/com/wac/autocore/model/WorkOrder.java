package com.wac.autocore.model;

import com.wac.autocore.workOrderType.WorkOrderTypeConverter;
import com.wac.autocore.workOrderType.WorkOrderTypeEnum;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    private int id;

    @Column(name = "booking_id")
    private int bookingId;

    @Column(name = "mechanic_id")
    private int mechanicId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "work_order_services",
            joinColumns = @JoinColumn(name = "work_order_id")
    )
    @Column(name = "service_item_id")
    private List<Integer> serviceItemIds = new ArrayList<>();

    @Convert(converter = WorkOrderStatusConverter.class)
    @Column(name = "status")
    private WorkOrderStatus status;

   @Convert(converter = WorkOrderTypeConverter.class)
    @Column(name = "work_order_type")
    private WorkOrderTypeEnum type;

    // Hibernate behöver en tom konstruktor.
    public WorkOrder() {
    }

    public WorkOrder(
            int id,
            int bookingId,
            int mechanicId,
            WorkOrderTypeEnum type
    ) {
        this.id = id;
        this.bookingId = bookingId;
        this.mechanicId = mechanicId;
        this.serviceItemIds = new ArrayList<>();
        this.status = WorkOrderStatus.CONFIRMED;
        this.type = type;
    }

    public static WorkOrder createDraft(int id, int bookingId, WorkOrderTypeEnum type) {
        if (type == null) {
            throw new IllegalArgumentException("Work order type is required");
        }

        WorkOrder draft = new WorkOrder();
        draft.id = id;
        draft.bookingId = bookingId;
        draft.mechanicId = 0;
        draft.serviceItemIds = new ArrayList<>();
        draft.status = WorkOrderStatus.DRAFT;
        draft.type = type;
        return draft;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(int mechanicId) {
        this.mechanicId = mechanicId;
    }

    public List<Integer> getServiceItemIds() {
        return serviceItemIds;
    }

    public void setServiceItemIds(List<Integer> serviceItemIds) {
        this.serviceItemIds = serviceItemIds;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void changeStatus(WorkOrderStatus newStatus) {
        if (!status.canChangeTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot change status from " + status + " to " + newStatus);
        }
        this.status = newStatus;
    }

    public void addServiceItem(int serviceItemId) {
        serviceItemIds.add(serviceItemId);
    }

    public void removeServiceItem(int serviceItemId) {
        serviceItemIds.remove(Integer.valueOf(serviceItemId));
    }

    public WorkOrderTypeEnum getType(){ return type; }

    public void setType(WorkOrderTypeEnum type) { this.type = type;}

    @Override
    public String toString() {
        return id +
                " - Booking ID: " + bookingId +
                " | Mechanic ID: " + mechanicId +
                " | Services: " + serviceItemIds +
                " | Status: " + status;
    }
}