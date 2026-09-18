import express, { type Express } from 'express';
import os from 'os';

export function createApp(): Express {
  const app = express();

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  // Returns the server's public IP by calling a free external API.
  // os.networkInterfaces() only gives you private/internal IPs,
  // which isn't what the spec asks for.
  app.get('/api/server-ip', async (_req, res) => {
    try {
      const response = await fetch('https://api.ipify.org?format=json');
      const data = await response.json() as { ip: string };
      res.json({ ip: data.ip });
    } catch {
      res.status(500).json({ error: 'Could not determine server IP' });
    }
  });

  // Returns server local time as "hh:mm:ss GMT+hh:mm"
  app.get('/api/server-time', (_req, res) => {
    const now = new Date();

    // Pad a number to 2 digits: 5 becomes "05"
    const pad = (n: number) => n.toString().padStart(2, '0');

    const time = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;

    // getTimezoneOffset() returns minutes WEST of UTC (so UTC-8 returns 480).
    // We flip the sign to get the conventional GMT+/- format.
    const offsetMin = now.getTimezoneOffset();
    const sign = offsetMin <= 0 ? '+' : '-';
    const absOffset = Math.abs(offsetMin);
    const offsetHours = pad(Math.floor(absOffset / 60));
    const offsetMins = pad(absOffset % 60);

    res.json({ time: `${time} GMT${sign}${offsetHours}:${offsetMins}` });
  });

  // Returns your name. Replace with your actual name.
  app.get('/api/my-name', (_req, res) => {
    res.json({ firstName: 'Glen', lastName: 'Zhu' });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}