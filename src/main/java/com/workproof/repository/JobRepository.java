package com.workproof.repository;

import com.workproof.model.Customer;
import com.workproof.model.Job;
import com.workproof.model.Location;
import com.workproof.model.User;
import com.workproof.model.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician LEFT JOIN FETCH j.createdBy LEFT JOIN FETCH j.approval WHERE j.id = :id")
    Optional<Job> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician LEFT JOIN FETCH j.approval WHERE j.proofCode = :code")
    Optional<Job> findByProofCode(@Param("code") String proofCode);

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician ORDER BY j.createdAt DESC")
    List<Job> findAllByOrderByCreatedAtDesc();

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician WHERE j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findByStatusOrderByCreatedAtDesc(@Param("status") JobStatus status);

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician WHERE j.assignedTechnician = :tech ORDER BY j.createdAt DESC")
    List<Job> findByAssignedTechnicianOrderByCreatedAtDesc(@Param("tech") User technician);

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician WHERE j.assignedTechnician = :tech AND j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findByAssignedTechnicianAndStatusOrderByCreatedAtDesc(@Param("tech") User technician, @Param("status") JobStatus status);

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician WHERE j.customer = :customer ORDER BY j.createdAt DESC")
    List<Job> findByCustomerOrderByCreatedAtDesc(@Param("customer") Customer customer);

    @Query("SELECT j FROM Job j JOIN FETCH j.customer JOIN FETCH j.location LEFT JOIN FETCH j.assignedTechnician WHERE j.customer = :customer AND j.status = :status ORDER BY j.createdAt DESC")
    List<Job> findByCustomerAndStatusOrderByCreatedAtDesc(@Param("customer") Customer customer, @Param("status") JobStatus status);

    List<Job> findByLocation(Location location);

    long countByStatus(JobStatus status);
    long countByStatusIn(List<JobStatus> statuses);
    long countByAssignedTechnician(User technician);
}

