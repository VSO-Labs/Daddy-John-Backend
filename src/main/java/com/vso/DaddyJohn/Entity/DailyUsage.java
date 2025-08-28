package com.vso.DaddyJohn.Entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "daily_usage")
@Data
@CompoundIndex(name = "user_date_unique", def = "{'user': 1, 'usageDate': 1}", unique = true)
public class DailyUsage {
    @Id
    private ObjectId id;

    @DBRef
    private Users user;

    private LocalDate usageDate;

    private int messagesSent = 0;

    private int tokensUsed = 0;
}
