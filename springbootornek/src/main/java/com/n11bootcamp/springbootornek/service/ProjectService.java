package com.n11bootcamp.springbootornek.service;

import com.n11bootcamp.springbootornek.entity.Project;

import java.util.List;



public interface ProjectService {

    List<Project> getAll();

    Project getById(Long id);

    Project save(Project project);

    Project update(Project project);

    Boolean delete(Long id);

}
