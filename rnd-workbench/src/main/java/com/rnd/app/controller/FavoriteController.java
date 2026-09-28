package com.rnd.app.controller;

import com.rnd.app.entity.Favorite;
import com.rnd.app.repository.FavoriteRepository;
import com.rnd.app.service.ProjectService;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepo;
    private final ProjectService projectService;

    @PostMapping("/projects/{id}/favorites")
    public ApiResponse addFavorite(@PathVariable Long id) {
        Long uid = SecurityUtil.currentUserId();
        projectService.ensureProjectMember(id, uid);
        if (!favoriteRepo.existsByUserIdAndTargetTypeAndTargetId(uid, "PROJECT", String.valueOf(id))) {
            favoriteRepo.save(Favorite.builder()
                    .userId(uid).targetType("PROJECT").targetId(String.valueOf(id)).build());
        }
        return ApiResponse.ok();
    }

    @DeleteMapping("/projects/{id}/favorites")
    public ApiResponse removeFavorite(@PathVariable Long id) {
        Long uid = SecurityUtil.currentUserId();
        favoriteRepo.deleteByUserIdAndTargetTypeAndTargetId(uid, "PROJECT", String.valueOf(id));
        return ApiResponse.ok();
    }

    @GetMapping("/favorites/projects")
    public ApiResponse listFavorites() {
        Long uid = SecurityUtil.currentUserId();
        List<String> ids = favoriteRepo.findByUserIdAndTargetType(uid, "PROJECT").stream()
                .map(Favorite::getTargetId).collect(Collectors.toList());
        return ApiResponse.ok(ids);
    }
}
