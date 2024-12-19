package com.tourbooking.controller.client;

import com.tourbooking.dto.response.ResponseObject;
import com.tourbooking.dto.response.PaymentDTO;
import com.tourbooking.service.payment.PaymentMomoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("${spring.application.api-prefix}/payment/momo")
@RequiredArgsConstructor
public class PaymentMomoController {
    private final PaymentMomoService paymentMomoService;


    @GetMapping
    public ResponseObject<PaymentDTO.PaymentResponse> pay( @RequestParam String bookingId) {
        PaymentDTO.PaymentResponse pay =paymentMomoService.createMomoPayment(bookingId);
        if(pay==null) return new ResponseObject<>(HttpStatus.NOT_FOUND, "Fail",null);
        return new ResponseObject<>(HttpStatus.OK, "Success",pay );
    }

    @GetMapping("/amount")
    public ResponseObject<PaymentDTO.PaymentResponse> pay( @RequestParam String amount,
                                                           @RequestParam String orderId,
                                                           @RequestParam String returnUrl) {
        if(amount==null||orderId==null||returnUrl==null) return new ResponseObject<>(HttpStatus.BAD_REQUEST, "Fail",null);
        PaymentDTO.PaymentResponse pay =paymentMomoService.createMomoPaymentWithAmount(amount,orderId,returnUrl);
        if(pay==null) return new ResponseObject<>(HttpStatus.NOT_FOUND, "Fail",null);
        return new ResponseObject<>(HttpStatus.OK, "Success",pay );
    }

    @GetMapping("/callback")
    public void payMomoCallbackHandler(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        String status = request.getParameter("resultCode");
        String bookingId = request.getParameter("orderId");
        if ("0".equals(status)) {
            if (paymentMomoService.OrderSuccess(bookingId))
                response.sendRedirect("/booking/" + bookingId);
        } else {
            // Chuyển hướng đến trang thất bại
            response.sendRedirect("/payment-failure");
        }
    }
}
