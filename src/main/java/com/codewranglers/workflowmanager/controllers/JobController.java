package com.codewranglers.workflowmanager.controllers;


import com.codewranglers.workflowmanager.models.*;
import com.codewranglers.workflowmanager.models.data.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Controller
@RequestMapping("/manager/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private LotRepository lotRepository;
    @Autowired
    private OperationRepository operationRepository;
    @Autowired
    private PartRepository partRepository;

    private String url;

    @GetMapping("")
    public String index(Model model) {
        Iterable<Job> allJobs = jobRepository.findAll();
        List<Job> sortedJobs = new ArrayList<>();

        for (Job j : allJobs) {
            sortedJobs.add(j);
        }
        Collections.reverse(sortedJobs);

        model.addAttribute("jobs", sortedJobs);
        url = "/jobs";
        return "jobs/index";
    }

    @GetMapping("/add")
    public String displayAddJobForm(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute(new Job());
        return "jobs/job_add";
    }

    @PostMapping("/add")
    public String processAddJobForm(@ModelAttribute("job") Job newJob, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("title", "Add Job");
            return "/jobs/job_add";
        }
        newJob.setWorkOrderNumber(createWONumber());
        Lot lot = createLotNumber(newJob.getProduct().getProductId());
        newJob.setLot(lot);
        newJob.setIsCompleted(Boolean.FALSE);
        newJob.setStartDate(LocalDate.now());

        Job job = jobRepository.save(newJob);

        createParts(newJob.getProduct().getProductId(), newJob.getQuantity(), newJob.getLot(), job);
        return "redirect:/manager/jobs";
    }

    @GetMapping("/edit_step/job_id/{jobId}")
    public String displayEditJobForm(Model model, @PathVariable int jobId) {
        Optional<Job> jobById = jobRepository.findById(jobId);
        if (jobById.isPresent()) {
            Job job = jobById.get();
            List<Operation> byproductProductId = operationRepository.findByproductProductId(job.getProduct().getProductId());
            model.addAttribute("steps", byproductProductId);
            model.addAttribute("job", job);
            return "/jobs/job_edit_step";
        } else {
            return "/jobs/edit_step/job_id/{jobId}";
        }
    }

    @PostMapping("/edit_step/job_id/{jobId}")
    public String processEditJobForm(@PathVariable int jobId,
                                     @ModelAttribute Job editedJob,
                                     Model model) {
        Optional<Job> jobById = jobRepository.findById(jobId);
        if (jobById.isPresent()) {
            Job job = jobById.get();
            job.setCurrentStep(editedJob.getCurrentStep());
            jobRepository.save(job);
        }

        return "redirect:/manager/jobs/edit/{jobId}";
    }

    @GetMapping("/edit/{jobId}")
    public String displayEditProductForm(Model model, @PathVariable int jobId) {
        Optional<Job> jobById = jobRepository.findById(jobId);
        if (jobById.isPresent()) {
            Job job = jobById.get();
            if (Boolean.TRUE.equals(job.getIsCompleted())) {
                return "redirect:/manager/jobs";
            } else {
                model.addAttribute("job", job);
                model.addAttribute("dueDate", job.getDueDate());
                model.addAttribute("isCompleted", job.getIsCompleted());
                return "/jobs/job_edit";
            }
        } else {
            return "/redirect:/manager/jobs/edit";
        }
    }

    @PostMapping("/edit/{jobId}")
    public String processEditProductForm(@PathVariable int jobId,
                                         @ModelAttribute @Valid Job editedJob) {

        Optional<Job> jobById = jobRepository.findById(jobId);

        if (jobById.isPresent()) {
            Job job = jobById.get();
            job.setDueDate(editedJob.getDueDate());

            if (Boolean.FALSE.equals(job.getIsCompleted())) {
                if (Boolean.TRUE.equals(editedJob.getIsCompleted())) {
                    job.setIsCompleted(Boolean.TRUE);
                    job.setCompletionDate(LocalDate.now());
                }
            }
            jobRepository.save(job);
        }

        if (("/jobs").equals(url)) {
            return "redirect:/manager/jobs";
        } else {
            return "redirect:/manager";
        }
    }

    @GetMapping("/home")
    public String home(Model model) {
        if (("/jobs").equals(url)) {
            return "redirect:/manager/jobs";
        } else {
            return "redirect:/manager";
        }
    }

    private String createWONumber() {
        Iterable<Job> jobs = jobRepository.findAll();
        int woNumber = 0;

        for (Job j : jobs) {
            woNumber = Integer.parseInt(j.getWorkOrderNumber().substring(2));
        }
        woNumber++;

        return "WO" + String.format(String.format("%04d", woNumber));
    }

    private Lot createLotNumber(int productId) {
        Optional<Product> byId = productRepository.findById(productId);
        Iterable<Lot> lots = lotRepository.findAll();
        Lot lot = new Lot();
        int lotNumber = 0;

        for (Lot l : lots) {
            lotNumber = Integer.parseInt(l.getLotNumber());
        }

        lotNumber++;

        lot.setLotNumber(String.format("%04d", lotNumber));
        lot.setProduct(byId.orElse(null));

        lotRepository.save(lot);

        return lot;
    }

    private void createParts(int productId, int quantity, Lot lot, Job job) {
        List<Part> byproductProductId = partRepository.findByproductProductId(productId);
        String productName = productRepository.findById(productId).orElse(null).getProductName();

        Part part = new Part();
        List<Part> totalParts = new ArrayList<>();

        part.setProduct(new Product(productId));

        if (byproductProductId.isEmpty()) {
            for (int i = 1; i < quantity + 1; i++) {
                part.setSerNum("SN" + "-" + String.format("%03d", i));
                part.setJob(job);
                totalParts.add(new Part(part.getSerNum(), lot, part.getProduct(), job));
            }
        } else {
            int serNum = 0;
            for (Part p : byproductProductId) {
                serNum = Integer.parseInt(p.getSerNum().substring(3));
            }
            for (int i = 1; i < quantity + 1; i++) {
                serNum++;
                part.setSerNum("SN" + "-" + String.format("%03d", serNum));
                totalParts.add(new Part(part.getSerNum(), lot, part.getProduct(), job));
            }
        }
        partRepository.saveAll(totalParts);
    }
}

