package com.rnd.app.controller;

import com.rnd.app.config.RndPrincipal;
import com.rnd.app.repository.FavoriteRepository;
import com.rnd.app.service.ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {
    @Mock FavoriteRepository favoriteRepo;
    @Mock ProjectService projectService;
    @InjectMocks FavoriteController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addingFavoriteRequiresProjectMembership() {
        RndPrincipal principal = new RndPrincipal(10L, "member", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
        when(favoriteRepo.existsByUserIdAndTargetTypeAndTargetId(10L, "PROJECT", "1")).thenReturn(true);

        controller.addFavorite(1L);

        verify(projectService).ensureProjectMember(1L, 10L);
    }
}
