package com.rnd.app.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.dto.ViewPreferenceDto;
import com.rnd.app.entity.UserViewPreference;
import com.rnd.app.repository.UserViewPreferenceRepository;
import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ViewPreferenceServiceTest {
 @Mock UserViewPreferenceRepository repo; ViewPreferenceService service;
 @BeforeEach void init(){ service=new ViewPreferenceService(repo,new ObjectMapper()); }
 @Test void returnsDefaultsWhenMissing(){ when(repo.findByProjectIdAndUserId(1L,2L)).thenReturn(Optional.empty()); assertEquals(ViewPreferenceService.DEFAULT_COLUMNS,service.get(1L,2L).getColumns()); }
 @Test void upsertsByUserAndProject(){ when(repo.findByProjectIdAndUserId(1L,2L)).thenReturn(Optional.empty()); when(repo.save(any())).thenAnswer(i->i.getArgument(0)); assertEquals(List.of("status"),service.save(1L,2L,new ViewPreferenceDto(List.of("status"),"updatedAt,desc",null)).getColumns()); verify(repo).save(any()); }
 @Test void rejectsUnknownDuplicateAndEmptyColumns(){ assertThrows(BusinessException.class,()->service.save(1L,2L,new ViewPreferenceDto(List.of(),"createdAt,desc",null))); assertThrows(BusinessException.class,()->service.save(1L,2L,new ViewPreferenceDto(List.of("status","status"),"createdAt,desc",null))); assertThrows(BusinessException.class,()->service.save(1L,2L,new ViewPreferenceDto(List.of("unknown"),"createdAt,desc",null))); }
 @Test void rejectsInvalidSort(){ assertThrows(BusinessException.class,()->service.save(1L,2L,new ViewPreferenceDto(List.of("status"),"id,desc",null))); }
 @Test void acceptsSupportedGroupsAndRejectsUnknownGroup(){ when(repo.findByProjectIdAndUserId(any(),any())).thenReturn(Optional.empty()); when(repo.save(any())).thenAnswer(i->i.getArgument(0)); for(String group:List.of("status","owner","type","sprint")) assertEquals(group,service.save(1L,2L,new ViewPreferenceDto(List.of("status"),"createdAt,desc",group)).getGroupBy()); assertThrows(BusinessException.class,()->service.save(1L,2L,new ViewPreferenceDto(List.of("status"),"createdAt,desc","module"))); }
}
