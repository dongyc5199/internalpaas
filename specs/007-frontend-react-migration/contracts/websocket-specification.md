# WebSocket API Specification

**Protocol**: STOMP over WebSocket
**Endpoint**: `ws://localhost:8080/ws`
**Authentication**: Session cookie (JSESSIONID)

## Connection Flow

```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: {
    // Session cookie sent automatically by browser
  },
  onConnect: (frame) => {
    console.log('Connected:', frame);
    // Subscribe to topics
  },
  onDisconnect: () => {
    console.log('Disconnected');
  },
  onStompError: (frame) => {
    console.error('STOMP error:', frame);
  },
  reconnectDelay: 5000, // 5 seconds
});

client.activate();
```

## Topics

### 1. Server Status Updates

**Topic**: `/topic/server-status/{serverId}`
**Description**: Real-time server status and metrics updates
**Subscription**:
```javascript
client.subscribe('/topic/server-status/123', (message) => {
  const update = JSON.parse(message.body);
  console.log('Server update:', update);
});
```

**Message Format**:
```json
{
  "serverId": 123,
  "status": "ONLINE",
  "metrics": {
    "cpuUsage": 45.2,
    "memoryUsage": 67.8,
    "diskUsage": 34.5,
    "networkIn": 1024,
    "networkOut": 2048,
    "timestamp": "2025-01-04T10:30:00Z"
  },
  "timestamp": "2025-01-04T10:30:00Z"
}
```

**Update Frequency**: Every 5 seconds

---

### 2. Application Status Updates

**Topic**: `/topic/application-status/{appId}`
**Description**: Application lifecycle events and status changes
**Subscription**:
```javascript
client.subscribe('/topic/application-status/456', (message) => {
  const event = JSON.parse(message.body);
  console.log('Application event:', event);
});
```

**Message Format**:
```json
{
  "applicationId": 456,
  "status": "RUNNING",
  "previousStatus": "STARTING",
  "port": 8080,
  "debugPort": 5005,
  "pid": 12345,
  "timestamp": "2025-01-04T10:30:00Z",
  "message": "Application started successfully"
}
```

**Events**:
- Status changes: `STOPPED → STARTING → RUNNING → STOPPING → STOPPED`
- Errors: `ERROR` with error message
- Crashes: Status `ERROR` with crash details

---

### 3. SSH Terminal (Bidirectional)

**Send Destination**: `/app/ssh/{sessionId}/input`
**Subscribe Topic**: `/topic/ssh/{sessionId}/output`

**Send Input**:
```javascript
// Send command
client.publish({
  destination: `/app/ssh/${sessionId}/input`,
  body: JSON.stringify({
    type: 'input',
    data: 'ls -la\n',
    timestamp: new Date().toISOString()
  })
});

// Send resize
client.publish({
  destination: `/app/ssh/${sessionId}/input`,
  body: JSON.stringify({
    type: 'resize',
    data: { rows: 24, cols: 80 },
    timestamp: new Date().toISOString()
  })
});
```

**Receive Output**:
```javascript
client.subscribe(`/topic/ssh/${sessionId}/output`, (message) => {
  const output = JSON.parse(message.body);
  if (output.type === 'output') {
    terminal.write(output.data);
  } else if (output.type === 'error') {
    console.error('SSH error:', output.data);
  }
});
```

**Message Format**:
```json
{
  "sessionId": "uuid-here",
  "type": "output",
  "data": "command output text",
  "timestamp": "2025-01-04T10:30:00Z"
}
```

**Types**:
- `output`: Terminal output text
- `error`: Error message
- `disconnect`: Session terminated

---

### 4. Application Logs Streaming

**Topic**: `/topic/application-logs/{appId}`
**Description**: Real-time application log tailing
**Subscription**:
```javascript
client.subscribe('/topic/application-logs/456', (message) => {
  const log = JSON.parse(message.body);
  console.log(`[${log.level}] ${log.message}`);
});
```

