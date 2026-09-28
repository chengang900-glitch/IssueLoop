package com.rnd.app.repository;

import com.rnd.app.entity.TaskType;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkItemIdGenerator {
    private final TaskTypeRepository taskTypeRepository;

    public GeneratedId next(String typeName) {
        TaskType type = taskTypeRepository.findByNameForUpdate(typeName)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "任务类型未初始化: " + typeName));
        int value = type.getNextValue();
        type.setNextValue(value + 1);
        taskTypeRepository.save(type);
        return new GeneratedId(type.getCodePrefix(), value);
    }

    public static class GeneratedId {
        private final String prefix;
        private final int sequence;

        public GeneratedId(String prefix, int sequence) { this.prefix = prefix; this.sequence = sequence; }
        public String getPrefix() { return prefix; }
        public int getSequence() { return sequence; }
    }
}
