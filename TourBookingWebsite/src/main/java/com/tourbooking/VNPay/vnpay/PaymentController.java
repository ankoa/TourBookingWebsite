package com.tourbooking.VNPay.vnpay;

import com.tourbooking.VNPay.core.response.ResponseObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("${spring.application.api-prefix}/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    // chua dung
    @GetMapping("/vn-pay")
    public ResponseObject<PaymentDTO.VNPayResponse> pay(HttpServletRequest request,
                                                        @RequestParam String orderInfo) {
        PaymentDTO.VNPayResponse pay =paymentService.createVnPayPayment(request, Integer.parseInt(orderInfo));
        if(pay==null) return new ResponseObject<>(HttpStatus.NOT_FOUND, "Fail",null);
        return new ResponseObject<>(HttpStatus.OK, "Success",pay );
    }

    // chua dung
    @GetMapping("/vn-pay-callback")
    public void payCallbackHandler(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        String status = request.getParameter("vnp_ResponseCode");
        String bookingId = request.getParameter("vnp_OrderInfo");
        if ("00".equals(status)) {
            if ( paymentService.OrderSuccess(bookingId))
                response.sendRedirect("/booking/"+bookingId);
        } else {
            // Chuyển hướng đến trang thất bại
            response.sendRedirect("/payment-failure");
        }
    }
}