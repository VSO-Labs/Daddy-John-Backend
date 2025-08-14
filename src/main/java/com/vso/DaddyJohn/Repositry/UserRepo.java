package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.Users;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends MongoRepository<Users, ObjectId> {
    Users findByUsername(String username);
}
