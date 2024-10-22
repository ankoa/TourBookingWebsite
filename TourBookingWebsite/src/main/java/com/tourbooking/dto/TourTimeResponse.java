package com.tourbooking.dto;

import com.tourbooking.model.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TourTimeResponse {
    Integer priceAdult;
    Integer priceChild;
    Integer quantity;
    LocalDateTime departureTime;
    LocalDateTime returnTime;
    String timeName;
    Integer tourId;
    String tourTimeCode;
    Integer tourTimeId;
    Integer discountPrice;
    Boolean isDiscount;
    Boolean isPercentage;
    Integer remainPax;
    String note;
    String dayStay;
    List<TransportResponse> transportResponses;

    public TourTimeResponse(TourTime tourTime, int tourReservedCount) {
        this.priceAdult = tourTime.getPriceAdult();
        this.priceChild = tourTime.getPriceChild();
        this.quantity = tourTime.getQuantity();
        this.departureTime = tourTime.getDepartureTime();
        this.returnTime = tourTime.getReturnTime();
        this.timeName = tourTime.getTimeName();
        this.tourId = tourTime.getTour().getTourId();
        this.tourTimeCode = tourTime.getTourTimeCode();
        this.tourTimeId = tourTime.getTourTimeId();
        Set<Discount> discounts = tourTime.getDiscounts();
        Date currentDate = new Date();
        if (!discounts.isEmpty())
            for (Discount discount : discounts) {
                if (currentDate.after(discount.getStartDate()) && currentDate.before(discount.getEndDate())) {
                    this.isDiscount = true;
                    this.discountPrice = discount.getDiscountValue();
                    this.isPercentage = discount.isPercentage();
                    break;
                }
            }
        else this.isDiscount = false;
        this.remainPax = tourTime.getQuantity() - tourReservedCount;

        ArrayList<TransportResponse> transportResponseList = new ArrayList<TransportResponse>();

        Set<TransportDetail> transportDetailsSet = tourTime.getTransportDetails();
        transportDetailsSet.forEach(transportDetail -> {
            transportResponseList.add(new TransportResponse(transportDetail.getTransport(), transportDetail));
        });
        this.transportResponses = transportResponseList;
        this.note = tourTime.getNote();
        this.dayStay=tourTime.getTour().getDayStay();
    }
}
