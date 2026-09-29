package com.rnd.app.service;

import com.rnd.app.entity.ProjectMember;
import com.rnd.app.entity.WorkItem;
import javax.persistence.criteria.*;

/** Personal inbox follows the existing workflow actor permissions; guests remain read-only. */
public final class WorkItemScope {
    private WorkItemScope() {}

    public static Predicate pending(Root<WorkItem> root, CriteriaQuery<?> query, CriteriaBuilder cb, Long userId) {
        Subquery<Long> writers = query.subquery(Long.class);
        Root<ProjectMember> member = writers.from(ProjectMember.class);
        writers.select(member.get("projectId")).where(cb.equal(member.get("userId"), userId),
                cb.notEqual(member.get("role"), "GUEST"));
        Subquery<Long> admins = query.subquery(Long.class);
        Root<ProjectMember> admin = admins.from(ProjectMember.class);
        admins.select(admin.get("projectId")).where(cb.equal(admin.get("userId"), userId),
                cb.equal(admin.get("role"), "PROJECT_ADMIN"));
        Predicate owner = cb.equal(root.get("ownerId"), userId);
        Predicate execution = cb.and(owner, root.get("status").in("新建", "进行中", "验收不通过"));
        Predicate review = cb.and(root.get("status").in("已完成", "延期处理"),
                cb.or(owner, cb.equal(root.get("creatorId"), userId), root.get("projectId").in(admins)));
        return cb.and(root.get("projectId").in(writers), cb.or(execution, review));
    }
}
