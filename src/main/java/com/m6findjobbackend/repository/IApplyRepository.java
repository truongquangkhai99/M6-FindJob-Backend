package com.m6findjobbackend.repository;

import com.m6findjobbackend.model.Apply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IApplyRepository extends JpaRepository<Apply, Long> {
    Page<Apply> findAllByUser_Id(Pageable pageable, Long id);
}
