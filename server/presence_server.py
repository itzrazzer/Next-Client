#!/usr/bin/env python3
"""Server presence NextFam (opsional, self-host). Tanpa dependency.
Jalankan:  python3 presence_server.py 8080
Lalu di config/nextfam/presence.json:  {"mode":"custom","url":"https://domain-kamu.example"}
Endpoint:  POST /hb   body {"u":"<uuid>"}    -> tandai online (TTL 15 menit)
           GET  /list                          -> {"users":["uuid", ...]}
Pasang di belakang HTTPS (nginx / Caddy / Cloudflare Tunnel)."""
import json, sys, time, uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

TTL = 15 * 60
SEEN = {}

def clean():
    now = time.time()
    for k in [k for k, t in SEEN.items() if now - t > TTL]:
        del SEEN[k]

class H(BaseHTTPRequestHandler):
    def _send(self, code, obj):
        b = json.dumps(obj).encode()
        self.send_response(code); self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(b))); self.end_headers(); self.wfile.write(b)

    def do_POST(self):
        if self.path != "/hb": return self._send(404, {})
        try:
            n = min(int(self.headers.get("Content-Length", 0)), 512)
            u = str(uuid.UUID(json.loads(self.rfile.read(n))["u"]))
        except Exception:
            return self._send(400, {"error": "bad request"})
        clean()
        if len(SEEN) < 50000: SEEN[u] = time.time()
        self._send(200, {"ok": True})

    def do_GET(self):
        if self.path != "/list": return self._send(404, {})
        clean(); self._send(200, {"users": list(SEEN.keys())[:5000]})

    def log_message(self, *a): pass

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print("NextFam presence server on port", port)
    ThreadingHTTPServer(("0.0.0.0", port), H).serve_forever()
