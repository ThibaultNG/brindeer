package org.gso.brinder.match.model;

import java.time.LocalDateTime;

import org.gso.brinder.match.dto.ProfileDto;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Document
@NoArgsConstructor
@AllArgsConstructor
public class ProfileModel {

    @Id
    private String id;
    private String userId;
    @Email
    private String mail;
    private int age;
    private String firstName;
    private String lastName;
    @CreatedDate
    private LocalDateTime created;
    @LastModifiedDate
    private LocalDateTime modified;
    private GeoJsonPoint location;

    public ProfileDto toDto() {
        return ProfileDto.builder()
                .id(this.id)
                .userId(this.userId)
                .mail(this.mail)
                .age(this.age)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .created(this.created)
                .modified(this.modified)
                .location(this.location)
                .build();
    }
}
