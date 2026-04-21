package com.n11bootcamp.springbootornek.repository;

import com.n11bootcamp.springbootornek.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ProjectRepository extends JpaRepository<Project,Long> {

}
