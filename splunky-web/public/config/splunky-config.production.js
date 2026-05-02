window.__SPLUNKY_CONFIG__ = {
  mockMode: 'off',
  // For split FE/BE deployment on PCF, set backend URL explicitly, e.g.
  // apiBaseUrl: 'https://splunky-service.example.com/api',
  // Leave empty to call same-origin '/api/*'.
  apiBaseUrl: '/api',
  routerBasename: '/',
}
// Keep this file non-secret. It is served to the browser.
