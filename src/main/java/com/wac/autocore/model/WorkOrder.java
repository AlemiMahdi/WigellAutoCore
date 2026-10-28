package com.wac.autocore.model;

import com.wac.autocore.repository.WorkOrderTypeConverter;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column()
    private int id;

    @Column(name = "booking_id")
    private int bookingId;

    @Column(name = "mechanic_id")
    private int mechanicId;

    @Column (name = "original_work_order_id")
    private Integer originalWorkOrderId;

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

    @Column(name = "estimated_price", precision = 12, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "customer_instructions", length = 4000)
    private String customerInstructions;

    @Column(name = "draft_comments", length = 4000)
    private String draftComments;

    // Hibernate behöver en tom konstruktor.
    public WorkOrder() {
    }

    public WorkOrder(
            int bookingId,
            int mechanicId,
            WorkOrderTypeEnum type
    ) {
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
    //Not in use
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

    public Integer getOriginalWorkOrderId() { return originalWorkOrderId; }
    public void setOriginalWorkOrderId(Integer originalWorkOrderId) {

        if (originalWorkOrderId != null) {

            if (this.type != WorkOrderTypeEnum.COMPLAINT) {
                throw new IllegalStateException(
                    "Only complaint work orders can reference an original work order."
                );
            }

            if (originalWorkOrderId == this.id) {
                throw new IllegalArgumentException(
                    "A work order cannot reference itself."
                );
            }
        }

        this.originalWorkOrderId = originalWorkOrderId;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public String getCustomerInstructions() {
        return customerInstructions;
    }

    public void setCustomerInstructions(String customerInstructions) {
        this.customerInstructions = customerInstructions;
    }

    public String getDraftComments() {
        return draftComments;
    }

    public void setDraftComments(String draftComments) {
        this.draftComments = draftComments;
    }

    @Override
    public String toString() {
        return id +
                " - Booking ID: " + bookingId +
                " | Mechanic ID: " + mechanicId +
                " | Status: " + status;
    }
}