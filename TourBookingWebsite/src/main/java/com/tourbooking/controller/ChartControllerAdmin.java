package com.tourbooking.controller;

import com.tourbooking.service.BookingService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class ChartControllerAdmin {

    private final BookingService bookingService;

    // Constructor injection
    public ChartControllerAdmin(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/chart/specificDate/{specificDate}")
    public ResponseEntity<List<Map<String, Object>>> getRevenueForDay(@PathVariable String specificDate) {
        try {
            List<Map<String, Object>> dailyRevenue = bookingService.getRevenueForDay(specificDate);
            return new ResponseEntity<>(dailyRevenue, HttpStatus.OK);
        } catch (Exception e) {
            // Xử lý lỗi
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/chart/byYear/{year}")
    public ResponseEntity<List<Map<String, Object>>> getRevenueByYear(@PathVariable String year) {
        try {
            // Chuyển đổi String year sang kiểu int nếu cần thiết trong service
            List<Map<String, Object>> revenue = bookingService.getRevenueByYear(Integer.parseInt(year)); 
            return new ResponseEntity<>(revenue, HttpStatus.OK);
        } catch (NumberFormatException e) {
            // Xử lý lỗi khi chuyển đổi String thành int không thành công
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // Nếu year không phải là số hợp lệ
        } catch (Exception e) {
            // Xử lý các lỗi khác
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Tính doanh thu theo ngày trong tháng
    @GetMapping("/chart/daily/{year}/{month}")
    public ResponseEntity<List<Map<String, Object>>> getDailyRevenue(@PathVariable int year, @PathVariable int month) {
        try {
            List<Map<String, Object>> dailyRevenue = bookingService.getDailyRevenue(year, month);
            return new ResponseEntity<>(dailyRevenue, HttpStatus.OK);
        } catch (Exception e) {
            // Xử lý lỗi
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tính doanh thu của 4 năm gần nhất
    @GetMapping("/chart/lastFourYears/{startYear}/{endYear}")
    public ResponseEntity<List<Map<String, Object>>> getRevenueOfLastFourYears(@PathVariable int startYear, @PathVariable int endYear) {
        try {
            List<Map<String, Object>> revenue = bookingService.getRevenueOfLastFourYears(startYear, endYear);
            return new ResponseEntity<>(revenue, HttpStatus.OK);
        } catch (Exception e) {
            // Xử lý lỗi
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Tính doanh thu trong 30 ngày
    @GetMapping("/chart/for30Days")
    public ResponseEntity<List<Map<String, Object>>> getRevenueFor30Days(@RequestParam String startDate, @RequestParam String endDate) {
        try {
            List<Map<String, Object>> revenue = bookingService.getRevenueFor30Days(startDate, endDate);
            return new ResponseEntity<>(revenue, HttpStatus.OK);
        } catch (Exception e) {
            // Xử lý lỗi
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}