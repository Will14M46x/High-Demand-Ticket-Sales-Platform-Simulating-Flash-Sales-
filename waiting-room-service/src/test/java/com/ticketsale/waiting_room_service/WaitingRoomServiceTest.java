package com.ticketsale.waiting_room_service;

import com.ticketsale.waiting_room_service.dto.QueueStatusResponse;
import com.ticketsale.waiting_room_service.service.WaitingRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingRoomServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @Mock
    private SetOperations<String, String> setOps;

    @InjectMocks
    private WaitingRoomService waitingRoomService;

    @BeforeEach
    void setup() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
    }

    // ---------------------------------------------------------
    // 1️⃣ JOIN QUEUE
    // ---------------------------------------------------------
    @Test
    void testJoinQueue() {
        when(zSetOps.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(zSetOps.rank(anyString(), anyString())).thenReturn(0L); // first in line

        Integer position = waitingRoomService.joinQueue("user1", 10L, 2);

        assertEquals(1, position); // rank (0) + 1
    }

    // ---------------------------------------------------------
    // 2️⃣ GET POSITION - USER IN QUEUE
    // ---------------------------------------------------------
    @Test
    void testGetPosition_InQueue() {
        when(zSetOps.rank("waitingroom:queue:10", "user1")).thenReturn(4L);

        Integer pos = waitingRoomService.getPosition("user1", 10L);
        assertEquals(5, pos); // rank (4) + 1
    }

    // ---------------------------------------------------------
    // 3️⃣ GET POSITION - USER ADMITTED
    // ---------------------------------------------------------
    @Test
    void testGetPosition_Admitted() {
        when(zSetOps.rank(anyString(), anyString())).thenReturn(null);
        when(setOps.isMember("waitingroom:admitted:10", "user1")).thenReturn(true);

        Integer pos = waitingRoomService.getPosition("user1", 10L);
        assertEquals(0, pos); // 0 = admitted
    }

    // ---------------------------------------------------------
    // 4️⃣ ADMIT BATCH
    // ---------------------------------------------------------
    @Test
    void testAdmitBatch() {
        Set<String> mockUsers = new LinkedHashSet<>(Arrays.asList("u1", "u2"));
        when(zSetOps.range("waitingroom:queue:10", 0, 1)).thenReturn(mockUsers);

        List<String> admitted = waitingRoomService.admitBatch(2, 10L);

        assertEquals(2, admitted.size());
        verify(zSetOps, times(2)).remove(eq("waitingroom:queue:10"), anyString());
        verify(setOps, times(2)).add(eq("waitingroom:admitted:10"), anyString());
    }

    // ---------------------------------------------------------
    // 5️⃣ REMOVE USER
    // ---------------------------------------------------------
    @Test
    void testRemoveUser() {
        when(zSetOps.remove("waitingroom:queue:10", "user1")).thenReturn(1L);

        boolean removed = waitingRoomService.removeUser("user1", 10L);

        assertTrue(removed);
    }

    // ---------------------------------------------------------
    // 6️⃣ QUEUE STATUS
    // ---------------------------------------------------------
    @Test
    void testGetQueueStatus() {
        when(zSetOps.size("waitingroom:queue:10")).thenReturn(5L);
        when(setOps.size("waitingroom:admitted:10")).thenReturn(2L);

        QueueStatusResponse status = waitingRoomService.getQueueStatus(10L);

        assertEquals(5L, status.getTotalWaiting());
        assertEquals(2L, status.getTotalAdmitted());
        assertEquals("150 seconds", status.getEstimatedWaitTime());
    }
}