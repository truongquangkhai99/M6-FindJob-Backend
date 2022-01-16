package com.m6findjobbackend.service.apply;

import com.m6findjobbackend.model.Apply;
import com.m6findjobbackend.repository.IApplyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApplyService implements IApplyService {
    @Autowired
    IApplyRepository applyRepository;
    @Override
    public Iterable<Apply> findAll() {
        return applyRepository.findAll();
    }

    @Override
    public Page<Apply> findAll(Pageable pageable) {
        return applyRepository.findAll(pageable);
    }

    @Override
    public void deleteById(Long id) {
        applyRepository.deleteById(id);
    }

    @Override
    public Apply save(Apply apply) {
        return applyRepository.save(apply);
    }

    @Override
    public Optional<Apply> findById(Long id) {
        return applyRepository.findById(id);
    }
}