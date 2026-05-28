package com.aitaskmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskHistoryDTO {
    private Long id;
    private String action;
    private String stateHash;
    private String previousHash;
    private LocalDateTime timestamp;
}
