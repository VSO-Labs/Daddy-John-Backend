package com.vso.DaddyJohn.Entity;

import lombok.Data;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@EntityScan
@Data
public class Users {
    @Id
    private ObjectId id;

    @Field("fname")
    @NonNull
    private String fname;

    @Field("lname")
    @NonNull
    private String lname;

    @NonNull
    @Indexed(unique = true)
    @Field("username")
    private String username;

    @Field("about_me")
    private String aboutMe;

    @NonNull
    @Indexed(unique = true)
    private String email;

    @Field("password_hash")
    private String passwordHash;

    private List<String> roles;

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

}
