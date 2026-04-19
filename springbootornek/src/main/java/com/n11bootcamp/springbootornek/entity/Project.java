package com.n11bootcamp.springbootornek.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="Project")
public class Project implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="project_name")
    private String projectName;


    @Column(name="project_code")
    private String projectCode;



    @Column(name="insert_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date insertDate;


    public Project()
    {

    }

    public Project(Long id, String projectName, String projectCode, Date insertDate) {
        super();
        this.id = id;
        this.projectName = projectName;
        this.projectCode = projectCode;
        this.insertDate = insertDate;
    }



    public Long getId() {
        return id;
    }



    public void setId(Long id) {
        this.id = id;
    }



    public String getProjectName() {
        return projectName;
    }



    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public Date getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(Date insertDate) {
        this.insertDate = insertDate;
    }

}
