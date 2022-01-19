package com.m6findjobbackend.controller;

import com.m6findjobbackend.dto.request.CvDTO;
import com.m6findjobbackend.dto.response.ResponseMessage;
import com.m6findjobbackend.model.CV;
import com.m6findjobbackend.model.Skill;
import com.m6findjobbackend.model.WorkExp;
import com.m6findjobbackend.service.CV.CVService;
import com.m6findjobbackend.service.skill.SkillService;
import com.m6findjobbackend.service.user.UserService;
import com.m6findjobbackend.service.workExp.WorkExpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RequestMapping("CV")
@RestController
public class CVController {
    @Autowired
    CVService cvService;

    @Autowired
    SkillService skillService;

    @Autowired
    WorkExpService workExpService;

    @Autowired
    UserService userService;

    @GetMapping("/showAll")
    public ResponseEntity<?> showAll() {
        return new ResponseEntity<>(cvService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detailCV(@PathVariable Long id) {
        Optional<CV> cv = cvService.findById(id);
        if (!cv.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(cv, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCV(@PathVariable Long id) {
        Optional<CV> cv = cvService.findById(id);
        if (!cv.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        cvService.deleteById(id);
        return new ResponseEntity<>(new ResponseMessage("yes"), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCvByUserId(@PathVariable Long id, @RequestBody CvDTO cvDTO) {
        Optional<CV> cv = cvService.findByUserId(id);
        cv.get().setUser(userService.findById(cvDTO.getUserId()).get());
        cv.get().setExpYear(cvDTO.getExpYear());
        cv.get().setFileCV(cvDTO.getFileCV());
        cv.get().setSalaryExpectation(cvDTO.getSalaryExpectation());
        cvService.save(cv.get());
        List<Skill> skills = new ArrayList<>();
        for (int i = 0; i < cvDTO.getSkills().size(); i++) {
            Skill skill = new Skill();
            Skill skill1 = skill.toEntity(cvDTO.getSkills().get(i));
            Optional<Skill> skillOptional = skillService.findById(skill1.getId());
            skillOptional.get().setCv(cv.get());
            skillOptional.get().setName(skill1.getName());
            skillOptional.get().setProficiency(skill1.getProficiency());
            skillService.save(skillOptional.get());
            skills.add(skillOptional.get());
        }
        List<WorkExp> workExps = new ArrayList<>();
        for (int i = 0; i < cvDTO.getWorkExps().size(); i++) {
            WorkExp workExp = new WorkExp();
            WorkExp workExp1 = workExp.toEntity(cvDTO.getWorkExps().get(i));
            Optional<WorkExp> workExpOptional = workExpService.findById(workExp1.getId());
            workExpOptional.get().setCv(cv.get());
            workExpOptional.get().setTitle(workExp1.getTitle());
            workExpOptional.get().setContent(workExp1.getContent());
            workExpOptional.get().setStartDate(workExp1.getStartDate());
            workExpOptional.get().setEndDate(workExp1.getEndDate());
            workExpService.save(workExpOptional.get());
            workExps.add(workExpOptional.get());
        }
        cv.get().setSkills(skills);
        cv.get().setWorkExps(workExps);
        cvService.save(cv.get());
        CvDTO cvDTO1 = cv.get().toDto(cv.get());
        return new ResponseEntity<>(cvDTO1, HttpStatus.OK);
    }

    @PostMapping("/createCV")
    public ResponseEntity<?> create(@RequestBody CvDTO cvDTO) {
        CV cv1 = new CV();
        cv1 = cv1.toEntity(cvDTO);
        if (cvService.existsByUserId(cvDTO.getUserId())) {
            return new ResponseEntity<>(new ResponseMessage("user_da_ton_tai"), HttpStatus.OK);
        }
        cv1.setUser(userService.findById(cvDTO.getUserId()).get());
        CV cv2 = cvService.save(cv1);
        List<Skill> skills = new ArrayList<>();
        for (int i = 0; i < cvDTO.getSkills().size(); i++) {
            Skill skill = new Skill();
            Skill skill1 = skill.toEntity(cvDTO.getSkills().get(i));
            skill1.setCv(cv2);
            skillService.save(skill1);
            skills.add(skill1);
        }

        List<WorkExp> workExps = new ArrayList<>();
        for (int i = 0; i < cvDTO.getWorkExps().size(); i++) {
            WorkExp workExp = new WorkExp();
            WorkExp workExp1 = workExp.toEntity(cvDTO.getWorkExps().get(i));
            workExp1.setCv(cv2);
            workExpService.save(workExp1);
            workExps.add(workExp1);
        }

        cv1.setSkills(skills);
        cv1.setWorkExps(workExps);
        CvDTO cvDTO1 = cv1.toDto(cv1);
        return new ResponseEntity<>(cvDTO1, HttpStatus.OK);
    }


    @GetMapping("/user/{id}")
    public ResponseEntity<?> findByUserIdToDto(@PathVariable Long id) {
        Optional<CV> cv = cvService.findByUserId(id);
        if (cv.isPresent()) {
            return new ResponseEntity<>(cv.get().toDto(cv.get()), HttpStatus.OK);
        }
        return new ResponseEntity<>(cv, HttpStatus.OK);
    }

    @GetMapping("/userId/{id}")
    public ResponseEntity<?> findByUserId(@PathVariable Long id) {
        Optional<CV> cv = cvService.findByUserId(id);
        return new ResponseEntity<>(cv, HttpStatus.OK);
    }
}
