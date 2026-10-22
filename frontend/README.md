# AssetPulse - Frontend

Vanilla HTML/CSS/JS single-page app for the AssetPulse backend. No build step.

## Run
1. Start the backend (http://localhost:8080).
2. Serve this folder (CORS already allows port 5500):
   ```bash
   python3 -m http.server 5500
   ```
3. Open http://localhost:5500 and sign in (demo accounts are shown on the login screen).

To use a different backend URL, run in the browser console: `localStorage.ap_api='https://your-host/api'` and reload.

## Screens
Login/Register, Dashboard, Assets (CRUD, QR, history), QR Scan (camera + manual), My Assets & Requests,
Approvals (manager -> admin), Alerts, Insights (idle assets + one-click reassignment), Users.
Menus are role-based (ADMIN / MANAGER / EMPLOYEE).
