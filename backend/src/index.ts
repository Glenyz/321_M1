import { WebSocketServer, WebSocket } from 'ws';
import { createApp } from './app';
import { env } from './config/env';

const app = createApp();

const server = app.listen(env.port, () => {
  console.log(`Server listening on port ${env.port}`);
});

const wss = new WebSocketServer({server});

const courseServerUrl = 'wss://8.229.22.124';
let courseSocket: WebSocket | null = null;

function connectToCourseServer() {
  courseSocket = new WebSocket(courseServerUrl, {
    rejectUnauthorized: false,
  });


  courseSocket.on('open', () => {
    console.log('Connected to course pixel art server');
  });

  courseSocket.on('message', (data) => {
    const message = data.toString();
    wss.clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    });
  });

  courseSocket.on('close', () => {
    console.log('Course server disconnected, reconnecting in 3s...');
    setTimeout(connectToCourseServer, 3000);
  });

  courseSocket.on('error', (err) => {
    console.error('Course server error:', err.message);
  });
}

connectToCourseServer();

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}
