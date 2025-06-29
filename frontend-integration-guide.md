# Frontend Entegrasyon Rehberi

## React Frontend için API Entegrasyonu

### 1. Environment Variables

`.env` dosyanızda API URL'ini tanımlayın:

```env
# Development
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080/ws

# Production
REACT_APP_API_URL=https://api.estimoo.co
REACT_APP_WS_URL=wss://api.estimoo.co/ws
```

### 2. API Service

```javascript
// services/api.js
const API_BASE_URL = process.env.REACT_APP_API_URL;

export const apiService = {
  // Oda oluştur
  createRoom: async (roomName) => {
    const response = await fetch(`${API_BASE_URL}/api/rooms`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ roomName }),
    });
    return response.json();
  },

  // Oda bilgilerini getir
  getRoom: async (roomCode) => {
    const response = await fetch(`${API_BASE_URL}/api/rooms/${roomCode}`);
    if (!response.ok) {
      throw new Error('Room not found');
    }
    return response.json();
  },

  // Oda adını güncelle
  updateRoomName: async (roomCode, roomName) => {
    const response = await fetch(`${API_BASE_URL}/api/rooms/${roomCode}/name`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ roomName }),
    });
    return response.json();
  },
};
```

### 3. WebSocket Service

```javascript
// services/websocket.js
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const WS_BASE_URL = process.env.REACT_APP_WS_URL;

export class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = new Map();
  }

  connect(sessionId) {
    return new Promise((resolve, reject) => {
      const socket = new SockJS(`${WS_BASE_URL}`);
      this.stompClient = Stomp.over(socket);

      // Session ID'yi header'a ekle
      this.stompClient.connect(
        {},
        (frame) => {
          console.log('WebSocket connected:', frame);
          this.connected = true;
          resolve();
        },
        (error) => {
          console.error('WebSocket connection error:', error);
          this.connected = false;
          reject(error);
        }
      );

      // Session ID'yi header'a ekle
      this.stompClient.beforeConnect = () => {
        this.stompClient.connectHeaders = {
          'sessionId': sessionId
        };
      };
    });
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect();
      this.connected = false;
    }
  }

  // Odaya katıl
  joinRoom(roomCode, nickname) {
    if (!this.connected) {
      throw new Error('WebSocket not connected');
    }

    this.stompClient.send('/app/join', {}, JSON.stringify({
      roomCode,
      nickname
    }));
  }

  // Oy ver
  vote(roomCode, vote) {
    if (!this.connected) {
      throw new Error('WebSocket not connected');
    }

    this.stompClient.send('/app/vote', {}, JSON.stringify({
      roomCode,
      vote
    }));
  }

  // Oyları aç
  revealVotes(roomCode) {
    if (!this.connected) {
      throw new Error('WebSocket not connected');
    }

    this.stompClient.send('/app/reveal', {}, JSON.stringify({
      roomCode
    }));
  }

  // Oyları sıfırla
  resetVotes(roomCode) {
    if (!this.connected) {
      throw new Error('WebSocket not connected');
    }

    this.stompClient.send('/app/reset', {}, JSON.stringify({
      roomCode
    }));
  }

  // Oda durumunu dinle
  subscribeToRoom(roomCode, callback) {
    if (!this.connected) {
      throw new Error('WebSocket not connected');
    }

    const subscription = this.stompClient.subscribe(
      `/topic/room/${roomCode}`,
      (message) => {
        const data = JSON.parse(message.body);
        callback(data);
      }
    );

    this.subscriptions.set(roomCode, subscription);
  }

  // Aboneliği iptal et
  unsubscribeFromRoom(roomCode) {
    const subscription = this.subscriptions.get(roomCode);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(roomCode);
    }
  }
}

export const wsService = new WebSocketService();
```

### 4. React Hook

