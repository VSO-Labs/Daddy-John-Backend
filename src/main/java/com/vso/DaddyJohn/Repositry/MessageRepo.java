package com.vso.DaddyJohn.Repositry;


import com.vso.DaddyJohn.Entity.Message;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepo extends MongoRepository<Message, ObjectId> {
    // Find all messages for a specific conversation, with pagination
    Page<Message> findByConversationId(ObjectId conversationId, Pageable pageable);

    void deleteAllByConversationId(ObjectId conversationId);
}