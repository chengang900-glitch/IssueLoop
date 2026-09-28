package com.rnd.app.service;
import com.rnd.app.dto.BulkUpdateRequest; import com.rnd.app.entity.*; import com.rnd.app.repository.*; import com.rnd.app.util.*;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.*;
import java.util.*;
@Service @RequiredArgsConstructor public class BulkWorkItemItemService {
 private final WorkItemRepository workItems; private final ProjectMemberRepository members; private final SprintRepository sprints; private final ActivityRepository activities; private final NotificationService notifications;
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void update(Long projectId,String id,BulkUpdateRequest r,Long actorId){
  WorkItem w=workItems.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
  if(!projectId.equals(w.getProjectId())) throw new BusinessException(ErrorCode.BAD_REQUEST,"工作项不属于当前项目");
  String oldStatus=w.getStatus(); if(r.getStatus()!=null){ if(!WorkItemService.isValidTransition(w.getStatus(),r.getStatus())) throw new BusinessException(ErrorCode.STATUS_CONFLICT,"状态流转非法"); w.setStatus(r.getStatus()); }
  if(r.getOwnerId()!=null){ if(!members.existsByProjectIdAndUserId(projectId,r.getOwnerId())) throw new BusinessException(ErrorCode.BAD_REQUEST,"负责人不是项目成员"); w.setOwnerId(r.getOwnerId()); }
  if(r.getSprintId()!=null){ Sprint s=sprints.findById(r.getSprintId()).orElseThrow(()->new BusinessException(ErrorCode.BAD_REQUEST,"Sprint 不存在")); if(!projectId.equals(s.getProjectId())) throw new BusinessException(ErrorCode.BAD_REQUEST,"Sprint 不属于当前项目"); w.setSprintId(r.getSprintId()); }
  if(r.getPriority()!=null) w.setPriority(r.getPriority()); if(r.getModule()!=null) w.setModule(r.getModule());
  LinkedHashSet<String> tags=new LinkedHashSet<>(); if(w.getTags()!=null&&!w.getTags().isBlank()) tags.addAll(Arrays.asList(w.getTags().split(",")));
  if(r.getAddTags()!=null) tags.addAll(clean(r.getAddTags())); if(r.getRemoveTags()!=null) tags.removeAll(clean(r.getRemoveTags())); w.setTags(tags.isEmpty()?null:String.join(",",tags));
  workItems.save(w); activities.save(Activity.builder().projectId(projectId).workItemId(id).actorId(actorId).type("BULK_UPDATE").content("批量更新工作项").build());
  if(r.getStatus()!=null) notifications.notifyWatchers(id,actorId,"STATUS_CHANGED",String.format("状态从「%s」变更为「%s」",oldStatus,r.getStatus()));
 }
 private Set<String> clean(List<String> values){ LinkedHashSet<String>s=new LinkedHashSet<>(); for(String v:values) if(v!=null&&!v.trim().isEmpty())s.add(v.trim()); return s; }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void delete(Long projectId,String id){ WorkItem w=workItems.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND)); if(!projectId.equals(w.getProjectId())) throw new BusinessException(ErrorCode.BAD_REQUEST,"工作项不属于当前项目"); workItems.delete(w); }
}
