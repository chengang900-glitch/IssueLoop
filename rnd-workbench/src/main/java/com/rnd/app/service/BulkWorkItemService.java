package com.rnd.app.service;
import com.rnd.app.dto.*; import com.rnd.app.util.*; import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service; import java.util.*;
@Service @RequiredArgsConstructor public class BulkWorkItemService {
 private final BulkWorkItemItemService itemService;
 public BulkUpdateResult update(Long projectId,BulkUpdateRequest r,Long actorId){ validate(r); List<String> ok=new ArrayList<>(); List<BulkUpdateResult.Failure> bad=new ArrayList<>();
  for(String id:r.getIds()) try{itemService.update(projectId,id,r,actorId);ok.add(id);}catch(BusinessException e){bad.add(new BulkUpdateResult.Failure(id,e.getErrorCode().code,e.getMessage()!=null?e.getMessage():e.getErrorCode().message));}catch(Exception e){bad.add(new BulkUpdateResult.Failure(id,ErrorCode.INTERNAL_ERROR.code,ErrorCode.INTERNAL_ERROR.message));}
  return new BulkUpdateResult(ok,bad); }
 void validate(BulkUpdateRequest r){ if(r.getIds()==null||r.getIds().isEmpty()||r.getIds().size()>200||new HashSet<>(r.getIds()).size()!=r.getIds().size()) throw new BusinessException(ErrorCode.BAD_REQUEST,"ID 数量或重复性不合法");
  if(r.getStatus()==null&&r.getOwnerId()==null&&r.getSprintId()==null&&r.getPriority()==null&&r.getModule()==null&&(r.getAddTags()==null||r.getAddTags().isEmpty())&&(r.getRemoveTags()==null||r.getRemoveTags().isEmpty())) throw new BusinessException(ErrorCode.BAD_REQUEST,"至少修改一个字段");
  if(r.getPriority()!=null&&!Set.of("P0","P1","P2","P3").contains(r.getPriority())) throw new BusinessException(ErrorCode.BAD_REQUEST,"优先级不合法");
  Set<String>a=clean(r.getAddTags()),b=clean(r.getRemoveTags()); a.retainAll(b); if(!a.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST,"新增和移除标签冲突"); }
 private Set<String> clean(List<String>v){Set<String>s=new HashSet<>();if(v!=null)for(String x:v)if(x!=null&&!x.trim().isEmpty())s.add(x.trim());return s;}
 public BulkUpdateResult delete(Long projectId,BulkDeleteRequest r){ if(r.getIds()==null||r.getIds().isEmpty()||r.getIds().size()>200||new HashSet<>(r.getIds()).size()!=r.getIds().size()) throw new BusinessException(ErrorCode.BAD_REQUEST,"ID 数量或重复性不合法"); if(!"确认删除".equals(r.getConfirmation())) throw new BusinessException(ErrorCode.BAD_REQUEST,"确认文字不正确"); List<String>ok=new ArrayList<>();List<BulkUpdateResult.Failure>bad=new ArrayList<>(); for(String id:r.getIds())try{itemService.delete(projectId,id);ok.add(id);}catch(BusinessException e){bad.add(new BulkUpdateResult.Failure(id,e.getErrorCode().code,e.getMessage()!=null?e.getMessage():e.getErrorCode().message));}catch(Exception e){bad.add(new BulkUpdateResult.Failure(id,ErrorCode.INTERNAL_ERROR.code,ErrorCode.INTERNAL_ERROR.message));} return new BulkUpdateResult(ok,bad); }
}
