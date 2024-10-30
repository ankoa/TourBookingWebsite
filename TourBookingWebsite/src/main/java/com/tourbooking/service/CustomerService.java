package com.tourbooking.service;

import com.tourbooking.model.Customer;
import com.tourbooking.repository.CustomerRepository; // Thêm import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository; // Sử dụng CustomerRepository

    // Lấy tất cả khách hàng
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll(); // Sử dụng phương thức từ JpaRepository
    }

    // Lấy khách hàng theo ID
    public Optional<Customer> getCustomerById(int id) { // Thay đổi kiểu trả về thành Optional<Customer>
        return customerRepository.findById(id); // Trả về Optional<Customer>
    }

    // Thêm khách hàng mới
    public Customer addCustomer(Customer customer) {
        // Kiểm tra xem khách hàng đã tồn tại chưa
        if (customerRepository.existsById(customer.getCustomerId())) {
            return null; // Nếu khách hàng đã tồn tại, trả về null
        }
        return customerRepository.save(customer); // Lưu khách hàng mới
    }

    // Cập nhật thông tin khách hàng
    public Customer updateCustomer(int id, Customer customer) {
        if (customerRepository.existsById(id)) {
            customer.setCustomerId(id); // Đặt ID cho khách hàng
            return customerRepository.save(customer); // Cập nhật thông tin khách hàng
        }
        return null; // Trả về null nếu không tìm thấy khách hàng
    }

    // Xóa khách hàng theo ID
    public boolean deleteCustomer(int id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id); // Xóa khách hàng
            return true; // Trả về true nếu xóa thành công
        }
        return false; // Trả về false nếu không tìm thấy khách hàng
    }
}