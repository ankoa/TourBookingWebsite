package com.tourbooking.service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import com.tourbooking.dto.request.BookingRequest;
import com.tourbooking.dto.response.BookingDetailResponse;
import com.tourbooking.dto.response.BookingResponse;
import com.tourbooking.dto.response.ResponseObject;
import com.tourbooking.mapper.BookingMapper;
import com.tourbooking.mapper.CustomerMapper;
import com.tourbooking.model.Account;
import com.tourbooking.model.Booking;
import com.tourbooking.model.BookingDetail;
import com.tourbooking.model.Customer;
import com.tourbooking.model.Discount;
import com.tourbooking.model.TourTime;
import com.tourbooking.repository.AccountRepository;
import com.tourbooking.repository.BookingDetailRepository;
import com.tourbooking.repository.BookingRepository;
import com.tourbooking.repository.CustomerRepository;
import com.tourbooking.repository.DiscountRepository;
import com.tourbooking.repository.TourTimeRepository;

@Service
public class BookingService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    CustomerMapper customerMapper;

    @Autowired
    private TourTimeService tourTimeService;

    @Autowired
    private TourTimeRepository tourTimeRepository;

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    BookingDetailService bookingDetailService;

    public BookingService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Lấy tất cả các tài khoản
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    // Tìm tài khoản theo ID
    public Account getAccountById(Integer accountId) {
        return accountRepository.findById(accountId).orElse(null);
    }

    public Account getAccountById(String accountId) {
        return getAccountById(Integer.parseInt(accountId));
    }

    // Thêm tài khoản mới
    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Cập nhật tài khoản
    public Account updateAccount(String accountId, Account accountDetails) {
        return accountRepository.findById(Integer.parseInt(accountId))
                .map(account -> {
                    // Kiểm tra dữ liệu hợp lệ trước khi cập nhật
                    if (accountDetails.getAccountName() != null) {
                        account.setAccountName(accountDetails.getAccountName());
                    }
                    if (accountDetails.getEmail() != null) {
                        account.setEmail(accountDetails.getEmail());
                    }
                    account.setStatus(accountDetails.getStatus());
                    account.setTime(accountDetails.getTime());
                    return accountRepository.save(account);
                })
                .orElse(null);
    }

    // Xóa tài khoản
    public void deleteAccount(String accountId) {
        accountRepository.deleteById(Integer.parseInt(accountId));
    }

    public ResponseObject<Booking> createBooking(BookingRequest bookingRequest,
                                                 Integer status) {
        // lay tour time da dat
        TourTime tourTime = tourTimeService.findById(bookingRequest.getTourTimeId(), status)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tour time với ID: " + bookingRequest.getTourTimeId()));

        if (status != null && tourTime.getStatus() != status)
            return ResponseObject.<Booking>builder()
                .code(HttpStatusCode.valueOf(400))
                .message("tour time status mismatch")
                .data(null).build();

        if (status != null && tourTime.getTour().getStatus() != status)
            return ResponseObject.<Booking>builder()
                .code(HttpStatusCode.valueOf(400))
                .message("tour status mismatch")
                .data(null).build();

        //check remainPax
        if (tourTimeService.calculateRemainPax(tourTime) <
                (bookingRequest.getAdults().size() + bookingRequest.getChildren().size()))
            return ResponseObject.<Booking>builder()
                .code(HttpStatusCode.valueOf(400))
                .message("slot out")
                .data(null).build();

        int voucherValue = 0;
        if (bookingRequest.getVoucherCode() != null) {
            Discount discount = discountRepository.findByDiscountCode(bookingRequest.getVoucherCode());
            if (discount != null && discount.getStatus() == 1)
                voucherValue = discount.getDiscountValue();
        }



        Date currentDate = new Date();

        //danh sach khach hang vua book
        List<Customer> customers = new ArrayList<>();

        //kiem tra va luu nguoi dai dien
        Customer relatedCustomer;
        if (bookingRequest.getAccountId() != 0) {
            Account account = getAccountById(bookingRequest.getAccountId());
            //
            if (status != null && account.getStatus() != status) {
                return ResponseObject.<Booking>builder()
                        .code(HttpStatusCode.valueOf(400))
                        .message("account error")
                        .data(null).build();
            }
            if (status != null && account.getCustomer().getStatus() != status) {
                return ResponseObject.<Booking>builder()
                        .code(HttpStatusCode.valueOf(400))
                        .message("customer error")
                        .data(null).build();
            }
            relatedCustomer = account.getCustomer();
        }
        else {
            relatedCustomer = customerMapper.toCustomer(bookingRequest.getRelatedCustomer());
            relatedCustomer.setCustomerType(1);
            relatedCustomer.setTime(currentDate);
            relatedCustomer.setStatus(1);
            customerRepository.save(relatedCustomer);
        }

        // luu danh sach nguoi lon
        bookingRequest.getAdults().forEach(customerRequest -> {
            Customer customer = customerMapper.toCustomer(customerRequest);
            customer.setRelatedCustomer(relatedCustomer);
            customer.setTime(currentDate);
            customer.setCustomerType(1);
            customer.setStatus(1);
            customerRepository.save(customer);
            customers.add(customer);
        });

        // luu danh sach tre em
        bookingRequest.getChildren().forEach(customerRequest -> {
            Customer customer = customerMapper.toCustomer(customerRequest);
            customer.setRelatedCustomer(relatedCustomer);
            customer.setTime(currentDate);
            customer.setCustomerType(2);
            customer.setStatus(1);
            customerRepository.save(customer);
            customers.add(customer);
        });


        //gia thuong
        int totalPrice = tourTime.getPriceAdult() * bookingRequest.getAdults().size() +
                tourTime.getPriceChild() * bookingRequest.getChildren().size();

        int discountValue = 0;

        //gia co discount
        if (!tourTime.getDiscounts().isEmpty()) {
            for (Discount discount : tourTime.getDiscounts()) {
                if (discount.getStartDate() != null)
                    if (!currentDate.after(discount.getStartDate())) continue;
                if (discount.getEndDate() != null)
                    if (!currentDate.before(discount.getEndDate())) continue;
                discountValue = discount.getDiscountValue()*(bookingRequest.getAdults().size() + bookingRequest.getChildren().size());
                break;
            }
        }

        //luu du lie booking
        Booking newBooking = new Booking();
        newBooking.setCustomer(relatedCustomer);
        newBooking.setAdultCount(bookingRequest.getAdults().size());
        newBooking.setChildCount(bookingRequest.getChildren().size());
        newBooking.setStatus(1);
        newBooking.setTime(currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        newBooking.setTourTime(tourTime);
        newBooking.setTotalPrice(totalPrice);
        newBooking.setTotalDiscount(discountValue + voucherValue);
        newBooking.setPaymentMethod(bookingRequest.getPaymentMethod());


        Booking bookingSaved=bookingRepository.save(newBooking);


        for (Customer customer : customers) {
            BookingDetail bookingDetail = new BookingDetail();
            bookingDetail.setCustomer(customer);
            bookingDetail.setBooking(newBooking);
            bookingDetail.setPrice(customer.getCustomerType() == 1 ? tourTime.getPriceAdult() : tourTime.getPriceChild());
            bookingDetail.setStatus(1);
            bookingDetailRepository.save(bookingDetail);
        }

        return ResponseObject.<Booking>builder()
                .code(HttpStatusCode.valueOf(200))
                .message("success")
                .data(bookingSaved).build();
    }

    public List<Booking> getBookingWithPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "time"));
        Page<Booking> listBooking = bookingRepository.findAll(pageable);
        return listBooking.getContent();
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll(); // Lấy tất cả booking từ repository
    }

    public boolean deactivateBooking(Integer id) {
        int updatedCount = bookingRepository.deactivateBooking(id);
        return updatedCount > 0;
    }

    public boolean addBooking(Booking booking) {
        try {
            // Lưu đối tượng Booking vào cơ sở dữ liệu
            bookingRepository.save(booking);
            // Nếu lưu thành công, trả về true
            return true;
        } catch (Exception e) {
            // Nếu có lỗi xảy ra (ví dụ: ngoại lệ khi lưu vào cơ sở dữ liệu), trả về false
            return false;
        }
    }
    public boolean updateBooking(Booking booking) {
        try {
            bookingRepository.save(booking); // Cập nhật thông tin booking
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public Optional<Booking> getBookingById(Integer bookingId) {
        return bookingRepository.findById(bookingId);
    }
    public Booking findById(int bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    public boolean orderSuccess(String orderInfo) {
        Optional<Booking> bookingOptional = bookingRepository.findById(Integer.parseInt(orderInfo));
        Booking booking ;
        if (bookingOptional.isPresent()) {
            booking = bookingOptional.get();
        } else return false;
        booking.setStatus(2);
        bookingRepository.save(booking);
        return true;
    }
/*
    // Phương thức lấy dữ liệu doanh thu hàng tháng từ repository
    public List<Map<String, Object>> getMonthlyRevenue() {
        return bookingRepository.findMonthlyRevenue();
    }
    public List<Map<String, Object>> getDailyRevenue(String month) {
        return bookingRepository.findDailyRevenue(month);
    }*/


    // Tính doanh thu theo năm
    public List<Map<String, Object>> getRevenueByYear(int year) {
        return bookingRepository.findRevenueByYear(year);
    }

    // Tính doanh thu theo ngày trong tháng
    public List<Map<String, Object>> getDailyRevenue(int year, int month) {
        return bookingRepository.findDailyRevenue(year, month);
    }

    // Tính doanh thu của 4 năm gần nhất
    public List<Map<String, Object>> getRevenueOfLastFourYears(int startYear, int endYear) {
        return bookingRepository.findRevenueOfLastFourYears(startYear, endYear);
    }

    // Tính doanh thu trong 30 ngày
    public List<Map<String, Object>> getRevenueFor30Days(String startDate, String endDate) {
        return bookingRepository.findRevenueFor30Days(startDate, endDate);
    }
    public List<Map<String, Object>> getRevenueForDay(String specificDate) {
        return bookingRepository.findRevenueForSpecificDay(specificDate);
    }
 // Tính số lượng tour được đặt theo số lần đặt nhiều nhất
    public List<Map<String, Object>> getTopBookedTours(int limit) {
        return bookingRepository.findTopBookedTours(limit);
    }

    // Tính số lượng tour được đặt theo năm
    public List<Map<String, Object>> getTourCountByYear(int year) {
        return bookingRepository.findTourCountByYear(year);
    }

    // Tính số lượng tour được đặt theo từng ngày trong tháng
    public List<Map<String, Object>> getTourCountByDayInMonth(int year, int month) {
        return bookingRepository.findTourCountByDayInMonth(year, month);
    }

    // Tính số lượng tour được đặt trong khoảng thời gian cụ thể (nhiều năm)
    public List<Map<String, Object>> getTourCountInRange(String startDate, String endDate) {
        return bookingRepository.findTourCountInRange(startDate, endDate);
    }
    public BookingResponse getBookingResponseById(String Id,Integer status) {
        BookingResponse bookingResponse = new BookingResponse();
        Optional<Booking> bookingOptional = bookingRepository.findById(Integer.parseInt(Id));
        if (bookingOptional.isPresent()) {
            Booking booking = bookingOptional.get();

            bookingResponse = bookingMapper.toBookingResponse(booking);
            bookingResponse.setTourTimeResponse(
                    tourTimeService
                            .toTourTimeResponse(booking.getTourTime(), status)
            );
            List<BookingDetailResponse> bookingDetailResponses = new ArrayList<>();
            for (BookingDetail bookingDetail : booking.getBookingDetails()) {
                bookingDetailResponses.add(bookingDetailService.toBookingDetailResponse(bookingDetail));
            }
            bookingResponse.setBookingDetailResponses(bookingDetailResponses);
        }
        return bookingResponse;
    }
}