**Message Format**:
```json
{
  "applicationId": 456,
  "timestamp": "2025-01-04T10:30:15.123Z",
  "level": "INFO",
  "logger": "com.example.Application",
  "message": "Server started on port 8080",
  "thread": "main"
}
```

**Log Levels**: `DEBUG`, `INFO`, `WARN`, `ERROR`

---

### 5. Global Server List Updates

**Topic**: `/topic/servers`
**Description**: Broadcast updates to all server statuses
**Subscription**:
```javascript
client.subscribe('/topic/servers', (message) => {
  const updates = JSON.parse(message.body);
  updates.forEach(update => {
    updateServerInCache(update.serverId, update.status);
  });
});
```

**Message Format**:
```json
{
  "updates": [
    {
      "serverId": 123,
      "status": "ONLINE",
      "timestamp": "2025-01-04T10:30:00Z"
    },
    {
      "serverId": 124,
      "status": "OFFLINE",
      "timestamp": "2025-01-04T10:30:00Z"
    }
  ]
}
```

**Update Frequency**: Whenever status changes (event-driven)

---

### 6. User Notifications

**Topic**: `/user/queue/notifications`
**Description**: Personal notifications for current user
**Subscription**:
```javascript
client.subscribe('/user/queue/notifications', (message) => {
  const notification = JSON.parse(message.body);
  displayNotification(notification);
});
```

**Message Format**:
```json
{
  "id": "uuid",
  "type": "info",
  "title": "Application Started",
  "message": "Application 'my-app' started successfully on server 'prod-1'",
  "action": {
    "label": "View Details",
    "route": "/applications/456"
  },
  "timestamp": "2025-01-04T10:30:00Z"
}
```

**Types**: `success`, `error`, `warning`, `info`

---

## Error Handling

### Connection Errors

```javascript
client.onStompError = (frame) => {
  console.error('STOMP error:', frame.headers.message);
  // Handle errors:
  // - Authentication failed
  // - Session expired
  // - Invalid subscription
};
```

### Reconnection Strategy

```javascript
const client = new Client({
  reconnectDelay: 5000, // Start with 5 seconds
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});

// Exponential backoff handled by library
```

### Subscription Errors

```javascript
const subscription = client.subscribe('/topic/test', (msg) => {
  // Handle message
}, {
  onError: (error) => {
    console.error('Subscription error:', error);
  }
});
```

---

## React Integration Pattern

### Custom Hook

```typescript
// hooks/useWebSocket.ts
import { useEffect, useRef, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';

export const useWebSocket = <T>(topic: string) => {
  const [message, setMessage] = useState<T | null>(null);
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
  const clientRef = useRef<Client>();

  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      onConnect: () => {
        setStatus('connected');
        client.subscribe(topic, (msg: IMessage) => {
          setMessage(JSON.parse(msg.body));
        });
      },
      onDisconnect: () => setStatus('disconnected'),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [topic]);

  return { message, status, client: clientRef.current };
};
```

### Component Usage

```typescript
// components/ServerMetrics.tsx
import { useWebSocket } from '../hooks/useWebSocket';

export const ServerMetrics: React.FC<{ serverId: number }> = ({ serverId }) => {
  const { message, status } = useWebSocket<ServerMetricsUpdate>(
    `/topic/server-status/${serverId}`
  );

  if (status !== 'connected') {
    return <ConnectionIndicator status={status} />;
  }

  return (
    <div>
      <h3>Real-time Metrics</h3>
      <p>CPU: {message?.metrics.cpuUsage}%</p>
      <p>Memory: {message?.metrics.memoryUsage}%</p>
    </div>
  );
};
```

---

## Testing with MSW

```typescript
// mocks/websocket.ts
import { setupWorker } from 'msw/browser';

// Mock WebSocket server for testing
export const mockWebSocketServer = () => {
  // In tests, use mock-socket library
  // Production code connects to real WebSocket
};
```

---

**Note**: All WebSocket communication requires valid session cookie. Unauthenticated connections will be rejected.
