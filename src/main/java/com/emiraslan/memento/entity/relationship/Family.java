package com.emiraslan.memento.entity.relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Families")
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "family_id")
    private Integer familyId;

    @Column(name = "family_name")
    private String familyName;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDate createdAt = LocalDate.now();

    @OneToMany(mappedBy = "family")
    @JsonIgnore // avoids StackOverflow, Family -> Members -> Family infinite recursion
    private List<FamilyMember> members;
}
