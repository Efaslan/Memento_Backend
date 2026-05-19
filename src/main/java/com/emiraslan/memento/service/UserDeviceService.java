package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.response.UserDeviceResponseDto;
import com.emiraslan.memento.entity.UserDevice;
import com.emiraslan.memento.repository.device.RefreshTokenRepository;
import com.emiraslan.memento.repository.device.UserDeviceRepository;
import com.emiraslan.memento.service.auth.JwtService;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.emiraslan.memento.service.auth.AuthService.BLACKLIST_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public List<UserDeviceResponseDto> getUserDevices(Integer userId) {
        return userDeviceRepository.findAllByUser_UserId(userId).stream()
                .map(MapperUtil::toUserDeviceResponseDto)
                .toList();
    }

    public List<UserDeviceResponseDto> getPatientDevices(Integer patientId) {
        return userDeviceRepository.findAllByUser_UserId(patientId).stream()
                .map(MapperUtil::toUserDeviceResponseDto)
                .toList();
    }

    @Transactional
    public void logoutDevice(Integer deviceId, String jwt){

        // find the device
        UserDevice device = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_NOT_FOUND"));

        // delete it. Refresh and Notification tokens of the device are also deleted through Cascade
        userDeviceRepository.delete(device);
        log.info("Device session and all associated tokens deleted for Device ID: {}", deviceId);

        // Blacklist the current JWT
        if (jwt != null) {
            long expirationTimeMillis = jwtService.getExpirationTime(jwt);
            long currentTimeMillis = System.currentTimeMillis();
            long remainingTimeMillis = expirationTimeMillis - currentTimeMillis;

            if (remainingTimeMillis > 0) {
                String redisKey = BLACKLIST_PREFIX + jwt;
                redisTemplate.opsForValue().set(redisKey, "val", remainingTimeMillis, TimeUnit.MILLISECONDS);
                log.info("JWT added to Redis blacklist. Remaining time (ms): {}", remainingTimeMillis);
            }
        }
    }
    @Transactional
    public int deleteExpiredRefreshTokens() {
        // deleting all refresh tokens that has an expiry date before now
        return refreshTokenRepository.deleteExpiredRefreshTokens(LocalDateTime.now());
    }

    @Transactional
    public void toggleBiometric(Integer deviceId, boolean isBiometricEnabled){
        int updatedRows = userDeviceRepository.updateBiometricStatus(deviceId, isBiometricEnabled);

        if (updatedRows == 0) {
            throw new EntityNotFoundException("DEVICE_NOT_FOUND");
        }

        log.info("Biometric status updated to {} for Device ID: {}", isBiometricEnabled, deviceId);
    }
}