```javascript
// hooks/useRoom.js
import { useState, useEffect, useCallback } from 'react';
import { apiService } from '../services/api';
import { wsService } from '../services/websocket';

export const useRoom = (roomCode) => {
  const [room, setRoom] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!roomCode) return;

    const loadRoom = async () => {
      try {
        setLoading(true);
        const roomData = await apiService.getRoom(roomCode);
        setRoom(roomData);
        setError(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadRoom();
  }, [roomCode]);

  useEffect(() => {
    if (!roomCode) return;

    // WebSocket bağlantısı
    const sessionId = Math.random().toString(36).substring(7);
    wsService.connect(sessionId);

    // Oda durumunu dinle
    wsService.subscribeToRoom(roomCode, (data) => {
      setRoom(prev => ({ ...prev, ...data }));
    });

    return () => {
      wsService.unsubscribeFromRoom(roomCode);
      wsService.disconnect();
    };
  }, [roomCode]);

  const joinRoom = useCallback((nickname) => {
    wsService.joinRoom(roomCode, nickname);
  }, [roomCode]);

  const vote = useCallback((vote) => {
    wsService.vote(roomCode, vote);
  }, [roomCode]);

  const revealVotes = useCallback(() => {
    wsService.revealVotes(roomCode);
  }, [roomCode]);

  const resetVotes = useCallback(() => {
    wsService.resetVotes(roomCode);
  }, [roomCode]);

  return {
    room,
    loading,
    error,
    joinRoom,
    vote,
    revealVotes,
    resetVotes
  };
};
```

### 5. React Component Örneği

```jsx
// components/Room.jsx
import React, { useState } from 'react';
import { useRoom } from '../hooks/useRoom';

export const Room = ({ roomCode }) => {
  const [nickname, setNickname] = useState('');
  const [joined, setJoined] = useState(false);
  const { room, loading, error, joinRoom, vote, revealVotes, resetVotes } = useRoom(roomCode);

  const handleJoin = () => {
    if (nickname.trim()) {
      joinRoom(nickname);
      setJoined(true);
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!room) return <div>Room not found</div>;

  return (
    <div>
      <h1>Room: {room.roomName}</h1>
      <p>Code: {room.roomCode}</p>

      {!joined ? (
        <div>
          <input
            type="text"
            placeholder="Enter your nickname"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
          />
          <button onClick={handleJoin}>Join Room</button>
        </div>
      ) : (
        <div>
          <h2>Users:</h2>
          <ul>
            {room.users?.map((user, index) => (
              <li key={index}>
                {user.nickname} - {user.vote || 'No vote'}
              </li>
            ))}
          </ul>

          <div>
            <button onClick={() => vote('1')}>1</button>
            <button onClick={() => vote('2')}>2</button>
            <button onClick={() => vote('3')}>3</button>
            <button onClick={() => vote('5')}>5</button>
            <button onClick={() => vote('8')}>8</button>
            <button onClick={() => vote('13')}>13</button>
          </div>

          <button onClick={revealVotes}>Reveal Votes</button>
          <button onClick={resetVotes}>Reset</button>
        </div>
      )}
    </div>
  );
};
```

### 6. Dependencies

```json
{
  "dependencies": {
    "sockjs-client": "^1.6.1",
    "@stomp/stompjs": "^7.0.0"
  }
}
```

### 7. CORS Ayarları

Frontend'inizin domain'ini backend'deki CORS ayarlarına eklemeyi unutmayın:

```properties
# application.properties
spring.web.cors.allowed-origins=https://estimoo.co,https://www.estimoo.co,http://localhost:3000
```

### 8. Error Handling

```javascript
// utils/errorHandler.js
export const handleApiError = (error) => {
  if (error.name === 'TypeError' && error.message.includes('fetch')) {
    return 'Network error. Please check your connection.';
  }
  
  if (error.status === 404) {
    return 'Room not found.';
  }
  
  if (error.status === 500) {
    return 'Server error. Please try again later.';
  }
  
  return error.message || 'An unexpected error occurred.';
};
```

Bu rehber ile React frontend'iniz Estimoo backend API'sini sorunsuz kullanabilir! 