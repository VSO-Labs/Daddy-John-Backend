package com.vso.DaddyJohn.Repositry;

import com.vso.DaddyJohn.Entity.Conversation;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepo extends MongoRepository<Conversation, ObjectId> {
    // Find all conversations for a specific user, with pagination
    Page<Conversation> findByUserId(ObjectId userId, Pageable pageable);
}