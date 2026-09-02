package com.notaskflow.controller;

import com.notaskflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * 公开用户头像代理控制器。
 *
 * @author LIN
 */
@Tag(name = "公开用户头像")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/users")
public class PublicUserAvatarController {

    private final UserService userService;

    /**
     * 代理输出指定用户头像。
     *
     * @param userId 用户标识
     * @return 头像流
     */
    @Operation(summary = "代理输出用户头像")
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<StreamingResponseBody> avatar(@PathVariable Long userId) {
        StreamingResponseBody body = outputStream -> userService.writeUserAvatar(userId, outputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(userService.avatarContentType(userId)))
                .cacheControl(CacheControl.noCache())
                .body(body);
    }
}
