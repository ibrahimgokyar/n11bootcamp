package com.n11bootcamp.springbootornek.controller;

import java.util.List;

import com.n11bootcamp.springbootornek.entity.Project;
import com.n11bootcamp.springbootornek.service.impl.ProjectServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class ProjectController {


    private final ProjectServiceImpl projectServiceImpl;

    public ProjectController(ProjectServiceImpl projectServiceImpl) {
        this.projectServiceImpl = projectServiceImpl;
    }


    @RequestMapping(value = "/allprojects", method = RequestMethod.GET)

    public List<Project> getAllProjects() {
        List<Project> liste =projectServiceImpl.getAll();
        return liste;
    }

}