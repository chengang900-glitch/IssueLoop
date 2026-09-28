package com.rnd.app.service;
import com.rnd.app.dto.*; import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class) class BulkWorkItemServiceTest {
 @Mock BulkWorkItemItemService items; @InjectMocks BulkWorkItemService service;
 @Test void rejectsEmptyDuplicateAndNoChanges(){ BulkUpdateRequest r=new BulkUpdateRequest(); r.setIds(List.of()); assertThrows(BusinessException.class,()->service.update(1L,r,2L)); r.setIds(List.of("A","A")); r.setPriority("P1"); assertThrows(BusinessException.class,()->service.update(1L,r,2L)); r.setIds(List.of("A")); r.setPriority(null); assertThrows(BusinessException.class,()->service.update(1L,r,2L)); }
 @Test void reportsPartialSuccess(){ BulkUpdateRequest r=new BulkUpdateRequest(); r.setIds(List.of("A","B")); r.setPriority("P1"); doAnswer(call->{ if("B".equals(call.getArgument(1))) throw new BusinessException(com.rnd.app.util.ErrorCode.NOT_FOUND); return null; }).when(items).update(eq(1L),anyString(),same(r),eq(2L)); BulkUpdateResult result=service.update(1L,r,2L); assertEquals(List.of("A"),result.getSuccesses()); assertEquals("B",result.getFailures().get(0).getId()); }
 @Test void rejectsConflictingTags(){ BulkUpdateRequest r=new BulkUpdateRequest(); r.setIds(List.of("A")); r.setAddTags(List.of("x")); r.setRemoveTags(List.of(" x ")); assertThrows(BusinessException.class,()->service.update(1L,r,2L)); }
 @Test void bulkDeleteRequiresConfirmationAndReportsPartialFailure(){BulkDeleteRequest r=new BulkDeleteRequest();r.setIds(List.of("A","B"));assertThrows(BusinessException.class,()->service.delete(1L,r));r.setConfirmation("确认删除");doAnswer(call->{if("B".equals(call.getArgument(1)))throw new BusinessException(com.rnd.app.util.ErrorCode.NOT_FOUND);return null;}).when(items).delete(eq(1L),anyString());BulkUpdateResult out=service.delete(1L,r);assertEquals(List.of("A"),out.getSuccesses());assertEquals(1,out.getFailures().size());}
}
